/** Legacy `premium` is the stored active-entitlement flag, not a content plan. */
export function hasSubscriptionAccess(user: {
  tier?: unknown;
  subscriptionExpiresAtMillis?: unknown;
  deletionInProgress?: unknown;
} | undefined, nowMillis = Date.now()): boolean {
  const expiry = user?.subscriptionExpiresAtMillis;
  return user?.tier === "premium" && user.deletionInProgress !== true &&
    typeof expiry === "number" && Number.isFinite(expiry) && expiry > nowMillis;
}
