# Wiccan

Wiccan is a Kotlin Android app that delivers one piece of witchcraft content — a spell, potion, or lesson — per day.

## Product and technical identity

- Product name: **Wiccan**
- Android application ID / Play-Firebase package identity: `com.nyxwell.app`
- Kotlin source namespace: `com.wiccan.app`

The source namespace is product-oriented, but the installed production application ID is intentionally preserved as `com.nyxwell.app`. Do not change it independently from Firebase registration, OAuth fingerprints, App Check, Google Play, or backend package verification.

## Environments

The Android app has `dev`, `staging`, and `prod` product flavors matching separate Firebase environments. `prod` has no suffix and therefore uses the canonical `com.nyxwell.app`; `dev` and `staging` add `.dev` and `.staging` so they can coexist on a device. Each environment must use its own Firebase project and matching `google-services.json`.

`.firebaserc` contains the matching backend aliases. Replace its placeholder project IDs before deployment. Functions environment files must be environment-specific; `ANDROID_PACKAGE_NAME` remains `com.nyxwell.app` for production.

## Security architecture

- `users/{uid}` and `users/{uid}/devices/{deviceId}` are server-write-only in Firestore Rules.
- User-facing callables require Firebase Auth and App Check.
- Content generation/approval are operator CLI/Admin SDK workflows, not public callables.
- Daily content uses a deterministic `{uid}_{YYYY-MM-DD}` history document committed inside a Firestore transaction.
- Billing entitlement is server-authoritative through Google Play Developer API verification, durable `subscriptions/{uid}` state, RTDN, and scheduled reconciliation.
- `systemConfig/global` provides server-only `dailyContentEnabled` and `notificationsEnabled` emergency kill switches.
- Release builds use Play Integrity App Check; the debug provider is a debug-only dependency.
- Android backup/data-transfer rules explicitly exclude device/session/rebuildable local state.

## Backend structure

`functions/src/index.ts` contains transport/trigger adapters. Business orchestration lives in:

```text
functions/src/services/contentService.ts
functions/src/services/subscriptionService.ts
functions/src/services/userService.ts
functions/src/services/notificationService.ts
functions/src/services/analyticsService.ts
```

Domain/repository modules hold transaction logic, Play verification, RTDN parsing, notification delivery, and deletion behavior.

## Content voice

`docs/CONTENT_VOICE_AND_LESSON_STANDARD.md` is authoritative. Lessons remain grounded, mature, historically careful, practical, reflective, and subtly grimoire-inspired. Preferred structure:

```text
Introduction
Lesson
Reflection
Practice      (optional)
Safety Note   (when warranted)
```

No physical lesson photography; use restrained Wiccan line-art.

## Android baseline/toolchain

```text
minSdk 24
compileSdk 36
targetSdk 36
Java 17
AGP 8.13.2
Kotlin 2.3.21
KSP 2.3.9
Play Billing Library 9.1.0
```

Kotlin/KSP/AGP were advanced together because Billing 9.1.0 publishes Kotlin metadata requiring a newer compiler. The build uses the modern `kotlin { compilerOptions { ... } }` DSL.

## Build and validation

CI builds the production flavor:

```bash
./gradlew ktlintCheck
./gradlew :app:testProdDebugUnitTest
./gradlew :app:assembleProdDebug
./gradlew :app:compileProdDebugAndroidTestKotlin
./gradlew :app:bundleProdRelease
./scripts/check-16kb-alignment.sh
bash ./scripts/play-preflight.sh
```

Functions:

```bash
cd functions
npm ci
npm run build
npm test
npm run lint
```

Final release gate:

```bash
STRICT_EXTERNAL=1 bash scripts/play-preflight.sh
```

Strict mode intentionally fails while real Firebase project IDs, hosted legal/deletion URLs, monitored support contact, signing identity, or other external release configuration remain placeholders.

## Content administration

Draft generation and approval remain admin-only CLI workflows:

```bash
cd functions
npm run generate-draft -- --tier free --type lesson --count 1
npm run approve-draft -- --contentId <id>
```

The scripts identify the active Firebase project before mutation. Generated content remains draft until explicitly approved.
