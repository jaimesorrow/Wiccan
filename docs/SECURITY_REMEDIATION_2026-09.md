# Wiccan — Security, Concurrency & Architecture Remediation

## Executive summary

This pass re-audits the repository against the final security architecture and
corrects the remaining mismatches without reopening already-fixed work.

Key outcomes:

1. Restores the canonical installed Android / Play / Firebase package identity to `com.nyxwell.app` while keeping Wiccan product branding and the product-oriented Kotlin source namespace.
2. Moves the Android billing client to Google Play Billing Library 9.1.0 and updates connection/pending-purchase handling for the current API.
3. Confirms public LLM generation/approval endpoints remain absent; generation and approval are admin/operator CLI workflows.
4. Confirms user-facing callable Functions enforce Firebase App Check and obtain UID from Firebase Auth context.
5. Confirms protected Firestore profile/device writes are denied to clients.
6. Confirms daily content assignment uses a deterministic `{uid}_{YYYY-MM-DD}` history document and `runTransaction`, closing the first-request TOCTOU race.
7. Confirms durable subscription state and RTDN processing remain server-authoritative.
8. Refactors Cloud Function entrypoints into thin transport adapters over internal `contentService`, `subscriptionService`, `userService`, `notificationService`, and `analyticsService` boundaries.
9. Adds an automated security/Play preflight and runs it from CI.

## File-by-file changes

### Android app

- `app/build.gradle.kts`
  - `applicationId = "com.nyxwell.app"`
  - preserves API 36 / minSdk 24 / Java 17 baseline
  - keeps source namespace `com.wiccan.app` explicitly documented as distinct from Play identity
  - release App Check debug provider remains excluded (`debugImplementation` only)
- `gradle/libs.versions.toml`
  - Play Billing Library `7.1.1 -> 9.1.0`
- `app/src/main/kotlin/com/wiccan/app/billing/WiccanBillingClient.kt`
  - PBL 9-compatible pending-purchase API
  - automatic service reconnection
  - explicit billing setup/flow/acknowledgement result checks
- `app/src/main/kotlin/com/wiccan/app/ui/settings/SettingsScreen.kt`
  - subscription management URL now uses `BuildConfig.APPLICATION_ID`, preventing a stale hardcoded package value
- `app/google-services.ci.json`
  - CI Firebase package aligned to `com.nyxwell.app`

### Backend / Cloud Functions

- `functions/src/services/contentService.ts`
  - internal daily-content orchestration boundary
- `functions/src/services/subscriptionService.ts`
  - internal Play verification / RTDN / reconciliation boundary
- `functions/src/services/userService.ts`
  - internal protected profile/device/terms/deletion mutation boundary
- `functions/src/services/notificationService.ts`
  - internal multi-device notification orchestration boundary
- `functions/src/services/analyticsService.ts`
  - structured backend event/logging boundary
- `functions/src/index.ts`
  - reduced to auth/App-Check/input validation, transport error mapping, and trigger wiring
  - canonical Play package default set to `com.nyxwell.app`
  - all user-facing callables continue to use `enforceAppCheck: true`
  - no `generateDraft` / `approveDraft` callable exports
- `functions/.env.example`
  - canonical `ANDROID_PACKAGE_NAME=com.nyxwell.app`

### Firestore / security

No weakening was introduced. Existing rules remain intentionally strict:

```text
users/{uid}: client read-own, write false
users/{uid}/devices/{deviceId}: client read/write false
userContentHistory: client read-own, write false
content: authenticated published read, write false
```

The existing daily assignment repository continues to use:

```text
history document ID: ${uid}_${YYYY-MM-DD}
Firestore runTransaction(...)
```

### CI / release checks

- `.github/workflows/ci.yml`
  - Android lint/unit/build/release-bundle checks
  - androidTest compilation
  - 16 KB native alignment check
  - security/Play preflight
  - separate Functions `npm ci`, build, test, lint job
- `scripts/play-preflight.sh`
  - application ID/API/Billing baseline
  - App Check debug isolation + callable enforcement
  - locked Firestore writes
  - absence of public admin endpoints
  - deterministic transaction-based daily assignment
  - durable subscription + RTDN plumbing
  - explicit Android backup rules
  - internal service boundaries
  - external placeholder warning/final strict gate

## Validation commands

### Android

```bash
./gradlew assembleDebug
./gradlew test
./gradlew bundleRelease
```

The CI workflow additionally runs:

```bash
./gradlew ktlintCheck
./gradlew compileDebugAndroidTestKotlin
./scripts/check-16kb-alignment.sh
```

### Backend

```bash
cd functions
npm ci
npm run lint
npm run build
npm test
```

### Preflight

```bash
bash scripts/play-preflight.sh
```

Final external release gate:

```bash
STRICT_EXTERNAL=1 bash scripts/play-preflight.sh
```

## Security invariants that must not regress

- Never make generation/approval a normal authenticated callable.
- Never let Android clients write protected profile/tier/device/terms state directly.
- Never accept a client-supplied UID as the authority for self-service operations.
- Never grant premium from Android billing state without server verification.
- Never remove the deterministic daily-history key or its transaction.
- Never ship Firebase App Check's debug provider in release.
- Never change `com.nyxwell.app` independently from Firebase/OAuth/App Check/Play configuration.

## External validation still required

The repository cannot honestly mark these PASS until real infrastructure exists:

- Firebase projects and production `google-services.json` for `com.nyxwell.app`
- OAuth/Play App Signing SHA fingerprints
- production Play Integrity App Check
- release/upload signing key
- Play listing and `wiccan_premium_monthly` configuration
- Google Play Developer API service-account access
- RTDN Pub/Sub IAM + end-to-end event
- real Play purchase/restore/cancel/expiry tests
- hosted Terms/Privacy/account-deletion pages and monitored support address
- connected-device/emulator Room migration and backup/restore execution

Production readiness remains blocked on those external validations and the separate remaining product/release work tracked in `docs/STATUS.md`.
