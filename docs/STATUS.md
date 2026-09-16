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
| Visual theme is still near-black (`#0A0810`-direction) / generic serif+sans typography, not a distinct final direction | **FIXED** — replaced with a six-palette dynamic design system (Near Black, Midnight Blue, Royal Plum, Deep Forest, Warm Earth, Light/Ivory) rolling automatically through the Wheel of the Year, or lockable to any single palette or Follow System, from Settings. See "Visual identity system" below. |
| No Android product flavors for dev/staging/prod (environment separation is `.firebaserc` + `.env` only) | **FIXED** — `dev`/`staging`/`prod` product flavors added (`app/build.gradle.kts`), matching `.firebaserc`'s aliases; only `prod` (applicationId `com.wiccan.app`, no suffix) has a CI-usable placeholder `google-services.json` today. Confirmed `dev`/`staging` fail cleanly and specifically (missing `google-services.json` for that applicationId) rather than in some unexpected way. |
| No runtime kill switches (`dailyContentEnabled`, `notificationsEnabled`, etc.) | **FIXED** — `systemConfig/global` (Firestore, no client access) with both flags, fail-open by default. `getTodayContent` and the hourly notification job both check it before doing anything. |
| Launch content pool is still 1 curated source, not the target pool | **Still open, unchanged** — real content curation, not fabricatable here (see `functions/src/contentSource.ts`'s own doc comment: never auto-populated). |
| Account deletion + active subscription | **FIXED** (see above) — this was the same finding as the billing-cancellation gap. |

## Visual identity system (this revision)

Replaces the single near-black theme with one design system expressed
through six coordinated Material3 palettes — typography, spacing,
component geometry, navigation, and icon language stay identical across
all six; only color changes:

| Palette | Mood |
|---|---|
| Near Black | Modern & minimal |
| Midnight Blue | Celestial & serene |
| Royal Plum | Mystical & elegant |
| Deep Forest | Earthy & grounded |
| Warm Earth | Ritual & candlelight |
| Light / Ivory | Airy & peaceful |

**Theme modes** (Settings → Theme): **Automatic — Wheel of the Year**
(default) rolls through all six on a documented, deterministic date
mapping (`SeasonalThemeResolver.kt`) with a gradual week-long cross-fade
around each seasonal boundary — never a harsh midnight switch. Any single
palette can be locked instead, or **Follow System** (Near Black in dark
mode, Light/Ivory in light mode). The choice persists via DataStore
(`ThemePreferences`) and always overrides the automatic behavior.
`docs/MAINTENANCE.md`'s "Seasonal theme mapping" section documents the
exact date-to-palette table and the reasoning behind it.

**Typography**: Cinzel (an elegant, letterspaced classical serif) for the
Wiccan wordmark, major headings, and ritual/lesson titles; Manrope (a
highly readable modern sans) for everything else — body copy, settings,
navigation, forms, buttons. Both are downloaded via Android's official
Google Fonts provider (`androidx.compose.ui:ui-text-google-fonts`,
verified against Android's own documentation and the real, published
provider certificates from Google's Jetchat sample — not fabricated),
falling back to the system font while loading or if Google Play Services
is unavailable.

**Verified working**: all six palettes compile into real Material3
`ColorScheme`s (`WiccanColorSchemesTest`), the seasonal resolver is unit
tested at every sabbat, at transition-window boundaries, across a
year-wrap, and for determinism (`SeasonalThemeResolverTest`), theme-mode
persistence/fallback-on-invalid-value is tested
(`WiccanThemeModeTest`), and Settings/root ViewModel wiring is tested
(`SettingsViewModelTest`, `ThemeViewModelTest`) — 73/73 unit tests pass,
`ktlintCheck`, `lintProdRelease`, `assembleProdDebug`, and
`bundleProdRelease` are all green, and the 16KB native-library check
still passes.

**Known limitation — no visual screenshots**: this sandbox has no Android
emulator or device (see the pre-existing `WiccanDatabaseMigrationTest`
caveat above — this is the same constraint, not a new one). Visual
validation for all six palettes is real `@Preview` composables
(`WiccanThemePreviews.kt`) for the Today screen and the Settings picker —
open in Android Studio's Design view to see all six side by side — rather
than a fabricated screenshot. Automated on-device screenshot tests
(Paparazzi/Roborazzi, or an instrumented run) would need a real device or
a new test-infrastructure dependency neither of which exists in this
sandbox; that's the one remaining manual-validation step before shipping
this pass.

## Known, still-flagged gaps (external infrastructure, or needing your input)

- **Billing Library major version** — real Kotlin/KSP toolchain blocker found and documented; needs KSP's official compatibility table to finish safely (`docs/MAINTENANCE.md`).
- `PolicyUrls` (Terms, Privacy, account deletion) and `SUPPORT_EMAIL` are placeholders — no real hosted pages exist yet.
- `WiccanDatabaseMigrationTest` compiles clean but has never run against a real SQLite engine — no emulator here.
- `handlePlayRtdn`, `verifyPurchase`'s field shapes, and the `users/{uid}/devices` collection-group query have never been exercised against a real Play Console / Firebase project.
- `dev`/`staging` product flavors exist but need their own real `google-services.json` (from their own real Firebase projects) before they'll build.
- No real Firebase project, signing keystore, or Play Console listing exists — `.firebaserc`, `keystore.properties.example`, and the store-listing checklist are all scaffolding for values only the app's owner can provide.

## Final status

**NOT READY — BLOCKED BY:** no real Firebase project, signing keystore, or
Play Console listing exists yet, **and** the Billing Library major-version
bump needs KSP's real compatibility table (a genuine toolchain blocker was
found and documented, not guessed past).

Every other code-level gap the external audit surfaced — CI coverage,
subscription cancellation, App Check hardening, real per-device
notifications, the 16KB check, ops docs, the device schema, product
flavors, runtime kill switches, and the visual/typography identity — has
been fixed and verified by this repo's own build/test/lint/CI commands.
The one manual step left on the visual identity system is opening the new
`@Preview`s on a real machine with Android Studio, since this sandbox has
no emulator to render them itself.
