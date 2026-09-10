import { Firestore } from "firebase-admin/firestore";
import { Auth } from "firebase-admin/auth";
import { AccountDeletionPort } from "./accountDeletionPort";

const USERS_COLLECTION = "users";
const HISTORY_COLLECTION = "userContentHistory";

export class FirestoreAccountDeletionRepository implements AccountDeletionPort {
  constructor(private readonly db: Firestore, private readonly auth: Auth) {}

  async deleteUserProfile(uid: string): Promise<void> {
    await this.db.collection(USERS_COLLECTION).doc(uid).delete();
  }

  async deleteUserContentHistory(uid: string): Promise<void> {
    const snap = await this.db.collection(HISTORY_COLLECTION).where("userId", "==", uid).get();
    if (snap.empty) {
      return;
    }
    const batch = this.db.batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  }

  async deleteAuthAccount(uid: string): Promise<void> {
    await this.auth.deleteUser(uid);
  }
}
