import { PlaySubscriptionState } from "./playBillingPort";

export interface SubscriptionRecordInput {
  productId: string;
  purchaseToken: string;
  subscriptionState: PlaySubscriptionState;
  expiryTimeMillis: number | null;
  autoRenewing: boolean;
}

/**
 * Port to the durable `subscriptions/{uid}` record - admin/server-only,
 * never read or written directly by the client. `users/{uid}.tier` stays
 * the fast-path entitlement flag; this is the fuller audit trail (which
 * product, which token, when it was last verified) that `tier` alone
 * doesn't capture.
 */
export interface SubscriptionRecordPort {
  save(uid: string, record: SubscriptionRecordInput): Promise<void>;
}
