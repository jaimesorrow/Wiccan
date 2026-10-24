import { createHash } from "node:crypto";
import { stableJson } from "./practicalLesson";

export interface CurriculumConcept {
  id: string;
  title: string;
  sourcePassageId: string;
  technique: string;
  purpose: string;
  materials: string[];
  procedure: string;
  learningObjective: string;
}
export interface SimilarityThresholds {
  ngramReview: number;
  longestRunReview: number;
  semanticReview: number;
  semanticReject: number;
}

export const DEFAULT_SIMILARITY_THRESHOLDS: SimilarityThresholds = {
  ngramReview: 0.72,
  longestRunReview: 0.55,
  semanticReview: 0.82,
  semanticReject: 0.92,
};

export interface SemanticSimilarityProvider {
  readonly name: string;
  similarity(left: CurriculumConcept, right: CurriculumConcept): Promise<number>;
}

export function normalizeConceptText(value: unknown): string {
  return typeof value === "string"
    ? value.normalize("NFKD").toLowerCase().replace(/[^a-z0-9\s]/g, " ").replace(/\s+/g, " ").trim()
    : "";
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string" && item.trim().length > 0) : [];
}

export function curriculumConcept(id: string, fields: Record<string, unknown>): CurriculumConcept {
  return {
    id,
    title: normalizeConceptText(fields.title),
    sourcePassageId: normalizeConceptText(fields.sourcePassageId ?? fields.sourceCitation),
    technique: normalizeConceptText(fields.lessonType ?? fields.primaryCategoryId ?? fields.category),
    purpose: normalizeConceptText(fields.summary ?? fields.intro ?? fields.introduction),
    materials: stringList(fields.tools ?? fields.materials).map(normalizeConceptText).filter(Boolean).sort(),
    procedure: normalizeConceptText(fields.lesson ?? fields.body),
    learningObjective: normalizeConceptText(fields.practice ?? fields.reflection),
  };
}

/** Stable identity for the complete practical concept, independent of document or title. */
export function conceptFingerprint(concept: CurriculumConcept): string {
  const identity = {
    sourcePassageId: concept.sourcePassageId,
    technique: concept.technique,
    purpose: concept.purpose,
    materials: concept.materials,
    procedure: concept.procedure,
    learningObjective: concept.learningObjective,
  };
  return createHash("sha256").update(stableJson(identity)).digest("hex");
}

function tokenNgrams(text: string, width = 3): Set<string> {
  const tokens = normalizeConceptText(text).split(" ").filter(Boolean);
  if (tokens.length < width) return new Set(tokens.length ? [tokens.join(" ")] : []);
  return new Set(tokens.slice(0, tokens.length - width + 1).map((_, index) => tokens.slice(index, index + width).join(" ")));
}

export function ngramJaccard(left: string, right: string, width = 3): number {
  const a = tokenNgrams(left, width);
  const b = tokenNgrams(right, width);
  if (!a.size && !b.size) return 1;
  const intersection = [...a].filter((value) => b.has(value)).length;
  return intersection / (a.size + b.size - intersection);
}

export function longestCommonTokenRunRatio(left: string, right: string): number {
  const a = normalizeConceptText(left).split(" ").filter(Boolean);
  const b = normalizeConceptText(right).split(" ").filter(Boolean);
  if (!a.length || !b.length) return 0;
  let previous = new Array<number>(b.length + 1).fill(0);
  let longest = 0;
  for (const leftToken of a) {
    const current = new Array<number>(b.length + 1).fill(0);
    for (let index = 1; index <= b.length; index++) {
      if (leftToken === b[index - 1]) current[index] = previous[index - 1] + 1;
      longest = Math.max(longest, current[index]);
    }
    previous = current;
  }
  return longest / Math.min(a.length, b.length);
}

export function lexicalSimilarity(left: CurriculumConcept, right: CurriculumConcept) {
  const leftComposite = [left.technique, left.purpose, left.materials.join(" "), left.procedure, left.learningObjective].join(" ");
  const rightComposite = [right.technique, right.purpose, right.materials.join(" "), right.procedure, right.learningObjective].join(" ");
  return {
    ngram: ngramJaccard(leftComposite, rightComposite),
    longestRun: longestCommonTokenRunRatio(left.procedure, right.procedure),
    samePassageTechnique: !!left.sourcePassageId && left.sourcePassageId === right.sourcePassageId && left.technique === right.technique,
  };
}

export function findLexicalDuplicateConcerns(
  concepts: CurriculumConcept[],
  thresholds: SimilarityThresholds = DEFAULT_SIMILARITY_THRESHOLDS,
) {
  const concerns: Array<{ leftId: string; rightId: string; reasons: string[]; ngram: number; longestRun: number }> = [];
  const fingerprints = concepts.map(conceptFingerprint);
  for (let leftIndex = 0; leftIndex < concepts.length; leftIndex++) {
    for (let rightIndex = leftIndex + 1; rightIndex < concepts.length; rightIndex++) {
      const left = concepts[leftIndex];
      const right = concepts[rightIndex];
      const leftComposite = [left.technique, left.purpose, left.materials.join(" "), left.procedure, left.learningObjective].join(" ");
      const rightComposite = [right.technique, right.purpose, right.materials.join(" "), right.procedure, right.learningObjective].join(" ");
      const ngram = ngramJaccard(leftComposite, rightComposite);
      const samePassageTechnique = !!left.sourcePassageId && left.sourcePassageId === right.sourcePassageId && left.technique === right.technique;
      const exactFingerprint = fingerprints[leftIndex] === fingerprints[rightIndex];
      const sameTitle = !!left.title && left.title === right.title;
      // Dynamic-programming LCS is reserved for pairs with some lexical or provenance signal.
      // This keeps a full 296-record audit bounded while still inspecting plausible paraphrases.
      const longestRun = ngram >= 0.05 || samePassageTechnique || exactFingerprint || sameTitle
        ? longestCommonTokenRunRatio(left.procedure, right.procedure)
        : 0;
      const reasons = [
        ...(exactFingerprint ? ["exact-concept-fingerprint"] : []),
        ...(sameTitle ? ["normalized-title"] : []),
        ...(samePassageTechnique ? ["same-passage-and-technique"] : []),
        ...(ngram >= thresholds.ngramReview ? ["ngram-overlap"] : []),
        ...(longestRun >= thresholds.longestRunReview ? ["longest-common-procedure-run"] : []),
      ];
      if (reasons.length) concerns.push({ leftId: left.id, rightId: right.id, reasons, ngram, longestRun });
    }
  }
  return concerns;
}

export async function screenSemanticSimilarity(
  pairs: Array<[CurriculumConcept, CurriculumConcept]>,
  provider: SemanticSimilarityProvider,
  thresholds: SimilarityThresholds = DEFAULT_SIMILARITY_THRESHOLDS,
) {
  const results = [];
  for (const [left, right] of pairs) {
    const score = await provider.similarity(left, right);
    if (!Number.isFinite(score) || score < 0 || score > 1) throw new Error(`${provider.name} returned an invalid similarity score.`);
    results.push({
      leftId: left.id,
      rightId: right.id,
      score,
      disposition: score >= thresholds.semanticReject ? "reject" : score >= thresholds.semanticReview ? "manual-review" : "clear",
    });
  }
  return results;
}
