import { ContentTier } from "../types";
import { PremiumUserRecord, UserRepositoryPort } from "../userRepositoryPort";

export class FakeUserRepository implements UserRepositoryPort {
  users = new Map<string, { tier: ContentTier; purchaseToken?: string }>();

  async setTier(uid: string, tier: ContentTier, purchaseToken?: string): Promise<void> {
    const existing = this.users.get(uid);
    this.users.set(uid, { tier, purchaseToken: purchaseToken ?? existing?.purchaseToken });
  }

  async listPremiumUsers(): Promise<PremiumUserRecord[]> {
    return [...this.users.entries()]
      .filter((entry): entry is [string, { tier: "premium"; purchaseToken: string }] => {
        const [, record] = entry;
        return record.tier === "premium" && typeof record.purchaseToken === "string";
      })
      .map(([uid, record]) => ({ uid, purchaseToken: record.purchaseToken }));
  }
}
