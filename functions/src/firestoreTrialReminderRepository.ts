import { Firestore, FieldValue } from "firebase-admin/firestore";
import { TrialReminderCandidate, TrialReminderRepositoryPort } from "./trialReminderPort";

const SUBSCRIPTIONS_COLLECTION = "subscriptions";

/** Server-only reads/writes of `subscriptions/{uid}` for the trial-ending reminder. */
export class FirestoreTrialReminderRepository implements TrialReminderRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async listTrialsEndingBetween(afterMillis: number, untilMillis: number): Promise<TrialReminderCandidate[]> {
    // Needs the (inFreeTrial, expiryTimeMillis) composite index in firestore.indexes.json.
    const snap = await this.db.collection(SUBSCRIPTIONS_COLLECTION)
      .where("inFreeTrial", "==", true)
      .where("expiryTimeMillis", ">", afterMillis)
      .where("expiryTimeMillis", "<=", untilMillis)
      .get();
    return snap.docs
      .filter((doc) => typeof doc.get("purchaseToken") === "string" && typeof doc.get("expiryTimeMillis") === "number")
      .filter((doc) => doc.get("trialReminderSentForExpiry") !== doc.get("expiryTimeMillis"))
      .map((doc) => ({
        uid: doc.id,
        purchaseToken: doc.get("purchaseToken") as string,
        expiryTimeMillis: doc.get("expiryTimeMillis") as number,
      }));
  }

  async claimReminder(uid: string, purchaseToken: string, expiryTimeMillis: number): Promise<boolean> {
    const ref = this.db.collection(SUBSCRIPTIONS_COLLECTION).doc(uid);
    return this.db.runTransaction(async (transaction) => {
      const [record, user] = await Promise.all([
        transaction.get(ref), transaction.get(this.db.collection("users").doc(uid)),
      ]);
      if (!record.exists || record.get("purchaseToken") !== purchaseToken) return false;
      if (!user.exists || user.get("deletionInProgress") === true) return false;
      if (record.get("trialReminderSentForExpiry") === expiryTimeMillis) return false;
      transaction.set(ref, {
        trialReminderSentForExpiry: expiryTimeMillis,
        trialReminderSentAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      return true;
    });
  }
}
