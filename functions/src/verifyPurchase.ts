import { ContentTier } from "./types";
import { PlayBillingPort } from "./playBillingPort";
import { PurchaseTokenOwnershipError, UserRepositoryPort } from "./userRepositoryPort";
import { SubscriptionRecordPort } from "./subscriptionRecordPort";
import { PREMIUM_PRODUCT_IDS, tierForSubscription } from "./tierPolicy";
import { obfuscatedAccountId } from "./billingIdentity";

export class PurchaseVerificationError extends Error {}

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

  if (info.obfuscatedExternalAccountId && info.obfuscatedExternalAccountId !== obfuscatedAccountId(uid)) {
    throw new PurchaseVerificationError("This purchase belongs to another account.");
  }
  if (info.linkedPurchaseToken) {
    const linkedOwner = await users.findUidByPurchaseToken(info.linkedPurchaseToken);
    if (linkedOwner && linkedOwner !== uid) {
      throw new PurchaseVerificationError("The previous subscription belongs to another account.");
    }
  }

  const tier = tierForSubscription(info);
  let syncedTier: ContentTier;
  try {
    // Ownership must be reserved before acknowledging a purchase on the caller's behalf.
    const result = await users.setTier(uid, tier, purchaseToken, "purchase", info.linkedPurchaseToken ?? undefined);
    if (result === null) throw new PurchaseVerificationError("The purchase could not be linked to this account.");
    syncedTier = result;
  } catch (error) {
    if (error instanceof PurchaseTokenOwnershipError) throw new PurchaseVerificationError(error.message);
    throw error;
  }
  if (syncedTier === tier) await subscriptions.save(uid, {
    productId,
    purchaseToken,
    subscriptionState: info.state,
    expiryTimeMillis: info.expiryTimeMillis,
    autoRenewing: info.autoRenewing,
  });

  if (tier === "premium" && !info.acknowledged) {
    await billing.acknowledgePurchaseIfNeeded(purchaseToken, productId);
  }

  return syncedTier;
}
