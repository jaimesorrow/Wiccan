/** Server-written entitlement for a closed test. The client cannot set these fields. */
export interface ClosedTestGrant {
  tier: "premium";
  subscriptionExpiresAtMillis: number;
  closedTestGrant: true;
  closedTestGrantedAtMillis: number;
}

/**
 * Time-limited premium for closed-test accounts. Access still goes through
 * `tier === "premium"` plus a future `subscriptionExpiresAtMillis`. No purchase
 * token is written, so Play reconciliation does not treat the grant as a subscription.
 */
export function closedTestGrant(endsAtMillis: number, nowMillis = Date.now()): ClosedTestGrant {
  if (!Number.isFinite(endsAtMillis) || endsAtMillis <= nowMillis) {
    throw new Error("Closed-test end must be a finite timestamp in the future.");
  }
  return {
    tier: "premium",
    subscriptionExpiresAtMillis: Math.floor(endsAtMillis),
    closedTestGrant: true,
    closedTestGrantedAtMillis: nowMillis,
  };
}

export function parseClosedTestEnd(value: string, nowMillis = Date.now()): number {
  if (/^\d{13}$/.test(value)) return closedTestGrant(Number(value), nowMillis).subscriptionExpiresAtMillis;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    throw new Error("--ends must be YYYY-MM-DD or epoch milliseconds.");
  }
  const ends = Date.parse(`${value}T23:59:59.999Z`);
  return closedTestGrant(ends, nowMillis).subscriptionExpiresAtMillis;
}
