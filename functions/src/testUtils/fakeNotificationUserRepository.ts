import { NotificationUserRecord, NotificationUserRepositoryPort } from "../notificationUserRepositoryPort";

export class FakeNotificationUserRepository implements NotificationUserRepositoryPort {
  users = new Map<string, string>(); // uid -> token
  clearedTokens: string[] = [];

  async listUsersWithToken(): Promise<NotificationUserRecord[]> {
    return [...this.users.entries()].map(([uid, fcmToken]) => ({ uid, fcmToken }));
  }

  async clearToken(uid: string): Promise<void> {
    this.clearedTokens.push(uid);
    this.users.delete(uid);
  }

  async setToken(uid: string, token: string): Promise<void> {
    this.users.set(uid, token);
  }
}
