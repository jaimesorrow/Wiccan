# Subscription-only access

Wiccan has two subscription plans: monthly and yearly. Eligible new Google Play
subscribers get a free trial at the length shown at checkout (7 days on the
monthly plan, 14 days on the yearly plan), then pay the plan they chose
($5.99 per month or $39.99 per year at the US base price). Google Play supplies
localized prices and determines trial eligibility. A trial is an active
subscription, with the same access as a paid period; it is not a separate free plan.

Navigation: sign up / log in → email verification → first/last name and unique username → current Terms/Privacy
acceptance → the app shell. The daily lesson stays locked until a verified trial, subscription, or closed-test grant is active. Tapping the locked lesson opens the subscription screen, where the trial length for the chosen plan is shown.
Email-password accounts are sent a Firebase verification email after registration; the verification
screen supports re-sending and explicit rechecking. Google sign-in uses Firebase verified provider credentials.
User names are stored server-side via the App Check-protected completeOnboardingProfile callable.
Forgot-password is a dedicated screen using Firebase password reset email, not an in-app password form. Returning subscribers bypass purchase only after verification. There is
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

Returning subscribers whose profiles predate the required name fields will be routed to Complete Profile.
Prepare migration communications before enforcing this gate. Firestore and the daily-content callable
also deny access for unverified email tokens. Deploy the Functions, rules and compatible Android app
as a coordinated rollout after testing.

## Google Play configuration required before release

Open [Play Console](https://play.google.com/console/) → select Wiccan →
Monetize with Play → Products → Subscriptions.

1. Monthly product `wiccan_premium_monthly`: auto-renewing base plan, US base
   price $5.99. New-subscriber offer: one free phase lasting seven days
   (`P7D` or `P1W`), then the monthly base plan.
2. Yearly product `wiccan_premium_yearly`: auto-renewing base plan, US base
   price $39.99. New-subscriber offer: one free phase lasting fourteen days
   (`P14D` or `P2W`), then the yearly base plan.
3. The client shows and sells the price Play returns (not a pinned USD amount).
   Trial eligibility, localized price, billing period, and trial length are
   shown at checkout. Make both products and their offers available to the
   app's test/release track. Keep the existing product IDs; do not rename them
   or create a second free plan.
4. Verify the Play API service-account permissions, RTDN topic, acknowledgement,
   license testers and package name (`com.jaimesorrow.wiccan`) using the existing
   [Firebase setup guide](FIREBASE_SETUP.md).
5. Test a new eligible account and a returning/ineligible account through Play.
   The displayed offer and checkout must agree. Ineligible accounts see the paid
   offer without a trial promise. A changed offer requires a refresh;
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

## Closed-test access

Operators grant a time-limited premium entitlement without a Play purchase. The grant sets `users/{uid}.tier` to `premium` and `subscriptionExpiresAtMillis` to the test end. It does not write a purchase token, so Play reconciliation does not treat it as a subscription, and the client cannot set either field.

Dry-run:

```
cd functions
npm run grant-closed-testers -- --ends 2026-12-31 --emails tester@example.com
```

Write, with Application Default Credentials for `wiccan-android`:

```
npm run grant-closed-testers -- --project wiccan-android --commit --ends 2026-12-31 --emails tester@example.com,second@example.com
```

`--ends` is a UTC calendar date or epoch milliseconds. Access ends when `subscriptionExpiresAtMillis` is no longer in the future. Existing profile fields are merged, not replaced.

Live Console configuration, real Play sandbox purchases, deployed rules and
production subscriber reconciliation require authenticated operator access.
