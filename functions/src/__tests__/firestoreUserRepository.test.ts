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
type Query = { path: string; field: string; value: unknown; max: number };

/** Minimal optimistic transaction store; retries when competing claims commit first. */
function transactionalStore() {
  const rows = new Map<string, Data>();
  let revision = 0;
  let attempts = 0;
  const snapshot = (path: string, source = rows) => ({
    id: path.split("/").at(-1), exists: source.has(path), get: (field: string) => source.get(path)?.[field],
  });
  const collection = (path: string) => ({
    doc: (id: string): Reference => ({ path: `${path}/${id}` }),
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
            if ("field" in ref) return { docs: [...source.entries()]
              .filter(([path, data]) => path.startsWith(`${ref.path}/`) && data[ref.field] === ref.value)
              .slice(0, ref.max).map(([path]) => snapshot(path, source)) };
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
