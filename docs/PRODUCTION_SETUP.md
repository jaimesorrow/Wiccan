# Wiccan production setup

Wiccan's Android application is native Kotlin with Jetpack Compose. TypeScript
under `functions/` runs on Firebase's servers; it is not a web wrapper for the
Android UI. Production uses application ID `com.jaimesorrow.wiccan` and Firebase
project `wiccan-android`.

Code validation cannot establish that an account is connected, an unsigned
bundle is uploadable, or a draft lesson is approved for subscribers. Complete
the account steps below before a production rollout.

## Set up Google Play Billing

Sign into [Google Play Console](https://play.google.com/console) and
[Firebase Console](https://console.firebase.google.com/project/wiccan-android/overview)
with the owner account. If Wiccan has no Play listing yet, choose **Create app**:

| Field | Value to use |
| --- | --- |
| App name | Wiccan |
| Default language | English (United States), if this is your launch language |
| App or game | App |
| Free or paid | Free download, with premium subscriptions purchased inside the app |
| Android package on first upload | `com.jaimesorrow.wiccan` |
| Firebase project | `wiccan-android` |

Complete Google's declarations in the owner account and save the app. Account
registration/verification or a registration fee may be required if you do not
already have a Play developer account. Your browser sign-in is separate from
the workspace's administrative API credentials; never copy passwords, MFA codes
or browser cookies into this repository or chat.

The Android purchase flow uses Google Play Billing for digital lesson access.
The product IDs are `wiccan_premium_monthly` and `wiccan_premium_yearly`.
Activate the products/base plans in Play Console, grant the Firebase Functions
runtime service account Android Publisher access, and configure RTDN as described
in [FIREBASE_SETUP.md](FIREBASE_SETUP.md). The app displays Google's localized
price for the monthly recurring plan; a price in documentation is not a live
Play product configuration.

Purchase ownership is stored using SHA-256 purchase-token and account hashes to
prevent a token from granting access to multiple accounts. These pseudonymous
anti-fraud bindings can remain after account deletion; raw tokens and account
profiles are removed by deletion. Include this limited retention in the reviewed
privacy policy rather than promising removal of every record.

1. Open [Google Play Console](https://play.google.com/console), complete developer
   account verification, and create/select **Wiccan**. Complete its merchant
   payments profile and payout/bank information in Play Console. Upload the signed
   production AAB to an internal test release so its package is registered as
   `com.jaimesorrow.wiccan`. Package names are permanent after registration.
2. Under the app's **Monetize with Play → Products → Subscriptions**, create
   `wiccan_premium_monthly`. Add an **auto-renewing** base plan (for example ID
   `monthly`) with a **one-month** billing period, set its supported countries and
   prices, and activate it. Configure a single eligible-new-subscriber offer with
   one free `P7D`/`P1W` phase followed by the monthly recurring phase. The app
   prefers that exact seven-day offer when Play reports the account eligible and
   otherwise selects the regular monthly plan without promising a trial. The
   previous planning price is USD 5.99/month; choose/confirm the actual base price
   in Play Console. Google's returned localized price and eligibility are
   authoritative in the app.
3. If offering annual subscriptions, create `wiccan_premium_yearly` with an
   auto-renewing **one-year** base plan (for example ID `yearly`). The previous
   planning price is USD 39.99/year. The backend/restoration accepts this product;
   the current paywall presents only the monthly purchase option. Do not advertise
   annual in-app checkout until its UI is implemented and tested.
4. In [Google Cloud](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com?project=wiccan-android),
   enable the **Google Play Android Developer API**. In the Firebase/Cloud Functions
   configuration, identify the actual service account used by the deployed billing
   functions. In Play Console **Users and permissions**, invite that service-account
   email, grant access to Wiccan, and grant the order/subscription permissions needed
   to view and manage purchases. These calls include acknowledgement and subscription
   cancellation. Do not grant other apps or account administration unnecessarily.
5. Set `ANDROID_PACKAGE_NAME=com.jaimesorrow.wiccan` in
   `functions/.env.wiccan-android` and deploy the backend using the intended runtime
   identity. It obtains credentials through ADC; do not deploy a local key-file
   path. Confirm Cloud Functions and Firestore are enabled and the project is on a
   billing plan supporting Functions deployment.
6. Configure **real-time developer notifications (RTDN)** in the app's
   **Monetization setup** to use `projects/wiccan-android/topics/play-rtdn`.
   Grant `google-play-developer-notifications@system.gserviceaccount.com` the
   **Pub/Sub Publisher** role on that topic. Deploy `handlePlayRtdn`, send the Play
   Console test notification, and confirm receipt in the function logs. A test
   notification confirms transport; a real test purchase confirms state updates.
7. Register the Play **app-signing** certificate SHA-256 in Firebase and configure
   production Play Integrity App Check. Register SHA-1 and enable Google Auth for
   Google sign-in, then download a refreshed production `google-services.json`.
   Keep the existing app ID and project selection.
8. Add your tester Google account under Play Console **License testing**, and to
   the app's internal-test tester list. Publish an internal release, open its
   opt-in URL on the device using that account, and install from Google Play.
   Purchase the monthly plan with a Google test payment method. Confirm the app
   changes to premium only after server verification, and check the matching
   server-owned `users/{uid}` and `subscriptions/{uid}` documents.
9. Test restore after reinstall, pending payment, verification retry, accelerated
   renewal, cancellation while access remains until expiry, payment decline/hold,
   RTDN, expiry and account deletion. A cancellation failure must leave the account
   available for retry so auto-renew cannot continue silently after deletion.

Use Google's [subscription setup guide](https://support.google.com/googleplay/android-developer/answer/140504)
and [billing test guide](https://developer.android.com/google/play/billing/test).
These are account actions; configuring the repository does not create products,
set payout details or authorize the Play API.

## Store listing draft

**App name:** Wiccan

**Short description (72 characters):**

> Daily lessons, spells, and folklore for a thoughtful spiritual practice.

**Full description draft:**

> Build a thoughtful daily practice with Wiccan. Explore original lessons, spells
> and ritual studies inspired by historical folklore and source material, with
> careful distinctions between historical belief, modern symbolism and personal
> reflection.
>
> Read one selected entry each day and revisit delivered entries in your Book of
> Shadows. Lessons can include an introduction, reflection question and optional
> practice. Safety notes accompany material that needs extra care.
>
> Choose an appearance that suits you, including themes that follow the seasons.
> Optional daily reminders help you return to your practice.
>
> Free accounts receive daily content from the free pool. A Wiccan Premium
> subscription adds access to the premium pool and a home-screen widget. The
> subscription price and billing terms are shown by Google Play before purchase.
> Manage or cancel your subscription in Google Play.
>
> Wiccan's content is educational and reflective. Ritual and folklore are not
> medical treatments and do not guarantee supernatural outcomes.

Review the description against your approved launch content and actual enabled
features before submitting it. Add genuine device screenshots, a 512×512 app
icon, a 1024×500 feature graphic and a monitored support email. Complete Data
Safety, content rating, target audience and reviewer app access using
[PLAY_CONSOLE_READINESS.md](PLAY_CONSOLE_READINESS.md).

## The 200 lesson entries

The canonical seed contains 200 documents whose `type` is `lesson`, alongside
the existing 59 spells and 37 potions (296 content documents total). All seed
entries are drafts. The 146 companion lessons added during this work are newly
authored drafts requiring editorial and source review; structural validation is
not that review. Existing rights-review flags also require resolution before
publication. See [ANCIENT_LESSON_SOURCING.md](ANCIENT_LESSON_SOURCING.md).

Validate locally:

```bash
cd functions
npm ci
npm run seed-content
```

Give an authorized local/environment Google Cloud identity access to Firestore
in `wiccan-android`. Admin tools use Application Default Credentials (ADC); the
public Android `google-services.json` does not grant administrative access. On
your own workstation, `gcloud auth application-default login` can configure ADC.
In a managed environment, connect the intended Google Cloud identity through
its secure environment settings. Service-account key files belong outside the
repository and should be used only when necessary.

Then explicitly select the project:

```bash
npm run seed-content -- --commit --project wiccan-android
npm run verify-lessons -- --project wiccan-android
```

The importer creates missing seed IDs and preserves existing documents. The
verifier must succeed against the live project before claiming its lesson pool
is complete. It distinguishes draft and published lessons and reports missing
canonical IDs; other existing lessons remain intact. More than 200 live lessons
is possible when the project already has additional content.

To make one reviewed lesson eligible for delivery, use
`npm run approve-draft -- --project wiccan-android --contentId <id>` from
`functions/`. To validate every draft and publish only those that pass, use
`npm run publish-eligible -- --project wiccan-android --output <report>` first,
then repeat with `--commit` and a new report path. Drafts are not delivered to
users. Never batch-publish unreviewed content simply to meet a numeric target.

## Production release prerequisites

- Before deploying the restricted content-read rules to a project with legacy
  delivery history, run `npm run backfill-content-usage -- --project wiccan-android`
  from `functions/` to inspect the migration, then repeat with `--commit` to create
  missing delivery markers. It preserves existing markers. Archive reads require
  those markers, and clients cannot list the content collection.
- Enable Firebase Auth providers, Firestore, Functions billing and production
  Play Integrity App Check; deploy rules, indexes and Functions. Register Play
  signing certificates and refresh OAuth configuration for Google sign-in.
- Set `ANDROID_PACKAGE_NAME=com.jaimesorrow.wiccan` for deployed Functions.
  The deployed runtime service account uses ADC; a local credential-file path
  does not deploy credentials to Cloud Functions.
- Register the Play RTDN topic and grant the Google Play publisher permission.
- Add the upload signing secrets used by `.github/workflows/release.yml`, then
  build and verify a signed AAB. Increase `versionCode` for each new submission.
- Replace the placeholder values in `PolicyUrls.kt` with real privacy, terms,
  account-deletion and support URLs/address. Review and host the pages before
  completing Play's Data Safety form.
- Complete store assets, content rating, target audience, review access and
  applicable Play testing requirements. Test a Play license-tester purchase,
  renewal, restoration after reinstall, pending payment, cancellation, RTDN and
  deletion on a real device through the internal test track.
- Run the Room migration tests for existing installations and smoke-test Today,
  Archive, offline lesson sections, notifications and the widget.

Use [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) for the complete release checklist.
Use [PRODUCTION_VALIDATION.md](PRODUCTION_VALIDATION.md) for this change's measured
validation results and remaining limits.
