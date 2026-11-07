import { Firestore, FieldValue } from "firebase-admin/firestore";
import { AcceptedPolicyVersions, UserProfileRepositoryPort } from "./userProfileRepositoryPort";
import { assertAccountWriteAllowed } from "./accountWriteGuard";

const USERS_COLLECTION = "users";

export class FirestoreUserProfileRepository implements UserProfileRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async createProfile(uid: string): Promise<void> {
    const ref = this.db.collection(USERS_COLLECTION).doc(uid);
    await this.db.runTransaction(async (transaction) => {
      // Auth triggers can be delivered more than once. Keep an existing profile,
      // including any entitlement or deletion marker, intact on a replay.
      const user = await transaction.get(ref);
      if (user.exists) return;
      transaction.create(ref, { tier: "free", createdAt: FieldValue.serverTimestamp() });
    });
  }

  async getAcceptedPolicyVersions(uid: string): Promise<AcceptedPolicyVersions> {
    const snap = await this.db.collection(USERS_COLLECTION).doc(uid).get();
    const version = (field: string): number | null => {
      const value = snap.get(field);
      return typeof value === "number" ? value : null;
    };
    return {
      termsVersion: version("acceptedTermsVersion"),
      privacyVersion: version("acceptedPrivacyVersion"),
    };
  }

  async completeProfile(uid: string, firstName: string, lastName: string): Promise<void> {
    const ref = this.db.collection(USERS_COLLECTION).doc(uid);
    await this.db.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(ref);
      assertAccountWriteAllowed(snapshot);
      transaction.update(ref, {
        firstName,
        lastName,
        profileCompletedAt: FieldValue.serverTimestamp(),
      });
    });
  }

  async acceptTerms(uid: string, termsVersion: number, privacyVersion: number): Promise<void> {
    const ref = this.db.collection(USERS_COLLECTION).doc(uid);
    await this.db.runTransaction(async (txn) => {
      const snap = await txn.get(ref);
      assertAccountWriteAllowed(snap);
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
