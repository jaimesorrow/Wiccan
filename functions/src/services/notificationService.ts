import { Firestore } from "firebase-admin/firestore";
import { Messaging } from "firebase-admin/messaging";
import { FirebaseFcmSender } from "../firebaseFcmSender";
import { FirestoreContentRepository } from "../firestoreContentRepository";
import { FirestoreNotificationUserRepository } from "../firestoreNotificationUserRepository";
import { FirestoreSystemConfigRepository } from "../firestoreSystemConfigRepository";
import { sendDailyNotifications } from "../sendDailyNotifications";

export type NotificationRunResult =
  | { skipped: true; reason: "disabled" }
  | { skipped: false; summary: Awaited<ReturnType<typeof sendDailyNotifications>> };

/** Internal service for timezone-aware multi-device notification delivery. */
export class NotificationService {
  constructor(
    private readonly db: Firestore,
    private readonly messaging: Messaging,
  ) {}

  async sendDueDailyNotifications(): Promise<NotificationRunResult> {
    const systemConfig = new FirestoreSystemConfigRepository(this.db);
    const config = await systemConfig.getConfig();
    if (!config.dailyContentEnabled || !config.notificationsEnabled) {
      return { skipped: true, reason: "disabled" };
    }

    const content = new FirestoreContentRepository(this.db);
    const users = new FirestoreNotificationUserRepository(this.db);
    const fcm = new FirebaseFcmSender(this.messaging);
    const summary = await sendDailyNotifications(users, content, fcm);
    return { skipped: false, summary };
  }
}
