import { Firestore, FieldValue } from "firebase-admin/firestore";
import { SubscriptionRecordInput, SubscriptionRecordPort } from "./subscriptionRecordPort";

const SUBSCRIPTIONS_COLLECTION = "subscriptions";

export class FirestoreSubscriptionRecordRepository implements SubscriptionRecordPort {
  constructor(private readonly db: Firestore) {}

  async save(uid: string, record: SubscriptionRecordInput): Promise<void> {
    await this.db.runTransaction(async (transaction) => {
      const user = await transaction.get(this.db.collection("users").doc(uid));
      // A different verified purchase may have become current since tier sync.
      if (!user.exists || user.get("deletionInProgress") === true || user.get("purchaseToken") !== record.purchaseToken) return;
      transaction.set(this.db.collection(SUBSCRIPTIONS_COLLECTION).doc(uid), {
        ...record,
        lastVerifiedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      // Mirror only the verified deadline onto the readable profile. Never
      // expose the server-only audit record or trust a client trial clock.
      const expiry = record.expiryTimeMillis;
      transaction.set(user.ref, {
        subscriptionExpiresAtMillis: typeof expiry === "number" && Number.isFinite(expiry) ? expiry : 0,
      }, { merge: true });
    });
  }
}
