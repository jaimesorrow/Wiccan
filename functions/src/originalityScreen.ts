import { longestCommonTokenRunRatio, ngramJaccard, normalizeConceptText } from "./contentSimilarity";

export interface OriginalityThresholds {
  sourceNgramReview: number;
  sourceLongestRunReview: number;
  existingNgramReview: number;
  existingLongestRunReview: number;
}
export const DEFAULT_ORIGINALITY_THRESHOLDS: OriginalityThresholds = {
  sourceNgramReview: 0.2,
  sourceLongestRunReview: 0.3,
  existingNgramReview: 0.65,
  existingLongestRunReview: 0.5,
};

function authoredText(fields: Record<string, unknown>): string {
  const strings = ["title", "summary", "introduction", "intro", "lesson", "body", "incantation", "reflection", "practice", "safetyNote", "application"]
    .map((key) => fields[key]).filter((value): value is string => typeof value === "string");
  return normalizeConceptText(strings.join(" "));
}

export function screenOriginality(
  candidate: Record<string, unknown>,
  sourcePassageText: string,
  existing: Array<{ id: string; fields: Record<string, unknown> }>,
  thresholds: OriginalityThresholds = DEFAULT_ORIGINALITY_THRESHOLDS,
) {
  const candidateText = authoredText(candidate);
  const sourceNgram = ngramJaccard(candidateText, sourcePassageText);
  const sourceLongestRun = longestCommonTokenRunRatio(candidateText, sourcePassageText);
  const findings: Array<{ kind: string; comparedId: string; ngram: number; longestRun: number }> = [];
  if (sourceNgram >= thresholds.sourceNgramReview || sourceLongestRun >= thresholds.sourceLongestRunReview) {
    findings.push({ kind: "source-overlap-review", comparedId: "source-passage", ngram: sourceNgram, longestRun: sourceLongestRun });
  }
  for (const document of existing) {
    const existingText = authoredText(document.fields);
    const ngram = ngramJaccard(candidateText, existingText);
    const longestRun = longestCommonTokenRunRatio(candidateText, existingText);
    if (ngram >= thresholds.existingNgramReview || longestRun >= thresholds.existingLongestRunReview) {
      findings.push({ kind: "existing-content-overlap-review", comparedId: document.id, ngram, longestRun });
    }
  }
  return {
    executed: true,
    candidateHasText: candidateText.length > 0,
    automatedClear: candidateText.length > 0 && findings.length === 0,
    humanReviewStillRequired: true,
    findings,
  };
}
