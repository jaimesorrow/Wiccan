import { FieldValue, Firestore } from "firebase-admin/firestore";
import { Auth } from "firebase-admin/auth";
import { AccountDeletionPort } from "./accountDeletionPort";
import { PlayBillingPort } from "./playBillingPort";
import { PREMIUM_PRODUCT_IDS } from "./tierPolicy";
import { obfuscatedAccountId, purchaseTokenDocumentId } from "./billingIdentity";

const USERS_COLLECTION = "users";
const HISTORY_COLLECTION = "userContentHistory";
const CONTENT_USAGE_COLLECTION = "userContentUsage";
const DEVICES_SUBCOLLECTION = "devices";
const SUBSCRIPTIONS_COLLECTION = "subscriptions";
const PURCHASE_TOKENS_COLLECTION = "purchaseTokens";
const USER_PURCHASE_TOKENS_COLLECTION = "userPurchaseTokens";

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
    const usage = this.db.collection(CONTENT_USAGE_COLLECTION).doc(uid);
    const [content, similarityGroups] = await Promise.all([
      usage.collection("content").get(),
      usage.collection("similarityGroups").get(),
    ]);
    await this.deleteAll([...content.docs, ...similarityGroups.docs].map((doc) => doc.ref));
    await usage.delete();
  }

  async deleteUserDevices(uid: string): Promise<void> {
    const snap = await this.db.collection(USERS_COLLECTION).doc(uid).collection(DEVICES_SUBCOLLECTION).get();
    await this.deleteAll(snap.docs.map((doc) => doc.ref));
  }

  async cancelActiveSubscription(uid: string): Promise<void> {
    const userRef = this.db.collection(USERS_COLLECTION).doc(uid);
    const tokens = await this.db.runTransaction(async (transaction) => {
      const [user, subscription, ownedTokens] = await Promise.all([
        transaction.get(userRef),
        transaction.get(this.db.collection(SUBSCRIPTIONS_COLLECTION).doc(uid)),
        transaction.get(this.db.collection(USER_PURCHASE_TOKENS_COLLECTION).doc(uid).collection("tokens")),
      ]);
      const knownTokens = [...new Set([
        user.get("purchaseToken"),
        subscription.get("purchaseToken"),
        ...ownedTokens.docs.map((doc) => doc.get("purchaseToken")),
      ].filter((token): token is string => typeof token === "string" && token.length > 0))];
      const ownershipRefs = knownTokens.map((token) =>
        this.db.collection(PURCHASE_TOKENS_COLLECTION).doc(purchaseTokenDocumentId(token)));
      const bindings = await Promise.all(ownershipRefs.map((ref) => transaction.get(ref)));
      const accountId = obfuscatedAccountId(uid);
      if (bindings.some((binding) => binding.get("accountId") && binding.get("accountId") !== accountId)) {
        throw new Error("A subscription token is linked to another account.");
      }
      // Freeze purchase verification before reading/canceling its token. This
      // marker stays set on failure so retry cannot race with another purchase.
      if (user.exists) transaction.set(userRef, { deletionInProgress: true }, { merge: true });
      for (let index = 0; index < knownTokens.length; index++) {
        transaction.set(ownershipRefs[index], {
          accountId, updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      }
      return knownTokens;
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
    const tokenRecords = await this.db.collection(USER_PURCHASE_TOKENS_COLLECTION)
      .doc(uid).collection("tokens").get();
    await this.deleteAll(tokenRecords.docs.map((doc) => doc.ref));
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
