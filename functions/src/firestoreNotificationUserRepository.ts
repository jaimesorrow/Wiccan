import { createHash } from "crypto";
import { Firestore, FieldValue } from "firebase-admin/firestore";
import { NotificationDeviceRecord, NotificationUserRepositoryPort } from "./notificationUserRepositoryPort";

const USERS_COLLECTION = "users";
const DEVICES_SUBCOLLECTION = "devices";

/**
 * FCM tokens can be long and aren't guaranteed safe as a raw Firestore
 * document ID (length limits, arbitrary characters), so each device's doc
 * is keyed by a stable hash of its token instead. Re-registering the same
 * token is then naturally idempotent (same hash, same doc) instead of
 * accumulating duplicate device rows.
 */
function deviceIdForToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

export class FirestoreNotificationUserRepository implements NotificationUserRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async listDevicesWithToken(): Promise<NotificationDeviceRecord[]> {
    const snap = await this.db.collectionGroup(DEVICES_SUBCOLLECTION).get();
    return snap.docs
      .filter((doc) => typeof doc.get("token") === "string")
      .map((doc) => ({
        uid: doc.ref.parent.parent?.id ?? "",
        token: doc.get("token") as string,
        timeZone: typeof doc.get("timeZone") === "string" ? (doc.get("timeZone") as string) : null,
        // Devices registered before this field existed default to enabled,
        // matching the behavior every device already had.
        notificationsEnabled: doc.get("notificationsEnabled") !== false,
      }))
      .filter((device) => device.uid !== "");
  }

  async clearToken(uid: string, token: string): Promise<void> {
    await this.deviceDoc(uid, token).delete();
  }

  async setToken(uid: string, token: string, timeZone?: string | null): Promise<void> {
    const ref = this.deviceDoc(uid, token);
    // A benign extra read on what is at most a once-per-app-launch call:
    // createdAt/notificationsEnabled should only ever be set once, on the
    // device's actual first registration, never reset on every later
    // token refresh.
    const existing = await ref.get();

    const data: Record<string, unknown> = {
      token,
      platform: "android",
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (timeZone) {
      data.timeZone = timeZone;
    }
    if (!existing.exists) {
      data.createdAt = FieldValue.serverTimestamp();
      data.notificationsEnabled = true;
    }

    await ref.set(data, { merge: true });
  }

  async setTimeZone(uid: string, token: string, timeZone: string): Promise<void> {
    await this.deviceDoc(uid, token).set({ timeZone }, { merge: true });
  }

  async setNotificationsEnabled(uid: string, token: string, enabled: boolean): Promise<void> {
    await this.deviceDoc(uid, token).set({ notificationsEnabled: enabled }, { merge: true });
  }

  private deviceDoc(uid: string, token: string) {
    return this.db.collection(USERS_COLLECTION).doc(uid).collection(DEVICES_SUBCOLLECTION).doc(deviceIdForToken(token));
  }
}
