# AdMob (ads for non-subscribers)

Status: **draft, test ads only.** Every flavor uses Google's public sample IDs,
which always serve test ads and never earn or charge anything:

| Setting | Value (test) | Where |
|---|---|---|
| AdMob app ID | `ca-app-pub-3940256099942544~3347511713` | `app/build.gradle.kts` → `admobIds(...)` per flavor → manifest placeholder `admobAppId` |
| Grimoire adaptive banner unit | `ca-app-pub-3940256099942544/9214589741` | same → `BuildConfig.ADMOB_GRIMOIRE_BANNER_UNIT_ID` |

## What the app does

- **Who sees ads:** only users without an active subscription. The gate reads the
  server-verified tier from `SubscriptionRepository.observeTier()` (monthly or
  yearly Premium, including a trial, hides ads at once). A FREE tier must hold for
  3 seconds before an ad may load, so a subscriber's cached FREE tier on a cold
  start never flashes an ad (`ads/AdVisibility.kt`, tested in `AdVisibilityTest`).
- **Where:** one adaptive anchored banner at the bottom of the Grimoire screen,
  above the navigation bar, in a midnight frame with a thin gold top rule and a
  plain "Advertisement" label. Nothing renders until an ad loads (offline or no
  fill leaves no gap). No ads in lessons, rituals, the paywall, Settings, account
  deletion, legal pages or verification. No interstitials, no app-open ads.
- **Consent:** Google's User Messaging Platform runs before the Mobile Ads SDK
  initializes or any ad is requested (`ads/UmpAdConsentController.kt`). Settings
  shows a **Privacy choices** row only when UMP says it is required.
- **Advertising ID:** the manifest now declares
  `com.google.android.gms.permission.AD_ID` explicitly. Nothing removed it before
  (Firebase Analytics already merged it in), but the app told Google that all ad
  consent signals were denied because it showed no ads. Firebase Analytics' ad
  signals stay denied (analytics is never used for ads); AdMob consent is
  separate, through UMP.

## Owner steps before real ads (not done by any bot)

1. Create the AdMob account (admob.google.com) with the Play developer identity,
   add the Wiccan app (package `com.jaimesorrow.wiccan`), and link it to Firebase
   project `wiccan-android` (AdMob → App settings → Link to Firebase).
2. Create one **Adaptive banner** ad unit (e.g. "Grimoire banner").
3. Replace the two test IDs in the `prod` flavor's `admobIds(...)` call with the
   real app ID and ad unit ID. Keep `dev`/`staging` on test IDs. Never tap your
   own live ads; add your phone as a test device in AdMob.
4. AdMob → Privacy & messaging: publish a **European regulations (GDPR)** message
   and a **U.S. state regulations** message for the app. UMP shows nothing until
   these exist.
5. Publish `https://wiccan.app/app-ads.txt` with the line AdMob gives you
   (`google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0`), and make sure
   the Play listing's developer website is `https://wiccan.app`.
6. Play Console (see below), then decide whether the ad change to the privacy
   policy needs a new policy **version 7** (re-acceptance in the app; requires
   the Android, Functions, Firestore rules and emulator script version bump
   together - see `scripts/release-preflight.sh`).

## Play Console changes (owner)

- **App content → Ads:** "Yes, my app contains ads."
- **App content → Advertising ID:** Yes, uses the advertising ID; purposes:
  Advertising or marketing, Analytics, Fraud prevention, security and compliance.
- **Data safety** (add to the existing answers):
  - *Device or other IDs* (advertising ID): collected **and shared** with Google
    (AdMob) for **Advertising or marketing**, Analytics, Fraud prevention.
  - *App activity → App interactions* (ad views/taps): collected and shared for
    Advertising or marketing, Analytics.
  - *App info and performance → Diagnostics*: collected and shared for Analytics,
    Fraud prevention.
  - *Location → Approximate location* (derived from IP by AdMob): collected and
    shared for Advertising or marketing, Fraud prevention.
  - Not optional for ad delivery to free users, but personalization follows UMP
    consent; data is encrypted in transit.
- **Store listing:** the "Contains ads" label appears automatically after the Ads
  declaration.
