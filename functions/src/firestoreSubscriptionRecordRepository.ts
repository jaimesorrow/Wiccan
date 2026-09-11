import { Firestore, FieldValue } from "firebase-admin/firestore";
import { SubscriptionRecordInput, SubscriptionRecordPort } from "./subscriptionRecordPort";

const SUBSCRIPTIONS_COLLECTION = "subscriptions";

export class FirestoreSubscriptionRecordRepository implements SubscriptionRecordPort {
  constructor(private readonly db: Firestore) {}

  async save(uid: string, record: SubscriptionRecordInput): Promise<void> {
    await this.db
      .collection(SUBSCRIPTIONS_COLLECTION)
      .doc(uid)
      .set(
        {
          ...record,
          lastVerifiedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
  }
}
