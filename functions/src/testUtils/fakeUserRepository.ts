import { ContentTier } from "../types";
import { PremiumUserRecord, PurchaseTokenOwnershipError, TierSyncSource, UserRepositoryPort } from "../userRepositoryPort";

export class FakeUserRepository implements UserRepositoryPort {
  users = new Map<string, { tier: ContentTier; purchaseToken?: string; deletionInProgress?: boolean }>();
  tokenOwners = new Map<string, string>();
  supersededTokens = new Set<string>();

  constructor(initialUids: string[] = []) {
    initialUids.forEach((uid) => this.users.set(uid, { tier: "free" }));
  }

  async setTier(
    uid: string,
    tier: ContentTier,
    purchaseToken: string,
    source: TierSyncSource = "purchase",
    previousPurchaseToken?: string,
  ): Promise<ContentTier | null> {
    const existing = this.users.get(uid);
    if (!existing || existing.deletionInProgress) {
      if (source !== "purchase") return null;
      throw new PurchaseTokenOwnershipError("This account is unavailable or being deleted. Sign in again before purchasing.");
    }
    const expectedToken = source === "replacement" ? previousPurchaseToken : purchaseToken;
    if (source !== "purchase" && (!expectedToken || existing?.purchaseToken !== expectedToken)) return null;
    const owner = this.tokenOwners.get(purchaseToken) ??
      [...this.users.entries()].find(([, record]) => record.purchaseToken === purchaseToken)?.[0];
    if (owner && owner !== uid) throw new PurchaseTokenOwnershipError("This purchase is already linked to another account.");
    if (this.supersededTokens.has(purchaseToken)) {
      if (source !== "purchase") return null;
      throw new PurchaseTokenOwnershipError("This purchase has been replaced by a newer subscription.");
    }
    const linkedOwner = previousPurchaseToken ? this.tokenOwners.get(previousPurchaseToken) : undefined;
    if (linkedOwner && linkedOwner !== uid) throw new PurchaseTokenOwnershipError("The previous subscription belongs to another account.");
    this.tokenOwners.set(purchaseToken, uid);
    if (source !== "refresh" && tier === "free" && existing?.tier === "premium" &&
      existing.purchaseToken && existing.purchaseToken !== purchaseToken) return "premium";
    if (source !== "refresh" && previousPurchaseToken && previousPurchaseToken !== purchaseToken) {
      this.supersededTokens.add(previousPurchaseToken);
    }
    this.users.set(uid, { tier, purchaseToken });
    return tier;
  }

  async listPremiumUsers(): Promise<PremiumUserRecord[]> {
    return [...this.users.entries()]
      .filter((entry): entry is [string, { tier: "premium"; purchaseToken: string }] => {
        const [, record] = entry;
        return record.tier === "premium" && typeof record.purchaseToken === "string";
      })
      .map(([uid, record]) => ({ uid, purchaseToken: record.purchaseToken }));
  }

  async findUidByPurchaseToken(purchaseToken: string): Promise<string | null> {
    const owner = this.tokenOwners.get(purchaseToken);
    if (owner) return owner;
    for (const [uid, record] of this.users.entries()) {
      if (record.purchaseToken === purchaseToken) return uid;
    }
    return null;
  }
}
