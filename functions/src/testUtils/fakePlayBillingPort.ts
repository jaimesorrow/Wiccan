import { PlayBillingPort, PlaySubscriptionInfo, PlaySubscriptionState } from "../playBillingPort";

/** In-memory stand-in for the Play Developer API, keyed by purchase token. */
export class FakePlayBillingPort implements PlayBillingPort {
  private readonly responses = new Map<string, PlaySubscriptionInfo>();

  respondWith(token: string, state: PlaySubscriptionState, productIds: string[]): void {
    this.responses.set(token, { state, productIds });
  }

  async getSubscriptionState(purchaseToken: string): Promise<PlaySubscriptionInfo> {
    const info = this.responses.get(purchaseToken);
    if (!info) {
      throw new Error(`No fake response configured for token '${purchaseToken}'.`);
    }
    return info;
  }
}
