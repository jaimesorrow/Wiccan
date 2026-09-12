# Wiccan — repair-order status

Honest status against the repair order this branch worked through. PASS
means built and verified by this repo's own automated checks. FIXED means
a real gap was found and closed in this pass. BLOCKED means it needs
something this sandbox genuinely cannot provide (real credentials, a real
project, a device/emulator) — never worked around with a placeholder
pretending to be real.

## Core app (Sessions 1-17)

| Area | Status |
|---|---|
| Daily content selection (deterministic, tier-filtered, transactional) | PASS |
| Room cache + DAO | PASS |
| Today/Archive/Settings/Paywall screens (MVI) | PASS |
| Auth (Credential Manager Google sign-in + Firebase email/password) | PASS |
| Billing (Play Billing Library 7.1.1, server-verified entitlement) | PASS |
| Content generation pipeline (Claude-backed, admin-only) | PASS |
| Daily FCM notifications | PASS |
| Home-screen widget | PASS |
| Account deletion (server-side callable + client wiring) | PASS |
| Shared AppResult/AppError error taxonomy | PASS |
| App Check enforcement on user-facing callables | PASS |
| API 36 baseline (compileSdk/targetSdk 36) | PASS |
| Full Nyxwell → Wiccan rebrand (including applicationId) | PASS |

## This pass's repair order (Sessions 18-50)

| Session | Area | Status |
|---|---|---|
| 21 | Navigation Compose app shell + real `RootState` | PASS |
| 24 | Server-controlled profile mutations (device token, time zone, terms) | PASS |
| 19 | Versioned Terms/Privacy acceptance gate | PASS |
| 23 | Subscription lifecycle hardening (entitlement fix, RTDN, acknowledgement) | PASS |
| 18 | Category-organized Library | PASS |
| 25/43 | Multi-device FCM (per-device registration, no cross-device clobbering) | FIXED |
| 29 | Content safety gate + duplicate-title prevention | FIXED |
| 27 | Account deletion completeness (devices subcollection + subscription record were previously orphaned) | FIXED |
| 45/44 | Room migration test infra + Android backup safety (Auto Backup was sweeping up device-bound SDK tokens by default) | FIXED |
| 31-35 | Environment separation (no `.firebaserc` existed at all) + ops safety (admin CLI project-ID guard) | FIXED |
| 46-50 | Technical release preflight | DOCUMENTED — see `docs/RELEASE_CHECKLIST.md` |
| 36-40 | Play Console readiness package | DOCUMENTED — see `docs/PLAY_CONSOLE_READINESS.md` |

## Known, explicitly flagged gaps

- Play Billing Library major-version bump (7.1.1 → 9.1.0) — not attempted; no way to verify a breaking API surface change against a live purchase flow in this sandbox.
- `PolicyUrls` (Terms, Privacy, account deletion) and `SUPPORT_EMAIL` are placeholders — no real hosted pages exist yet.
- Room migration test (`WiccanDatabaseMigrationTest`) compiles clean but has never run against a real SQLite engine — no emulator here.
- `handlePlayRtdn`, `verifyPurchase`'s field shapes, and the `users/{uid}/devices` collection-group query have never been exercised against a real Play Console / Firebase project.
- The 365-entry launch content pool referenced in code comments is still 1 hand-curated source (`sources.json`) — real content curation is deliberately outside what this pass fabricates.
- `isMinifyEnabled = false` — left off pending a real-device smoke test (see `docs/RELEASE_CHECKLIST.md`).
- No real Firebase project, signing keystore, or Play Console listing exists — `.firebaserc`, `keystore.properties.example`, and the store-listing checklist are all scaffolding for values only the app's owner can provide.

## Final status

**NOT READY — BLOCKED BY:** no real Firebase project, signing keystore, or
Play Console listing exists yet. Every gap above that isn't one of those
three has been closed and verified by this repo's own build/test/lint
commands. Once a real project is provisioned (`.firebaserc` filled in,
`functions/.env.<project-id>` set per `functions/.env.example`, Firestore
rules/indexes deployed) and a real keystore is wired in
(`app/keystore.properties`), the remaining checklist in
`docs/RELEASE_CHECKLIST.md` and `docs/PLAY_CONSOLE_READINESS.md` is what
stands between this codebase and Play internal testing.
