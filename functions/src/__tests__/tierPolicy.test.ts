import { tierForSubscription } from "../tierPolicy";
import { PlaySubscriptionInfo, PlaySubscriptionState } from "../playBillingPort";

const NOW = new Date("2026-09-11T00:00:00.000Z");

function info(state: PlaySubscriptionState, expiryTimeMillis: number | null = null): PlaySubscriptionInfo {
  return {
    state,
    productIds: ["wiccan_premium_monthly"],
    expiryTimeMillis,
    autoRenewing: false,
    acknowledged: true,
  };
}

describe("tierForSubscription", () => {
  it.each<[PlaySubscriptionState, "free" | "premium"]>([
    ["active", "premium"],
    ["in_grace_period", "premium"],
    ["on_hold", "free"],
    ["paused", "free"],
    ["pending", "free"],
    ["expired", "free"],
    ["unspecified", "free"],
  ])("maps %s to %s regardless of expiry", (state, expected) => {
    expect(tierForSubscription(info(state), NOW)).toBe(expected);
  });

  it.each(["unrelated_subscription", ""])(
    "does not grant premium for an unapproved product %s",
    (productId) => {
      const subscription = info("active");
      subscription.productIds = productId ? [productId] : [];
      expect(tierForSubscription(subscription, NOW)).toBe("free");
    },
  );

  it("grants premium if a recognized product is present alongside another line item", () => {
    const subscription = info("active");
    subscription.productIds = ["unrelated_subscription", "wiccan_premium_yearly"];
    expect(tierForSubscription(subscription, NOW)).toBe("premium");
  });

  it("grants premium for a canceled subscription that hasn't reached its paid-through expiry yet", () => {
    const notYetExpired = NOW.getTime() + 1000 * 60 * 60 * 24; // 1 day from now
    expect(tierForSubscription(info("canceled", notYetExpired), NOW)).toBe("premium");
  });

  it("downgrades a canceled subscription once it's past its expiry", () => {
    const alreadyExpired = NOW.getTime() - 1000 * 60 * 60 * 24; // 1 day ago
    expect(tierForSubscription(info("canceled", alreadyExpired), NOW)).toBe("free");
  });

  it("treats a canceled subscription with no known expiry as free", () => {
    expect(tierForSubscription(info("canceled", null), NOW)).toBe("free");
  });
});
