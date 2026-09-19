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
| Billing (Play Billing Library 7.1.1, server-verified entitlement) | PASS |
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
| Billing Library still 7.1.1, not a later major version | **Attempted, real blocker found** — 9.1.0 (confirmed the real latest via Google's Maven metadata) requires Kotlin ≥2.3.0, which itself surfaced a KSP/Hilt Gradle-plugin classloader conflict this sandbox couldn't resolve with confidence (needs KSP's real compatibility table). Reverted rather than guess further; exact next steps recorded in `docs/MAINTENANCE.md`. |
| Device registration schema is minimal (token + time zone + updatedAt; no `notificationsEnabled`/`platform`/`createdAt`/`lastSeenAt`) | **FIXED** — added all four: `createdAt`/`platform` written on first registration, `notificationsEnabled` is a real per-device toggle with a Settings switch (DataStore-backed locally, since Firestore rules give the client no read access to its own device doc), and `sendDailyNotifications` now skips opted-out devices. |
| Visual theme is still near-black (`#0A0810`-direction) / generic serif+sans typography, not a distinct final direction | **Still open — needs your input, not mine to decide.** I have no record in this session of a specific rejected direction or an approved replacement; redoing the visual identity on an unverified third-party claim risks guessing wrong twice. Tell me what direction you want and I'll build it. |
| No Android product flavors for dev/staging/prod (environment separation is `.firebaserc` + `.env` only) | **FIXED** — `dev`/`staging`/`prod` product flavors added (`app/build.gradle.kts`), matching `.firebaserc`'s aliases; only `prod` (applicationId `com.jaimesorrow.wiccan`, no suffix) has a CI-usable placeholder `google-services.json` today. Confirmed `dev`/`staging` fail cleanly and specifically (missing `google-services.json` for that applicationId) rather than in some unexpected way. |
| No runtime kill switches (`dailyContentEnabled`, `notificationsEnabled`, etc.) | **FIXED** — `systemConfig/global` (Firestore, no client access) with both flags, fail-open by default. `getTodayContent` and the hourly notification job both check it before doing anything. |
| Launch content pool is still 1 curated source, not the target pool | **Still open, unchanged** — real content curation, not fabricatable here (see `functions/src/contentSource.ts`'s own doc comment: never auto-populated). |
| Account deletion + active subscription | **FIXED** (see above) — this was the same finding as the billing-cancellation gap. |

## Known, still-flagged gaps (external infrastructure, or needing your input)

- **Visual/typography redesign** — needs your direction (see table above); not attempted.
- **Billing Library major version** — real Kotlin/KSP toolchain blocker found and documented; needs KSP's official compatibility table to finish safely (`docs/MAINTENANCE.md`).
- `PolicyUrls` (Terms, Privacy, account deletion) and `SUPPORT_EMAIL` are placeholders — no real hosted pages exist yet.
- `WiccanDatabaseMigrationTest` compiles clean but has never run against a real SQLite engine — no emulator here.
- `handlePlayRtdn`, `verifyPurchase`'s field shapes, and the `users/{uid}/devices` collection-group query have never been exercised against a real Play Console / Firebase project.
- `dev`/`staging` product flavors exist but need their own real `google-services.json` (from their own real Firebase projects) before they'll build.
- The real Firebase project (`wiccan-android`) now exists and is wired into `.firebaserc`'s `production` alias, but its console-side services are not yet enabled — see `docs/FIREBASE_SETUP.md`. No signing keystore or Play Console listing exists yet; `keystore.properties.example` and the store-listing checklist remain scaffolding for values only the app's owner can provide.
- **The app does not currently compile against the real `google-services.json`.** Google Sign-In is not enabled on `wiccan-android`, so its `oauth_client` array is empty, so the Google Services plugin generates no `default_web_client_id` — the string `AuthRepositoryImpl.kt:69` requires. Verified by a real `compileProdDebugKotlin` run (`Unresolved reference 'default_web_client_id'`), and verified fixed by supplying an `oauth_client` entry. CI misses it because `google-services.ci.json` carries a dummy one. Fix is console-side: `docs/FIREBASE_SETUP.md` §0.

## Final status

**NOT READY — BLOCKED BY:** the app does not compile against the real
`google-services.json` until Google Sign-In is enabled on the
`wiccan-android` project (`docs/FIREBASE_SETUP.md` §0), **and** that
project's console-side services (Blaze, Firestore, Auth, App Check) are
not yet enabled, **and** no signing keystore or Play Console listing
exists yet, **and** the visual/typography redesign needs your direction
before it can be built, **and** the Billing Library major-version bump
needs KSP's real compatibility table (a genuine toolchain blocker was
found and documented, not guessed past).

Every other code-level gap the external audit surfaced — CI coverage,
subscription cancellation, App Check hardening, real per-device
notifications, the 16KB check, ops docs, the device schema, product
flavors, and runtime kill switches — has been fixed and verified by this
repo's own build/test/lint/CI commands.
