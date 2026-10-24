# Subscription-only access

Wiccan has one subscription. New eligible Google Play subscribers get seven
days free, then pay $5.99 per month at the US base price. Google Play supplies
localized prices and determines trial eligibility. A trial is an active
subscription, with the same access as a paid period; it is not a separate free plan.

Navigation: sign up / log in → profile initialization → current Terms/Privacy
acceptance (the existing onboarding) → required subscription → Today / Grimoire /
Settings. Returning subscribers bypass purchase only after verification. There is
no skip button, free lesson, or second content tier. Links and restored navigation
state cannot mount the app shell before the root gate grants access.

The subscription screen keeps restore, account sign-out/deletion, subscription
management, legal documents and support available. Its account screen has no
lesson navigation. Canceling auto-renew normally retains access until Play's
verified expiry; expiration, hold, pause or revocation returns the app to the
subscription screen. An applicable Play grace period is still entitled. Delivery
history is preserved and becomes readable again on renewal.

## Enforcement

- `users/{uid}.tier` retains the legacy wire values: `premium` means verified
  access and `free` means inactive. They no longer represent two product plans.
- Access also requires a finite `subscriptionExpiresAtMillis` in the future and
  no account deletion in progress. Purchase verification, RTDN and reconciliation
  mirror the Play expiry onto the profile, guarded by the current purchase token.
- The callable checks access before same-day history and again before returning
  content. New assignments recheck in the Firestore transaction. Published
  content uses one pool, ignoring legacy `content.tier` values.
- Firestore requires the same active subscription for content and history reads.
  The owner can still read their account profile to restore and manage access.
  All client entitlement writes remain denied.
- Android waits for a server snapshot; disk cache alone cannot grant access.
  A deadline timer closes the shell and hides the daily Room cache at expiry.
  Offline/unverified accounts stay outside the app. Foreground entry reconciles
  purchases. Pending purchases do not unlock anything.
- The launcher widget opens the gated app without embedding lesson text. A
  launcher may retain its last rendering after the subscription expires.

## Google Play configuration required before release

Open [Play Console](https://play.google.com/console/) → select Wiccan →
Monetize with Play → Products → Subscriptions → `wiccan_premium_monthly`.

1. Configure/activate the monthly auto-renewing base plan, with a US base price
   of $5.99. Use a customer-facing name such as "Wiccan subscription", keeping
   the existing product ID. Set intended countries and review localized prices/taxes.
2. Add/activate a Google Play eligibility-controlled offer for new subscribers:
   one free phase lasting seven days, followed directly by the monthly base plan.
   The client accepts `P7D` or `P1W`, zero cost, one finite cycle (or a single
   nonrecurring trial phase), then `P1M` infinite renewal.
   It rejects other introductory phases and a USD renewal price other than $5.99.
3. Make the product and offer available to the app's test/release track. Keep the
   existing product IDs; do not rename them or create a second free plan. Legacy
   yearly purchases remain restorable but are not offered for new checkout.
4. Verify the Play API service-account permissions, RTDN topic, acknowledgement,
   license testers and package name (`com.jaimesorrow.wiccan`) using the existing
   [Firebase setup guide](FIREBASE_SETUP.md).
5. Test a new eligible account and a returning/ineligible account through Play.
   The displayed offer and checkout must agree. Ineligible accounts see the paid
   monthly offer without a trial promise. A changed offer requires a refresh;
   the client never silently replaces the displayed trial with an immediate charge.

References: [subscription offers](https://developer.android.com/google/play/billing/subscriptions),
[Billing integration](https://developer.android.com/google/play/billing/integrate),
[subscription lifecycle](https://developer.android.com/google/play/billing/lifecycle/subscriptions).
See also [Play Console subscription setup](https://support.google.com/googleplay/android-developer/answer/140504)
and the [pricing-phase API](https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.PricingPhase).

## Coordinated rollout

Code changes alone do not configure Play or deploy Firebase. Before publishing:

1. Configure and sandbox-test the Play offer above.
2. Deploy the Functions changes to the intended project after review, including
   Terms version 3. Reconcile existing verified subscribers so their profiles have
   an authoritative expiry before deploying the stricter rules. The foreground
   restore path can backfill a missing expiry; never invent expiry dates or grant
   access merely because an old profile says `premium`.
3. Deploy the subscription-required Firestore rules and release the matching
   Android build together. Terms version 3 is a material change; plan the app
   update and acceptance rollout for existing clients. History and purchase-token
   ownership records must remain intact.
4. Test sign-up → onboarding → paywall; Play cancellation and Back; trial purchase;
   pending payment; restore/reinstall; account switch; expiry while open; hold,
   revocation and renewal; offline launch; direct Today/Grimoire/widget links;
   cached daily content; account deletion and legal/support access before payment.
5. Confirm both callable and direct Firestore reads deny inactive accounts,
   including already delivered legacy content and history. Confirm renewal reveals
   existing history without a repeated daily assignment.

Live Console configuration, real Play sandbox purchases, deployed rules and
production subscriber reconciliation require authenticated operator access.
