import { androidpublisher, auth } from "@googleapis/androidpublisher";
import { GooglePlayBillingRepository } from "../googlePlayBillingRepository";

jest.mock("@googleapis/androidpublisher", () => ({
  androidpublisher: jest.fn(), auth: { getClient: jest.fn() },
}));

describe("GooglePlayBillingRepository", () => {
  const get = jest.fn();
  const acknowledge = jest.fn();
  const cancel = jest.fn();
  const repository = new GooglePlayBillingRepository("com.jaimesorrow.wiccan");

  beforeEach(() => {
    jest.clearAllMocks();
    (auth.getClient as jest.Mock).mockResolvedValue({});
    (androidpublisher as jest.Mock).mockReturnValue({
      purchases: { subscriptionsv2: { get }, subscriptions: { acknowledge, cancel } },
    });
    acknowledge.mockResolvedValue({});
    cancel.mockResolvedValue({});
  });

  it("uses the latest premium expiry instead of an unrelated first line item", async () => {
    get.mockResolvedValue({ data: {
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
      externalAccountIdentifiers: { obfuscatedExternalAccountId: "account-hash" },
      linkedPurchaseToken: "previous-token",
      lineItems: [
        { productId: "another_subscription", expiryTime: "2030-01-01T00:00:00Z" },
        { productId: "wiccan_premium_monthly", expiryTime: "2026-10-20T00:00:00Z" },
        { productId: "wiccan_premium_yearly", expiryTime: "2027-10-20T00:00:00Z",
          autoRenewingPlan: { autoRenewEnabled: true } },
      ],
    } });
    expect(await repository.getSubscriptionState("token")).toEqual({
      state: "active", productIds: ["another_subscription", "wiccan_premium_monthly", "wiccan_premium_yearly"],
      expiryTimeMillis: Date.parse("2027-10-20T00:00:00Z"), autoRenewing: true, acknowledged: true,
      obfuscatedExternalAccountId: "account-hash", linkedPurchaseToken: "previous-token",
    });
    expect(get).toHaveBeenCalledWith({ packageName: "com.jaimesorrow.wiccan", token: "token" });
  });

  it("normalizes missing/invalid premium expiry to null, never NaN", async () => {
    get.mockResolvedValue({ data: { subscriptionState: "SUBSCRIPTION_STATE_ACTIVE", lineItems: [
      { productId: "wiccan_premium_monthly", expiryTime: "invalid" },
      { productId: "unrelated", expiryTime: "2030-01-01T00:00:00Z" },
    ] } });
    expect((await repository.getSubscriptionState("token")).expiryTimeMillis).toBeNull();
  });

  it("treats a retained token that Play no longer serves (410) as expired", async () => {
    get.mockRejectedValue({ response: { status: 410 } });
    expect(await repository.getSubscriptionState("old-token")).toMatchObject({
      state: "expired", autoRenewing: false, expiryTimeMillis: null,
    });
  });

  it.each([401, 403, 404, 429, 500])("propagates HTTP %s without revoking access", async (status) => {
    const error = { response: { status } };
    get.mockRejectedValue(error);
    await expect(repository.getSubscriptionState("token")).rejects.toBe(error);
  });

  it("does not cancel a subscription already confirmed to have no auto-renewal", async () => {
    get.mockResolvedValue({ data: { subscriptionState: "SUBSCRIPTION_STATE_CANCELED", lineItems: [] } });
    await repository.cancelSubscription("token", "wiccan_premium_monthly");
    expect(cancel).not.toHaveBeenCalled();
  });

  it("propagates cancellation failure when Play confirms renewal is still enabled", async () => {
    get.mockResolvedValue({ data: { subscriptionState: "SUBSCRIPTION_STATE_ACTIVE", lineItems: [
      { productId: "wiccan_premium_monthly", expiryTime: "2030-01-01T00:00:00Z",
        autoRenewingPlan: { autoRenewEnabled: true } },
    ] } });
    cancel.mockRejectedValue(new Error("Play unavailable"));
    await expect(repository.cancelSubscription("token", "wiccan_premium_monthly")).rejects.toThrow("Play unavailable");
  });

  it("propagates acknowledgement failure unless Play confirms acknowledgement", async () => {
    get.mockResolvedValue({ data: { acknowledgementState: "ACKNOWLEDGEMENT_STATE_PENDING" } });
    acknowledge.mockRejectedValue(new Error("Play unavailable"));
    await expect(repository.acknowledgePurchaseIfNeeded("token", "wiccan_premium_monthly"))
      .rejects.toThrow("Play unavailable");
    get.mockResolvedValue({ data: { acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED" } });
    await expect(repository.acknowledgePurchaseIfNeeded("token", "wiccan_premium_monthly")).resolves.toBeUndefined();
  });
});
