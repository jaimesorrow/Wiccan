import type { Firestore } from "firebase-admin/firestore";
import { FirestoreSubscriptionRecordRepository } from "../firestoreSubscriptionRecordRepository";

describe("verified subscription expiry mirror", () => {
  const userRef = { path: "users/u" };
  const auditRef = { path: "subscriptions/u" };
  const fields: Record<string, unknown> = { purchaseToken: "token" };
  let exists = true;
  const transaction = {
    get: jest.fn(async () => ({ exists, ref: userRef, get: (key: string) => fields[key] })),
    set: jest.fn(),
  };
  const db = {
    collection: (name: string) => ({ doc: () => name === "users" ? userRef : auditRef }),
    runTransaction: async (run: (txn: typeof transaction) => Promise<void>) => run(transaction),
  } as unknown as Firestore;
  const repo = new FirestoreSubscriptionRecordRepository(db);

  beforeEach(() => {
    exists = true;
    fields.purchaseToken = "token";
    fields.deletionInProgress = false;
    jest.clearAllMocks();
  });

  it.each([1_900_000_000_000, null, NaN])("writes the current token audit and a sanitized readable expiry atomically: %p", async (expiry) => {
    await repo.save("u", {
      productId: "wiccan_premium_monthly", purchaseToken: "token", subscriptionState: "active",
      expiryTimeMillis: expiry, autoRenewing: true,
    });
    expect(transaction.set).toHaveBeenCalledWith(userRef, {
      subscriptionExpiresAtMillis: typeof expiry === "number" && Number.isFinite(expiry) ? expiry : 0,
    }, { merge: true });
    expect(transaction.set).toHaveBeenCalledWith(auditRef, expect.objectContaining({ purchaseToken: "token" }), { merge: true });
  });

  it.each(["deleted", "deleting", "replaced"])("does not grant access to a %s account", async (state) => {
    if (state === "deleted") exists = false;
    if (state === "deleting") fields.deletionInProgress = true;
    if (state === "replaced") fields.purchaseToken = "new-token";
    await repo.save("u", {
      productId: "wiccan_premium_monthly", purchaseToken: "token", subscriptionState: "active",
      expiryTimeMillis: 1_900_000_000_000, autoRenewing: true,
    });
    expect(transaction.set).not.toHaveBeenCalled();
  });
});
