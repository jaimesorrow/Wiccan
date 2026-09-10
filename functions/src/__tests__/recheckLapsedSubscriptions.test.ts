import { recheckLapsedSubscriptions } from "../recheckLapsedSubscriptions";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { FakeUserRepository } from "../testUtils/fakeUserRepository";

const PRODUCT_ID = "nyxwell_premium_monthly";

describe("recheckLapsedSubscriptions", () => {
  it("downgrades only users whose subscription has lapsed, leaving active ones alone", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository();

    billing.respondWith("token-still-active", "active", [PRODUCT_ID]);
    billing.respondWith("token-expired", "expired", [PRODUCT_ID]);
    billing.respondWith("token-canceled", "canceled", [PRODUCT_ID]);

    await users.setTier("user-active", "premium", "token-still-active");
    await users.setTier("user-expired", "premium", "token-expired");
    await users.setTier("user-canceled", "premium", "token-canceled");
    await users.setTier("user-already-free", "free", "token-irrelevant");

    const summary = await recheckLapsedSubscriptions(billing, users);

    expect(summary).toEqual({ checked: 3, downgraded: 2 });
    expect(users.users.get("user-active")?.tier).toBe("premium");
    expect(users.users.get("user-expired")?.tier).toBe("free");
    expect(users.users.get("user-canceled")?.tier).toBe("free");
  });

  it("reports zero checked when nobody is currently premium", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository();

    const summary = await recheckLapsedSubscriptions(billing, users);

    expect(summary).toEqual({ checked: 0, downgraded: 0 });
  });
});
