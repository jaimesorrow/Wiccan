import { AccountDeletionPort } from "./accountDeletionPort";

/**
 * Erases everything tied to a user's account: their delivery history,
 * their registered devices, their subscription audit record, their
 * profile doc, and finally the Firebase Auth account itself. Firestore
 * does not cascade-delete subcollections when a parent doc is removed, so
 * users/{uid}/devices needs its own explicit sweep rather than being
 * implied by deleting users/{uid}. The Auth account is deleted last so
 * the uid stays valid (and every step above stays retryable) until the
 * Firestore data is actually gone.
 */
export async function deleteAccount(uid: string, repo: AccountDeletionPort): Promise<void> {
  await repo.deleteUserContentHistory(uid);
  await repo.deleteUserDevices(uid);
  await repo.deleteSubscriptionRecord(uid);
  await repo.deleteUserProfile(uid);
  await repo.deleteAuthAccount(uid);
}
