# Release checklist (technical preflight)

This checklist distinguishes what the repository can verify automatically
from what still requires a real device, Firebase project, signing identity,
or Play Console configuration.

## Canonical release baseline

```text
Product: Wiccan
applicationId: com.nyxwell.app
minSdk: 24
compileSdk: 36
targetSdk: 36
Java: 17
Play Billing Library: 9.1.0
```

The Kotlin source namespace may remain `com.wiccan.app`; the Play/Firebase
package identity is `com.nyxwell.app`.

## Signing

- [ ] Generate/store a real upload/release key outside the repository.
- [ ] Fill `app/keystore.properties` from the committed example; never commit the real file or keystore.
- [ ] Use Play App Signing unless there is a documented reason not to.
- [ ] Record upload-key and Play App Signing SHA-1/SHA-256 fingerprints in the correct Firebase/OAuth configuration.

## Build hardening

- Current state: `isMinifyEnabled = false`.
- [ ] Before production, decide whether to enable R8/resource shrinking.
- [ ] If enabled, run a real-device smoke test covering Auth, Today, Archive/Library, Paywall/Billing, widget, notifications, sign-out, and deletion.
- [ ] Retain/upload Crashlytics mapping data for every minified release.

## Versioning

- [ ] Bump `versionCode` for every Play upload.
- [ ] Maintain an intentional `versionName` scheme.

## Play Billing 9.1.0

- [x] Android dependency baseline set to 9.1.0.
- [x] Uses ProductDetails / QueryProductDetailsParams and QueryPurchasesParams APIs.
- [x] Pending-purchase API uses `PendingPurchasesParams` rather than the removed no-arg method.
- [x] Automatic billing service reconnection enabled.
- [ ] Verify a real Play internal-test purchase, acknowledgement, restore, cancellation, grace/hold, expiry, and RTDN lifecycle before production.

## 16 KB page size / native ABI

- [x] `scripts/check-16kb-alignment.sh` inspects native libraries in the release `.aab`.
- [x] CI runs the check after `bundleRelease`.
- [ ] Re-run against the exact release candidate immediately before upload.

## Automated repository checks

CI runs:

```bash
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

The internal preflight verifies application ID/API baseline, Billing 9.1.0,
App Check enforcement/debug isolation, locked Firestore writes, absence of
public admin callables, deterministic transaction-based daily assignment,
durable subscription/RTDN plumbing, backup rules, and internal service
boundaries.

Before final release run:

```bash
STRICT_EXTERNAL=1 bash scripts/play-preflight.sh
```

Strict mode must remain red while known Firebase/legal/support placeholders
remain.

## Needs a real device/emulator

- [ ] Run `WiccanDatabaseMigrationTest` using `connectedAndroidTest` against a real SQLite engine.
- [ ] Test `wiccan://today` and `wiccan://premium` deep links.
- [ ] Back up/restore a real install and confirm excluded device/session/cache data does not return.
- [ ] Deny notification permission and verify the app remains functional.
- [ ] Test widget free/premium state and refresh behavior.
- [ ] Test sign-out → sign-in as another account and confirm no prior account cache leaks.

## Needs a real Firebase project

- [ ] Replace `.firebaserc` placeholder project IDs.
- [ ] Register Android package `com.nyxwell.app` for each intended Firebase environment.
- [ ] Supply environment-appropriate `google-services.json`.
- [ ] Configure OAuth SHA fingerprints and Play Integrity App Check.
- [ ] Deploy Firestore rules/indexes and verify actual reads/writes.
- [ ] Deploy Functions with `ANDROID_PACKAGE_NAME=com.nyxwell.app`.
- [ ] Verify device collection-group queries and production indexes against real Firestore.

## Needs Google Play Console

- [ ] Create/verify the app under `com.nyxwell.app`.
- [ ] Configure Play App Signing.
- [ ] Configure `wiccan_premium_monthly` and license testers.
- [ ] Configure Google Play Developer API access.
- [ ] Configure RTDN Pub/Sub/IAM.
- [ ] Complete Data Safety, App Access, content rating, target audience, health, financial features, account deletion, privacy policy, support, and other current declarations.
- [ ] Run Play pre-launch report/internal test and fix any blocking issues.
