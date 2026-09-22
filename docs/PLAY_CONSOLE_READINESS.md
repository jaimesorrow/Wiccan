# Play Console readiness package

Everything here is grounded in what the code actually does — checked
against the manifest, the Firestore/Functions data model, and the billing
implementation — not a generic template. Where a Play Console field
requires a business/legal judgment call (content rating, target audience),
this says so instead of guessing an answer on your behalf.

## Data Safety section

Data types this app actually collects or transmits, per the real code:

| Data type | Collected? | Where / why |
|---|---|---|
| Email address | Yes | Firebase Auth (Google sign-in or email/password) — account identity only |
| User IDs | Yes | Firebase Auth uid, used as the Firestore document key everywhere |
| Purchase history | Yes | Google Play Billing subscription state, verified server-side (`verifyPurchase`) and stored in `subscriptions/{uid}` for entitlement/audit |
| Device or other IDs | Yes | FCM registration token, stored per-device in `users/{uid}/devices` for push delivery |
| App activity (content viewed) | Yes | `userContentHistory` — which spell/potion/lesson was delivered to which user on which day |
| Precise or approximate location | No | Not requested, not read; only a device's own IANA time zone string (`America/Denver`, etc.) is stored, which is not a location data type |
| Contacts, photos, videos, audio, financial info (beyond purchase), health/fitness | No | Never requested or read |

Notes for the Data Safety form:
- Data is encrypted in transit (Firebase's TLS by default) — check that box.
- Account deletion: `deleteUserData`/the in-app Settings flow removes profile, history, devices, and the subscription record, then the Auth account itself (see `functions/src/deleteAccount.ts`). Google's Data Safety form also asks for a **web page** describing the deletion process reachable without installing the app — `PolicyUrls.ACCOUNT_DELETION_URL` is the placeholder for that; **it needs a real hosted page before submission**, this repo doesn't host one.
- Purpose for each data type collected above: account functionality (email, user ID), app functionality (device ID for push, content history so the daily-selection logic doesn't repeat itself), and "other" for purchase history if Play's taxonomy doesn't offer a closer fit — confirm against the current form, Google revises these categories periodically.

## Content rating questionnaire

The app's subject matter — spells, potions, "witchcraft" framed as a
lifestyle/spirituality practice — will likely trigger Play's occult/
supernatural-themes question in the content rating questionnaire. This
repo can't answer that questionnaire for you: it's a business/legal
judgment about how the app frames itself (entertainment vs. genuine
practice), and Google's rating bodies (IARC etc.) make the final call
based on your answers. Two things worth confirming before you fill it out:
- `claudeDraftGenerator.ts`'s generation prompt already forbids medical/
  health claims, unsafe ingestion, hazardous materials, and guaranteed
  supernatural outcomes (Session 18/29) — useful context for how you
  describe the app's content in the questionnaire.
- No user-generated content, no chat/messaging between users, no ads —
  simplifies several other questionnaire branches.

## Store listing requirements

- [ ] App icon (512×512 PNG) and feature graphic (1024×500) — not part of this repo; the in-app launcher icon (`ic_launcher_foreground.xml`/`ic_launcher_background.xml`) is a placeholder vector, not a store-ready asset.
- [ ] At least 2 phone screenshots (Play requires this minimum).
- [ ] Short description (≤80 chars) and full description.
- [ ] Privacy Policy URL — `PolicyUrls.PRIVACY_POLICY` is a placeholder (`https://wiccan.app/privacy`); needs a real hosted page.
- [ ] Support email — `PolicyUrls.SUPPORT_EMAIL` is a placeholder (`support@wiccan.app`); needs a real, monitored address wired to the same value used in Settings' Contact Support link.
- [ ] Terms of Service URL — `PolicyUrls.TERMS_OF_SERVICE` placeholder, same treatment.
- [ ] Category and target audience/age selection — business decision, not inferable from code.

## Subscriptions / Monetization setup (Play Console side, not code)

- [ ] Create `wiccan_premium_monthly` at **$5.99/month** and `wiccan_premium_yearly` at **$39.99/year** in Play Console. The backend allow-lists only these two product IDs for premium entitlement. The current paywall launches the monthly product; yearly purchase UI can be exposed once the yearly product is active in Play.
- [ ] Enable Real-time developer notifications (Monetization setup) pointed at the `PLAY_RTDN_TOPIC` Pub/Sub topic (`play-rtdn` by default), and grant Play's publisher service account permission to publish to it — `handlePlayRtdn` (Session 23) is written and tested against fakes, but has no live RTDN traffic to react to until this is done.
- [ ] Link a Google Play Developer API service account with access to this app's listing, and put its key path in `GOOGLE_APPLICATION_CREDENTIALS` for whichever environment needs to call `verifyPurchase`/`recheckLapsedSubscriptions` (see `functions/.env.example`).

## Cross-reference

See `docs/RELEASE_CHECKLIST.md` for the technical (signing, minification,
versioning) side of preflight, and the README's Environments section for
the dev/staging/production project separation this all assumes.


## Production Firebase config

- The production Android identity is `com.jaimesorrow.wiccan` in Firebase project `wiccan-android`.
- `app/src/prod/google-services.json` contains that production project's base Android config.
- The supplied Firebase config currently has no OAuth client entries. Google Sign-In therefore remains unavailable until the Play App Signing SHA certificates are added in Firebase/Google Cloud, Google Authentication is enabled, and a refreshed config contains the web OAuth client. Email/password auth and the other Firebase services do not require that generated web client resource.
