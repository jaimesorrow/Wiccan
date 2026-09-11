import { SubscriptionRecordInput, SubscriptionRecordPort } from "../subscriptionRecordPort";

export class FakeSubscriptionRecordRepository implements SubscriptionRecordPort {
  records = new Map<string, SubscriptionRecordInput>();

  async save(uid: string, record: SubscriptionRecordInput): Promise<void> {
    this.records.set(uid, record);
  }
}
