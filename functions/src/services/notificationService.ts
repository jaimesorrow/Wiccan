import { Firestore } from "firebase-admin/firestore";
import { Messaging } from "firebase-admin/messaging";
import { FirebaseFcmSender } from "../firebaseFcmSender";
import { FirestoreContentRepository } from "../firestoreContentRepository";
import { FirestoreNotificationUserRepository } from "../firestoreNotificationUserRepository";
import { sendDailyNotifications } from "../sendDailyNotifications";

/** Internal service for timezone-aware multi-device notification delivery. */
export class NotificationService {
  constructor(
    private readonly db: Firestore,
    private readonly messaging: Messaging,
  ) {}

  async sendDueDailyNotifications() {
    const content = new FirestoreContentRepository(this.db);
    const users = new FirestoreNotificationUserRepository(this.db);
    const fcm = new FirebaseFcmSender(this.messaging);
    return sendDailyNotifications(users, content, fcm);
  }
}
