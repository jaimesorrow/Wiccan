import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { App, deleteApp, initializeApp } from "firebase-admin/app";
import { Firestore, getFirestore } from "firebase-admin/firestore";
import { FirestoreTrialReminderRepository } from "../firestoreTrialReminderRepository";
import { TRIAL_REMINDER_WINDOW_HOURS } from "../trialEndingReminders";

// Runs only against the Firestore emulator (firestore-tests `npm test`).
// Needs the (inFreeTrial, expiryTimeMillis) composite index in firestore.indexes.json.
const describeWithEmulator = process.env.FIRESTORE_EMULATOR_HOST ? describe : describe.skip;

const NOW = new Date("2027-01-07T09:00:00.000Z").getTime();
const HOUR = 60 * 60 * 1000;
const UNTIL = NOW + TRIAL_REMINDER_WINDOW_HOURS * HOUR;

jest.setTimeout(15_000);

describe("trial reminder Firestore index", () => {
  it("declares the (inFreeTrial, expiryTimeMillis) composite index the query needs", () => {
    const indexes = JSON.parse(readFileSync(resolve(__dirname, "../../../firestore.indexes.json"), "utf8")) as {
      indexes: Array<Record<string, unknown>>;
    };
    expect(indexes.indexes).toContainEqual({
      collectionGroup: "subscriptions",
      queryScope: "COLLECTION",
      fields: [
        { fieldPath: "inFreeTrial", order: "ASCENDING" },
        { fieldPath: "expiryTimeMillis", order: "ASCENDING" },
      ],
    });
  });
});


describeWithEmulator("FirestoreTrialReminderRepository", () => {
  let app: App;
  let db: Firestore;
  let repo: FirestoreTrialReminderRepository;

  beforeAll(() => {
    app = initializeApp({ projectId: "demo-wiccan" }, "trial-reminder-tests");
    db = getFirestore(app);
    repo = new FirestoreTrialReminderRepository(db);
  });

  beforeEach(async () => {
    for (const name of ["subscriptions", "users"]) {
      const documents = await db.collection(name).listDocuments();
      await Promise.all(documents.map((document) => db.recursiveDelete(document)));
    }
  });

  afterAll(async () => {
    await db.terminate();
    await deleteApp(app);
  });

  async function seedSubscription(uid: string, data: Record<string, unknown>): Promise<void> {
    await db.collection("subscriptions").doc(uid).set(data);
  }

  async function seedUser(uid: string, data: Record<string, unknown> = {}): Promise<void> {
    await db.collection("users").doc(uid).set(data);
  }

  it("returns in-window trials in expiry order and excludes non-matching and boundary misses", async () => {
    await Promise.all([
      // Document IDs sort opposite the expiries, so a doc-id order would fail this assertion.
      seedSubscription("z-soon", {
        inFreeTrial: true, purchaseToken: "token-soon", expiryTimeMillis: NOW + HOUR,
      }),
      seedSubscription("a-later", {
        inFreeTrial: true, purchaseToken: "token-later", expiryTimeMillis: NOW + 20 * HOUR,
      }),
      seedSubscription("just-after-now", {
        inFreeTrial: true, purchaseToken: "token-after-now", expiryTimeMillis: NOW + 1,
      }),
      seedSubscription("at-until-bound", {
        inFreeTrial: true, purchaseToken: "token-until", expiryTimeMillis: UNTIL,
      }),
      // Inclusive upper bound, exclusive lower bound: these must not appear.
      seedSubscription("at-after-bound", {
        inFreeTrial: true, purchaseToken: "token-now", expiryTimeMillis: NOW,
      }),
      seedSubscription("just-after-window", {
        inFreeTrial: true, purchaseToken: "token-after-window", expiryTimeMillis: UNTIL + 1,
      }),
      seedSubscription("not-in-trial", {
        inFreeTrial: false, purchaseToken: "token-paid", expiryTimeMillis: NOW + 5 * HOUR,
      }),
      seedSubscription("missing-flag", {
        purchaseToken: "token-flagless", expiryTimeMillis: NOW + 5 * HOUR,
      }),
      seedSubscription("already-sent", {
        inFreeTrial: true,
        purchaseToken: "token-sent",
        expiryTimeMillis: NOW + 8 * HOUR,
        trialReminderSentForExpiry: NOW + 8 * HOUR,
      }),
      // A reminder claimed for a previous trial end still qualifies for this one.
      seedSubscription("renewed-trial", {
        inFreeTrial: true,
        purchaseToken: "token-renewed",
        expiryTimeMillis: NOW + 10 * HOUR,
        trialReminderSentForExpiry: NOW - 30 * HOUR,
      }),
      seedSubscription("non-string-token", {
        inFreeTrial: true, purchaseToken: 123, expiryTimeMillis: NOW + 6 * HOUR,
      }),
      seedSubscription("missing-token", {
        inFreeTrial: true, expiryTimeMillis: NOW + 6 * HOUR,
      }),
      seedSubscription("string-expiry", {
        inFreeTrial: true, purchaseToken: "token-string-expiry", expiryTimeMillis: String(NOW + 6 * HOUR),
      }),
    ]);

    const candidates = await repo.listTrialsEndingBetween(NOW, UNTIL);
    expect(candidates.map((candidate) => candidate.uid)).toEqual([
      "just-after-now",
      "z-soon",
      "renewed-trial",
      "a-later",
      "at-until-bound",
    ]);
    expect(candidates.map((candidate) => candidate.purchaseToken)).toEqual([
      "token-after-now",
      "token-soon",
      "token-renewed",
      "token-later",
      "token-until",
    ]);
    expect(candidates.map((candidate) => candidate.expiryTimeMillis)).toEqual([
      NOW + 1,
      NOW + HOUR,
      NOW + 10 * HOUR,
      NOW + 20 * HOUR,
      UNTIL,
    ]);
  });

  it("claims a reminder once, records the expiry, and hides the trial from a later list", async () => {
    const expiry = NOW + 12 * HOUR;
    await seedSubscription("user-1", {
      inFreeTrial: true, purchaseToken: "token-1", expiryTimeMillis: expiry,
    });
    await seedUser("user-1");

    expect(await repo.claimReminder("user-1", "token-1", expiry)).toBe(true);
    const record = await db.collection("subscriptions").doc("user-1").get();
    expect(record.get("trialReminderSentForExpiry")).toBe(expiry);
    expect(record.get("trialReminderSentAt")).toBeDefined();

    expect(await repo.claimReminder("user-1", "token-1", expiry)).toBe(false);
    expect((await repo.listTrialsEndingBetween(NOW, UNTIL)).map((candidate) => candidate.uid)).toEqual([]);
  });

  it("refuses a claim when the token, user, or deletion state no longer matches", async () => {
    const expiry = NOW + 12 * HOUR;
    await seedSubscription("user-1", {
      inFreeTrial: true, purchaseToken: "token-1", expiryTimeMillis: expiry,
    });
    await seedSubscription("missing-user", {
      inFreeTrial: true, purchaseToken: "token-missing", expiryTimeMillis: expiry,
    });
    await seedSubscription("deleting", {
      inFreeTrial: true, purchaseToken: "token-deleting", expiryTimeMillis: expiry,
    });
    await seedUser("user-1");
    await seedUser("deleting", { deletionInProgress: true });

    expect(await repo.claimReminder("user-1", "wrong-token", expiry)).toBe(false);
    expect(await repo.claimReminder("missing-user", "token-missing", expiry)).toBe(false);
    expect(await repo.claimReminder("deleting", "token-deleting", expiry)).toBe(false);
    expect(await repo.claimReminder("no-such-subscription", "token-1", expiry)).toBe(false);

    const unchanged = await db.collection("subscriptions").doc("user-1").get();
    expect(unchanged.get("trialReminderSentForExpiry")).toBeUndefined();
  });

  it("lets exactly one of two concurrent claims win", async () => {
    const expiry = NOW + 12 * HOUR;
    await seedSubscription("user-1", {
      inFreeTrial: true, purchaseToken: "token-1", expiryTimeMillis: expiry,
    });
    await seedUser("user-1");

    const results = await Promise.all([
      repo.claimReminder("user-1", "token-1", expiry),
      repo.claimReminder("user-1", "token-1", expiry),
    ]);
    expect(results.filter((claimed) => claimed)).toEqual([true]);
    expect((await db.collection("subscriptions").doc("user-1").get()).get("trialReminderSentForExpiry")).toBe(expiry);
  });
});
