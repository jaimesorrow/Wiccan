import { UserProfileRepositoryPort } from "./userProfileRepositoryPort";

/**
 * Runs on every new Firebase Auth account (see the `onUserCreated` trigger
 * in index.ts) so `users/{uid}` exists before the client ever needs it —
 * the client never creates its own profile document.
 */
export async function createUserProfileOnSignUp(
  uid: string,
  repo: UserProfileRepositoryPort,
): Promise<void> {
  await repo.createProfile(uid);
}
