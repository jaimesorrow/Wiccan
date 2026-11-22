# Wiccan — project status

**As of 2026-11-19.** This page is the current snapshot; it replaces the
long repair-order log that used to live here (still available in git history
at `5d0742f`). Other docs record detail: [PRODUCTION_VALIDATION.md](PRODUCTION_VALIDATION.md),
[PRODUCTION_SETUP.md](PRODUCTION_SETUP.md), [PLAY_CONSOLE_READINESS.md](PLAY_CONSOLE_READINESS.md),
[RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) and [FIREBASE_SETUP.md](FIREBASE_SETUP.md).

**Release status: NOT READY for production. Ready to prepare a first
internal/closed-test upload once the blockers below marked "before first
upload" are cleared.**

## What the app is

Wiccan is a Kotlin / Jetpack Compose Android app (`com.jaimesorrow.wiccan`,
Firebase project `wiccan-android`) that delivers one piece of practical
witchcraft content per day — a lesson, spell or potion — on a subscription
(7-day trial for eligible new subscribers, then $5.99/month US base price).
Content is original writing grounded in vetted ancient/public-domain sources.
The operator is Jaime Sorrow, an individual developer in Alaska. Support and
privacy contact: `hello@wiccan.app`.

Stack: Compose + Hilt + Room, Firebase Auth / Firestore / Functions / FCM /
App Check / Analytics / Crashlytics, Play Billing 9.1.0, Cloud Functions on
Node 22. Build: Java 17, Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21,
`compileSdk` 37, `targetSdk` 36, `minSdk` 24, `versionCode` 1 / `versionName` 1.0.

## What is done

- Core app: server-side daily selection, Today / Grimoire / Settings / Paywall
  screens, Google and email sign-in, FCM reminders at local 9am, home-screen
  widget, in-app account deletion (cancels the active Play subscription first).
- Subscription access: required after onboarding, verified server-side
  (Play Developer API, RTDN, daily lapse re-check). See
  [SUBSCRIPTION_ACCESS.md](SUBSCRIPTION_ACCESS.md).
- Legal: native Terms, Privacy, Content & Safety and Potion Safety documents
  (policy version 5), a blocking acceptance gate enforced on the server,
  opt-in analytics/crash reporting, and matching public pages in `hosting/`.
- Brand identity (icons, full logo, six palettes); Play icon (512×512) and
  feature graphic (1024×500) are prepared in `brand/exports/google-play/`.
- Release plumbing: `release.yml` builds a signed `.aab` on a runner and
  refuses to publish an unsigned one; CI runs Android and Functions jobs,
  a 16 KB alignment check, and repository validation. All open PRs are merged
  and `main` CI is passing.
- Play-readiness audit: only `INTERNET` and `POST_NOTIFICATIONS` permissions,
  no ads, no leaked keys, `.gitignore` covers keystores and credentials.
- Domain: `wiccan.app` was bought on Cloudflare Registrar on 2026-11-19 and
  added to Firebase Hosting for `wiccan-android`. Policy URLs in the app,
  `hosting/` pages and docs now use `https://wiccan.app/<page>.html`.

## Google Play Console — current state

- The Play Console app for `com.jaimesorrow.wiccan` is a **draft**.
- **No app bundle has been uploaded** (no release in any track).
- **No store listing** (descriptions, screenshots, graphics, category).
- **All 11 App content declarations are pending** (e.g. privacy policy, ads,
  app access, content rating, target audience, data safety, account
  deletion). Draft answers
  are in [PLAY_CONSOLE_READINESS.md](PLAY_CONSOLE_READINESS.md) and
  [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md).
- The developer account is a **personal account**, so production access
  requires a **closed test with at least 12 testers opted in for 14
  continuous days** first. **0 testers so far**; the 14 days only start once a
  closed-test release is live and 12 testers have opted in. The realistic first
  upload is therefore to the internal or closed testing track.

## Release blockers

Before the first upload (internal / closed testing):

1. **No signed AAB.** `release.yml` needs four repository secrets:
   `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
   `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`. Create the upload keystore
   (steps in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md)), back it up outside
   the repo, add the secrets, then run the workflow.
2. **Policy URLs are not live yet.** The app and Play listing point to
   `https://wiccan.app/...`, which only resolves once the Cloudflare DNS
   records exist (A `199.36.158.100`, TXT `hosting-site=wiccan-android`; see
   [GITHUB_PAGES_DOMAIN.md](GITHUB_PAGES_DOMAIN.md)) and `hosting/` is deployed
   with `firebase deploy --only hosting`. Verify with `curl -I` before entering
   the URLs in Play Console. Tracked by
   [#162](https://github.com/jaimesorrow/Wiccan/issues/162).
3. **Play Console forms** — store listing and all App content declarations.

Before production (can overlap with the 14-day closed test):

4. **[#86](https://github.com/jaimesorrow/Wiccan/issues/86) — branded
   verification email.** Needs a production SMTP provider, `SMTP_URL` secret,
   `SMTP_FROM` (wiccan.app can be the verified sender domain, e.g.
   `noreply@wiccan.app`, with SPF/DKIM/DMARC records) and a deploy of
   `sendVerificationEmail`. Until then new accounts use Firebase's built-in
   verification email. Deploy the callable before shipping an Android build
   that invokes it.
5. **[#162](https://github.com/jaimesorrow/Wiccan/issues/162) — legal review.**
   Product/legal review of Terms, Privacy, Content & Safety and the new Potion
   Safety page, and the policy-version / renewed-acceptance decision.
6. **Firebase production deploy still unverified.** Firestore rules and
   indexes, Functions, App Check enforcement and the Firestore database itself
   have not been confirmed live for `wiccan-android` from this repo. The
   Firebase project may still be on Spark; Functions need Blaze.
7. **Content and legal review gaps.** The 296-entry seed (200 lessons, 59
   spells, 37 potions) is draft; source passages, citations and rights still
   need review and approval before anything is published to Firestore.
   Trademark review of the WICCAN name is also open.
8. **After the first upload:** copy the **Play App Signing** certificate
   SHA-1 (and SHA-256) from Play Console → Test and release → Setup → App
   signing into Firebase (Project settings → your Android app). Without it,
   Google sign-in and App Check fail for Play-distributed builds. Use
   `scripts/signing-fingerprints.sh` to print the debug/upload fingerprints.

Other open item: [#156](https://github.com/jaimesorrow/Wiccan/issues/156)
(review-thread cleanup from merged PRs; not a release blocker).

## Next steps, in order

1. Merge this cleanup PR.
2. Add the Cloudflare DNS records for `wiccan.app`, wait for Firebase to show
   the domain as connected, deploy `hosting/`, verify the policy URLs load.
3. Create the upload keystore, add the four `RELEASE_*` secrets, run the
   *Release bundle* workflow and download the signed `.aab`.
4. Upload it to **Internal testing** in Play Console (accept Play App Signing),
   then add the Play App Signing SHA-1/SHA-256 to Firebase and re-test Google
   sign-in from the Play-installed build.
5. Fill in the store listing and the 11 App content declarations (privacy
   policy URL `https://wiccan.app/privacy.html`, account deletion URL
   `https://wiccan.app/delete-account.html`).
6. Create a **Closed testing** release, recruit 12+ testers, and keep them
   opted in for 14 days.
7. In parallel: finish #86 (SMTP + deploy), #162 (legal review), the Firebase
   production deploy and verification, and the content review.
8. Apply for production access, then promote with a higher `versionCode`.

## Validation

Latest recorded automated validation (2026-11-15 and 2026-11-16, see
[BUILD_VALIDATION.md](BUILD_VALIDATION.md) and
[PRODUCTION_VALIDATION.md](PRODUCTION_VALIDATION.md)): `ktlintCheck`, 207
prod-debug unit tests, `:app:assembleProdDebug`, Functions build/lint and tests
(398 passed, 34 skipped on 2026-11-19), Firestore rules tests, and the hosted CI matrix. Not yet exercised:
a real-device pass, live Play Billing purchase, RTDN, and Google sign-in on a
Play-distributed build.
