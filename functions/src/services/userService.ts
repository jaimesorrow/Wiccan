import { Auth } from "firebase-admin/auth";
import { Firestore } from "firebase-admin/firestore";
import { acceptTerms } from "../acceptTerms";
import { createUserProfileOnSignUp } from "../createUserProfile";
import { deleteAccount } from "../deleteAccount";
import { FirestoreAccountDeletionRepository } from "../firestoreAccountDeletionRepository";
import { FirestoreNotificationUserRepository } from "../firestoreNotificationUserRepository";
import { FirestoreUserProfileRepository } from "../firestoreUserProfileRepository";
import { GooglePlayBillingRepository } from "../googlePlayBillingRepository";

/**
 * Internal service for protected profile/account mutations. Callers must
 * derive uid from Firebase Auth context; this service never accepts an
 * arbitrary client-selected uid through a public transport contract.
 */
export class UserService {
  constructor(
    private readonly db: Firestore,
    private readonly auth: Auth,
    private readonly androidPackageName: string,
  ) {}

  async registerDevice(uid: string, token: string, timeZone?: string) {
    const users = new FirestoreNotificationUserRepository(this.db);
    await users.setToken(uid, token, timeZone);
  }

  async unregisterDevice(uid: string, token: string) {
    const users = new FirestoreNotificationUserRepository(this.db);
    await users.clearToken(uid, token);
  }

  async updateTimeZone(uid: string, token: string, timeZone: string) {
    const users = new FirestoreNotificationUserRepository(this.db);
    await users.setTimeZone(uid, token, timeZone);
  }

  async setNotificationsEnabled(uid: string, token: string, enabled: boolean) {
    const users = new FirestoreNotificationUserRepository(this.db);
    await users.setNotificationsEnabled(uid, token, enabled);
  }

  async acceptTerms(uid: string, termsVersion: number, privacyVersion: number) {
    const repo = new FirestoreUserProfileRepository(this.db);
    await acceptTerms(uid, termsVersion, privacyVersion, repo);
  }

  async createProfile(uid: string) {
    const repo = new FirestoreUserProfileRepository(this.db);
    await createUserProfileOnSignUp(uid, repo);
  }

  async deleteAccount(uid: string) {
    const billing = new GooglePlayBillingRepository(this.androidPackageName);
    const repo = new FirestoreAccountDeletionRepository(this.db, this.auth, billing);
    await deleteAccount(uid, repo);
  }
}
