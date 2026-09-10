import { ContentTier } from "./types";
import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { tierForSubscriptionState } from "./tierPolicy";

export class PurchaseVerificationError extends Error {}

/**
 * Verifies a purchase token server-side and syncs `users/{uid}.tier`
 * accordingly. This — and `recheckLapsedSubscriptions` — are the ONLY
 * writers of that field; the client's own reported purchase state is
 * never trusted, matching how getTodayContent already treats tier reads.
 */
export async function verifyPurchaseAndSyncTier(
  uid: string,
  purchaseToken: string,
  productId: string,
  billing: PlayBillingPort,
  users: UserRepositoryPort,
): Promise<ContentTier> {
  const info = await billing.getSubscriptionState(purchaseToken);

  if (!info.productIds.includes(productId)) {
    throw new PurchaseVerificationError(
      `Purchase token does not correspond to product '${productId}'.`,
    );
  }

  const tier = tierForSubscriptionState(info.state);
  await users.setTier(uid, tier, purchaseToken);
  return tier;
}
