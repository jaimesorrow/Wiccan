export interface AccountDeletionPort {
  deleteUserProfile(uid: string): Promise<void>;
  deleteUserContentHistory(uid: string): Promise<void>;
  deleteAuthAccount(uid: string): Promise<void>;
}
