import { ContentTier } from "./types";
import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { SubscriptionRecordPort } from "./subscriptionRecordPort";
import { tierForSubscription } from "./tierPolicy";

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
  const uid = await users.findUidByPurchaseToken(purchaseToken);
  if (!uid) {
    return null;
  }

  const info = await billing.getSubscriptionState(purchaseToken);
  const tier = tierForSubscription(info);

  await users.setTier(uid, tier, purchaseToken);
  await subscriptions.save(uid, {
    productId: info.productIds[0] ?? "unknown",
    purchaseToken,
    subscriptionState: info.state,
    expiryTimeMillis: info.expiryTimeMillis,
    autoRenewing: info.autoRenewing,
  });

  return { uid, tier };
}

/**
 * Parses the base64-encoded JSON payload Pub/Sub delivers for an RTDN
 * message. Returns null for a payload that isn't a subscription
 * notification (Play also sends one-time-product and test notifications
 * through the same topic) or that fails to parse.
 */
export function parseRtdnPurchaseToken(base64Data: string): string | null {
  try {
    const payload = JSON.parse(Buffer.from(base64Data, "base64").toString("utf-8")) as {
      subscriptionNotification?: { purchaseToken?: string };
    };
    return payload.subscriptionNotification?.purchaseToken ?? null;
  } catch {
    return null;
  }
}
