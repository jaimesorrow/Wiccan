import { createHash } from "node:crypto";
import { citationForPassage, findPassageViolations, nonblank } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";

export const PRACTICAL_CATEGORIES = [
  "candle-magic", "herbs-botanicals", "potions-infusions", "crystals-stones",
  "cord-knot-magic", "moon-phases-timing", "elemental-magic", "altars-sacred-space", "divination",
  "protection-cleansing-warding",
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
  sourceTradition?: string;
  sourcePassageId?: string;
  practicalReview?: PracticalReview;
  /** Additive alias used by the revised schema; legacy `status` remains authoritative during migration. */
  contentStatus?: "draft" | "published";
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

export const AUTHORED_FIELDS = ["title", "slug", "type", "tier", "tradition", "sourceTradition", "summary", "intro", "introduction", "body", "lesson", "reflection", "practice", "materials", "tools", "safetyNote", "tags", "category", "primaryCategoryId", "secondaryCategoryIds", "lessonType", "requiresPotionDisclosure", "durationMinutes", "incantation", "incantationOmissionReason", "timing", "application", "correspondences", "citations", "adaptationNotes", "sourcePassageId", "sourceId", "sourceCitation", "sourceUrl", "similarityGroupId", "rightsReviewRequired", "rightsNotes"] as const;
export function practicalContentHash(content: object): string {
  const fields = content as Record<string, unknown>;
  // Firestore readers commonly materialize an absent optional field as null.
  // Treat null and missing alike so a reviewed hash survives storage round trips.
  return createHash("sha256").update(stableJson(Object.fromEntries(AUTHORED_FIELDS.map((key) => [key, fields[key] ?? undefined])))).digest("hex");
}

function lessonText(fields: Record<string, unknown>): string {
  return [
    "title", "summary", "intro", "introduction", "body", "lesson", "reflection", "practice",
    "safetyNote", "incantation", "timing", "application",
  ].map((key) => typeof fields[key] === "string" ? fields[key] : "")
    .concat(["materials", "tools", "correspondences", "adaptationNotes", "tags"].flatMap((key) =>
      Array.isArray(fields[key]) ? (fields[key] as unknown[]).filter(nonblank) : []))
    .join(" ")
    .toLowerCase();
}

/** Conservative automated screening. A pass never replaces the required human safety review. */
export function findPracticalSafetyViolations(content: object): string[] {
  const fields = content as Record<string, unknown>;
  const text = lessonText(fields);
  const method = String(fields.lesson ?? fields.body ?? "").toLowerCase();
  const safety = String(fields.safetyNote ?? "").toLowerCase();
  const issues: string[] = [];

  if (/\b(?:guarantees?|ensures?)\b|\bwill certainly\b|\b(?:always|definitely) (?:works?|causes?|brings?)\b/.test(text)) {
    issues.push("lesson promises a guaranteed outcome");
  }
  if (/\bmake (?:him|her|them|that person|[a-z]+) (?:love|obey|return|submit)\b|\bagainst (?:his|her|their) will\b|\bwithout (?:their )?consent\b/.test(text)) {
    issues.push("lesson contains coercive targeting rather than consent-based practice");
  }
  if (/\b(?:drink|swallow|ingest|consume)\b/.test(method)) {
    issues.push("generated practical lessons may not instruct ingestion; require separate qualified review");
  }
  if (/essential oils?[^.]{0,80}\b(?:drink|swallow|ingest|consume|oral)\b|\b(?:drink|swallow|ingest|consume)[^.]{0,80}essential oils?\b/.test(text)) {
    issues.push("essential oils must never be ingested");
  }
  if (/\b(?:cures?|treats?|prevents?)\s+(?:disease|illness|infection|cancer|depression|anxiety|diabetes|pain)\b|\breplace\w* (?:a |your )?(?:doctor|clinician|medical|mental health)\b/.test(text)) {
    issues.push("lesson contains an unsupported medical claim or replacement-of-care instruction");
  }
  if (/\b(?:safe|harmless) (?:during|for|with)[^.]{0,50}\b(?:pregnan|medication|medicine)\b|\b(?:pregnan\w*|medication|medicine)[^.]{0,50}\b(?:safe|harmless)\b/.test(text)) {
    issues.push("lesson makes an unsupported pregnancy or medication safety claim");
  }
  if (/\b(?:candle|open flame|burn|burning)\b/.test(text) &&
      !/\b(?:flameless|extinguish|unattended|fireproof|heat-safe|fire-safe)\b/.test(safety)) {
    issues.push("flame-related lesson needs explicit extinguishing, unattended-flame, or flameless guidance");
  }
  if (/\b(?:incense|smoke|smudg\w*)\b/.test(text) && !/\b(?:ventilat|smoke|asthma|respiratory|outdoors)\b/.test(safety)) {
    issues.push("smoke-related lesson needs ventilation and respiratory precautions");
  }
  if (/\b(?:skin|bath|ritual oil|essential oil|topical|anoint)\b/.test(text) && !/\b(?:patch test|allerg|skin|external use|irritat)\b/.test(safety)) {
    issues.push("skin-contact lesson needs allergy, patch-test, or irritation precautions");
  }
  if (/\b(?:cord|knot|bind|binding)\b/.test(text) && !/\b(?:never bind|do not bind|circulation|loose|children|pets)\b/.test(safety)) {
    issues.push("cord or binding lesson needs body, circulation, child, or pet precautions");
  }
  return issues;
}

export function findPracticalLessonViolations(content: object, passages: SourcePassage[], requireEditorialReview = true): string[] {
  const fields = content as Record<string, unknown>;
  const issues: string[] = [...findPracticalSafetyViolations(content)];
  for (const key of ["title", "slug", "summary", "introduction", "lesson", "reflection", "practice", "safetyNote", "application", "lessonType"] as const) {
    if (!nonblank(fields[key])) issues.push(`${key} is required`);
  }
  if (nonblank(fields.slug) && !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(fields.slug)) issues.push("slug must be lowercase kebab-case");
  if (!nonblank(fields.reflection) || !fields.reflection.trim().endsWith("?")) issues.push("reflection must be a question");
  if (fields.tradition !== "Wiccan / Modern Witchcraft") issues.push("tradition must identify the lesson as Wiccan / Modern Witchcraft");
  if (!isPracticalCategory(fields.primaryCategoryId)) issues.push("primaryCategoryId must be one of the ten practical categories");
  if (fields.category !== fields.primaryCategoryId) issues.push("category must mirror primaryCategoryId for compatible readers");
  if (fields.contentStatus !== fields.status) issues.push("contentStatus must mirror the preserved legacy status");
  const secondaryCategoryIds = fields.secondaryCategoryIds;
  if (!Array.isArray(secondaryCategoryIds) || !secondaryCategoryIds.every(isPracticalCategory) || new Set(secondaryCategoryIds).size !== secondaryCategoryIds.length || secondaryCategoryIds.some((categoryId) => categoryId === fields.primaryCategoryId)) issues.push("secondaryCategoryIds must contain distinct practical categories other than the primary");
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
    if (fields.sourceTradition !== passage.tradition) issues.push("sourceTradition must preserve the historical tradition accurately");
    if (fields.sourceCitation !== passage.locator || fields.sourceUrl !== passage.url) issues.push("source locator/URL differ from the inspected passage");
    if (!Array.isArray(fields.citations) || !fields.citations.some((citation) => stableJson(citation) === stableJson(citationForPassage(passage)))) issues.push("structured citation must retain exact verified edition, dating and locator metadata");
    if (nonblank(fields.incantation) && !nonblank(passage.formula) &&
        (!Array.isArray(fields.adaptationNotes) ||
          !(fields.adaptationNotes as unknown[]).some((note) => nonblank(note) && /original modern (?:incantation|wording)/i.test(note)))) {
      issues.push("a modern incantation absent from the source must be labeled as original modern wording");
    }
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
