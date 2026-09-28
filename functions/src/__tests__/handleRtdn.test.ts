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
});

describe("handleRtdnNotification", () => {
  it("re-verifies and updates the tier for a known purchase token", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "expired", [PRODUCT_ID]);
    const users = new FakeUserRepository();
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
    const users = new FakeUserRepository();
    await users.setTier("user-1", "premium", TOKEN);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const result = await handleRtdnNotification(TOKEN, billing, users, subscriptions);

    expect(result).toEqual({ uid: "user-1", tier: "premium" });
  });

  it("downgrades an active token switched to an unrelated product", async () => {
    const billing = new FakePlayBillingPort();
    billing.respondWith(TOKEN, "active", ["unrelated_subscription"]);
    const users = new FakeUserRepository();
    await users.setTier("user-1", "premium", TOKEN);
    const subscriptions = new FakeSubscriptionRecordRepository();

    const result = await handleRtdnNotification(TOKEN, billing, users, subscriptions);

    expect(result).toEqual({ uid: "user-1", tier: "free" });
    expect(users.users.get("user-1")?.tier).toBe("free");
    expect(subscriptions.records.get("user-1")?.productId).toBe("unrelated_subscription");
  });

  it("is a no-op for a purchase token that doesn't belong to any known user", async () => {
    const billing = new FakePlayBillingPort();
    const users = new FakeUserRepository();
    const subscriptions = new FakeSubscriptionRecordRepository();

    const result = await handleRtdnNotification("unknown-token", billing, users, subscriptions);

    expect(result).toBeNull();
    expect(subscriptions.records.size).toBe(0);
  });
});
