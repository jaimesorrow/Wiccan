import { verifyPurchaseAndSyncTier, PurchaseVerificationError } from "../verifyPurchase";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { FakeUserRepository } from "../testUtils/fakeUserRepository";

const UID = "user-123";
const TOKEN = "token-abc";
const PRODUCT_ID = "wiccan_premium_monthly";

describe("verifyPurchaseAndSyncTier", () => {
  it("grants premium for an active subscription and records the token", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID]);
    const users = new FakeUserRepository();

    const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users);

    expect(tier).toBe("premium");
    expect(users.users.get(UID)).toEqual({ tier: "premium", purchaseToken: TOKEN });
  });

  it("grants premium during the grace period", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "in_grace_period", [PRODUCT_ID]);
    const users = new FakeUserRepository();

    const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users);

    expect(tier).toBe("premium");
  });

  it.each(["canceled", "expired", "on_hold", "paused"] as const)(
    "sets tier to free for a %s subscription",
    async (state) => {
      const billing = new FakePlayBillingPort();
      billing.respondWith(TOKEN, state, [PRODUCT_ID]);
      const users = new FakeUserRepository();

      const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users);

      expect(tier).toBe("free");
      expect(users.users.get(UID)?.tier).toBe("free");
    },
  );

  it("rejects a token whose product doesn't match, without writing a tier", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", ["some_other_product"]);
    const users = new FakeUserRepository();

    await expect(
      verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users),
    ).rejects.toThrow(PurchaseVerificationError);
    expect(users.users.has(UID)).toBe(false);
  });
});
