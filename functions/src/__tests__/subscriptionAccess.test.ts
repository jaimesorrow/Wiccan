import { hasSubscriptionAccess } from "../subscriptionAccess";

describe("subscription access", () => {
  const now = 1_800_000_000_000;

  it("allows a verified unexpired trial or paid period, including canceled renewal before expiry", () => {
    expect(hasSubscriptionAccess({ tier: "premium", subscriptionExpiresAtMillis: now + 1 }, now)).toBe(true);
  });

  it.each([undefined, {}, { tier: "free", subscriptionExpiresAtMillis: now + 1 },
    { tier: "premium" }, { tier: "premium", subscriptionExpiresAtMillis: now },
    { tier: "premium", subscriptionExpiresAtMillis: now - 1 },
    { tier: "premium", subscriptionExpiresAtMillis: String(now + 1) },
    { tier: "premium", subscriptionExpiresAtMillis: Infinity },
    { tier: "premium", subscriptionExpiresAtMillis: NaN },
    { tier: "premium", subscriptionExpiresAtMillis: now + 1, deletionInProgress: true },
  ])("fails closed for missing, expired, invalid or revoked entitlement: %p", (user) => {
    expect(hasSubscriptionAccess(user, now)).toBe(false);
  });
});
