# Wiccan — current implementation status

This file records the codebase state after the September 2026 security,
concurrency, billing, and architecture remediation pass.

## Canonical identity and platform baseline

| Area | Status |
|---|---|
| Product name | PASS — Wiccan |
| Android application ID / Play-Firebase package identity | PASS — `com.nyxwell.app` |
| Kotlin source namespace | PASS — `com.wiccan.app` (source namespace only; not Play identity) |
| minSdk | PASS — 24 |
| compileSdk / targetSdk | PASS — 36 / 36 |
| Java | PASS — 17 |
| Play Billing Library | PASS — 9.1.0 |

The application ID is intentionally different from the source namespace.
Do not migrate `com.nyxwell.app` without coordinating Firebase app
registration, OAuth fingerprints, App Check, backend package verification,
and the Google Play listing.

## Security and concurrency

| Area | Status |
|---|---|
| User Firestore writes | PASS — `users/{uid}` and `users/{uid}/devices/{deviceId}` are server-write-only |
| Public content generation/approval callables | PASS — absent; operator CLI/Admin SDK only |
| App Check | PASS — enforced on all user-facing callable mutations/reads; Play Integrity in release |
| Debug App Check provider | PASS — debug dependency/source set only; excluded from release classpath |
| Daily assignment race | PASS — deterministic `{uid}_{YYYY-MM-DD}` doc + Firestore transaction |
| Auth trust boundary | PASS — UID derived from Firebase Auth context, not client-supplied user IDs |
| Android permissions | PASS — manifest requests only INTERNET and POST_NOTIFICATIONS |
| Backup/data extraction | PASS — explicit backup/data-extraction rules configured |

## Billing lifecycle

| Area | Status |
|---|---|
| Android billing client | PASS — Play Billing Library 9.1.0 |
| Client entitlement trust | PASS — client purchase state is never authoritative |
| Google Play Developer API verification | PASS — backend verification path exists |
| Durable subscription state | PASS — `subscriptions/{uid}` |
| Purchase acknowledgement | PASS |
| Canceled-but-unexpired entitlement | PASS — remains premium until paid-through expiry |
| RTDN | PASS in code — live Play/Pub/Sub path still requires external configuration/testing |
| Scheduled reconciliation | PASS — retained as missed-RTDN safety net |
| Account deletion subscription handling | PASS in code — active Play subscription cancellation is attempted server-side before local record deletion |

## Backend architecture

Cloud Function exports in `functions/src/index.ts` are transport adapters.
Business orchestration is separated into internal services:

- `services/contentService.ts`
- `services/subscriptionService.ts`
- `services/userService.ts`
- `services/notificationService.ts`
- `services/analyticsService.ts`

Domain/repository modules continue to own the testable implementation details
such as deterministic assignment, Play verification, RTDN parsing, and
account deletion.

## CI and automated preflight

CI runs:

```text
./gradlew ktlintCheck
./gradlew test
./gradlew assembleDebug
./gradlew compileDebugAndroidTestKotlin
./gradlew :app:bundleRelease
./scripts/check-16kb-alignment.sh
bash ./scripts/play-preflight.sh

cd functions
npm ci
npm run build
npm test
npm run lint
```

`play-preflight.sh` fails on internal security/release regressions and warns
on external placeholders by default. Final release operators should run:

```bash
STRICT_EXTERNAL=1 bash scripts/play-preflight.sh
```

## Existing product implementation

- Today / daily content
- Book of Shadows Timeline + category Library
- Settings
- Paywall
- Credential Manager Google sign-in + Firebase email/password auth
- versioned Terms/Privacy acceptance gate
- multi-device FCM registrations and timezone-aware daily notification logic
- Glance home-screen widget
- Room cache + explicit migration infrastructure
- Crashlytics / Analytics
- admin-only content generation and approval workflow
- Wiccan editorial/content standard in `docs/CONTENT_VOICE_AND_LESSON_STANDARD.md`

## External / runtime validation still required

The repository must not claim these are complete until they are exercised
against real infrastructure:

- real Firebase dev/staging/production project IDs and `google-services.json`;
- production OAuth SHA fingerprints / Play App Signing identity;
- Play Integrity App Check configuration in the production Firebase project;
- real signing/upload key;
- live Play Console app listing for package `com.nyxwell.app`;
- `wiccan_premium_monthly` product/base plan and license testers;
- RTDN Pub/Sub topic/IAM wiring and an end-to-end live RTDN test;
- live Play Billing sandbox purchase / restore / cancellation / expiry test;
- real hosted Terms, Privacy, account-deletion URLs and monitored support email;
- connected-device/emulator execution of Room migration and backup/restore tests.

## Remaining product/release work outside this security pass

These are known work items rather than security fixes falsely marked done:

- final visual redesign away from the near-black theme to the approved
  serious modern-dark Wiccan direction;
- richer device metadata/preferences if product requirements require it;
- complete curated launch content pool;
- final store assets/screenshots and Play Console declarations;
- release-device smoke testing and staged rollout.

## Final status

**NOT READY FOR PRODUCTION.**

Code-level security and architecture are substantially hardened, but release
readiness still depends on real Firebase/Play configuration, signing,
legal/support endpoints, Play-distributed billing/App Check/RTDN validation,
and the remaining product/release work listed above.
