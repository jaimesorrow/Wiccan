import { Content, ContentCategory, ContentTier } from "../types";
import { ContentRepositoryPort } from "../contentRepositoryPort";

/** In-memory stand-in for Firestore, for testing selectTodayContent. */
export class FakeContentRepository implements ContentRepositoryPort {
  tier: ContentTier = "free";
  timeZone: string | null = null;
  content: Content[] = [];
  history: {
    userId: string;
    contentId: string;
    deliveredDate: string;
    title: string;
    type: string;
    tradition: string;
    category: ContentCategory;
  }[] = [];
  poolExhaustionEvents: { userId: string; tier: ContentTier; today: string }[] = [];

  async getUserTier(): Promise<ContentTier> {
    return this.tier;
  }

  async getUserTimeZone(): Promise<string | null> {
    return this.timeZone;
  }

  async findTodayDeliveredContentId(uid: string, today: string): Promise<string | null> {
    const match = this.history.find((h) => h.userId === uid && h.deliveredDate === today);
    return match?.contentId ?? null;
  }

  async getDeliveredContentIds(uid: string): Promise<string[]> {
    return this.history.filter((h) => h.userId === uid).map((h) => h.contentId);
  }

  async getContentById(id: string): Promise<Content | null> {
    return this.content.find((c) => c.id === id) ?? null;
  }

  async getPublishedContentForTier(tier: ContentTier): Promise<Content[]> {
    const eligible = tier === "premium" ? ["free", "premium"] : ["free"];
    return this.content.filter((c) => c.status === "published" && eligible.includes(c.tier));
  }

  /**
   * Mirrors FirestoreContentRepository's transactional behavior: check
   * and write happen with no `await` in between, so this method's whole
   * body runs to completion in one synchronous pass. That models a real
   * Firestore transaction closely enough for concurrency tests driven
   * via `Promise.all` - two calls racing for the same (uid, today) can
   * never interleave inside this function, only around it.
   */
  async assignDailyContent(userId: string, deliveredDate: string, candidate: Content): Promise<string> {
    const existing = this.history.find((h) => h.userId === userId && h.deliveredDate === deliveredDate);
    if (existing) {
      return existing.contentId;
    }
    this.history.push({
      userId,
      contentId: candidate.id,
      deliveredDate,
      title: candidate.title,
      type: candidate.type,
      tradition: candidate.tradition,
      category: candidate.category,
    });
    return candidate.id;
  }

  async logPoolExhaustion(userId: string, tier: ContentTier, today: string): Promise<void> {
    this.poolExhaustionEvents.push({ userId, tier, today });
  }
}

export function makeContent(overrides: Partial<Content> & { id: string }): Content {
  return {
    type: "spell",
    tier: "free",
    tradition: "European folk magic",
    category: "MoonAndSeasonal",
    title: `Title ${overrides.id}`,
    intro: "intro",
    body: "body",
    materials: null,
    safetyNote: null,
    tags: [],
    status: "published",
    ...overrides,
  };
}
