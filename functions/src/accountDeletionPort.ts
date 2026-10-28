export interface AccountDeletionPort {
  deleteUserProfile(uid: string): Promise<void>;
  deleteUserContentHistory(uid: string): Promise<void>;
  /** Transactional used-content markers must be erased with the history. */
  deleteUserContentUsage(uid: string): Promise<void>;
  /** users/{uid}/devices is a subcollection - deleting the parent profile doc does not cascade to it. */
  deleteUserDevices(uid: string): Promise<void>;
  /**
   * Stops auto-renewal on the user's live Play subscription, if any, so
   * deleting their local subscription record doesn't leave them still
   * being billed by Google with no app left to manage it from. Must run
   * before deleteSubscriptionRecord, while the purchase token is still on
   * file. A safe no-op when the user never subscribed or is already
   * canceled/expired.
   */
  cancelActiveSubscription(uid: string): Promise<void>;
  /** The durable subscriptions/{uid} audit record - a separate top-level collection from users/{uid}. */
  deleteSubscriptionRecord(uid: string): Promise<void>;
  /** Removes the server-only resend throttle timestamp associated with the account. */
  deleteVerificationEmailRequest(uid: string): Promise<void>;
  deleteAuthAccount(uid: string): Promise<void>;
}
