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

  async acceptTerms(uid: string, termsVersion: number, privacyVersion: number): Promise<void> {
    const ref = this.db.collection(USERS_COLLECTION).doc(uid);
    await this.db.runTransaction(async (txn) => {
      const snap = await txn.get(ref);
      const update: Record<string, unknown> = {
        acceptedTermsVersion: termsVersion,
        acceptedPrivacyVersion: privacyVersion,
      };
      if (!snap.get("acceptedTermsAt")) {
        update.acceptedTermsAt = FieldValue.serverTimestamp();
      }
      txn.set(ref, update, { merge: true });
    });
  }
}
