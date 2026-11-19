import { App, deleteApp, initializeApp } from "firebase-admin/app";
import { Firestore, getFirestore } from "firebase-admin/firestore";
import { FirestoreContentAgentStateRepository } from "../firestoreContentAgentStateRepository";
import type { ContentAgentRunReport } from "../contentAgent";

// Runs only against the Firestore emulator (firestore-tests `npm test`).
const describeWithEmulator = process.env.FIRESTORE_EMULATOR_HOST ? describe : describe.skip;
const NOW = new Date("2026-11-08T04:00:00.000Z");
const LEASE_MS = 35 * 60_000;

jest.setTimeout(15_000);

function report(runId: string, cursor: number): ContentAgentRunReport {
  return {
    status: "completed",
    runId,
    startedAt: NOW.toISOString(),
    cursor,
    passageId: "passage-a",
    sourceId: "source-a",
    tier: "free",
    requested: 1,
    accepted: [],
    rejected: [],
    attempts: 1,
    error: null,
    published: false,
  };
}

describeWithEmulator("FirestoreContentAgentStateRepository", () => {
  let app: App;
  let db: Firestore;
  let state: FirestoreContentAgentStateRepository;

  beforeAll(() => {
    app = initializeApp({ projectId: "demo-wiccan" }, "content-agent-state-tests");
    db = getFirestore(app);
    state = new FirestoreContentAgentStateRepository(db);
  });

  beforeEach(async () => {
    await db.recursiveDelete(db.collection("contentAgentState"));
    await db.recursiveDelete(db.collection("contentGenerationRuns"));
  });

  afterAll(async () => {
    await deleteApp(app);
  });

  it("claims consecutive slots, records runs and releases the lease", async () => {
    expect(await state.claimSlot("run-1", NOW, LEASE_MS)).toBe(0);
    await state.recordRun("run-1", report("run-1", 0));
    expect(await state.claimSlot("run-2", new Date(NOW.getTime() + 60_000), LEASE_MS)).toBe(1);

    const run = await db.collection("contentGenerationRuns").doc("run-1").get();
    expect(run.get("status")).toBe("completed");
    expect(run.get("published")).toBe(false);
    const scheduler = await db.collection("contentAgentState").doc("scheduler").get();
    expect(scheduler.get("cursor")).toBe(2);
    expect(scheduler.get("leaseRunId")).toBe("run-2");
  });

  it("refuses a duplicate run identity even after the lease is released", async () => {
    expect(await state.claimSlot("run-1", NOW, LEASE_MS)).toBe(0);
    await state.recordRun("run-1", report("run-1", 0));
    expect(await state.claimSlot("run-1", NOW, LEASE_MS)).toBeNull();
  });

  it("lets exactly one of two concurrent claims win while the lease is live", async () => {
    const results = await Promise.all([
      state.claimSlot("run-a", NOW, LEASE_MS),
      state.claimSlot("run-b", NOW, LEASE_MS),
    ]);
    expect(results.filter((cursor) => cursor !== null)).toEqual([0]);
    const scheduler = await db.collection("contentAgentState").doc("scheduler").get();
    expect(scheduler.get("cursor")).toBe(1);
  });

  it("allows a new claim once an abandoned lease expires, and the stale run cannot release it", async () => {
    expect(await state.claimSlot("run-stale", NOW, LEASE_MS)).toBe(0);
    const later = new Date(NOW.getTime() + LEASE_MS + 1_000);
    expect(await state.claimSlot("run-fresh", later, LEASE_MS)).toBe(1);
    await state.recordRun("run-stale", { ...report("run-stale", 0), status: "failed", error: "timeout" });
    const scheduler = await db.collection("contentAgentState").doc("scheduler").get();
    expect(scheduler.get("leaseRunId")).toBe("run-fresh");
  });
  describe("daily draft counter", () => {
    const DATE = "2026-11-08";
    const daily = () => db.collection("contentAgentState").doc(`daily-${DATE}`).get();

    it("grants at most the daily limit across sequential reservations", async () => {
      expect(await state.reserveDailyDrafts(DATE, 5, 2, 3)).toBe(2);
      expect(await state.reserveDailyDrafts(DATE, 1, 2, 3)).toBe(0);
      expect((await daily()).get("reserved")).toBe(2);
    });

    it("never over-grants when reservations race", async () => {
      const grants = await Promise.all(
        Array.from({ length: 6 }, () => state.reserveDailyDrafts(DATE, 2, 2, 10)),
      );
      expect(grants.reduce((sum, grant) => sum + grant, 0)).toBe(2);
      expect((await daily()).get("reserved")).toBe(2);
    });

    it("stops admitting runs after the run allowance, whatever they wrote", async () => {
      expect(await state.reserveDailyDrafts(DATE, 1, 2, 2)).toBe(1);
      await state.settleDailyDrafts(DATE, 1, 0);
      expect(await state.reserveDailyDrafts(DATE, 1, 2, 2)).toBe(1);
      await state.settleDailyDrafts(DATE, 1, 0);
      expect(await state.reserveDailyDrafts(DATE, 1, 2, 2)).toBe(0);
    });

    it("returns unused slots on settle, and keeps each UTC date separate", async () => {
      expect(await state.reserveDailyDrafts(DATE, 2, 2, 3)).toBe(2);
      await state.settleDailyDrafts(DATE, 2, 1);
      expect((await daily()).get("reserved")).toBe(1);
      expect(await state.reserveDailyDrafts(DATE, 2, 2, 3)).toBe(1);
      expect(await state.reserveDailyDrafts("2026-11-09", 2, 2, 3)).toBe(2);
    });

    it("refunds the run allowance for a run that never reached the model", async () => {
      expect(await state.reserveDailyDrafts(DATE, 2, 2, 3)).toBe(2);
      await state.settleDailyDrafts(DATE, 2, 0, false);
      const snapshot = await daily();
      expect(snapshot.get("reserved")).toBe(0);
      expect(snapshot.get("runs")).toBe(0);
    });

    it("rejects a malformed date key", async () => {
      await expect(state.reserveDailyDrafts("../scheduler", 1, 2, 3)).rejects.toThrow(/Invalid daily counter date key/);
    });
  });
});
