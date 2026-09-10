/** Normalized subscription lifecycle states, independent of the Play API's own enum spelling. */
export type PlaySubscriptionState =
  | "active"
  | "in_grace_period"
  | "canceled"
  | "on_hold"
  | "paused"
  | "pending"
  | "expired"
  | "unspecified";

export interface PlaySubscriptionInfo {
  state: PlaySubscriptionState;
  /** Product IDs actually present on this purchase, for a defense-in-depth check against the client-supplied productId. */
  productIds: string[];
}

/**
 * Port between the billing logic and the Google Play Developer API. Kept
 * as an interface so verification/re-check logic can be unit tested
 * against an in-memory fake instead of live Play credentials.
 */
export interface PlayBillingPort {
  getSubscriptionState(purchaseToken: string): Promise<PlaySubscriptionInfo>;
}
