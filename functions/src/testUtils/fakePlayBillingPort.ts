import { PlayBillingPort, PlaySubscriptionInfo, PlaySubscriptionState } from "../playBillingPort";

/** In-memory stand-in for the Play Developer API, keyed by purchase token. */
export class FakePlayBillingPort implements PlayBillingPort {
  private readonly responses = new Map<string, PlaySubscriptionInfo>();
  acknowledgedTokens: { purchaseToken: string; productId: string }[] = [];
  canceledTokens: { purchaseToken: string; productId: string }[] = [];

  respondWith(
    token: string,
    state: PlaySubscriptionState,
    productIds: string[],
    options: {
      expiryTimeMillis?: number | null;
      autoRenewing?: boolean;
      acknowledged?: boolean;
    } = {},
  ): void {
    this.responses.set(token, {
      state,
      productIds,
      expiryTimeMillis: options.expiryTimeMillis ?? null,
      autoRenewing: options.autoRenewing ?? false,
      acknowledged: options.acknowledged ?? false,
    });
  }

  async getSubscriptionState(purchaseToken: string): Promise<PlaySubscriptionInfo> {
    const info = this.responses.get(purchaseToken);
    if (!info) {
      throw new Error(`No fake response configured for token '${purchaseToken}'.`);
    }
    return info;
  }

  async acknowledgePurchaseIfNeeded(purchaseToken: string, productId: string): Promise<void> {
    this.acknowledgedTokens.push({ purchaseToken, productId });
  }

  async cancelSubscription(purchaseToken: string, productId: string): Promise<void> {
    this.canceledTokens.push({ purchaseToken, productId });
  }
}
