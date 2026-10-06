import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { collection, doc, getDoc, getDocs, setDoc } from "firebase/firestore";
import { initializeApp, deleteApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

// This suite must never reach a live Firebase project.
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Run this suite through the Firestore emulator.");
const projectId = "demo-wiccan";
const require = createRequire(import.meta.url);
const { FirestoreUserRepository } = require("../lib/firestoreUserRepository.js");
const { FirestoreSubscriptionRecordRepository } = require("../lib/firestoreSubscriptionRecordRepository.js");
const { PurchaseTokenOwnershipError } = require("../lib/userRepositoryPort.js");
const { obfuscatedAccountId, purchaseTokenDocumentId } = require("../lib/billingIdentity.js");
const { backfillContentUsage } = require("../lib/contentUsageBackfill.js");
const { FirestoreAccountDeletionRepository } = require("../lib/firestoreAccountDeletionRepository.js");
const { FakePlayBillingPort } = require("../lib/testUtils/fakePlayBillingPort.js");
let environment;
let adminApp;
let adminDb;

before(async () => {
  environment = await initializeTestEnvironment({
    projectId, firestore: { rules: await readFile(new URL("../../firestore.rules", import.meta.url), "utf8") },
  });
  adminApp = initializeApp({ projectId }, "security-integration");
  adminDb = getFirestore(adminApp);
});
beforeEach(async () => { await environment.clearFirestore(); });
after(async () => { await environment?.cleanup(); if (adminApp) await deleteApp(adminApp); });

async function seedArchive() {
  await environment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all([
      setDoc(doc(db, "users/alice"), { tier: "free" }),
      setDoc(doc(db, "users/bob"), { tier: "premium" }),
      setDoc(doc(db, "content/free-lesson"), { status: "published", tier: "free" }),
      setDoc(doc(db, "content/premium-lesson"), { status: "published", tier: "premium" }),
      setDoc(doc(db, "content/draft-lesson"), { status: "draft", tier: "free" }),
      setDoc(doc(db, "userContentUsage/alice/content/premium-lesson"), { deliveredDate: "2026-10-01" }),
      setDoc(doc(db, "userContentUsage/alice/content/draft-lesson"), { deliveredDate: "2026-10-01" }),
    ]);
  });
}

test("content requires an authenticated delivery marker; a premium tier alone cannot bypass daily selection", async () => {
  await seedArchive();
  const alice = environment.authenticatedContext("alice").firestore();
  const bob = environment.authenticatedContext("bob").firestore();
  const signedOut = environment.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(alice, "content/premium-lesson")));
  await assertFails(getDoc(doc(alice, "content/free-lesson")));
  await assertFails(getDoc(doc(alice, "content/draft-lesson")));
  await assertFails(getDoc(doc(bob, "content/premium-lesson")));
  await assertFails(getDoc(doc(signedOut, "content/premium-lesson")));
  await assertFails(getDocs(collection(alice, "content")));
});

test("clients cannot forge content delivery, ownership, subscription records, or their tier", async () => {
  await seedArchive();
  const alice = environment.authenticatedContext("alice").firestore();
  for (const path of ["users/alice", "content/free-lesson", "subscriptions/alice", "purchaseTokens/token-hash",
    "userContentUsage/alice/content/free-lesson"]) {
    await assertFails(setDoc(doc(alice, path), { tier: "premium" }));
  }
  await assertSucceeds(getDoc(doc(alice, "users/alice")));
  await assertFails(getDoc(doc(alice, "users/bob")));
});

test("actual Firestore transactions allow one owner when two accounts race to claim a token", async () => {
  await Promise.all([adminDb.doc("users/alice").set({ tier: "free" }), adminDb.doc("users/bob").set({ tier: "free" })]);
  const users = new FirestoreUserRepository(adminDb);
  const results = await Promise.allSettled([
    users.setTier("alice", "premium", "purchase/token"),
    users.setTier("bob", "premium", "purchase/token"),
  ]);
  const fulfilled = results.filter((result) => result.status === "fulfilled");
  const rejected = results.filter((result) => result.status === "rejected");
  assert.equal(fulfilled.length, 1);
  assert.equal(rejected.length, 1);
  assert.ok(rejected[0].reason instanceof PurchaseTokenOwnershipError);
  const accounts = await adminDb.collection("users").get();
  const premiumAccounts = accounts.docs.filter((account) => account.get("tier") === "premium");
  assert.equal(premiumAccounts.length, 1);
  const owner = premiumAccounts[0].id;
  const registry = await adminDb.doc(`purchaseTokens/${purchaseTokenDocumentId("purchase/token")}`).get();
  assert.equal(registry.get("accountId"), obfuscatedAccountId(owner));
  assert.equal(registry.get("uid"), undefined);
  assert.equal(registry.get("purchaseToken"), undefined);
});

test("refreshes cannot overwrite a newer token or recreate deleted accounts and audit records", async () => {
  await Promise.all([adminDb.doc("users/alice").set({ tier: "free" }), adminDb.doc("users/bob").set({ tier: "free" })]);
  const users = new FirestoreUserRepository(adminDb);
  const records = new FirestoreSubscriptionRecordRepository(adminDb);
  await users.setTier("alice", "premium", "new-token");
  assert.equal(await users.setTier("alice", "free", "old-token", "refresh"), null);
  assert.equal(await users.setTier("alice", "premium", "replacement-token", "replacement", "old-token"), null);
  await records.save("alice", {
    productId: "wiccan_premium_monthly", purchaseToken: "old-token", subscriptionState: "expired",
    expiryTimeMillis: null, autoRenewing: false,
  });
  assert.equal((await adminDb.doc("subscriptions/alice").get()).exists, false);
  await adminDb.doc("users/alice").delete();
  assert.equal(await users.setTier("alice", "premium", "new-token", "refresh"), null);
  assert.equal((await adminDb.doc("users/alice").get()).exists, false);
  await assert.rejects(users.setTier("bob", "premium", "new-token"), PurchaseTokenOwnershipError);
  await assert.rejects(users.setTier("alice", "premium", "new-token"), PurchaseTokenOwnershipError);
});

test("Play-linked predecessors cannot be restored or claimed across deleted accounts", async () => {
  await Promise.all([adminDb.doc("users/alice").set({ tier: "free" }), adminDb.doc("users/bob").set({ tier: "free" })]);
  const users = new FirestoreUserRepository(adminDb);
  await users.setTier("alice", "premium", "predecessor-token");
  await users.setTier("alice", "premium", "replacement-token", "purchase", "predecessor-token");
  await assert.rejects(users.setTier("alice", "premium", "predecessor-token"), PurchaseTokenOwnershipError);
  await adminDb.doc("users/alice").delete();
  await assert.rejects(users.setTier("bob", "premium", "foreign-replacement", "purchase", "predecessor-token"),
    PurchaseTokenOwnershipError);
});

test("deletion freezes transaction claims before canceling the current token, including an unsaved audit record", async () => {
  await adminDb.doc("users/alice").set({ tier: "premium", purchaseToken: "current-token" });
  const billing = new FakePlayBillingPort();
  billing.respondWith("current-token", "active", ["wiccan_premium_monthly"], { autoRenewing: true });
  const users = new FirestoreUserRepository(adminDb);
  billing.cancelSubscription = async (token) => {
    assert.equal(token, "current-token");
    await assert.rejects(users.setTier("alice", "premium", "new-purchase"), PurchaseTokenOwnershipError);
  };
  const deletion = new FirestoreAccountDeletionRepository(adminDb, {}, billing);
  await deletion.cancelActiveSubscription("alice");
  assert.equal((await adminDb.doc("users/alice").get()).get("deletionInProgress"), true);
  assert.equal((await adminDb.doc("subscriptions/alice").get()).exists, false);
});

test("legacy Archive backfill dry run does not write; commit preserves existing markers and skips deleted accounts", async () => {
  await adminDb.doc("users/alice").set({ tier: "free" });
  await adminDb.doc("userContentHistory/h1").set({ userId: "alice", contentId: "one", deliveredDate: "2026-10-01" });
  await adminDb.doc("userContentHistory/h2").set({ userId: "alice", contentId: "two", deliveredDate: "2026-10-02" });
  await adminDb.doc("userContentHistory/h3").set({ userId: "deleted-user", contentId: "three", deliveredDate: "2026-10-01" });
  await adminDb.doc("userContentUsage/alice/content/two").set({ deliveredDate: "2026-09-01", custom: "preserve" });
  const markers = [
    { uid: "alice", contentId: "one", deliveredDate: "2026-10-01", historyId: "h1" },
    { uid: "alice", contentId: "two", deliveredDate: "2026-10-02", historyId: "h2" },
    { uid: "deleted-user", contentId: "three", deliveredDate: "2026-10-01", historyId: "h3" },
  ];
  assert.deepEqual(await backfillContentUsage(adminDb, markers, false), {
    planned: 3, missing: 1, preserved: 1, skippedOrphanHistory: 1, created: 0,
  });
  assert.equal((await adminDb.doc("userContentUsage/alice/content/one").get()).exists, false);
  assert.deepEqual(await backfillContentUsage(adminDb, markers, true), {
    planned: 3, missing: 1, preserved: 1, skippedOrphanHistory: 1, created: 1,
  });
  assert.equal((await adminDb.doc("userContentUsage/alice/content/two").get()).get("custom"), "preserve");
  assert.equal((await adminDb.doc("userContentUsage/deleted-user/content/three").get()).exists, false);
});
