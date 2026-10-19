import { createHash } from "node:crypto";
import { citationForPassage, findPassageViolations, nonblank } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";

export const PRACTICAL_CATEGORIES = [
  "candle-magic", "herbs-botanicals", "potions-infusions", "crystals-stones",
  "cord-knot-magic", "moon-phases-timing", "elemental-magic", "altars-sacred-space", "divination",
] as const;
export type PracticalCategory = (typeof PRACTICAL_CATEGORIES)[number];
export const LESSON_TYPES = ["candle", "botanical", "potion", "tea", "bath", "oil", "spray", "jar", "skin-contact", "stone", "cord", "lunar", "elemental", "altar", "divination", "tool-light"] as const;
export const DISCLOSURE_TYPES: ReadonlySet<string> = new Set(["potion", "tea", "bath", "oil", "spray", "jar", "skin-contact"]);
export interface PracticalReview {
  reviewedBy: string;
  reviewedAt: string;
  contentHash: string;
  sourceFidelityApproved: boolean;
  scopeApproved: boolean;
  safetyApproved: boolean;
  rightsApproved: boolean;
  duplicateReviewed: boolean;
}

/** Proposed additive Firestore fields; existing intro/body/materials/status remain compatible. */
export interface PracticalLessonFields {
  slug?: string;
  summary?: string;
  introduction?: string;
  lesson?: string;
  tools?: string[];
  primaryCategoryId?: string;
  secondaryCategoryIds?: string[];
  lessonType?: string;
  requiresPotionDisclosure?: boolean;
  durationMinutes?: number | null;
  incantation?: string | null;
  incantationOmissionReason?: string | null;
  timing?: string | null;
  application?: string;
  correspondences?: string[];
  citations?: Record<string, string>[];
  adaptationNotes?: string[];
  sourcePassageId?: string;
  practicalReview?: PracticalReview;
}

export function isPracticalCategory(value: unknown): value is PracticalCategory {
  return typeof value === "string" && (PRACTICAL_CATEGORIES as readonly string[]).includes(value);
}
export function requiresDisclosure(content: Record<string, unknown>): boolean {
  return content.type === "potion" || (typeof content.lessonType === "string" && DISCLOSURE_TYPES.has(content.lessonType));
}

/** Sorted serialization makes hashes stable across Firestore/map key order. */
export function stableJson(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(stableJson).join(",")}]`;
  if (value && typeof value === "object") return `{${Object.entries(value).filter(([, v]) => v !== undefined).sort(([a], [b]) => a.localeCompare(b)).map(([k, v]) => `${JSON.stringify(k)}:${stableJson(v)}`).join(",")}}`;
  return JSON.stringify(value) ?? "null";
}

export const AUTHORED_FIELDS = ["title", "slug", "type", "tier", "tradition", "summary", "intro", "introduction", "body", "lesson", "reflection", "practice", "materials", "tools", "safetyNote", "tags", "category", "primaryCategoryId", "secondaryCategoryIds", "lessonType", "requiresPotionDisclosure", "durationMinutes", "incantation", "incantationOmissionReason", "timing", "application", "correspondences", "citations", "adaptationNotes", "sourcePassageId", "sourceId", "sourceCitation", "sourceUrl", "similarityGroupId", "rightsReviewRequired", "rightsNotes"] as const;
export function practicalContentHash(content: object): string {
  const fields = content as Record<string, unknown>;
  return createHash("sha256").update(stableJson(Object.fromEntries(AUTHORED_FIELDS.map((key) => [key, fields[key]])))).digest("hex");
}

export function findPracticalLessonViolations(content: object, passages: SourcePassage[], requireEditorialReview = true): string[] {
  const fields = content as Record<string, unknown>;
  const issues: string[] = [];
  for (const key of ["title", "summary", "intro", "body", "reflection", "practice", "safetyNote", "application", "lessonType"] as const) {
    const alias = key === "intro" ? "introduction" : key === "body" ? "lesson" : key;
    if (!nonblank(fields[alias] ?? fields[key])) issues.push(`${key} is required`);
  }
  if (!nonblank(fields.reflection) || !fields.reflection.trim().endsWith("?")) issues.push("reflection must be a question");
  if (!isPracticalCategory(fields.primaryCategoryId)) issues.push("primaryCategoryId must be one of the nine practical categories");
  if (fields.category !== fields.primaryCategoryId) issues.push("category must mirror primaryCategoryId for compatible readers");
  if (!Array.isArray(fields.secondaryCategoryIds) || !fields.secondaryCategoryIds.every(isPracticalCategory) || new Set(fields.secondaryCategoryIds).size !== fields.secondaryCategoryIds.length || fields.secondaryCategoryIds.includes(fields.primaryCategoryId)) issues.push("secondaryCategoryIds must contain distinct practical categories other than the primary");
  if (!(LESSON_TYPES as readonly unknown[]).includes(fields.lessonType)) issues.push("lessonType is unsupported");
  if (typeof fields.requiresPotionDisclosure !== "boolean" || (requiresDisclosure(fields) && fields.requiresPotionDisclosure !== true)) issues.push("required shared potion disclosure is missing");
  for (const key of ["tools", "adaptationNotes", "correspondences", "tags"] as const) {
    if (!Array.isArray(fields[key]) || !(fields[key] as unknown[]).every(nonblank)) issues.push(`${key} must be an explicit list without blank entries`);
  }
  if (fields.durationMinutes != null && (typeof fields.durationMinutes !== "number" || !Number.isFinite(fields.durationMinutes) || fields.durationMinutes <= 0)) issues.push("durationMinutes must be positive when supplied");
  const method = fields.lesson ?? fields.body;
  const numbers = typeof method === "string" ? [...method.matchAll(/^\s*(\d+)[.)]\s+\S/gm)].map((match) => Number(match[1])) : [];
  if (numbers.length < 2 || numbers.some((number, index) => number !== index + 1)) issues.push("main lesson needs a sequential numbered method starting at 1");
  for (const [canonical, legacy] of [["introduction", "intro"], ["lesson", "body"], ["tools", "materials"]]) {
    if (fields[canonical] !== undefined && fields[legacy] !== undefined && stableJson(fields[canonical]) !== stableJson(fields[legacy])) issues.push(`${canonical}/${legacy} disagree`);
  }
  const passage = passages.find((candidate) => candidate.id === fields.sourcePassageId && candidate.sourceId === fields.sourceId);
  if (!passage) issues.push("verified source passage is missing from the editor-owned register");
  else {
    issues.push(...findPassageViolations(passage));
    if (fields.tradition !== passage.tradition) issues.push("source tradition must be preserved accurately");
    if (fields.sourceCitation !== passage.locator || fields.sourceUrl !== passage.url) issues.push("source locator/URL differ from the inspected passage");
    if (!Array.isArray(fields.citations) || !fields.citations.some((citation) => stableJson(citation) === stableJson(citationForPassage(passage)))) issues.push("structured citation must retain exact verified edition, dating and locator metadata");
    if (nonblank(fields.incantation) && !nonblank(passage.formula)) issues.push("source has no approved formula; do not invent an incantation");
    if (!nonblank(fields.incantation) && !nonblank(fields.incantationOmissionReason)) issues.push("explain the omission of an incantation");
  }
  if (requireEditorialReview) {
    const review = fields.practicalReview as PracticalReview | undefined;
    if (!review || !nonblank(review.reviewedBy) || !Number.isFinite(Date.parse(review.reviewedAt)) || !review.sourceFidelityApproved || !review.scopeApproved || !review.safetyApproved || !review.rightsApproved || !review.duplicateReviewed || review.contentHash !== practicalContentHash(content)) {
      issues.push("current content needs a dated, hash-bound fidelity, scope, rights, safety and duplicate review");
    }
  }
  return issues;
}
