export interface NotificationUserRecord {
  uid: string;
  fcmToken: string;
  /** IANA time zone identifier, e.g. "America/Denver". Null if never recorded. */
  timeZone: string | null;
}

/**
 * Port for the daily-notification job's `users` reads/writes. Kept
 * separate from UserRepositoryPort (billing) — different concern, same
 * collection.
 */
export interface NotificationUserRepositoryPort {
  listUsersWithToken(): Promise<NotificationUserRecord[]>;

  /** Called when FCM reports a token as permanently invalid, so future runs skip it. */
  clearToken(uid: string): Promise<void>;

  /** `timeZone` is optional so a bare token refresh doesn't have to resend a value the client already sent once. */
  setToken(uid: string, token: string, timeZone?: string | null): Promise<void>;

  /** Updates only the time zone, independent of the device token (e.g. the user traveled). */
  setTimeZone(uid: string, timeZone: string): Promise<void>;
}
