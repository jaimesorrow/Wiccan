import { Firestore } from "firebase-admin/firestore";
import { Auth } from "firebase-admin/auth";
import * as logger from "firebase-functions/logger";
import { AccountDeletionPort } from "./accountDeletionPort";
import { PlayBillingPort } from "./playBillingPort";

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
    const doc = await this.db.collection(SUBSCRIPTIONS_COLLECTION).doc(uid).get();
    if (!doc.exists) {
      return;
    }

    const purchaseToken = doc.get("purchaseToken") as string | undefined;
    const productId = doc.get("productId") as string | undefined;
    const subscriptionState = doc.get("subscriptionState") as string | undefined;
    if (!purchaseToken || !productId || subscriptionState === "canceled" || subscriptionState === "expired") {
      return;
    }

    try {
      await this.billing.cancelSubscription(purchaseToken, productId);
    } catch (err) {
      // Never let a Play API hiccup block the rest of account deletion -
      // the user's data still needs to be erased even if this one call
      // fails. Logged so a stuck subscription is visible, not silent.
      logger.error("cancelActiveSubscription failed", { uid, error: (err as Error).message });
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
