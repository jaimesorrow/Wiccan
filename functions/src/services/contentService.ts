import { Firestore } from "firebase-admin/firestore";
import { getUserLocalDate } from "../dateUtils";
import { FirestoreContentRepository } from "../firestoreContentRepository";
import { FirestoreSystemConfigRepository } from "../firestoreSystemConfigRepository";
import { selectTodayContent } from "../getTodayContent";

export class DailyContentDisabledError extends Error {
  constructor() {
    super("Daily content is temporarily unavailable.");
    this.name = "DailyContentDisabledError";
  }
}

/**
 * Internal business service for daily-content assignment.
 * HTTP/callable concerns stay in index.ts; selection, timezone semantics,
 * kill-switch enforcement, and the transaction-backed repository remain
 * server-side here.
 */
export class ContentService {
  constructor(private readonly db: Firestore) {}

  async getTodayContent(uid: string) {
    const systemConfig = new FirestoreSystemConfigRepository(this.db);
    if (!(await systemConfig.getConfig()).dailyContentEnabled) {
      throw new DailyContentDisabledError();
    }

    const repo = new FirestoreContentRepository(this.db);
    const timeZone = await repo.getUserTimeZone(uid);
    return selectTodayContent(uid, getUserLocalDate(timeZone), repo);
  }
}
