import { androidpublisher, auth } from "@googleapis/androidpublisher";
import { PlayBillingPort, PlaySubscriptionInfo, PlaySubscriptionState } from "./playBillingPort";

const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";

function mapSubscriptionState(raw: string | null | undefined): PlaySubscriptionState {
  switch (raw) {
    case "SUBSCRIPTION_STATE_ACTIVE":
      return "active";
    case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
      return "in_grace_period";
    case "SUBSCRIPTION_STATE_CANCELED":
      return "canceled";
    case "SUBSCRIPTION_STATE_ON_HOLD":
      return "on_hold";
    case "SUBSCRIPTION_STATE_PAUSED":
      return "paused";
    case "SUBSCRIPTION_STATE_PENDING":
      return "pending";
    case "SUBSCRIPTION_STATE_EXPIRED":
      return "expired";
    default:
      return "unspecified";
  }
}

/**
 * Verifies subscriptions server-side via the Google Play Developer API
 * (subscriptionsv2), per Google's guidance to never trust a client-
 * reported purchase state. Requires the Cloud Functions runtime service
 * account to be linked as an authorized API access account for this app
 * in Play Console — an operational setup step, not something this code
 * can do on its own.
 *
 * Uses the scoped `@googleapis/androidpublisher` package rather than the
 * `googleapis` umbrella package: the latter bundles type definitions for
 * every Google API and made a single ts-jest run of this file take over
 * ten minutes (and once OOM-killed a worker) in this project's test suite.
 */
export class GooglePlayBillingRepository implements PlayBillingPort {
  constructor(private readonly packageName: string) {}

  async getSubscriptionState(purchaseToken: string): Promise<PlaySubscriptionInfo> {
    const authClient = await auth.getClient({ scopes: [ANDROID_PUBLISHER_SCOPE] });
    const client = androidpublisher({ version: "v3", auth: authClient });

    const response = await client.purchases.subscriptionsv2.get({
      packageName: this.packageName,
      token: purchaseToken,
    });

    const productIds =
      response.data.lineItems
        ?.map((item) => item.productId)
        .filter((id): id is string => Boolean(id)) ?? [];

    return {
      state: mapSubscriptionState(response.data.subscriptionState),
      productIds,
    };
  }
}
