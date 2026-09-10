export type ContentType = "spell" | "potion" | "lesson";
export type ContentTier = "free" | "premium";
export type ContentStatus = "draft" | "published";

/** Mirrors the Firestore `content` collection. */
export interface Content {
  id: string;
  type: ContentType;
  tier: ContentTier;
  tradition: string;
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
}
