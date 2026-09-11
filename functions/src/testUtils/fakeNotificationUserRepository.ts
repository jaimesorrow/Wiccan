import { NotificationDeviceRecord, NotificationUserRepositoryPort } from "../notificationUserRepositoryPort";

export class FakeNotificationUserRepository implements NotificationUserRepositoryPort {
  /** uid -> token -> timeZone. A uid may have any number of registered devices. */
  devices = new Map<string, Map<string, string | null>>();
  cleared: Array<{ uid: string; token: string }> = [];

  async listDevicesWithToken(): Promise<NotificationDeviceRecord[]> {
    const records: NotificationDeviceRecord[] = [];
    for (const [uid, tokens] of this.devices.entries()) {
      for (const [token, timeZone] of tokens.entries()) {
        records.push({ uid, token, timeZone });
      }
    }
    return records;
  }

  async clearToken(uid: string, token: string): Promise<void> {
    this.cleared.push({ uid, token });
    this.devices.get(uid)?.delete(token);
  }

  async setToken(uid: string, token: string, timeZone: string | null = null): Promise<void> {
    const tokens = this.devices.get(uid) ?? new Map<string, string | null>();
    const existing = tokens.get(token);
    tokens.set(token, timeZone ?? existing ?? null);
    this.devices.set(uid, tokens);
  }

  async setTimeZone(uid: string, token: string, timeZone: string): Promise<void> {
    const tokens = this.devices.get(uid) ?? new Map<string, string | null>();
    tokens.set(token, timeZone);
    this.devices.set(uid, tokens);
  }
}
