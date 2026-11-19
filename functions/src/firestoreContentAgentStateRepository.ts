import { FieldValue, Firestore, Timestamp } from "firebase-admin/firestore";
import { ContentAgentRunReport, ContentAgentStatePort, DailyDraftAdmission, DailyDraftClaim } from "./contentAgent";

const STATE_COLLECTION = "contentAgentState";
const SCHEDULER_DOC = "scheduler";
const RUNS_COLLECTION = "contentGenerationRuns";

function validCursor(value: unknown): number {
  return typeof value === "number" && Number.isInteger(value) && value >= 0 ? value : 0;
}

export class FirestoreContentAgentStateRepository implements ContentAgentStatePort {
  constructor(private readonly db: Firestore) {}

  private get stateRef() {
    return this.db.collection(STATE_COLLECTION).doc(SCHEDULER_DOC);
  }

  async claimSlot(runId: string, now: Date, leaseMs: number): Promise<number | null> {
    const result = await this.claim(runId, now, leaseMs);
    return result.status === "claimed" ? result.cursor : null;
  }

  async claimDailySlot(runId: string, now: Date, leaseMs: number, admission: DailyDraftAdmission): Promise<DailyDraftClaim> {
    return this.claim(runId, now, leaseMs, admission);
  }

  private async claim(runId: string, now: Date, leaseMs: number, admission?: DailyDraftAdmission): Promise<DailyDraftClaim> {
    const runRef = this.db.collection(RUNS_COLLECTION).doc(runId);
    return this.db.runTransaction(async (transaction) => {
      const [state, run] = await transaction.getAll(this.stateRef, runRef);
      if (run.exists) return { status: "skipped" };
      const leaseUntil = state.get("leaseUntil");
      if (leaseUntil instanceof Timestamp && leaseUntil.toMillis() > now.getTime()) return { status: "skipped" };
      let grant = 0;
      if (admission) {
        const ref = this.dailyRef(admission.dateKey);
        const counter = await transaction.get(ref);
        const reserved = validCursor(counter.get("reserved"));
        const runs = validCursor(counter.get("runs"));
        grant = runs >= admission.maxRuns ? 0 : Math.max(0, Math.min(admission.requested, admission.limit - reserved));
        if (grant === 0) return { status: "daily-limit-reached" };
        transaction.set(ref, {
          dateKey: admission.dateKey,
          reserved: reserved + grant,
          runs: runs + 1,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      }
      const cursor = validCursor(state.get("cursor"));
      transaction.set(this.stateRef, {
        cursor: cursor + 1,
        leaseRunId: runId,
        leaseUntil: Timestamp.fromMillis(now.getTime() + leaseMs),
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      transaction.create(runRef, { status: "running", runId, startedAt: now.toISOString(), createdAt: FieldValue.serverTimestamp() });
      return { status: "claimed", cursor, reserved: grant };
    });
  }

  private dailyRef(dateKey: string) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(dateKey)) throw new Error(`Invalid daily counter date key '${dateKey}'.`);
    return this.db.collection(STATE_COLLECTION).doc(`daily-${dateKey}`);
  }

  async reserveDailyDrafts(dateKey: string, requested: number, limit: number, maxRuns: number): Promise<number> {
    const ref = this.dailyRef(dateKey);
    return this.db.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(ref);
      const reserved = validCursor(snapshot.get("reserved"));
      const runs = validCursor(snapshot.get("runs"));
      const grant = runs >= maxRuns ? 0 : Math.max(0, Math.min(requested, limit - reserved));
      if (grant > 0) {
        transaction.set(ref, {
          dateKey,
          reserved: reserved + grant,
          runs: runs + 1,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      }
      return grant;
    });
  }

  async settleDailyDrafts(dateKey: string, reserved: number, written: number, countRun = true): Promise<void> {
    const release = Math.max(0, reserved - written);
    if (release === 0 && countRun) return;
    const ref = this.dailyRef(dateKey);
    await this.db.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(ref);
      const current = validCursor(snapshot.get("reserved"));
      transaction.set(ref, {
        reserved: Math.max(0, current - release),
        runs: countRun ? validCursor(snapshot.get("runs")) : Math.max(0, validCursor(snapshot.get("runs")) - 1),
        written: validCursor(snapshot.get("written")) + Math.min(written, reserved),
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    });
  }

  async recordRun(runId: string, report: ContentAgentRunReport): Promise<void> {
    const runRef = this.db.collection(RUNS_COLLECTION).doc(runId);
    await this.db.runTransaction(async (transaction) => {
      const state = await transaction.get(this.stateRef);
      transaction.set(runRef, { ...report, finishedAt: FieldValue.serverTimestamp() }, { merge: true });
      // Release only our own lease; an expired lease may already belong to a newer run.
      const release = state.get("leaseRunId") === runId
        ? { leaseRunId: FieldValue.delete(), leaseUntil: FieldValue.delete() }
        : {};
      transaction.set(this.stateRef, { ...release, lastStatus: report.status, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
    });
  }
}
