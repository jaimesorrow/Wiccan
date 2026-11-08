import { createHash } from "node:crypto";
import { ngramJaccard, normalizeConceptText } from "./contentSimilarity";
import { stableJson } from "./practicalLesson";

export interface GenerationInventoryRecord {
  id: string;
  fields: Record<string, unknown>;
}

export interface GenerationConceptSignature {
  primaryMethod: string;
  secondaryMethods: string[];
  intendedPurpose: string;
  primaryTool: string;
  supportingTools: string[];
  ritualActions: string[];
  setting: string;
  timing: string;
  symbolicStructure: string;
  incantationTheme: string;
  followUpAction: string;
}

export interface GenerationOriginalityThresholds {
  titleReject: number;
  summaryReject: number;
  instructionReject: number;
  conceptReject: number;
  ritualActionReject: number;
}

export const DEFAULT_GENERATION_ORIGINALITY_THRESHOLDS: GenerationOriginalityThresholds = {
  titleReject: 0.85,
  summaryReject: 0.82,
  instructionReject: 0.78,
  conceptReject: 0.8,
  ritualActionReject: 0.75,
};

export interface GenerationOriginalityComparison {
  comparedId: string;
  comparedTitle: string;
  titleSimilarity: number;
  summarySimilarity: number;
  instructionSimilarity: number;
  conceptSimilarity: number;
  ritualActionSimilarity: number;
  reasons: string[];
}

export interface GenerationOriginalityReport {
  accepted: boolean;
  normalizedTitle: string;
  normalizedSlug: string;
  contentHash: string;
  conceptHash: string;
  signature: GenerationConceptSignature;
  reservationKeys: string[];
  closestMatch: GenerationOriginalityComparison | null;
  rejectionReasons: string[];
}

const SYNONYMS: ReadonlyArray<[RegExp, string]> = [
  [/\b(?:string|thread|yarn)\b/g, "cord"],
  [/\b(?:taper|votive|tea light|tealight)\b/g, "candle"],
  [/\b(?:clearing|purification|purifying)\b/g, "cleansing"],
  [/\b(?:amulet|talisman)\b/g, "charm"],
  [/\b(?:brew|tea)\b/g, "infusion"],
  [/\b(?:centering|steadying)\b/g, "grounding"],
  [/\b(?:botanical|plant)\b/g, "herb"],
];

export function normalizeGenerationText(value: unknown): string {
  let normalized = normalizeConceptText(value);
  for (const [pattern, replacement] of SYNONYMS) normalized = normalized.replace(pattern, replacement);
  return normalized.replace(/\s+/g, " ").trim();
}

function stringList(value: unknown): string[] {
  if (typeof value === "string") return [normalizeGenerationText(value)].filter(Boolean);
  if (Array.isArray(value)) return value.flatMap(stringList);
  if (value && typeof value === "object") {
    return Object.values(value as Record<string, unknown>).flatMap(stringList).filter(Boolean);
  }
  return [];
}

function firstString(fields: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    if (typeof fields[key] === "string" && fields[key].trim()) return normalizeGenerationText(fields[key]);
  }
  return "";
}

function numberedActions(value: unknown): string[] {
  if (typeof value !== "string") return [];
  const matches = [...value.matchAll(/^\s*\d+[.)]\s+(.+)$/gm)]
    .map((match) => normalizeGenerationText(match[1]))
    .filter(Boolean);
  if (matches.length) return matches;
  return value.split(/[.!?]\s+/).map(normalizeGenerationText).filter((item) => item.split(" ").length >= 3);
}

function detectedSetting(text: string): string {
  const settings = ["threshold", "doorway", "altar", "bedroom", "kitchen", "garden", "outdoors", "bath", "home", "window"];
  return settings.filter((setting) => text.includes(setting)).join(" ");
}

export function generationConceptSignature(fields: Record<string, unknown>): GenerationConceptSignature {
  const tools = stringList(fields.tools ?? fields.materials);
  const secondaryMethods = stringList(fields.secondaryCategoryIds);
  const lesson = firstString(fields, ["lesson", "body"]);
  const incantation = firstString(fields, ["incantation"]);
  const correspondences = stringList(fields.correspondences);
  return {
    primaryMethod: firstString(fields, ["primaryCategoryId", "category", "lessonType", "type"]),
    secondaryMethods,
    intendedPurpose: firstString(fields, ["summary", "introduction", "intro"]),
    primaryTool: tools[0] ?? "",
    supportingTools: tools.slice(1).sort(),
    ritualActions: numberedActions(typeof fields.lesson === "string" ? fields.lesson : fields.body),
    setting: detectedSetting(`${lesson} ${firstString(fields, ["application", "practice"])}`),
    timing: firstString(fields, ["timing"]),
    symbolicStructure: correspondences.sort().join(" "),
    incantationTheme: incantation,
    followUpAction: firstString(fields, ["practice", "reflection"]),
  };
}

function sha256(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

function conceptHash(signature: GenerationConceptSignature): string {
  return sha256(stableJson(signature));
}

function authoredContentHash(fields: Record<string, unknown>): string {
  const authored = [
    "title", "slug", "summary", "introduction", "intro", "lesson", "body", "incantation",
    "reflection", "practice", "safetyNote", "primaryCategoryId", "secondaryCategoryIds",
    "lessonType", "tools", "materials", "correspondences", "tags",
  ].map((key) => [key, fields[key] ?? null]);
  return sha256(stableJson(Object.fromEntries(authored)));
}

function signatureText(signature: GenerationConceptSignature): string {
  return normalizeGenerationText([
    signature.primaryMethod,
    signature.secondaryMethods.join(" "),
    signature.intendedPurpose,
    signature.primaryTool,
    signature.supportingTools.join(" "),
    signature.ritualActions.join(" "),
    signature.setting,
    signature.timing,
    signature.symbolicStructure,
    signature.followUpAction,
  ].join(" "));
}

function similarity(left: string, right: string, width: number): number {
  if (!left || !right) return 0;
  if (left === right) return 1;
  return ngramJaccard(left, right, width);
}

function compareCandidate(
  candidate: Record<string, unknown>,
  candidateSignature: GenerationConceptSignature,
  candidateHash: string,
  existing: GenerationInventoryRecord,
  thresholds: GenerationOriginalityThresholds,
): GenerationOriginalityComparison {
  const existingSignature = generationConceptSignature(existing.fields);
  const candidateTitle = normalizeGenerationText(candidate.title);
  const existingTitle = normalizeGenerationText(existing.fields.title);
  const candidateSlug = normalizeGenerationText(candidate.slug);
  const existingSlug = normalizeGenerationText(existing.fields.slug);
  const candidateSummary = firstString(candidate, ["summary", "introduction", "intro"]);
  const existingSummary = firstString(existing.fields, ["summary", "introduction", "intro"]);
  const candidateInstructions = firstString(candidate, ["lesson", "body"]);
  const existingInstructions = firstString(existing.fields, ["lesson", "body"]);
  const candidateActions = candidateSignature.ritualActions.join(" ");
  const existingActions = existingSignature.ritualActions.join(" ");
  const scores = {
    titleSimilarity: similarity(candidateTitle, existingTitle, 1),
    summarySimilarity: similarity(candidateSummary, existingSummary, 2),
    instructionSimilarity: similarity(candidateInstructions, existingInstructions, 3),
    conceptSimilarity: similarity(signatureText(candidateSignature), signatureText(existingSignature), 2),
    ritualActionSimilarity: similarity(candidateActions, existingActions, 2),
  };
  const exactConcept = candidateHash === conceptHash(existingSignature);
  const sameSimilarityGroup = typeof candidate.similarityGroupId === "string" && candidate.similarityGroupId.trim() !== "" &&
    candidate.similarityGroupId === existing.fields.similarityGroupId;
  const strongSignals = [
    scores.summarySimilarity >= thresholds.summaryReject,
    scores.instructionSimilarity >= thresholds.instructionReject,
    scores.conceptSimilarity >= thresholds.conceptReject,
    scores.ritualActionSimilarity >= thresholds.ritualActionReject,
  ].filter(Boolean).length;
  const reasons = [
    ...(candidateTitle && candidateTitle === existingTitle ? ["normalized-title"] : []),
    ...(candidateSlug && candidateSlug === existingSlug ? ["normalized-slug"] : []),
    ...(authoredContentHash(candidate) === authoredContentHash(existing.fields) ? ["exact-authored-content"] : []),
    ...(exactConcept ? ["exact-concept-signature"] : []),
    ...(sameSimilarityGroup ? ["same-similarity-group"] : []),
    ...(scores.titleSimilarity >= thresholds.titleReject ? ["title-similarity"] : []),
    ...(scores.summarySimilarity >= thresholds.summaryReject ? ["summary-similarity"] : []),
    ...(scores.instructionSimilarity >= thresholds.instructionReject ? ["instruction-similarity"] : []),
    ...(scores.conceptSimilarity >= thresholds.conceptReject ? ["concept-similarity"] : []),
    ...(scores.ritualActionSimilarity >= thresholds.ritualActionReject ? ["ritual-action-similarity"] : []),
    ...(strongSignals >= 3 ? ["multiple-moderate-duplicate-signals"] : []),
  ];
  return {
    comparedId: existing.id,
    comparedTitle: typeof existing.fields.title === "string" ? existing.fields.title : "",
    ...scores,
    reasons: [...new Set(reasons)],
  };
}

function comparisonScore(comparison: GenerationOriginalityComparison): number {
  return Math.max(
    comparison.titleSimilarity,
    comparison.summarySimilarity,
    comparison.instructionSimilarity,
    comparison.conceptSimilarity,
    comparison.ritualActionSimilarity,
  );
}

export function screenGeneratedCandidate(
  candidate: Record<string, unknown>,
  inventory: GenerationInventoryRecord[],
  thresholds: GenerationOriginalityThresholds = DEFAULT_GENERATION_ORIGINALITY_THRESHOLDS,
): GenerationOriginalityReport {
  const signature = generationConceptSignature(candidate);
  const normalizedTitle = normalizeGenerationText(candidate.title);
  const normalizedSlug = normalizeGenerationText(candidate.slug);
  const contentHash = authoredContentHash(candidate);
  const signatureHash = conceptHash(signature);
  const comparisons = inventory.map((record) => compareCandidate(candidate, signature, signatureHash, record, thresholds));
  const closestMatch = comparisons.sort((left, right) => comparisonScore(right) - comparisonScore(left))[0] ?? null;
  const rejectionReasons = comparisons.flatMap((comparison) =>
    comparison.reasons.map((reason) => `${reason}:${comparison.comparedId}`));
  const reservationKeys = [
    `title:${sha256(normalizedTitle)}`,
    `slug:${sha256(normalizedSlug)}`,
    `concept:${signatureHash}`,
    `content:${contentHash}`,
  ];
  return {
    accepted: rejectionReasons.length === 0,
    normalizedTitle,
    normalizedSlug,
    contentHash,
    conceptHash: signatureHash,
    signature,
    reservationKeys,
    closestMatch,
    rejectionReasons: [...new Set(rejectionReasons)],
  };
}
