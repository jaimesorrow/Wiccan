import { ContentTier } from "./types";
import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { SubscriptionRecordPort } from "./subscriptionRecordPort";
import { PREMIUM_PRODUCT_IDS, tierForSubscription } from "./tierPolicy";
import { obfuscatedAccountId } from "./billingIdentity";

export interface RtdnResult {
  uid: string;
  tier: ContentTier;
}

/**
 * Core logic for a Google Play Real-Time Developer Notification. RTDN
 * payloads carry only a purchase token and subscription ID - never a
 * Firebase uid - so the token is used to look up which user it belongs
 * to. A token this server has never seen (e.g. a test notification, or a
 * purchase made before this user ever called verifyPurchase) resolves to
 * `null` and is a no-op rather than an error: there's nothing to update.
 *
 * The notification's own `notificationType` is intentionally not
 * branched on - whatever the type, re-querying the Play API for the
 * token's authoritative current state and applying the same entitlement
 * rule as everywhere else is simpler and strictly more correct than
 * trusting the notification's claim about what changed.
 */
export async function handleRtdnNotification(
  purchaseToken: string,
  billing: PlayBillingPort,
  users: UserRepositoryPort,
  subscriptions: SubscriptionRecordPort,
): Promise<RtdnResult | null> {
  let uid = await users.findUidByPurchaseToken(purchaseToken);
  const info = await billing.getSubscriptionState(purchaseToken);
  const isReplacement = !uid && Boolean(info.linkedPurchaseToken);
  let predecessorForSync = info.linkedPurchaseToken;
  for (let attempts = 0; !uid && predecessorForSync && attempts < 5; attempts++) {
    uid = await users.findUidByPurchaseToken(predecessorForSync);
    if (uid) break;
    const predecessor = await billing.getSubscriptionState(predecessorForSync);
    predecessorForSync = predecessor.linkedPurchaseToken;
  }
  if (!uid) return null;
  if (info.obfuscatedExternalAccountId && info.obfuscatedExternalAccountId !== obfuscatedAccountId(uid)) return null;
  if (isReplacement && (info.state === "pending" || info.state === "pending_purchase_canceled")) return null;
  const tier = tierForSubscription(info);

  const syncedTier = await users.setTier(
    uid, tier, purchaseToken, isReplacement ? "replacement" : "refresh", predecessorForSync ?? undefined,
  );
  if (syncedTier === null) return null;
  await subscriptions.save(uid, {
    productId: info.productIds.find((id) => PREMIUM_PRODUCT_IDS.has(id)) ?? info.productIds[0] ?? "unknown",
    purchaseToken,
    subscriptionState: info.state,
    expiryTimeMillis: info.expiryTimeMillis,
    autoRenewing: info.autoRenewing,
  });
  const premiumProduct = info.productIds.find((id) => PREMIUM_PRODUCT_IDS.has(id));
  if (syncedTier === "premium" && !info.acknowledged && premiumProduct) {
    await billing.acknowledgePurchaseIfNeeded(purchaseToken, premiumProduct);
  }

  return { uid, tier: syncedTier };
}

/**
 * Parses the base64-encoded JSON payload Pub/Sub delivers for an RTDN
 * message. Returns null for a payload that isn't a subscription
 * notification (Play also sends one-time-product and test notifications
 * through the same topic) or that fails to parse.
 */
export function parseRtdnPurchaseToken(base64Data: string, expectedPackageName?: string): string | null {
  try {
    const payload = JSON.parse(Buffer.from(base64Data, "base64").toString("utf-8")) as {
      packageName?: unknown;
      subscriptionNotification?: { purchaseToken?: unknown };
    };
    if (expectedPackageName && payload.packageName !== expectedPackageName) return null;
    const token = payload.subscriptionNotification?.purchaseToken;
    return typeof token === "string" && token.trim().length > 0 && token.length <= 4096 ? token : null;
  } catch {
    return null;
  }
}
