import { recheckLapsedSubscriptions } from "../recheckLapsedSubscriptions";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { FakeUserRepository } from "../testUtils/fakeUserRepository";
import { FakeSubscriptionRecordRepository } from "../testUtils/fakeSubscriptionRecordRepository";

const PRODUCT_ID = "wiccan_premium_monthly";

describe("recheckLapsedSubscriptions", () => {
  it("downgrades only users whose subscription is no longer entitled, leaving active ones alone", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository(["user-active", "user-expired", "user-canceled", "user-already-free", "legacy-user", "user-bad"]);
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

    expect(summary).toEqual({ checked: 3, downgraded: 2, failed: 0 });
    expect(users.users.get("user-active")?.tier).toBe("premium");
    expect(users.users.get("user-expired")?.tier).toBe("free");
    expect(users.users.get("user-canceled")?.tier).toBe("free");
    expect(subscriptions.records.get("user-active")?.subscriptionState).toBe("active");
  });

  it("keeps a canceled-but-not-yet-expired subscription at premium", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository(["user-active", "user-expired", "user-canceled", "user-already-free", "legacy-user", "user-bad"]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    billing.respondWith("token-canceled-not-expired", "canceled", [PRODUCT_ID], {
      expiryTimeMillis: Date.now() + 1000 * 60 * 60 * 24,
    });
    await users.setTier("user-canceled", "premium", "token-canceled-not-expired");

    const summary = await recheckLapsedSubscriptions(billing, users, subscriptions);

    expect(summary).toEqual({ checked: 1, downgraded: 0, failed: 0 });
    expect(users.users.get("user-canceled")?.tier).toBe("premium");
  });

  it("downgrades a legacy premium user whose active token is for another product", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith("legacy-token", "active", ["unrelated_subscription"]);
    const users = new FakeUserRepository(["user-active", "user-expired", "user-canceled", "user-already-free", "legacy-user", "user-bad"]);
    await users.setTier("legacy-user", "premium", "legacy-token");
    const subscriptions = new FakeSubscriptionRecordRepository();

    expect(await recheckLapsedSubscriptions(billing, users, subscriptions)).toEqual({ checked: 1, downgraded: 1, failed: 0 });
    expect(users.users.get("legacy-user")?.tier).toBe("free");
    expect(subscriptions.records.get("legacy-user")?.productId).toBe("unrelated_subscription");
  });

  it("reports zero checked when nobody is currently premium", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository(["user-active", "user-expired", "user-canceled", "user-already-free", "legacy-user", "user-bad"]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const summary = await recheckLapsedSubscriptions(billing, users, subscriptions);

    expect(summary).toEqual({ checked: 0, downgraded: 0, failed: 0 });
  });

  it("continues past a failing purchase without incorrectly revoking its entitlement", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository(["user-active", "user-expired", "user-canceled", "user-already-free", "legacy-user", "user-bad"]);
    await users.setTier("user-bad", "premium", "bad-token");
    await users.setTier("user-expired", "premium", "expired-token");
    billing.respondWith("expired-token", "expired", [PRODUCT_ID]);
    expect(await recheckLapsedSubscriptions(billing, users, new FakeSubscriptionRecordRepository()))
      .toEqual({ checked: 2, downgraded: 1, failed: 1 });
    expect(users.users.get("user-bad")?.tier).toBe("premium");
    expect(users.users.get("user-expired")?.tier).toBe("free");
  });
});
