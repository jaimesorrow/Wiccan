import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { SubscriptionRecordPort } from "./subscriptionRecordPort";
import { PREMIUM_PRODUCT_IDS, tierForSubscription } from "./tierPolicy";
import * as logger from "firebase-functions/logger";

export interface RecheckSummary {
  checked: number;
  downgraded: number;
  failed: number;
}

/**
 * Subscriptions can lapse (cancellation past its paid-through expiry, a
 * failed renewal charge moving to on_hold/expired) without any app-side
 * event ever firing, so this periodically re-verifies every user
 * with a stored Play token. This also restores a user whose subscription
 * recovered from on_hold while their account was marked free. Still-entitled
 * users (including canceled-but-not-yet-expired) remain premium. One invalid
 * token/API failure is logged and counted
 * without preventing the remaining users from being reconciled.
 */
export async function recheckLapsedSubscriptions(
  billing: PlayBillingPort,
  users: UserRepositoryPort,
  subscriptions: SubscriptionRecordPort,
): Promise<RecheckSummary> {
  const usersWithPurchaseTokens = await users.listUsersWithPurchaseTokens();
  let downgraded = 0;
  let failed = 0;

  for (const user of usersWithPurchaseTokens) {
    try {
      const info = await billing.getSubscriptionState(user.purchaseToken);
      const tier = tierForSubscription(info);
      const syncedTier = await users.setTier(user.uid, tier, user.purchaseToken, "refresh");
      if (syncedTier === null) continue;
      await subscriptions.save(user.uid, {
        productId: info.productIds.find((id) => PREMIUM_PRODUCT_IDS.has(id)) ?? info.productIds[0] ?? "unknown",
        purchaseToken: user.purchaseToken,
        subscriptionState: info.state,
        expiryTimeMillis: info.expiryTimeMillis,
        autoRenewing: info.autoRenewing,
      });
      if (syncedTier === "free") downgraded++;
      const premiumProduct = info.productIds.find((id) => PREMIUM_PRODUCT_IDS.has(id));
      if (syncedTier === "premium" && !info.acknowledged && premiumProduct) {
        await billing.acknowledgePurchaseIfNeeded(user.purchaseToken, premiumProduct);
      }
    } catch (error) {
      failed++;
      logger.error("Subscription reconciliation failed", { uid: user.uid, error: (error as Error).message });
    }
  }

  return { checked: usersWithPurchaseTokens.length, downgraded, failed };
}
