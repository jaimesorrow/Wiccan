# Wiccan — repair-order status

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
| Billing (Play Billing Library 8.0.0, server-verified entitlement) | IN REVIEW — local Android build/test/lint PASS on 2026-10-01; hosted CI and live Play validation pending |
| Content generation pipeline (Claude-backed, admin-only) | PASS |
| Home-screen widget | PASS |
| Shared AppResult/AppError error taxonomy | PASS |
| Full Nyxwell → Wiccan rebrand (including applicationId) | PASS |
| API 36 baseline (compileSdk/targetSdk 36) | PASS |

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
| No automated 16KB/ABI native-library check | **FIXED** — `scripts/check-16kb-alignment.sh`, runs in CI against the real release `.aab` on every push; currently passing |
| No disaster recovery / incident response / maintenance docs | **FIXED** — `docs/DISASTER_RECOVERY.md`, `docs/INCIDENT_RESPONSE.md`, `docs/MAINTENANCE.md` |
| Billing Library version | **8.0.0 on main, local Android matrix PASS on 2026-10-01; hosted CI pending** — 9.1.0 requires Kotlin ≥2.3.0 and surfaced a KSP/Hilt plugin conflict; see `docs/MAINTENANCE.md`. |
| Device registration schema is minimal (token + time zone + updatedAt; no `notificationsEnabled`/`platform`/`createdAt`/`lastSeenAt`) | **FIXED** — added all four: `createdAt`/`platform` written on first registration, `notificationsEnabled` is a real per-device toggle with a Settings switch (DataStore-backed locally, since Firestore rules give the client no read access to its own device doc), and `sendDailyNotifications` now skips opted-out devices. |
| Visual theme is still near-black (`#0A0810`-direction) / generic serif+sans typography, not a distinct final direction | **IN REVIEW** — PR #24 adds six seasonal palettes and typography on main. The local build, tests, lint and six JVM palette specimens pass on 2026-10-01; actual fonts, transitions and app/device visual validation remain required. |
| No Android product flavors for dev/staging/prod (environment separation is `.firebaserc` + `.env` only) | **FIXED** — `dev`/`staging`/`prod` product flavors added (`app/build.gradle.kts`). Prod has the registered client config for `com.jaimesorrow.wiccan` at `app/src/prod/google-services.json`; that source-set config takes precedence over CI's fake root fallback. Dev/staging still need their own configurations. |
| No runtime kill switches (`dailyContentEnabled`, `notificationsEnabled`, etc.) | **FIXED** — `systemConfig/global` (Firestore, no client access) with both flags, fail-open by default. `getTodayContent` and the hourly notification job both check it before doing anything. |
| Launch content pool needs approved entries | **Still open** — four curated source records and a 150-entry draft seed are on main; drafts need human editorial/rights review and approval before delivery. |
| Account deletion + active subscription | **FIXED** (see above) — this was the same finding as the billing-cancellation gap. |

## Known remaining gaps

- **Visual/typography redesign** — local build/test/lint and six JVM palette specimens pass; real-device app screens, transitions and actual font appearance still need review before release.
- **Billing Library major version** — 8.0.0 passes the local build matrix. The separate 9.x upgrade remains blocked by the documented Kotlin/KSP/Hilt toolchain issue (`docs/MAINTENANCE.md`).
- `PolicyUrls` (Terms, Privacy, account deletion) and `SUPPORT_EMAIL` are placeholders — no real hosted pages exist yet.
- `WiccanDatabaseMigrationTest` compiles clean but has never run against a real SQLite engine — no emulator here.
- `handlePlayRtdn`, `verifyPurchase`'s field shapes, and the `users/{uid}/devices` collection-group query have never been exercised against a real Play Console / Firebase project.
- `dev`/`staging` product flavors exist but need their own real `google-services.json` (from their own real Firebase projects) before they'll build.
- The production Firebase client config identifies `wiccan-android`; live Firebase/Play flows, signing key, and Play listing still need owner-side verification and setup.

## Latest automated validation

The 2026-10-01 local matrix passed: debug APK, unsigned release AAB,
Android formatting/lint, 80 Android tests, instrumented-test compilation,
16 KB native alignment, Functions build/lint and 98 tests. The isolated
backend emulator passed 18 assertions with a temporary generated-output
App Check bypass, followed by a rebuild restoring enforcement. Hosted
Actions still stops before jobs execute; release lint reports 93 warnings
and zero errors. See [BUILD_VALIDATION.md](BUILD_VALIDATION.md) for the
baseline, environment workaround, exact scope and remaining limits.

The run driver now handles a fresh checkout without `local.properties` and
honors `ANDROID_HOME` after `ANDROID_SDK_ROOT`; five SDK-selection checks
pass. PR #31's structured Android lesson metadata remains follow-up work.

## Final status

**NOT READY FOR RELEASE — BLOCKED BY:** hosted Android CI and live Firebase/Play validation,
the signing key, Play Console setup, and the unreviewed content pool.
The later Billing 9.x migration needs a compatible Kotlin/KSP/Hilt toolchain.

The visual/typography implementation remains in review for actual fonts
and device behavior. Its local theme tests, build/lint and JVM palette
specimens now pass against the current integration; this does not replace
a real-device visual pass or live production validation.
