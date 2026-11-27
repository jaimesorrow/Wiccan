/** A server-recorded free trial whose end (and first charge) is approaching. */
export interface TrialReminderCandidate {
  uid: string;
  purchaseToken: string;
  /** Epoch millis the trial ends, as last verified with Google Play. */
  expiryTimeMillis: number;
}

/**
 * Port over the server-only `subscriptions/{uid}` records for the
 * trial-ending reminder. Kept as an interface so the reminder job is unit
 * tested against an in-memory fake.
 */
export interface TrialReminderRepositoryPort {
  /** Records still marked as in a free trial whose expiry falls in (afterMillis, untilMillis]. */
  listTrialsEndingBetween(afterMillis: number, untilMillis: number): Promise<TrialReminderCandidate[]>;

  /**
   * Atomically records that the reminder for this exact trial end was sent.
   * Returns false when it was already claimed (an overlapping or retried run)
   * or the record no longer matches the token, so each trial gets at most one
   * reminder.
   */
  claimReminder(uid: string, purchaseToken: string, expiryTimeMillis: number): Promise<boolean>;
}
