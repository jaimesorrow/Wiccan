# Wiccan — repair-order status

For the latest Play Billing hardening, structured Android lessons, 200-lesson
manifest and measured validation, see [PRODUCTION_VALIDATION.md](PRODUCTION_VALIDATION.md)
and [PRODUCTION_SETUP.md](PRODUCTION_SETUP.md). The notes below record earlier
repair work and do not establish live production account setup.

Honest status against the repair order this branch worked through. PASS
means built and verified by this repo's own automated checks. FIXED means
a real gap was found and closed in this pass. BLOCKED means it needs
something this sandbox genuinely cannot provide (real credentials, a real
project, a device/emulator) — never worked around with a placeholder
pretending to be real.

**Revision note**: an earlier version of this file claimed "every gap
that isn't [a real Firebase project/keystore/Play listing] has been
closed." An external audit of the codebase correctly caught that this was
an overclaim — several real, code-level gaps were still open despite not
depending on external infrastructure. This revision corrects that: the
audit's findings are listed below, and every one of them either has been
fixed (with the fix verified the same way as everything else in this
file) or is named explicitly as still open, with a reason.

## Core app (pre-existing, prior sessions)

| Area | Status |
|---|---|
| Daily content selection (deterministic, tier-filtered, transactional) | PASS |
| Room cache + DAO | PASS |
| Today/Archive/Settings/Paywall screens (MVI) | PASS |
| Auth (Credential Manager Google sign-in + Firebase email/password) | PASS |
| Billing (Play Billing Library 9.1.0, server-verified entitlement) | IN REVIEW — local and hosted Android build/test/lint PASS on 2026-10-02; live Play validation pending |
| Content generation pipeline (Claude-backed, admin-only) | PASS |
| Home-screen widget | PASS |
| Shared AppResult/AppError error taxonomy | PASS |
| Full Nyxwell → Wiccan rebrand (including applicationId) | PASS |
| compileSdk 37 / targetSdk 36 baseline | PASS — local debug and release builds |

## This pass's repair order

| Area | Status |
|---|---|
| Navigation Compose app shell + real `RootState` | PASS |
| Server-controlled profile mutations (device token, time zone, terms) | PASS |
| Versioned Terms/Privacy acceptance gate | PASS |
| Subscription lifecycle hardening (entitlement fix, RTDN, acknowledgement) | PASS |
| Category-organized Library | PASS |
| Multi-device FCM (per-device registration, no cross-device clobbering) | FIXED |
| Content safety gate + duplicate-title prevention | FIXED |
| Account deletion completeness (devices subcollection + subscription record were orphaned) | FIXED |
| Room migration test infra + Android backup safety | FIXED |
| Environment separation (`.firebaserc`, `.env.example`) + admin CLI project-ID guard | FIXED |
| Technical release preflight + Play Console readiness | DOCUMENTED — `docs/RELEASE_CHECKLIST.md`, `docs/PLAY_CONSOLE_READINESS.md` |

## Audit remediation round (this revision)

An external audit reviewed the codebase directly (not just this file) and
found the following. Each is now addressed as noted:

| Finding | Resolution |
|---|---|
| CI never ran Cloud Functions build/test/lint | **FIXED** — CI split into `android` and `functions` jobs; `functions` job verified green on its first real run |
| No dependency-update automation | **FIXED** — `.github/dependabot.yml` (npm, gradle, github-actions) |
| Account deletion didn't cancel the live Play subscription (local record deleted, but Google kept billing) | **FIXED** — `cancelActiveSubscription` calls `purchases.subscriptions.cancel` before the local record is erased |
| App Check's debug provider shipped inside release builds | **FIXED** — split into build-variant source sets (`src/debug`, `src/release`); verified absent from the release `.aab`'s dex strings |
| Notifications fired once at a fixed UTC instant, not per-device local 9am | **FIXED** — hourly job now filters to devices whose local time currently reads 9am (`getUserLocalHour`); see the documented hourly-poll-vs-exact-dispatch limitation below |
| No automated 16KB/ABI native-library check | **FIXED** — `scripts/check-16kb-alignment.sh` runs in CI against the release `.aab`; all eight libraries pass on the 2026-10-02 Billing 9.1.0 bundle |
| No disaster recovery / incident response / maintenance docs | **FIXED** — `docs/DISASTER_RECOVERY.md`, `docs/INCIDENT_RESPONSE.md`, `docs/MAINTENANCE.md` |
| Billing Library version | **9.1.0 on main** with Java 17, Gradle 9.8.0, AGP 9.4.1 and Kotlin 2.4.20. Local Android matrix passes; live Play validation remains pending. |
| Device registration schema is minimal (token + time zone + updatedAt; no `notificationsEnabled`/`platform`/`createdAt`/`lastSeenAt`) | **FIXED** — added all four: `createdAt`/`platform` written on first registration, `notificationsEnabled` is a real per-device toggle with a Settings switch (DataStore-backed locally, since Firestore rules give the client no read access to its own device doc), and `sendDailyNotifications` now skips opted-out devices. |
| Visual theme is still near-black (`#0A0810`-direction) / generic serif+sans typography, not a distinct final direction | **IN REVIEW** — PR #24 adds six seasonal palettes and typography on main. The local build, tests, lint and six JVM palette specimens pass on 2026-10-02; actual fonts, transitions and app/device visual validation remain required. |
| No Android product flavors for dev/staging/prod (environment separation is `.firebaserc` + `.env` only) | **FIXED** — `dev`/`staging`/`prod` product flavors added (`app/build.gradle.kts`). Prod has the registered client config for `com.jaimesorrow.wiccan` at `app/src/prod/google-services.json`; that source-set config takes precedence over CI's fake root fallback. Dev/staging still need their own configurations. |
| No runtime kill switches (`dailyContentEnabled`, `notificationsEnabled`, etc.) | **FIXED** — `systemConfig/global` (Firestore, no client access) with both flags, fail-open by default. `getTodayContent` and the hourly notification job both check it before doing anything. |
| Launch content pool needs approved entries | **Still open** — four curated source records and a 150-entry draft seed are on main; drafts need human editorial/rights review and approval before delivery. |
| Account deletion + active subscription | **FIXED** (see above) — this was the same finding as the billing-cancellation gap. |

## Known remaining gaps

- **Visual/typography redesign** — local build/test/lint and six JVM palette specimens pass; real-device app screens, transitions and actual font appearance still need review before release.
- **Billing runtime validation** — 9.1.0 is integrated with the modernized toolchain and local unit tests pass; live purchases, acknowledgement, restore and RTDN still need Play/Firebase verification.
- `PolicyUrls` (Terms, Privacy, account deletion) and `SUPPORT_EMAIL` are placeholders — no real hosted pages exist yet.
- `WiccanDatabaseMigrationTest` compiles clean but has never run against a real SQLite engine — no emulator here.
- `handlePlayRtdn`, `verifyPurchase`'s field shapes, and the `users/{uid}/devices` collection-group query have never been exercised against a real Play Console / Firebase project.
- `dev`/`staging` product flavors exist but need their own real `google-services.json` (from their own real Firebase projects) before they'll build.
- The production Firebase client config identifies `wiccan-android` and includes its verified web OAuth client ID. Build/signing-certificate matching, device sign-in, live Firebase/Play flows and release setup remain unverified.

## Firebase and Google Play console verification — 2026-10-06 UTC

- Firebase's Android registration matches `com.jaimesorrow.wiccan`; SHA-1
  and SHA-256 certificates are registered. The build and Play App Signing
  certificates still need to be matched to those registrations.
- Google and Email/Password authentication are enabled. The registered
  public web OAuth client ID is included in the production config.
  Android resource generation and device sign-in have not been run for
  this configuration update.
- Google Play is linked to Firebase for App Distribution, Crashlytics and
  Google Analytics. The Wiccan Play app remains **Draft**.
- Cloud Firestore has not been created: the console shows **Create database**.
  The repository's seed has 150 draft entries: **54 lessons, 59 spells and
  37 potions**. These are not an uploaded or published 200-lesson collection.
- The Firebase project is on **Spark**. Cloud Functions deployment still
  requires the billing-plan setup in [FIREBASE_SETUP.md](FIREBASE_SETUP.md).

## Latest automated validation

The 2026-10-02 branch integration run uses Java 17, Gradle 9.8.0, AGP 9.4.1,
Kotlin 2.4.20 and Billing 9.1.0, with `compileSdk` 37 / `targetSdk` 36.
Android unit tests pass: 86 tests across 18 suites. Ktlint plugin 14.2.0
restores Android source checks after AGP 9; the pinned 1.0.1 engine passes
the source formatting checks. Functions on Node 22 pass a fresh `npm ci`, build,
lint and 98 tests across 14 suites. Eight isolated validator regression
tests pass. Debug APK and unsigned release AAB builds, instrumented-test
compilation and all eight native libraries' 16 KB alignment pass. Release
lint reports zero errors and 62 nonblocking warnings.

The 2026-10-02 isolated backend emulator on Node 22, Firebase CLI 15 and
Java 21 passed all 18 business-flow assertions, after separately verifying
App Check rejection. A generated-output-only bypass was rebuilt afterward,
restoring all eight enforcement settings; Functions source hashes are unchanged.
A separate Auth-trigger diagnostic passed three assertions after an isolated
loopback CLI proxy workaround; that tool-only change was restored. Hosted main
[CI run 37000862807](https://github.com/jaimesorrow/Wiccan/actions/runs/37000862807)
subsequently passed the full Android and Functions matrix after the Firebase
package migration. It supersedes the earlier startup failures. See
[BUILD_VALIDATION.md](BUILD_VALIDATION.md) for the baseline and remaining limits.

The pre-migration integration audit verifies all 36 previously merged PRs and preserves
all 28 audited branch heads. Google sign-in is hidden when OAuth is absent,
with email authentication retained; six new tests cover availability and
Credential Manager context. CI reads the Functions Node runtime declaration
and cancels superseded runs. Draft PR #42's safe Node/locked-install fixes
and adapted package/toolchain gate pass 21 regression tests; its namespace/flavor
migration branch is retained for widget upgrade and installation-identity work. See
[BRANCH_INTEGRATION_AUDIT.md](BRANCH_INTEGRATION_AUDIT.md).

The run driver installs API 37.0 / Build Tools 37.0.0, reuses an existing
SDK manager and preserves local properties. It handles fresh checkouts and
honors `ANDROID_HOME` after `ANDROID_SDK_ROOT`; six isolated SDK setup
checks pass. PR #31's structured Android lesson metadata remains follow-up work.

## Final status

**NOT READY FOR RELEASE — BLOCKED BY:** live Firebase/Play validation,
the signing key, Play Console setup, and the unreviewed content pool.

The visual/typography implementation remains in review for actual fonts
and device behavior. Its local build/lint, theme tests and JVM palette
specimens pass. These specimens do not replace
a real-device visual pass or live production validation.

## Package/toolchain consolidation proposal — 2026-10-02

The proposed consolidation combines #40/#41 and aligns all Android flavors,
Kotlin packages and namespace with Firebase's `com.jaimesorrow.wiccan`.
Node 22 now agrees across deployment, CI, local selection and lock metadata.
Configuration/preflight checks and the hosted production build matrix pass;
upgrade-device checks remain outstanding. See [PR_RECONCILIATION.md](PR_RECONCILIATION.md)
for the complete PR/branch inventory and explicitly unmerged proposals.

## PR #42 conflict reconciliation

PR #42 merged the Firebase package migration and main `6a375a0e` into main
`b13dc64b`, including Google sign-in availability handling and all six auth
tests from PR #43.
All source/test packages and flavor application IDs use `com.jaimesorrow.wiccan`.
Its merged gate retains the environment project/alias separation checks, CI
cancellation and runtime selection from package.json. The earlier local build
evidence preceded this namespace migration; upgraded widget and app behavior
still need device verification.
The resolved branch passes 25 package/toolchain regression tests, eight validator
regression tests and all 31 release preflight checks. Its source migration has
received the full Android build matrix in hosted main CI after PR #42 merged.

## CI/dependency follow-up — 2026-10-03

PR #44's initial push matrix also passed with 25 package/toolchain regression
tests. CI/release actions now use their supported Node 24 runtimes; app builds
still use Java 17 and Functions use Node 22. The UUID advisory inherited through
Gaxios is resolved with a targeted override. A fresh locked Functions install,
build, all 98 tests and lint pass locally; `npm audit` reports zero vulnerabilities.
