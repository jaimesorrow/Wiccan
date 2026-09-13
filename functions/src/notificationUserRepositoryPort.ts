export interface NotificationDeviceRecord {
  uid: string;
  token: string;
  /** IANA time zone identifier, e.g. "America/Denver". Null if never recorded for this device. */
  timeZone: string | null;
  /** Per-device opt-out of the daily push, independent of unregistering the device entirely. Defaults to true. */
  notificationsEnabled: boolean;
}

/**
 * Port for the daily-notification job's device-registration reads/writes.
 * A user can have any number of registered devices (phone, tablet, ...),
 * each with its own token and time zone, so every read/write here is
 * scoped by (uid, token) rather than uid alone. Kept separate from
 * UserRepositoryPort (billing) - different concern, same `users` root.
 */
export interface NotificationUserRepositoryPort {
  /** One entry per registered device across every user - the daily job's full send list. */
  listDevicesWithToken(): Promise<NotificationDeviceRecord[]>;

  /** Called when FCM reports a token as permanently invalid, so future runs skip it. */
  clearToken(uid: string, token: string): Promise<void>;

  /** `timeZone` is optional so a bare token refresh doesn't have to resend a value the client already sent once. */
  setToken(uid: string, token: string, timeZone?: string | null): Promise<void>;

  /** Updates only the time zone for one already-registered device, independent of its token. */
  setTimeZone(uid: string, token: string, timeZone: string): Promise<void>;

  /** Toggles the daily push for one already-registered device, without unregistering it. */
  setNotificationsEnabled(uid: string, token: string, enabled: boolean): Promise<void>;
}
