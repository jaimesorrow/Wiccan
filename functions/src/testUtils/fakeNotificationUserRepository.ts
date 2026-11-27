import { NotificationDeviceRecord, NotificationUserRepositoryPort } from "../notificationUserRepositoryPort";
import { SupportedLocale } from "../locale";

interface FakeDeviceState {
  timeZone: string | null;
  notificationsEnabled: boolean;
  locale: SupportedLocale;
}

export class FakeNotificationUserRepository implements NotificationUserRepositoryPort {
  /** uid -> token -> device state. A uid may have any number of registered devices. */
  devices = new Map<string, Map<string, FakeDeviceState>>();
  cleared: Array<{ uid: string; token: string }> = [];

  async listDevicesWithToken(): Promise<NotificationDeviceRecord[]> {
    const records: NotificationDeviceRecord[] = [];
    for (const [uid, tokens] of this.devices.entries()) {
      for (const [token, state] of tokens.entries()) {
        records.push({ uid, token, timeZone: state.timeZone, notificationsEnabled: state.notificationsEnabled, locale: state.locale });
      }
    }
    return records;
  }

  async clearToken(uid: string, token: string): Promise<void> {
    this.cleared.push({ uid, token });
    this.devices.get(uid)?.delete(token);
  }

  async setToken(uid: string, token: string, timeZone: string | null = null, locale: SupportedLocale = "en"): Promise<void> {
    const tokens = this.devices.get(uid) ?? new Map<string, FakeDeviceState>();
    const existing = tokens.get(token);
    tokens.set(token, {
      timeZone: timeZone ?? existing?.timeZone ?? null,
      notificationsEnabled: existing?.notificationsEnabled ?? true,
      locale,
    });
    this.devices.set(uid, tokens);
  }

  async setTimeZone(uid: string, token: string, timeZone: string): Promise<void> {
    const tokens = this.devices.get(uid) ?? new Map<string, FakeDeviceState>();
    const existing = tokens.get(token);
    tokens.set(token, { timeZone, notificationsEnabled: existing?.notificationsEnabled ?? true, locale: existing?.locale ?? "en" });
    this.devices.set(uid, tokens);
  }

  async setNotificationsEnabled(uid: string, token: string, enabled: boolean): Promise<void> {
    const tokens = this.devices.get(uid) ?? new Map<string, FakeDeviceState>();
    const existing = tokens.get(token);
    tokens.set(token, { timeZone: existing?.timeZone ?? null, notificationsEnabled: enabled, locale: existing?.locale ?? "en" });
    this.devices.set(uid, tokens);
  }
}
