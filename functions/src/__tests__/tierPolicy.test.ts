import { tierForSubscriptionState } from "../tierPolicy";
import { PlaySubscriptionState } from "../playBillingPort";

describe("tierForSubscriptionState", () => {
  it.each<[PlaySubscriptionState, "free" | "premium"]>([
    ["active", "premium"],
    ["in_grace_period", "premium"],
    ["canceled", "free"],
    ["on_hold", "free"],
    ["paused", "free"],
    ["pending", "free"],
    ["expired", "free"],
    ["unspecified", "free"],
  ])("maps %s to %s", (state, expected) => {
    expect(tierForSubscriptionState(state)).toBe(expected);
  });
});
