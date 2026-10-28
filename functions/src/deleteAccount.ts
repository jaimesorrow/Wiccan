import { AccountDeletionPort } from "./accountDeletionPort";

/**
 * Erases everything tied to a user's account: their delivery history,
 * their registered devices, their live Play subscription (canceled, not
 * just its local record erased), their subscription audit record, their
 * profile doc, and finally the Firebase Auth account itself. Firestore
 * does not cascade-delete subcollections when a parent doc is removed, so
 * users/{uid}/devices needs its own explicit sweep rather than being
 * implied by deleting users/{uid}. cancelActiveSubscription runs first,
 * preserving the account and purchase details for retry if Play fails.
 * The Auth account is deleted last so the uid
 * stays valid (and every step above stays retryable) until the Firestore
 * data is actually gone.
 */
export async function deleteAccount(uid: string, repo: AccountDeletionPort): Promise<void> {
  await repo.cancelActiveSubscription(uid);
  await repo.deleteUserContentHistory(uid);
  await repo.deleteUserContentUsage(uid);
  await repo.deleteUserDevices(uid);
  await repo.deleteSubscriptionRecord(uid);
  await repo.deleteVerificationEmailRequest(uid);
  await repo.deleteUserProfile(uid);
  await repo.deleteAuthAccount(uid);
}
