import { Firestore } from "firebase-admin/firestore";
import { FirestoreUserRepository } from "../firestoreUserRepository";
import { FirestoreSubscriptionRecordRepository } from "../firestoreSubscriptionRecordRepository";
import { obfuscatedAccountId, purchaseTokenDocumentId } from "../billingIdentity";
import { PurchaseTokenOwnershipError } from "../userRepositoryPort";
import { FirestoreAccountDeletionRepository } from "../firestoreAccountDeletionRepository";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { Auth } from "firebase-admin/auth";

type Data = Record<string, unknown>;
type Reference = { path: string };
type Query = { path: string; field?: string; value?: unknown; max?: number };

/** Minimal optimistic transaction store; retries when competing claims commit first. */
function transactionalStore() {
  const rows = new Map<string, Data>();
  let revision = 0;
  let attempts = 0;
  const snapshot = (path: string, source = rows) => ({
    id: path.split("/").at(-1), exists: source.has(path), get: (field: string) => source.get(path)?.[field],
  });
  const collection = (path: string) => ({
    path,
    doc: (id: string) => ({
      path: `${path}/${id}`,
      collection: (name: string) => collection(`${path}/${id}/${name}`),
    }),
    where: (field: string, _operator: string, value: unknown) => ({
      limit: (max: number): Query => ({ path, field, value, max }),
    }),
  });
  const db = {
    collection,
    runTransaction: async <T>(callback: (transaction: object) => Promise<T>): Promise<T> => {
      for (;;) {
        attempts++;
        const startingRevision = revision;
        const source = new Map(rows);
        const writes: [string, Data][] = [];
        const result = await callback({
          get: async (ref: Reference | Query) => {
            if ("field" in ref || ref.path.split("/").length % 2 === 1) return { docs: [...source.entries()]
              .filter(([path, data]) => path.startsWith(`${ref.path}/`) &&
                (ref.field === undefined || data[ref.field] === ref.value))
              .filter(([path]) => path.split("/").length === ref.path.split("/").length + 1)
              .slice(0, ref.max).map(([path]) => ({
                ...snapshot(path, source), ref: { path },
              })) };
            return snapshot(ref.path, source);
          },
          set: (ref: Reference, data: Data, options?: { merge: boolean }) => {
            writes.push([ref.path, options?.merge ? { ...source.get(ref.path), ...data } : data]);
          },
        });
        if (startingRevision !== revision) continue;
        for (const [path, data] of writes) rows.set(path, data);
        if (writes.length) revision++;
        return result;
      }
    },
  };
  return { db: db as unknown as Firestore, rows, attempts: () => attempts };
}

describe("Firestore subscription ownership transactions", () => {
  it("allows exactly one concurrent claimant and retains only hashed identity", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "free" });
    store.rows.set("users/bob", { tier: "free" });
    const users = new FirestoreUserRepository(store.db);
    const results = await Promise.allSettled([
      users.setTier("alice", "premium", "token/with/slashes"),
      users.setTier("bob", "premium", "token/with/slashes"),
    ]);
    expect(results.filter((result) => result.status === "fulfilled")).toHaveLength(1);
    const rejected = results.find((result) => result.status === "rejected") as PromiseRejectedResult;
    expect(rejected.reason).toBeInstanceOf(PurchaseTokenOwnershipError);
    expect(store.attempts()).toBeGreaterThan(2);
    expect(store.rows.get(`purchaseTokens/${purchaseTokenDocumentId("token/with/slashes")}`)).toMatchObject({
      accountId: obfuscatedAccountId("alice"),
    });
    expect(store.rows.get(`purchaseTokens/${purchaseTokenDocumentId("token/with/slashes")}`)).not.toHaveProperty("uid");
    expect(store.rows.get("users/bob")?.tier).toBe("free");
  });

  it("protects pre-registry legacy ownership during migration", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "premium", purchaseToken: "legacy-token" });
    store.rows.set("users/bob", { tier: "free" });
    await expect(new FirestoreUserRepository(store.db).setTier("bob", "premium", "legacy-token"))
      .rejects.toBeInstanceOf(PurchaseTokenOwnershipError);
    expect(store.rows.get("users/bob")?.tier).toBe("free");
  });

  it("reserves an independent legacy token before replacing the current token", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "premium", purchaseToken: "monthly-token" });
    store.rows.set(`purchaseTokens/${purchaseTokenDocumentId("monthly-token")}`, {
      accountId: obfuscatedAccountId("alice"), auditMarker: "preserve",
    });
    const users = new FirestoreUserRepository(store.db);

    await users.setTier("alice", "premium", "yearly-token");

    expect(store.rows.get(`purchaseTokens/${purchaseTokenDocumentId("monthly-token")}`))
      .toMatchObject({ accountId: obfuscatedAccountId("alice"), auditMarker: "preserve" });
    expect(store.rows.get(`purchaseTokens/${purchaseTokenDocumentId("monthly-token")}`)).not.toHaveProperty("superseded");
    expect(store.rows.get(`userPurchaseTokens/alice/tokens/${purchaseTokenDocumentId("monthly-token")}`))
      .toMatchObject({ purchaseToken: "monthly-token" });
    expect(store.rows.get(`userPurchaseTokens/alice/tokens/${purchaseTokenDocumentId("yearly-token")}`))
      .toMatchObject({ purchaseToken: "yearly-token" });
  });

  it("rejects a stale refresh/replacement and cannot recreate deleted users", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "free" });
    store.rows.set("users/bob", { tier: "free" });
    const users = new FirestoreUserRepository(store.db);
    await users.setTier("alice", "premium", "new-token");
    expect(await users.setTier("alice", "free", "old-token", "refresh")).toBeNull();
    expect(await users.setTier("alice", "premium", "replacement", "replacement", "old-token")).toBeNull();
    store.rows.delete("users/alice");
    expect(await users.setTier("alice", "premium", "new-token", "refresh")).toBeNull();
    expect(await users.setTier("alice", "premium", "replacement", "replacement", "new-token")).toBeNull();
    expect(store.rows.has("users/alice")).toBe(false);
    await expect(users.setTier("bob", "premium", "new-token")).rejects.toBeInstanceOf(PurchaseTokenOwnershipError);
  });

  it("does not overwrite audit data with a superseded token", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "premium", purchaseToken: "new-token" });
    const subscriptions = new FirestoreSubscriptionRecordRepository(store.db);
    await subscriptions.save("alice", {
      productId: "wiccan_premium_monthly", purchaseToken: "old-token", subscriptionState: "expired",
      expiryTimeMillis: null, autoRenewing: false,
    });
    expect(store.rows.has("subscriptions/alice")).toBe(false);
  });

  it("marks only the Play-linked predecessor superseded and protects its owner after account deletion", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "free" });
    store.rows.set("users/bob", { tier: "free" });
    const users = new FirestoreUserRepository(store.db);
    await users.setTier("alice", "premium", "old-token");
    await users.setTier("alice", "premium", "replacement", "purchase", "old-token");
    await expect(users.setTier("alice", "premium", "old-token")).rejects.toThrow("replaced");
    store.rows.delete("users/alice");
    await expect(users.setTier("bob", "premium", "another-replacement", "purchase", "old-token"))
      .rejects.toThrow("previous subscription belongs");
    expect(store.rows.get("users/bob")?.tier).toBe("free");
  });

  it("cancels the authoritative current token even when no audit record was saved", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "premium", purchaseToken: "current-token" });
    const billing = new FakePlayBillingPort();
    billing.respondWith("current-token", "active", ["wiccan_premium_monthly"], { autoRenewing: true });
    const deletion = new FirestoreAccountDeletionRepository(store.db, {} as Auth, billing);
    await deletion.cancelActiveSubscription("alice");
    expect(billing.canceledTokens).toEqual([{ purchaseToken: "current-token", productId: "wiccan_premium_monthly" }]);
    expect(store.rows.get("users/alice")?.deletionInProgress).toBe(true);
    await expect(new FirestoreUserRepository(store.db).setTier("alice", "premium", "new-token"))
      .rejects.toThrow("being deleted");
  });

  it("reserves a pre-registry token before deletion, including without an initial registry entry", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "premium", purchaseToken: "legacy-token" });
    const billing = new FakePlayBillingPort();
    billing.respondWith("legacy-token", "active", ["wiccan_premium_monthly"], { autoRenewing: false });
    const deletion = new FirestoreAccountDeletionRepository(store.db, {} as Auth, billing);

    await deletion.cancelActiveSubscription("alice");

    expect(store.rows.get(`purchaseTokens/${purchaseTokenDocumentId("legacy-token")}`))
      .toMatchObject({ accountId: obfuscatedAccountId("alice") });
    expect(store.rows.get("users/alice")?.deletionInProgress).toBe(true);
  });

  it("cancels every independently owned renewing token before deletion", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "premium", purchaseToken: "yearly-token" });
    const users = new FirestoreUserRepository(store.db);
    await users.setTier("alice", "premium", "monthly-token");
    await users.setTier("alice", "premium", "yearly-token");
    const billing = new FakePlayBillingPort();
    billing.respondWith("monthly-token", "active", ["wiccan_premium_monthly"], { autoRenewing: true });
    billing.respondWith("yearly-token", "active", ["wiccan_premium_yearly"], { autoRenewing: true });
    const deletion = new FirestoreAccountDeletionRepository(store.db, {} as Auth, billing);

    await deletion.cancelActiveSubscription("alice");

    expect(billing.canceledTokens).toEqual(expect.arrayContaining([
      { purchaseToken: "monthly-token", productId: "wiccan_premium_monthly" },
      { purchaseToken: "yearly-token", productId: "wiccan_premium_yearly" },
    ]));
    expect(billing.canceledTokens).toHaveLength(2);
  });

  it("preserves purchase details and keeps new purchases frozen when cancellation fails", async () => {
    const store = transactionalStore();
    store.rows.set("users/alice", { tier: "premium", purchaseToken: "current-token" });
    store.rows.set("subscriptions/alice", { purchaseToken: "previous-token", productId: "stale-product" });
    const billing = new FakePlayBillingPort();
    billing.respondWith("current-token", "active", ["wiccan_premium_monthly"], { autoRenewing: true });
    billing.cancelSubscription = async () => { throw new Error("Play unavailable"); };
    const deletion = new FirestoreAccountDeletionRepository(store.db, {} as Auth, billing);
    await expect(deletion.cancelActiveSubscription("alice")).rejects.toThrow("Play unavailable");
    expect(store.rows.get("users/alice")).toMatchObject({ purchaseToken: "current-token", deletionInProgress: true });
    expect(store.rows.get("subscriptions/alice")?.purchaseToken).toBe("previous-token");
  });
});
