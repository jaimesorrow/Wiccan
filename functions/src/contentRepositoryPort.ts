import { Content, ContentTier } from "./types";

/** A different day's transaction reserved this content before our assignment committed. */
export class ContentAlreadyDeliveredError extends Error {
  constructor(contentId: string) {
    super(`Content '${contentId}' was already delivered to this user.`);
    this.name = "ContentAlreadyDeliveredError";
  }
}

/** The pool snapshot became stale before the assignment transaction committed. */
export class ContentNoLongerEligibleError extends Error {
  constructor(contentId: string) {
    super(`Content '${contentId}' is no longer published or eligible for this user.`);
    this.name = "ContentNoLongerEligibleError";
  }
}

/**
 * Port between the daily-selection logic and Firestore. Kept as an
 * interface so `selectTodayContent` can be unit tested against an
 * in-memory fake instead of a live Firestore/emulator instance.
 */
export interface ContentRepositoryPort {
  /** Defaults to "free" for users with no subscription record. */
  getUserTier(uid: string): Promise<ContentTier>;

  /** IANA time zone identifier for this user, e.g. "America/Denver". Null if never recorded. */
  getUserTimeZone(uid: string): Promise<string | null>;

  /**
   * Fast, non-transactional read of the contentId already delivered to
   * this user today, if any. Used only to skip candidate selection on
   * repeat same-day calls - never relied on for correctness, since a
   * plain read-then-later-write here would reintroduce the race
   * `assignDailyContent` exists to close.
   */
  findTodayDeliveredContentId(uid: string, today: string): Promise<string | null>;

  /** Every contentId ever delivered to this user, across all days. */
  getDeliveredContentIds(uid: string): Promise<string[]>;

  getContentById(id: string): Promise<Content | null>;

  /** Published content whose tier is within what `tier` is entitled to. */
  getPublishedContentForTier(tier: ContentTier): Promise<Content[]>;

  /**
   * Atomically commits `candidate` as this user's assignment for `today`
   * unless one already exists, in which case the existing contentId is
   * returned unchanged and nothing is written. Implementations must
   * guarantee that concurrent callers racing for the same (uid, today)
   * agree on exactly one winner and produce exactly one history record.
   * The transaction also reserves (uid, contentId) and any editorial
   * similarity group across dates, including legacy history lacking
   * reservations. It must recheck current published status and user tier.
   */
  assignDailyContent(uid: string, today: string, candidate: Content, ancientLessonSourceIds: ReadonlySet<string>): Promise<string>;

  /** Fired when no new eligible content remains. */
  logPoolExhaustion(uid: string, tier: ContentTier, today: string): Promise<void>;
}
