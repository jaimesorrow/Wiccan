import { ContentTier } from "./types";
import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { SubscriptionRecordPort } from "./subscriptionRecordPort";
import { tierForSubscription } from "./tierPolicy";

export class PurchaseVerificationError extends Error {}

/**
 * Verifies a purchase token server-side and syncs `users/{uid}.tier`
 * accordingly. This — and `recheckLapsedSubscriptions`/the RTDN handler —
 * are the ONLY writers of that field; the client's own reported purchase
 * state is never trusted, matching how getTodayContent already treats
 * tier reads.
 *
 * Also persists the fuller `subscriptions/{uid}` audit record and
 * acknowledges the purchase with Google when it grants premium (a new,
 * unacknowledged purchase is auto-refunded after 3 days otherwise).
 */
export async function verifyPurchaseAndSyncTier(
  uid: string,
  purchaseToken: string,
  productId: string,
  billing: PlayBillingPort,
  users: UserRepositoryPort,
  subscriptions: SubscriptionRecordPort,
): Promise<ContentTier> {
  const info = await billing.getSubscriptionState(purchaseToken);

  if (!info.productIds.includes(productId)) {
    throw new PurchaseVerificationError(
      `Purchase token does not correspond to product '${productId}'.`,
    );
  }

  const tier = tierForSubscription(info);
  await users.setTier(uid, tier, purchaseToken);
  await subscriptions.save(uid, {
    productId,
    purchaseToken,
    subscriptionState: info.state,
    expiryTimeMillis: info.expiryTimeMillis,
    autoRenewing: info.autoRenewing,
  });

  if (tier === "premium") {
    await billing.acknowledgePurchaseIfNeeded(purchaseToken, productId);
  }

  return tier;
}
