import { FieldValue, Firestore, Timestamp } from "firebase-admin/firestore";
import { ContentAgentRunReport, ContentAgentStatePort } from "./contentAgent";

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
    const runRef = this.db.collection(RUNS_COLLECTION).doc(runId);
    return this.db.runTransaction(async (transaction) => {
      const [state, run] = await transaction.getAll(this.stateRef, runRef);
      if (run.exists) return null;
      const leaseUntil = state.get("leaseUntil");
      if (leaseUntil instanceof Timestamp && leaseUntil.toMillis() > now.getTime()) return null;
      const cursor = validCursor(state.get("cursor"));
      transaction.set(this.stateRef, {
        cursor: cursor + 1,
        leaseRunId: runId,
        leaseUntil: Timestamp.fromMillis(now.getTime() + leaseMs),
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      transaction.create(runRef, { status: "running", runId, startedAt: now.toISOString(), createdAt: FieldValue.serverTimestamp() });
      return cursor;
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
