import { AccountDeletionPort } from "../accountDeletionPort";

export class FakeAccountDeletionRepository implements AccountDeletionPort {
  calls: string[] = [];

  async deleteUserProfile(uid: string): Promise<void> {
    this.calls.push(`profile:${uid}`);
  }

  async deleteUserContentHistory(uid: string): Promise<void> {
    this.calls.push(`history:${uid}`);
  }

  async deleteUserDevices(uid: string): Promise<void> {
    this.calls.push(`devices:${uid}`);
  }

  async cancelActiveSubscription(uid: string): Promise<void> {
    this.calls.push(`cancel-subscription:${uid}`);
  }

  async deleteSubscriptionRecord(uid: string): Promise<void> {
    this.calls.push(`subscription:${uid}`);
  }

  async deleteAuthAccount(uid: string): Promise<void> {
    this.calls.push(`auth:${uid}`);
  }
}
