import { Firestore } from "firebase-admin/firestore";
import { Auth } from "firebase-admin/auth";
import { AccountDeletionPort } from "./accountDeletionPort";

const USERS_COLLECTION = "users";
const HISTORY_COLLECTION = "userContentHistory";
const DEVICES_SUBCOLLECTION = "devices";
const SUBSCRIPTIONS_COLLECTION = "subscriptions";

export class FirestoreAccountDeletionRepository implements AccountDeletionPort {
  constructor(private readonly db: Firestore, private readonly auth: Auth) {}

  async deleteUserProfile(uid: string): Promise<void> {
    await this.db.collection(USERS_COLLECTION).doc(uid).delete();
  }

  async deleteUserContentHistory(uid: string): Promise<void> {
    const snap = await this.db.collection(HISTORY_COLLECTION).where("userId", "==", uid).get();
    await this.deleteAll(snap.docs.map((doc) => doc.ref));
  }

  async deleteUserDevices(uid: string): Promise<void> {
    const snap = await this.db.collection(USERS_COLLECTION).doc(uid).collection(DEVICES_SUBCOLLECTION).get();
    await this.deleteAll(snap.docs.map((doc) => doc.ref));
  }

  async deleteSubscriptionRecord(uid: string): Promise<void> {
    // A plain delete on a doc that never existed (a user who never
    // subscribed) is a no-op, not an error.
    await this.db.collection(SUBSCRIPTIONS_COLLECTION).doc(uid).delete();
  }

  async deleteAuthAccount(uid: string): Promise<void> {
    await this.auth.deleteUser(uid);
  }

  private async deleteAll(refs: FirebaseFirestore.DocumentReference[]): Promise<void> {
    if (refs.length === 0) {
      return;
    }
    const batch = this.db.batch();
    refs.forEach((ref) => batch.delete(ref));
    await batch.commit();
  }
}
