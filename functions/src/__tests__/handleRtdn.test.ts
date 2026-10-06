import { handleRtdnNotification, parseRtdnPurchaseToken } from "../handleRtdn";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { FakeUserRepository } from "../testUtils/fakeUserRepository";
import { FakeSubscriptionRecordRepository } from "../testUtils/fakeSubscriptionRecordRepository";

const PRODUCT_ID = "wiccan_premium_monthly";
const TOKEN = "token-abc";

function encodeRtdnPayload(payload: unknown): string {
  return Buffer.from(JSON.stringify(payload)).toString("base64");
}

describe("parseRtdnPurchaseToken", () => {
  it("extracts the purchase token from a subscription notification payload", () => {
    const data = encodeRtdnPayload({
      subscriptionNotification: { version: "1.0", notificationType: 4, purchaseToken: TOKEN, subscriptionId: PRODUCT_ID },
    });
    expect(parseRtdnPurchaseToken(data)).toBe(TOKEN);
  });

  it("returns null for a non-subscription notification", () => {
    const data = encodeRtdnPayload({ testNotification: { version: "1.0" } });
    expect(parseRtdnPurchaseToken(data)).toBeNull();
  });

  it("returns null for malformed data instead of throwing", () => {
    expect(parseRtdnPurchaseToken("not-valid-base64-json")).toBeNull();
  });

  it.each([null, 123, {}, "", "   ", "a".repeat(4097)])("rejects a malformed token %p", (purchaseToken) => {
    expect(parseRtdnPurchaseToken(encodeRtdnPayload({ subscriptionNotification: { purchaseToken } }))).toBeNull();
  });

  it("only accepts notifications for the configured Android package", () => {
    const payload = { packageName: "com.jaimesorrow.wiccan", subscriptionNotification: { purchaseToken: TOKEN } };
    expect(parseRtdnPurchaseToken(encodeRtdnPayload(payload), "com.jaimesorrow.wiccan")).toBe(TOKEN);
    expect(parseRtdnPurchaseToken(encodeRtdnPayload(payload), "com.other.app")).toBeNull();
    expect(parseRtdnPurchaseToken(encodeRtdnPayload({ subscriptionNotification: { purchaseToken: TOKEN } }),
      "com.jaimesorrow.wiccan")).toBeNull();
  });
});

describe("handleRtdnNotification", () => {
  it("re-verifies and updates the tier for a known purchase token", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "expired", [PRODUCT_ID]);
    const users = new FakeUserRepository(["user-1"]);
    await users.setTier("user-1", "premium", TOKEN);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const result = await handleRtdnNotification(TOKEN, billing, users, subscriptions);

    expect(result).toEqual({ uid: "user-1", tier: "free" });
    expect(users.users.get("user-1")?.tier).toBe("free");
    expect(subscriptions.records.get("user-1")?.subscriptionState).toBe("expired");
  });

  it("keeps a renewed subscription at premium", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", [PRODUCT_ID]);
    const users = new FakeUserRepository(["user-1"]);
    await users.setTier("user-1", "premium", TOKEN);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const result = await handleRtdnNotification(TOKEN, billing, users, subscriptions);

    expect(result).toEqual({ uid: "user-1", tier: "premium" });
  });

  it("downgrades an active token switched to an unrelated product", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", ["unrelated_subscription"]);
    const users = new FakeUserRepository(["user-1"]);
    await users.setTier("user-1", "premium", TOKEN);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const result = await handleRtdnNotification(TOKEN, billing, users, subscriptions);

    expect(result).toEqual({ uid: "user-1", tier: "free" });
    expect(users.users.get("user-1")?.tier).toBe("free");
    expect(subscriptions.records.get("user-1")?.productId).toBe("unrelated_subscription");
  });

  it("is a no-op for a purchase token that doesn't belong to any known user", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith("unknown-token", "active", [PRODUCT_ID]);
    const users = new FakeUserRepository(["user-1"]);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const result = await handleRtdnNotification("unknown-token", billing, users, subscriptions);

    expect(result).toBeNull();
    expect(subscriptions.records.size).toBe(0);
  });

  it("does not apply a delayed expiry while an upgrade becomes current during verification", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository(["user-1"]);
    await users.setTier("user-1", "premium", TOKEN);
    billing.respondWith(TOKEN, "expired", [PRODUCT_ID]);
    const getState = billing.getSubscriptionState.bind(billing);
    billing.getSubscriptionState = async (token) => {
      await users.setTier("user-1", "premium", "new-token");
      return getState(token);
    };
    const subscriptions = new FakeSubscriptionRecordRepository();
    expect(await handleRtdnNotification(TOKEN, billing, users, subscriptions)).toBeNull();
    expect(users.users.get("user-1")).toEqual({ tier: "premium", purchaseToken: "new-token" });
    expect(subscriptions.records.size).toBe(0);
  });

  it("does not recreate a deleted account after a Play API request completes", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository(["user-1"]);
    await users.setTier("user-1", "premium", TOKEN);
    billing.respondWith(TOKEN, "active", [PRODUCT_ID]);
    const getState = billing.getSubscriptionState.bind(billing);
    billing.getSubscriptionState = async (token) => {
      users.users.delete("user-1");
      return getState(token);
    };
    expect(await handleRtdnNotification(TOKEN, billing, users,
      new FakeSubscriptionRecordRepository())).toBeNull();
    expect(users.users.size).toBe(0);
  });

  it("binds and acknowledges a verified replacement linked to the current token", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith("replacement-token", "active", [PRODUCT_ID], { linkedPurchaseToken: TOKEN });
    const users = new FakeUserRepository(["user-1"]);
    await users.setTier("user-1", "premium", TOKEN);
    const subscriptions = new FakeSubscriptionRecordRepository();
    expect(await handleRtdnNotification("replacement-token", billing, users, subscriptions))
      .toEqual({ uid: "user-1", tier: "premium" });
    expect(users.users.get("user-1")?.purchaseToken).toBe("replacement-token");
    expect(billing.acknowledgedTokens).toEqual([{ purchaseToken: "replacement-token", productId: PRODUCT_ID }]);
  });
});
