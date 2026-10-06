import { ContentTier } from "./types";
import { PlaySubscriptionInfo } from "./playBillingPort";

export const PREMIUM_PRODUCT_IDS: ReadonlySet<string> = new Set([
  "wiccan_premium_monthly",
  "wiccan_premium_yearly",
]);

/**
 * The real Play subscription entitlement rule:
 *
 * - active / in_grace_period -> premium (grace period still grants access
 *   while Google retries a failed renewal charge)
 * - canceled but not yet past its paid-through expiry -> premium until
 *   expiry (canceling auto-renew does not revoke access the user already
 *   paid for)
 * - on_hold / paused / pending / expired / unspecified, or canceled past
 *   expiry -> free
 */
export function tierForSubscription(info: PlaySubscriptionInfo, now: Date = new Date()): ContentTier {
  if (!info.productIds.some((productId) => PREMIUM_PRODUCT_IDS.has(productId))) {
    return "free";
  }

  switch (info.state) {
    case "active":
    case "in_grace_period":
      // Missing/invalid or elapsed expiry must fail closed; API state alone
      // cannot identify which of several line items grants Wiccan access.
      return info.expiryTimeMillis !== null && Number.isFinite(info.expiryTimeMillis) &&
        info.expiryTimeMillis > now.getTime() ? "premium" : "free";
    case "canceled":
      return info.expiryTimeMillis !== null && info.expiryTimeMillis > now.getTime() ? "premium" : "free";
    default:
      return "free";
  }
}
