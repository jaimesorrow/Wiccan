import { verifyPurchaseAndSyncTier, PurchaseVerificationError } from "../verifyPurchase";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { FakeUserRepository } from "../testUtils/fakeUserRepository";
import { FakeSubscriptionRecordRepository } from "../testUtils/fakeSubscriptionRecordRepository";
import { obfuscatedAccountId } from "../billingIdentity";

const UID = "user-123";
const TOKEN = "token-abc";
const PRODUCT_ID = "wiccan_premium_monthly";
const YEARLY_PRODUCT_ID = "wiccan_premium_yearly";

describe("verifyPurchaseAndSyncTier", () => {
  it("grants premium for an active subscription, records the token, and acknowledges the purchase", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID]);
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions);

    expect(tier).toBe("premium");
    expect(users.users.get(UID)).toEqual({ tier: "premium", purchaseToken: TOKEN });
    expect(billing.acknowledgedTokens).toEqual([{ purchaseToken: TOKEN, productId: PRODUCT_ID }]);
    expect(subscriptions.records.get(UID)).toEqual({
      productId: PRODUCT_ID,
      purchaseToken: TOKEN,
      subscriptionState: "active",
      expiryTimeMillis: expect.any(Number),
      autoRenewing: false,
    });
  });

  it("accepts the yearly premium product", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [YEARLY_PRODUCT_ID]);
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const tier = await verifyPurchaseAndSyncTier(
      UID,
      TOKEN,
      YEARLY_PRODUCT_ID,
      billing,
      users,
      subscriptions,
    );

    expect(tier).toBe("premium");
    expect(subscriptions.records.get(UID)?.productId).toBe(YEARLY_PRODUCT_ID);
  });

  it("does not acknowledge again when Play reports an active purchase already acknowledged", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID], { acknowledged: true });
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions);

    expect(tier).toBe("premium");
    expect(billing.acknowledgedTokens).toEqual([]);
  });

  it("grants premium during the grace period", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "in_grace_period", [PRODUCT_ID]);
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions);

    expect(tier).toBe("premium");
  });

  it("grants premium for a canceled subscription that hasn't reached its expiry, without acknowledging again", async () => {
    const billing = new FakePlayBillingPort();
    const notYetExpired = Date.now() + 1000 * 60 * 60 * 24;
    billing.respondWith(TOKEN, "canceled", [PRODUCT_ID], { expiryTimeMillis: notYetExpired });
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions);

    expect(tier).toBe("premium");
    expect(billing.acknowledgedTokens).toEqual([{ purchaseToken: TOKEN, productId: PRODUCT_ID }]);
  });

  it.each(["expired", "on_hold", "paused"] as const)(
    "sets tier to free for a %s subscription, without acknowledging",
    async (state) => {
      const billing = new FakePlayBillingPort();
      billing.respondWith(TOKEN, state, [PRODUCT_ID]);
      const users = new FakeUserRepository([UID]);
      const subscriptions = new FakeSubscriptionRecordRepository();

      const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions);

      expect(tier).toBe("free");
      expect(users.users.get(UID)?.tier).toBe("free");
      expect(billing.acknowledgedTokens).toEqual([]);
    },
  );

  it("sets tier to free for a canceled subscription past its expiry", async () => {
    const billing = new FakePlayBillingPort();
    const alreadyExpired = Date.now() - 1000 * 60 * 60 * 24;
    billing.respondWith(TOKEN, "canceled", [PRODUCT_ID], { expiryTimeMillis: alreadyExpired });
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const tier = await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions);

    expect(tier).toBe("free");
  });

  it("rejects a token whose product doesn't match, without writing a tier or a subscription record", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", ["some_other_product"]);
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    await expect(
      verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions),
    ).rejects.toThrow(PurchaseVerificationError);
    expect(users.users.get(UID)?.tier).toBe("free");
    expect(subscriptions.records.has(UID)).toBe(false);
  });

  it("rejects an arbitrary active subscription product even when the token matches it", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", ["unrelated_subscription"]);
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    await expect(
      verifyPurchaseAndSyncTier(
        UID,
        TOKEN,
        "unrelated_subscription",
        billing,
        users,
        subscriptions,
      ),
    ).rejects.toThrow(PurchaseVerificationError);

    expect(users.users.get(UID)?.tier).toBe("free");
    expect(subscriptions.records.has(UID)).toBe(false);
    expect(billing.acknowledgedTokens).toEqual([]);
  });

  it("rejects a valid token bound by Play to another Firebase account", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID], { obfuscatedExternalAccountId: obfuscatedAccountId("other-user") });
    const users = new FakeUserRepository([UID]);
    const subscriptions = new FakeSubscriptionRecordRepository();
    await expect(verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions))
      .rejects.toThrow(PurchaseVerificationError);
    expect(users.users.get(UID)?.tier).toBe("free");
    expect(billing.acknowledgedTokens).toHaveLength(0);
  });

  it("accepts the account hash matching the caller", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID], { obfuscatedExternalAccountId: obfuscatedAccountId(UID) });
    expect(await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing,
      new FakeUserRepository([UID]), new FakeSubscriptionRecordRepository())).toBe("premium");
  });

  it("grants one account ownership when two users concurrently submit the same legacy token", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID]);
    const users = new FakeUserRepository([UID, "other-user"]);
    const subscriptions = new FakeSubscriptionRecordRepository();
    const attempts = await Promise.allSettled([
      verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions),
      verifyPurchaseAndSyncTier("other-user", TOKEN, PRODUCT_ID, billing, users, subscriptions),
    ]);
    expect(attempts.filter((result) => result.status === "fulfilled")).toHaveLength(1);
    expect(attempts.filter((result) => result.status === "rejected")).toHaveLength(1);
    expect([...users.users.values()].filter((user) => user.tier === "premium")).toHaveLength(1);
    expect(subscriptions.records.size).toBe(1);
    expect(billing.acknowledgedTokens).toHaveLength(1);
  });

  it("rejects replacing a token owned by another account", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID], { linkedPurchaseToken: "previous-token" });
    const users = new FakeUserRepository([UID, "other-user"]);
    await users.setTier("other-user", "premium", "previous-token");
    await expect(verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users,
      new FakeSubscriptionRecordRepository())).rejects.toThrow(PurchaseVerificationError);
    expect(users.users.get(UID)?.tier).toBe("free");
  });

  it("keeps a newer premium purchase when an older expired purchase is restored", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "expired", [PRODUCT_ID]);
    const users = new FakeUserRepository([UID]);
    await users.setTier(UID, "premium", "newer-token");
    const subscriptions = new FakeSubscriptionRecordRepository();
    expect(await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions)).toBe("premium");
    expect(users.users.get(UID)?.purchaseToken).toBe("newer-token");
    expect(subscriptions.records.size).toBe(0);
  });

  it("rejects a delayed purchase after its Firebase profile was deleted, without acknowledgement", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository([UID]);
    billing.respondWith(TOKEN, "active", [PRODUCT_ID]);
    const original = billing.getSubscriptionState.bind(billing);
    billing.getSubscriptionState = async (token) => { users.users.delete(UID); return original(token); };
    await expect(verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users,
      new FakeSubscriptionRecordRepository())).rejects.toThrow(PurchaseVerificationError);
    expect(users.users.has(UID)).toBe(false);
    expect(billing.acknowledgedTokens).toHaveLength(0);
  });

  it("does not restore a canceled-but-entitled predecessor after its linked replacement", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository([UID]);
    await users.setTier(UID, "premium", "old-token");
    billing.respondWith(TOKEN, "active", [PRODUCT_ID], { linkedPurchaseToken: "old-token" });
    const subscriptions = new FakeSubscriptionRecordRepository();
    await verifyPurchaseAndSyncTier(UID, TOKEN, PRODUCT_ID, billing, users, subscriptions);
    billing.respondWith("old-token", "canceled", [PRODUCT_ID], { expiryTimeMillis: Date.now() + 86400000 });
    await expect(verifyPurchaseAndSyncTier(UID, "old-token", PRODUCT_ID, billing, users, subscriptions))
      .rejects.toThrow("replaced by a newer subscription");
    expect(users.users.get(UID)?.purchaseToken).toBe(TOKEN);
  });
});
