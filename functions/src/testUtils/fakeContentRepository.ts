import { Content, ContentCategory, ContentTier } from "../types";
import { ContentAlreadyDeliveredError, ContentNoLongerEligibleError, ContentRepositoryPort } from "../contentRepositoryPort";
import { contentSimilarityKey, isEligibleForDailyDelivery, sharesContentSimilarity } from "../contentEligibility";

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
    similarityGroupId?: string;
    sourceId?: string;
    sourceCitation?: string;
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
  async assignDailyContent(userId: string, deliveredDate: string, candidate: Content, ancientLessonSourceIds: ReadonlySet<string>): Promise<string> {
    const existing = this.history.find((h) => h.userId === userId && h.deliveredDate === deliveredDate);
    if (existing) {
      return existing.contentId;
    }
    if (this.history.some((entry) => entry.userId === userId && entry.contentId === candidate.id)) {
      throw new ContentAlreadyDeliveredError(candidate.id);
    }
    const current = this.content.find((content) => content.id === candidate.id);
    if (!current || current.status !== "published" || (current.tier === "premium" && this.tier !== "premium") ||
        !isEligibleForDailyDelivery(current, ancientLessonSourceIds)) {
      throw new ContentNoLongerEligibleError(candidate.id);
    }
    const key = contentSimilarityKey(current);
    if (key && this.history.some((entry) => {
      if (entry.userId !== userId) return false;
      if (entry.similarityGroupId && contentSimilarityKey(entry) === key) return true;
      const historical = this.content.find((content) => content.id === entry.contentId) ?? entry;
      return sharesContentSimilarity(historical, current);
    })) {
      throw new ContentAlreadyDeliveredError(candidate.id);
    }
    this.history.push({
      userId,
      contentId: candidate.id,
      deliveredDate,
      title: current.title,
      type: current.type,
      tradition: current.tradition,
      category: current.category,
      ...(current.similarityGroupId ? { similarityGroupId: current.similarityGroupId } : {}),
      ...(current.sourceId ? { sourceId: current.sourceId } : {}),
      ...(current.sourceCitation ? { sourceCitation: current.sourceCitation } : {}),
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
