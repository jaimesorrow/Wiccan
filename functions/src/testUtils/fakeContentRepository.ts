import { Content, ContentTier } from "../types";
import { ContentRepositoryPort } from "../contentRepositoryPort";

/** In-memory stand-in for Firestore, for testing selectTodayContent. */
export class FakeContentRepository implements ContentRepositoryPort {
  tier: ContentTier = "free";
  content: Content[] = [];
  history: { userId: string; contentId: string; deliveredDate: string }[] = [];
  poolExhaustionEvents: { userId: string; tier: ContentTier; today: string }[] = [];

  async getUserTier(): Promise<ContentTier> {
    return this.tier;
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

  async recordDelivery(userId: string, contentId: string, deliveredDate: string): Promise<void> {
    this.history.push({ userId, contentId, deliveredDate });
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
