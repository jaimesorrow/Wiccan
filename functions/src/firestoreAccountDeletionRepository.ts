import { Firestore } from "firebase-admin/firestore";
import { Auth } from "firebase-admin/auth";
import { AccountDeletionPort } from "./accountDeletionPort";
import { PlayBillingPort } from "./playBillingPort";
import { PREMIUM_PRODUCT_IDS } from "./tierPolicy";

const USERS_COLLECTION = "users";
const HISTORY_COLLECTION = "userContentHistory";
const CONTENT_USAGE_COLLECTION = "userContentUsage";
const DEVICES_SUBCOLLECTION = "devices";
const SUBSCRIPTIONS_COLLECTION = "subscriptions";

export class FirestoreAccountDeletionRepository implements AccountDeletionPort {
  constructor(
    private readonly db: Firestore,
    private readonly auth: Auth,
    private readonly billing: PlayBillingPort,
  ) {}

  async deleteUserProfile(uid: string): Promise<void> {
    await this.db.collection(USERS_COLLECTION).doc(uid).delete();
  }

  async deleteUserContentHistory(uid: string): Promise<void> {
    const snap = await this.db.collection(HISTORY_COLLECTION).where("userId", "==", uid).get();
    await this.deleteAll(snap.docs.map((doc) => doc.ref));
  }

  async deleteUserContentUsage(uid: string): Promise<void> {
    const snap = await this.db.collection(CONTENT_USAGE_COLLECTION).doc(uid).collection("content").get();
    await this.deleteAll(snap.docs.map((doc) => doc.ref));
  }

  async deleteUserDevices(uid: string): Promise<void> {
    const snap = await this.db.collection(USERS_COLLECTION).doc(uid).collection(DEVICES_SUBCOLLECTION).get();
    await this.deleteAll(snap.docs.map((doc) => doc.ref));
  }

  async cancelActiveSubscription(uid: string): Promise<void> {
    const userRef = this.db.collection(USERS_COLLECTION).doc(uid);
    const tokens = await this.db.runTransaction(async (transaction) => {
      const [user, subscription] = await Promise.all([
        transaction.get(userRef), transaction.get(this.db.collection(SUBSCRIPTIONS_COLLECTION).doc(uid)),
      ]);
      // Freeze purchase verification before reading/canceling its token. This
      // marker stays set on failure so retry cannot race with another purchase.
      if (user.exists) transaction.set(userRef, { deletionInProgress: true }, { merge: true });
      return [...new Set([user.get("purchaseToken"), subscription.get("purchaseToken")]
        .filter((token): token is string => typeof token === "string" && token.length > 0))];
    });
    for (const purchaseToken of tokens) {
      const info = await this.billing.getSubscriptionState(purchaseToken);
      if (!info.autoRenewing) continue;
      const productId = info.productIds.find((id) => PREMIUM_PRODUCT_IDS.has(id));
      if (!productId) throw new Error("The subscription product could not be verified for cancellation.");
      // The user's current token remains authoritative even if the audit
      // record is missing, stale, or its last verification failed halfway.
      await this.billing.cancelSubscription(purchaseToken, productId);
    }
  }

  async deleteSubscriptionRecord(uid: string): Promise<void> {
    // A plain delete on a doc that never existed (a user who never
    // subscribed) is a no-op, not an error.
    await this.db.collection(SUBSCRIPTIONS_COLLECTION).doc(uid).delete();
  }

  async deleteAuthAccount(uid: string): Promise<void> {
    await this.auth.deleteUser(uid);
  }

  private async deleteAll(refs: FirebaseFirestore.DocumentReference[]): Promise<void> {
    if (refs.length === 0) {
      return;
    }
    for (let offset = 0; offset < refs.length; offset += 400) {
      const batch = this.db.batch();
      refs.slice(offset, offset + 400).forEach((ref) => batch.delete(ref));
      await batch.commit();
    }
  }
}
