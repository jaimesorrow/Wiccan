import { Firestore, FieldValue } from "firebase-admin/firestore";
import { NotificationUserRecord, NotificationUserRepositoryPort } from "./notificationUserRepositoryPort";

const USERS_COLLECTION = "users";

export class FirestoreNotificationUserRepository implements NotificationUserRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async listUsersWithToken(): Promise<NotificationUserRecord[]> {
    const snap = await this.db.collection(USERS_COLLECTION).where("fcmToken", "!=", null).get();
    return snap.docs
      .filter((doc) => typeof doc.get("fcmToken") === "string")
      .map((doc) => ({
        uid: doc.id,
        fcmToken: doc.get("fcmToken") as string,
        timeZone: typeof doc.get("timeZone") === "string" ? (doc.get("timeZone") as string) : null,
      }));
  }

  async clearToken(uid: string): Promise<void> {
    await this.db.collection(USERS_COLLECTION).doc(uid).update({ fcmToken: FieldValue.delete() });
  }

  async setToken(uid: string, token: string): Promise<void> {
    await this.db.collection(USERS_COLLECTION).doc(uid).set({ fcmToken: token }, { merge: true });
  }
}
