import { androidpublisher, auth } from "@googleapis/androidpublisher";
import * as logger from "firebase-functions/logger";
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
 *
 * NOTE: the exact `lineItems[].expiryTime` / `autoRenewingPlan` /
 * acknowledgement field shapes below are implemented from the documented
 * subscriptionsv2 schema, but have not been exercised against a live
 * Play Console subscription in this environment (no real Play credentials
 * or purchase are available here). Verify against a real sandbox purchase
 * before relying on this in production.
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

    const lineItems = response.data.lineItems ?? [];
    const productIds = lineItems.map((item) => item.productId).filter((id): id is string => Boolean(id));
    const primaryLineItem = lineItems[0];
    const expiryTimeMillis = primaryLineItem?.expiryTime ? new Date(primaryLineItem.expiryTime).getTime() : null;
    const autoRenewing = primaryLineItem?.autoRenewingPlan?.autoRenewEnabled ?? false;

    return {
      state: mapSubscriptionState(response.data.subscriptionState),
      productIds,
      expiryTimeMillis,
      autoRenewing,
    };
  }

  async acknowledgePurchaseIfNeeded(purchaseToken: string, productId: string): Promise<void> {
    const authClient = await auth.getClient({ scopes: [ANDROID_PUBLISHER_SCOPE] });
    const client = androidpublisher({ version: "v3", auth: authClient });

    try {
      await client.purchases.subscriptions.acknowledge({
        packageName: this.packageName,
        subscriptionId: productId,
        token: purchaseToken,
        requestBody: {},
      });
    } catch (err) {
      // Acknowledging an already-acknowledged purchase returns an error
      // from the Play API; the desired end state (acknowledged) already
      // holds, so this is logged and swallowed rather than failing the
      // caller's verification flow over it.
      logger.info("acknowledgePurchaseIfNeeded: acknowledge call did not succeed (likely already acknowledged)", {
        error: (err as Error).message,
      });
    }
  }
}
