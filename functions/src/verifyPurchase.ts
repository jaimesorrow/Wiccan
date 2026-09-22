import { ContentTier } from "./types";
import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { SubscriptionRecordPort } from "./subscriptionRecordPort";
import { tierForSubscription } from "./tierPolicy";

export class PurchaseVerificationError extends Error {}

export const PREMIUM_PRODUCT_IDS = new Set([
  "wiccan_premium_monthly",
  "wiccan_premium_yearly",
]);

/**
 * Verifies a purchase token server-side and syncs `users/{uid}.tier`
 * accordingly. Only explicitly allow-listed Wiccan premium products may
 * grant entitlement; the client cannot submit an arbitrary active
 * subscription product and have it treated as premium.
 */
export async function verifyPurchaseAndSyncTier(
  uid: string,
  purchaseToken: string,
  productId: string,
  billing: PlayBillingPort,
  users: UserRepositoryPort,
  subscriptions: SubscriptionRecordPort,
): Promise<ContentTier> {
  if (!PREMIUM_PRODUCT_IDS.has(productId)) {
    throw new PurchaseVerificationError(
      `Product '${productId}' is not a recognized Wiccan premium product.`,
    );
  }

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
