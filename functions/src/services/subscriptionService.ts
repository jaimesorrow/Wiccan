import { Firestore } from "firebase-admin/firestore";
import { FirestoreSubscriptionRecordRepository } from "../firestoreSubscriptionRecordRepository";
import { FirestoreUserRepository } from "../firestoreUserRepository";
import { GooglePlayBillingRepository } from "../googlePlayBillingRepository";
import { handleRtdnNotification, parseRtdnPurchaseToken } from "../handleRtdn";
import { recheckLapsedSubscriptions } from "../recheckLapsedSubscriptions";
import { verifyPurchaseAndSyncTier } from "../verifyPurchase";

/**
 * Server-authoritative subscription lifecycle service.
 * All client purchase data is treated as input to verify against Google Play,
 * never as entitlement truth.
 */
export class SubscriptionService {
  constructor(
    private readonly db: Firestore,
    private readonly androidPackageName: string,
  ) {}

  async verifyPurchase(uid: string, purchaseToken: string, productId: string) {
    const billing = new GooglePlayBillingRepository(this.androidPackageName);
    const users = new FirestoreUserRepository(this.db);
    const subscriptions = new FirestoreSubscriptionRecordRepository(this.db);
    return verifyPurchaseAndSyncTier(uid, purchaseToken, productId, billing, users, subscriptions);
  }

  async recheckLapsedSubscriptions() {
    const billing = new GooglePlayBillingRepository(this.androidPackageName);
    const users = new FirestoreUserRepository(this.db);
    const subscriptions = new FirestoreSubscriptionRecordRepository(this.db);
    return recheckLapsedSubscriptions(billing, users, subscriptions);
  }

  async handleRtdnMessage(messageData: string) {
    const purchaseToken = parseRtdnPurchaseToken(messageData);
    if (!purchaseToken) return null;

    const billing = new GooglePlayBillingRepository(this.androidPackageName);
    const users = new FirestoreUserRepository(this.db);
    const subscriptions = new FirestoreSubscriptionRecordRepository(this.db);
    return handleRtdnNotification(purchaseToken, billing, users, subscriptions);
  }
}
