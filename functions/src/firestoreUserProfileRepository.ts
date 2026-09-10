import { Firestore, FieldValue } from "firebase-admin/firestore";
import { UserProfileRepositoryPort } from "./userProfileRepositoryPort";

const USERS_COLLECTION = "users";

export class FirestoreUserProfileRepository implements UserProfileRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async createProfile(uid: string): Promise<void> {
    await this.db.collection(USERS_COLLECTION).doc(uid).set({
      tier: "free",
      createdAt: FieldValue.serverTimestamp(),
    });
  }
}
