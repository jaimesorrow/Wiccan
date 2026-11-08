import { FieldValue, Firestore } from "firebase-admin/firestore";
import { ContentAgentRunReport, ContentAgentStatePort } from "./contentAgent";

const STATE_COLLECTION = "contentAgentState";
const SCHEDULER_DOC = "scheduler";
const RUNS_COLLECTION = "contentGenerationRuns";

export class FirestoreContentAgentStateRepository implements ContentAgentStatePort {
  constructor(private readonly db: Firestore) {}

  async getCursor(): Promise<number> {
    const doc = await this.db.collection(STATE_COLLECTION).doc(SCHEDULER_DOC).get();
    const cursor = doc.get("cursor");
    return typeof cursor === "number" && Number.isInteger(cursor) && cursor >= 0 ? cursor : 0;
  }

  async recordRun(report: ContentAgentRunReport, nextCursor: number): Promise<void> {
    const batch = this.db.batch();
    batch.create(this.db.collection(RUNS_COLLECTION).doc(), { ...report, createdAt: FieldValue.serverTimestamp() });
    batch.set(
      this.db.collection(STATE_COLLECTION).doc(SCHEDULER_DOC),
      { cursor: nextCursor, lastStatus: report.status, updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
    await batch.commit();
  }
}
