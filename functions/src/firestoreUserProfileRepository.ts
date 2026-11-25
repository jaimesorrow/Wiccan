import { Firestore, FieldValue } from "firebase-admin/firestore";
import { AcceptedPolicyVersions, UserProfileRepositoryPort } from "./userProfileRepositoryPort";
import { assertAccountWriteAllowed } from "./accountWriteGuard";
import { parseUsername } from "./callableInput";
import { decideUsernameReservation, USERNAMES_COLLECTION, UsernameRequiredError } from "./usernameReservation";

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

  /** Reserves a handle without marking the profile complete. Safe before email verification. */
  async reserveUsername(uid: string, username: string): Promise<void> {
    await this.writeUsername(uid, username, null);
  }

  async completeProfile(uid: string, firstName: string, lastName: string, username?: string): Promise<void> {
    await this.writeUsername(uid, username, { firstName, lastName });
  }

  /**
   * Claims `usernames/{lowercase}` and writes `users/{uid}.username` in one
   * transaction. Profile completion is stamped only when names are supplied
   * and the reservation succeeds, so a taken or invalid handle cannot finish
   * onboarding or erase the rest of the profile.
   */
  private async writeUsername(
    uid: string,
    requested: string | undefined,
    names: { firstName: string; lastName: string } | null,
  ): Promise<void> {
    const userRef = this.db.collection(USERS_COLLECTION).doc(uid);
    await this.db.runTransaction(async (transaction) => {
      const user = await transaction.get(userRef);
      if (user.exists) assertAccountWriteAllowed(user);
      const current = user.get("username");
      if (requested !== undefined && parseUsername(requested) === null) {
        throw new UsernameRequiredError();
      }
      const candidate = requested === undefined
        ? (typeof current === "string" ? parseUsername(current) : null)
        : parseUsername(requested);
      const reservationRef = this.db.collection(USERNAMES_COLLECTION).doc((candidate ?? "invalid").toLowerCase());
      const reservation = candidate ? await transaction.get(reservationRef) : null;
      const owner = reservation?.exists ? reservation.get("uid") : null;
      const decision = decideUsernameReservation({
        uid,
        requested,
        currentUsername: current,
        reservedByUid: typeof owner === "string" ? owner : null,
      });
      const previousRef = decision.previousKey
        ? this.db.collection(USERNAMES_COLLECTION).doc(decision.previousKey) : null;
      const previous = previousRef ? await transaction.get(previousRef) : null;
      if (previousRef && previous?.get("uid") === uid) transaction.delete(previousRef);
      transaction.set(reservationRef, {
        uid,
        username: decision.displayUsername,
        updatedAt: FieldValue.serverTimestamp(),
      });
      const profile: Record<string, unknown> = { username: decision.displayUsername };
      if (names) {
        profile.firstName = names.firstName;
        profile.lastName = names.lastName;
        profile.profileCompletedAt = FieldValue.serverTimestamp();
      }
      if (user.exists) {
        transaction.set(userRef, profile, { merge: true });
      } else {
        transaction.set(userRef, {
          tier: "free",
          createdAt: FieldValue.serverTimestamp(),
          ...profile,
        });
      }
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
