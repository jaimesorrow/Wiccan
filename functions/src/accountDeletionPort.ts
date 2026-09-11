export interface AccountDeletionPort {
  deleteUserProfile(uid: string): Promise<void>;
  deleteUserContentHistory(uid: string): Promise<void>;
  /** users/{uid}/devices is a subcollection - deleting the parent profile doc does not cascade to it. */
  deleteUserDevices(uid: string): Promise<void>;
  /** The durable subscriptions/{uid} audit record - a separate top-level collection from users/{uid}. */
  deleteSubscriptionRecord(uid: string): Promise<void>;
  deleteAuthAccount(uid: string): Promise<void>;
}
