import { PlayBillingPort } from "./playBillingPort";
import { UserRepositoryPort } from "./userRepositoryPort";
import { tierForSubscriptionState } from "./tierPolicy";

export interface RecheckSummary {
  checked: number;
  downgraded: number;
}

/**
 * Subscriptions can lapse (cancellation, a failed renewal charge) without
 * any app-side event ever firing, so this periodically re-verifies every
 * user currently marked premium and downgrades any that are no longer
 * active. Still-active users are left untouched — no redundant write.
 */
export async function recheckLapsedSubscriptions(
  billing: PlayBillingPort,
  users: UserRepositoryPort,
): Promise<RecheckSummary> {
  const premiumUsers = await users.listPremiumUsers();
  let downgraded = 0;

  for (const user of premiumUsers) {
    const info = await billing.getSubscriptionState(user.purchaseToken);
    if (tierForSubscriptionState(info.state) === "free") {
      await users.setTier(user.uid, "free");
      downgraded++;
    }
  }

  return { checked: premiumUsers.length, downgraded };
}
