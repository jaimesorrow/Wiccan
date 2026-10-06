/** Normalized subscription lifecycle states, independent of the Play API's own enum spelling. */
export type PlaySubscriptionState =
  | "active"
  | "in_grace_period"
  | "canceled"
  | "on_hold"
  | "paused"
  | "pending"
  | "pending_purchase_canceled"
  | "expired"
  | "unspecified";

export interface PlaySubscriptionInfo {
  state: PlaySubscriptionState;
  /** Product IDs actually present on this purchase, for a defense-in-depth check against the client-supplied productId. */
  productIds: string[];
  /** Epoch milliseconds the current billing period ends, if Play reports one. */
  expiryTimeMillis: number | null;
  autoRenewing: boolean;
  /** Whether Google Play reports this purchase as already acknowledged. */
  acknowledged: boolean;
  /** SHA-256 Firebase uid supplied by the app when launching the purchase. */
  obfuscatedExternalAccountId?: string | null;
  /** Previous token when Play replaces a subscription during an upgrade/resubscription. */
  linkedPurchaseToken?: string | null;
}

/**
 * Port between the billing logic and the Google Play Developer API. Kept
 * as an interface so verification/re-check logic can be unit tested
 * against an in-memory fake instead of live Play credentials.
 */
export interface PlayBillingPort {
  getSubscriptionState(purchaseToken: string): Promise<PlaySubscriptionInfo>;

  /**
   * Acknowledges a subscription purchase, required within 3 days of
   * purchase or Google auto-refunds it. Acknowledging an
   * already-acknowledged purchase is expected to be a safe no-op -
   * implementations must not throw for that case.
   */
  acknowledgePurchaseIfNeeded(purchaseToken: string, productId: string): Promise<void>;

  /**
   * Stops auto-renewal for a subscription (the user keeps access through
   * the current paid period, matching tierForSubscription's own policy).
   * Called when a user deletes their account, so deleting the app-side
   * record doesn't leave them still being charged by Google. Canceling an
   * already-canceled/expired subscription is expected to be a safe no-op -
   * implementations must not throw for that case.
   */
  cancelSubscription(purchaseToken: string, productId: string): Promise<void>;
}
