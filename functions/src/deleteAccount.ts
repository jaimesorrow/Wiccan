import { AccountDeletionPort } from "./accountDeletionPort";

/**
 * Erases everything tied to a user's account: their profile doc, their
 * delivery history, and the Firebase Auth account itself. Order matches
 * the spec — profile, then history, then the Auth account last so the
 * uid stays valid (and re-deletable on retry) until the Firestore data is
 * actually gone.
 */
export async function deleteAccount(uid: string, repo: AccountDeletionPort): Promise<void> {
  await repo.deleteUserProfile(uid);
  await repo.deleteUserContentHistory(uid);
  await repo.deleteAuthAccount(uid);
}
