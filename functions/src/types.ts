import { PRACTICAL_CATEGORIES } from "./practicalLesson";
import type { PracticalCategory, PracticalLessonFields } from "./practicalLesson";

export type ContentType = "spell" | "potion" | "lesson";
export type ContentTier = "free" | "premium";
export type ContentStatus = "draft" | "published";

export const CONTENT_CATEGORIES = [
  "Protection",
  "LoveAndRelationships",
  "ProsperityAndAbundance",
  "HealingAndWellness",
  "DivinationAndClarity",
  "BanishingAndRelease",
  "HomeAndHearth",
  "MoonAndSeasonal",
] as const;

export type ContentCategory = (typeof CONTENT_CATEGORIES)[number] | PracticalCategory;

export function isContentCategory(value: unknown): value is ContentCategory {
  return typeof value === "string" && ([...CONTENT_CATEGORIES, ...PRACTICAL_CATEGORIES] as readonly string[]).includes(value);
}

/** Mirrors the Firestore `content` collection. */
export interface Content extends PracticalLessonFields {
  id: string;
  type: ContentType;
  tier: ContentTier;
  tradition: string;
  category: ContentCategory;
  title: string;
  intro: string;
  body: string;
  /** One thoughtful question closing the lesson. See docs/CONTENT_VOICE_AND_LESSON_STANDARD.md. */
  reflection?: string | null;
  /** Optional safe modern action, explicitly not a reconstruction of the historical rite. */
  practice?: string | null;
  materials?: string[] | null;
  safetyNote?: string | null;
  tags: string[];
  status: ContentStatus;
  /** Provenance: the sources.json entry this piece was generated from. Only set on generated drafts. */
  sourceId?: string;
  /** Work and passage/reference retained with the delivered lesson. */
  sourceCitation?: string;
  sourceUrl?: string;
  rightsReviewRequired?: boolean;
  rightsNotes?: string | null;
  /**
   * Stable, editor-assigned identity for one lesson/concept. Rewordings and
   * near-duplicates share it, even across content types or tiers. Different
   * lessons from the same broad source must have different group IDs.
   */
  similarityGroupId?: string;
}

/** Mirrors the Firestore `userContentHistory` collection. */
export interface UserContentHistory {
  userId: string;
  contentId: string;
  /** ISO date string (yyyy-MM-dd), server-set. */
  deliveredDate: string;
  /** Denormalized from the delivered Content so Grimoire can list without extra reads. */
  title: string;
  type: ContentType;
  tradition: string;
  category: ContentCategory;
  /** Snapshot of the delivered lesson's editorial identity, when available. */
  similarityGroupId?: string;
  /** Fallback provenance retained even if the original content document is later removed. */
  sourceId?: string;
  sourceCitation?: string;
}

/**
 * One entry of the checked-in ancient-source seed pool
 * (`functions/ancientContentSeed.json`). A superset of the fields
 * `content` documents carry, plus the editorial provenance the seed file
 * is reviewed against. Validated by `contentSeed.ts`.
 */
export interface SeedContent extends Content {
  reflection: string;
  practice: string | null;
  safetyNote: string | null;
  materials: string[] | null;
  /** Compose icon tag; resolved client-side, never a drawable resource name. */
  iconTag: string;
  sourceId: string;
  /** Text, tablet/column/line reference the entry was written from. */
  sourceCitation: string;
  sourceUrl: string;
  /** Rights for a quoted phrase or museum prose are unconfirmed; do not publish until cleared. */
  rightsReviewRequired: boolean;
  status: "draft";
}

