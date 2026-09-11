import { recheckLapsedSubscriptions } from "../recheckLapsedSubscriptions";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { FakeUserRepository } from "../testUtils/fakeUserRepository";
import { FakeSubscriptionRecordRepository } from "../testUtils/fakeSubscriptionRecordRepository";

const PRODUCT_ID = "wiccan_premium_monthly";

describe("recheckLapsedSubscriptions", () => {
  it("downgrades only users whose subscription is no longer entitled, leaving active ones alone", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository();
    const subscriptions = new FakeSubscriptionRecordRepository();

    billing.respondWith("token-still-active", "active", [PRODUCT_ID]);
    billing.respondWith("token-expired", "expired", [PRODUCT_ID]);
    billing.respondWith("token-canceled-past-expiry", "canceled", [PRODUCT_ID], {
      expiryTimeMillis: Date.now() - 1000,
    });

    await users.setTier("user-active", "premium", "token-still-active");
    await users.setTier("user-expired", "premium", "token-expired");
    await users.setTier("user-canceled", "premium", "token-canceled-past-expiry");
    await users.setTier("user-already-free", "free", "token-irrelevant");

    const summary = await recheckLapsedSubscriptions(billing, users, subscriptions);

    expect(summary).toEqual({ checked: 3, downgraded: 2 });
    expect(users.users.get("user-active")?.tier).toBe("premium");
    expect(users.users.get("user-expired")?.tier).toBe("free");
    expect(users.users.get("user-canceled")?.tier).toBe("free");
    expect(subscriptions.records.get("user-active")?.subscriptionState).toBe("active");
  });

  it("keeps a canceled-but-not-yet-expired subscription at premium", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository();
    const subscriptions = new FakeSubscriptionRecordRepository();

    billing.respondWith("token-canceled-not-expired", "canceled", [PRODUCT_ID], {
      expiryTimeMillis: Date.now() + 1000 * 60 * 60 * 24,
    });
    await users.setTier("user-canceled", "premium", "token-canceled-not-expired");

    const summary = await recheckLapsedSubscriptions(billing, users, subscriptions);

    expect(summary).toEqual({ checked: 1, downgraded: 0 });
    expect(users.users.get("user-canceled")?.tier).toBe("premium");
  });

  it("reports zero checked when nobody is currently premium", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository();
    const subscriptions = new FakeSubscriptionRecordRepository();

    const summary = await recheckLapsedSubscriptions(billing, users, subscriptions);

    expect(summary).toEqual({ checked: 0, downgraded: 0 });
  });
});
