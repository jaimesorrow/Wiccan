import { NotificationUserRecord, NotificationUserRepositoryPort } from "../notificationUserRepositoryPort";

export class FakeNotificationUserRepository implements NotificationUserRepositoryPort {
  users = new Map<string, { fcmToken: string; timeZone: string | null }>();
  clearedTokens: string[] = [];

  async listUsersWithToken(): Promise<NotificationUserRecord[]> {
    return [...this.users.entries()].map(([uid, { fcmToken, timeZone }]) => ({ uid, fcmToken, timeZone }));
  }

  async clearToken(uid: string): Promise<void> {
    this.clearedTokens.push(uid);
    this.users.delete(uid);
  }

  async setToken(uid: string, token: string, timeZone: string | null = null): Promise<void> {
    this.users.set(uid, { fcmToken: token, timeZone });
  }
}
