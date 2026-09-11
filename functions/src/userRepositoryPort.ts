import { ContentTier } from "./types";

export interface PremiumUserRecord {
  uid: string;
  purchaseToken: string;
}

/**
 * Port between the billing logic and the `users` collection. `tier` is
 * only ever written here — verifyPurchase, the daily lapse re-check, and
 * the RTDN handler are the sole callers, per the "server verification is
 * the only source of truth" rule that already governs getTodayContent's
 * tier reads.
 */
export interface UserRepositoryPort {
  setTier(uid: string, tier: ContentTier, purchaseToken?: string): Promise<void>;

  /** Every user currently marked premium, with the token needed to re-verify them. */
  listPremiumUsers(): Promise<PremiumUserRecord[]>;

  /**
   * Finds the uid whose stored purchase token matches, for RTDN handling -
   * Play's notifications carry a purchase token, never a Firebase uid.
   * Null if no user currently has this token on record.
   */
  findUidByPurchaseToken(purchaseToken: string): Promise<string | null>;
}
