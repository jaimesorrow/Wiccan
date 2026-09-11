import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { SubscriptionRecordPort } from "./subscriptionRecordPort";
import { tierForSubscription } from "./tierPolicy";

export interface RecheckSummary {
  checked: number;
  downgraded: number;
}

/**
 * Subscriptions can lapse (cancellation past its paid-through expiry, a
 * failed renewal charge moving to on_hold/expired) without any app-side
 * event ever firing, so this periodically re-verifies every user
 * currently marked premium and downgrades any that are no longer
 * entitled. Still-entitled users (including canceled-but-not-yet-expired)
 * are left at `tier: "premium"` - only their `subscriptions/{uid}` audit
 * record is refreshed - no redundant tier write.
 */
export async function recheckLapsedSubscriptions(
  billing: PlayBillingPort,
  users: UserRepositoryPort,
  subscriptions: SubscriptionRecordPort,
): Promise<RecheckSummary> {
  const premiumUsers = await users.listPremiumUsers();
  let downgraded = 0;

  for (const user of premiumUsers) {
    const info = await billing.getSubscriptionState(user.purchaseToken);
    const tier = tierForSubscription(info);
    await subscriptions.save(user.uid, {
      productId: info.productIds[0] ?? "unknown",
      purchaseToken: user.purchaseToken,
      subscriptionState: info.state,
      expiryTimeMillis: info.expiryTimeMillis,
      autoRenewing: info.autoRenewing,
    });
    if (tier === "free") {
      await users.setTier(user.uid, "free");
      downgraded++;
    }
  }

  return { checked: premiumUsers.length, downgraded };
}
