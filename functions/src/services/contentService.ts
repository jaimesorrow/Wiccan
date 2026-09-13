import { Firestore } from "firebase-admin/firestore";
import { getUserLocalDate } from "../dateUtils";
import { FirestoreContentRepository } from "../firestoreContentRepository";
import { selectTodayContent } from "../getTodayContent";

/**
 * Internal business service for daily-content assignment.
 * HTTP/callable concerns stay in index.ts; selection, timezone semantics,
 * and the transaction-backed repository remain server-side here.
 */
export class ContentService {
  constructor(private readonly db: Firestore) {}

  async getTodayContent(uid: string) {
    const repo = new FirestoreContentRepository(this.db);
    const timeZone = await repo.getUserTimeZone(uid);
    return selectTodayContent(uid, getUserLocalDate(timeZone), repo);
  }
}
