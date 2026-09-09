import { Content, ContentTier } from "./types";

/**
 * Port between the daily-selection logic and Firestore. Kept as an
 * interface so `selectTodayContent` can be unit tested against an
 * in-memory fake instead of a live Firestore/emulator instance.
 */
export interface ContentRepositoryPort {
  /** Defaults to "free" for users with no subscription record. */
  getUserTier(uid: string): Promise<ContentTier>;

  /** The contentId already delivered to this user today, if any. */
  findTodayDeliveredContentId(uid: string, today: string): Promise<string | null>;

  /** Every contentId ever delivered to this user, across all days. */
  getDeliveredContentIds(uid: string): Promise<string[]>;

  getContentById(id: string): Promise<Content | null>;

  /** Published content whose tier is within what `tier` is entitled to. */
  getPublishedContentForTier(tier: ContentTier): Promise<Content[]>;

  recordDelivery(uid: string, contentId: string, today: string): Promise<void>;

  /** Fired when a user has exhausted the published pool and repeats begin. */
  logPoolExhaustion(uid: string, tier: ContentTier, today: string): Promise<void>;
}
