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

export type ContentCategory = (typeof CONTENT_CATEGORIES)[number];

export function isContentCategory(value: unknown): value is ContentCategory {
  return typeof value === "string" && (CONTENT_CATEGORIES as readonly string[]).includes(value);
}

/** Mirrors the Firestore `content` collection. */
export interface Content {
  id: string;
  type: ContentType;
  tier: ContentTier;
  tradition: string;
  category: ContentCategory;
  title: string;
  intro: string;
  body: string;
  materials?: string[] | null;
  safetyNote?: string | null;
  tags: string[];
  status: ContentStatus;
  /** Provenance: the sources.json entry this piece was generated from. Only set on generated drafts. */
  sourceId?: string;
}

/** Mirrors the Firestore `userContentHistory` collection. */
export interface UserContentHistory {
  userId: string;
  contentId: string;
  /** ISO date string (yyyy-MM-dd), server-set. */
  deliveredDate: string;
  /** Denormalized from the delivered Content so Archive can list without extra reads. */
  title: string;
  type: ContentType;
  tradition: string;
  category: ContentCategory;
}
