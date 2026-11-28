import { ACCEPTED_PRACTICAL_CATEGORIES } from "./practicalLesson";
import type { AcceptedPracticalCategory, PracticalLessonFields } from "./practicalLesson";

/** The only `content.type` values the Android app can render (see ContentType.kt). */
export const CONTENT_TYPES = ["spell", "potion", "lesson"] as const;
export type ContentType = (typeof CONTENT_TYPES)[number];
export type ContentTier = "free" | "premium";
export type ContentStatus = "draft" | "published";

export const SABBAT_IDS = [
  "imbolc", "ostara", "beltane", "litha", "lughnasadh", "mabon", "samhain", "yule",
] as const;
export type SabbatId = (typeof SABBAT_IDS)[number];
export type MoonPhaseId = "new" | "waxing" | "full" | "waning";
export type SeasonalWindow =
  | { kind: "sabbat"; id: SabbatId }
  | { kind: "moon"; phase: MoonPhaseId };

/**
 * True only for a supported `content.type`. Values such as "ritual" belong in
 * the v2 `lessonType` field, not `type`, and are rejected here.
 */
export function isContentType(value: unknown): value is ContentType {
  return typeof value === "string" && (CONTENT_TYPES as readonly string[]).includes(value);
}

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

export type ContentCategory = (typeof CONTENT_CATEGORIES)[number] | AcceptedPracticalCategory;

export function isContentCategory(value: unknown): value is ContentCategory {
  return typeof value === "string" && ([...CONTENT_CATEGORIES, ...ACCEPTED_PRACTICAL_CATEGORIES] as readonly string[]).includes(value);
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
  /** HTTPS URL of this item's own original hero image. Required to approve or publish a draft. */
  illustrationUrl?: string;
  /**
   * Provenance flag: true only when illustrationUrl was created originally for this
   * item and is not copied or reused. Required to approve or publish a draft.
   * Existing published items may omit this until the image backfill runs.
   */
  illustrationOriginal?: boolean;
  /** Optional calendar window used only to prefer this item on a matching day. */
  seasonalWindow?: SeasonalWindow;
  /** Editorial first-week order. Values 1-7 are preferred for a subscriber's first seven deliveries. */
  starterRank?: number;
}

/**
 * Firestore `userContentHistory` documents are written only by
 * `FirestoreContentRepository`. Each document has `userId`, `contentId`,
 * `deliveredDate` (server-set `yyyy-MM-dd`), `title`, `type`, `tradition`,
 * and `category`. `similarityGroupId`, `sourceId`, and `sourceCitation` are
 * copied from the delivered content when that content has them. The Android
 * Grimoire maps the same documents to `HistoryEntry`; nothing in this package
 * reads them through a shared type.
 */

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
