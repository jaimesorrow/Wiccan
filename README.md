# Wiccan

Wiccan is a Kotlin Android app that delivers one piece of witchcraft
content — a spell, a potion, or a lesson — per day on a subscription.
Content is original writing inspired by public-domain folklore and
grimoires.

## Product and technical identity

- Product name: **Wiccan**
- Android application ID / Play-Firebase package identity: `com.nyxwell.app`
- Kotlin source namespace: `com.wiccan.app`

The source namespace is product-oriented, but the installed application ID
is intentionally preserved as `com.nyxwell.app`. Do not change the
application ID independently from Firebase registration, OAuth SHA
fingerprints, App Check, Google Play, deep links, or server-side package
verification.

## Structure

```text
app/                         Android app module (Kotlin source namespace com.wiccan.app)
  domain/model/                Content, ContentType, ContentTier, ContentStatus,
                                UserContentHistory, AuthState
  domain/repository/           ContentRepository, SubscriptionRepository,
                                NotificationRepository, AuthRepository
  data/local/                  Room entity + DAO + database (CachedDailyContent)
  data/remote/                 Firebase Functions/Auth/Messaging/Credential Manager
                                wrappers
  data/repository/             Firebase-backed repository implementations
  di/                          Hilt modules
  billing/                     Play Billing Library 9.1.0 wrapper
  notification/                FirebaseMessagingService
  widget/                      Home-screen Glance widget
  worker/                      WorkManager daily cache-refresh worker
  ui/today/                    Today screen (MVI)
  ui/auth/                     Sign-in screen (MVI)

functions/                    Cloud Functions (Node/TypeScript)
  src/services/                Internal business-service layer
    contentService.ts          Daily-content assignment orchestration
    subscriptionService.ts     Play verification / RTDN / reconciliation
    userService.ts             Protected profile/device/account mutations
    notificationService.ts     Multi-device notification orchestration
    analyticsService.ts        Structured backend events/logging boundary
  src/getTodayContent.ts        Daily-selection domain logic
  src/verifyPurchase.ts         Server-side Play verification domain logic
  src/recheckLapsedSubscriptions.ts  Subscription reconciliation
  src/generateDraft.ts          Admin-only content generation core
  src/approveDraft.ts           Admin-only draft approval core
  src/index.ts                  Thin callable/scheduled/trigger adapters only
```

## Data model (Firestore)

- `content` — spells/potions/lessons. Only `status: published` docs are
  eligible for delivery.
- `userContentHistory` — deterministic `{uid}_{YYYY-MM-DD}` daily assignment
  documents, written only by the backend transaction.
- `users/{uid}` — server-controlled account/profile data. Client writes are
  denied by Firestore Rules.
- `users/{uid}/devices/{deviceId}` — server-controlled device messaging data.
- `subscriptions/{uid}` — durable server-verified subscription state.

## Core security mechanics

- **Daily selection** runs entirely server-side. The daily assignment is
  committed using a deterministic document ID inside a Firestore transaction,
  so concurrent first requests converge on one assignment.
- **Billing** uses Play Billing Library 9.1.0 on Android, but entitlement is
  server-authoritative via the Google Play Developer API. RTDN handles
  lifecycle changes in near real time and scheduled reconciliation remains a
  safety net.
- **App Check** uses Play Integrity in release builds. The debug provider is a
  debug-only dependency/source-set path and does not ship in release.
- **Profile/device mutations** are auth- and App-Check-gated Cloud Functions.
  Firestore clients cannot directly mutate protected user state.
- **Content generation/approval** are not public callable endpoints. They are
  operator/admin CLI workflows using Admin SDK credentials.
- **Account deletion** is self-only, derives UID from Firebase Auth context,
  and runs server-side.

## Content voice and lesson format

`docs/CONTENT_VOICE_AND_LESSON_STANDARD.md` is the authoritative editorial
standard for Wiccan lessons, spells, potions, generation prompts,
validation, and content-detail UI.

Lessons should feel grounded, mature, historically careful, practical,
reflective, and subtly grimoire-inspired — never theatrical or cheesy.
The preferred lesson structure is:

```text
Introduction
Lesson
Reflection
Practice      (optional)
Safety Note   (when warranted)
```

Historical/customary claims must be qualified appropriately, symbolic
claims must not be presented as scientifically established supernatural
facts, and safety notes are required whenever meaningful material or
health risks exist. Lesson artwork should use restrained Wiccan line-art,
not physical photography.

## Building and validation

### Android

```bash
./gradlew assembleDebug
./gradlew test
./gradlew bundleRelease
```

Requires Android API 36, minSdk 24, and Java 17. A real
`app/google-services.json` is required outside CI and must register
`com.nyxwell.app`.

### Cloud Functions

```bash
cd functions
npm ci
npm run lint
npm run build
npm test
```

### Security / Play preflight

```bash
bash scripts/play-preflight.sh
```

For a final release gate that also fails on known external placeholders:

```bash
STRICT_EXTERNAL=1 bash scripts/play-preflight.sh
```

## Environments

Dev, staging, and production must be three **separate** Firebase projects,
never one project reused with a naming convention — Firestore data, Auth
users, and Cloud Function config all live at the project level.

- `.firebaserc` declares project aliases; replace placeholder project IDs
  before any deployment.
- Each project needs its own Cloud Functions config using
  `functions/.env.<project-id>` and its own credentials.
- `ANDROID_PACKAGE_NAME` must remain aligned with `com.nyxwell.app` unless a
  deliberate package migration is performed across Android, Firebase, OAuth,
  App Check, and Play together.
- Admin CLI scripts print the resolved Firebase project ID before mutating
  content so an operator can catch an environment mistake.
