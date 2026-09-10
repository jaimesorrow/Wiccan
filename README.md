# Wiccan

Wiccan is a Kotlin Android app that delivers one piece of witchcraft
content — a spell, a potion, or a lesson — per day on a subscription.
Content is original writing inspired by public-domain folklore and
grimoires.

## Structure

```
app/                         Android app module (Kotlin, package com.nyxwell.app)
  domain/model/                Content, ContentType, ContentTier, ContentStatus,
                                UserContentHistory, AuthState
  domain/repository/           ContentRepository, SubscriptionRepository,
                                NotificationRepository, AuthRepository
  data/local/                  Room entity + DAO + database (CachedDailyContent)
  data/remote/                 Firebase Functions/Auth/Messaging/Credential Manager
                                wrappers
  data/repository/             Firebase-backed repository implementations
  di/                          Hilt modules
  billing/                     Play Billing Library wrapper
  notification/                FirebaseMessagingService
  widget/                      Home-screen Glance widget
  worker/                      WorkManager daily cache-refresh worker
  ui/today/                    Today screen (MVI)
  ui/auth/                     Sign-in screen (MVI) — sign in, create account,
                                password reset, all in one screen
  ui/subscription/             Minimal, unstyled Upgrade CTA

functions/                    Cloud Functions (Node/TypeScript)
  src/getTodayContent.ts        Daily-selection logic (server-side, never a
                                 client-side Firestore query)
  src/verifyPurchase.ts         Google Play purchase verification
  src/recheckLapsedSubscriptions.ts  Daily subscription re-check
  src/generateDraft.ts          Content generation from curated sources.json
  src/approveDraft.ts           draft -> published
  src/sendDailyNotifications.ts Daily FCM push
  src/createUserProfile.ts      users/{uid} creation on sign-up
  src/index.ts                  Callable/scheduled/trigger entry points
```

## Data model (Firestore)

- `content` — spells/potions/lessons. Only `status: published` docs are
  eligible for delivery.
- `userContentHistory` — one row per (user, day) delivery, written only by
  the Cloud Function.
- `users/{uid}` — `tier` ("free"/"premium"), `fcmToken`, `createdAt`.
  Created server-side on sign-up; the client never writes it directly.

## Core mechanics

- **Daily selection** runs entirely server-side (`getTodayContent`) so a
  user can't reset or game their delivery history. It's idempotent for
  repeat calls the same day, tier-filters candidates, and falls back to
  allowing repeats (with monitoring) once a user exhausts the published
  pool for their tier.
- **Billing** is a single subscription product verified server-side via
  the Google Play Developer API; the client's own purchase state is never
  trusted as the tier source of truth. A daily job re-checks active
  subscriptions for lapses.
- **Content generation** draws only from a hand-curated `sources.json` of
  public-domain sources, using each source as thematic inspiration only —
  never quoted or closely paraphrased. Every generated draft needs a
  separate `approveDraft` call before it's eligible for delivery.
- **Auth** uses Android Credential Manager for Google sign-in and
  Firebase Auth for email/password. `users/{uid}` creation is a 1st-gen
  Auth trigger, never a client-side write.

## Building

### Android app

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Requires the Android SDK (`compileSdk`/`targetSdk` 35) — set `sdk.dir` in
a local `local.properties` file or the `ANDROID_HOME` environment
variable. You'll also need a real `app/google-services.json` from your
Firebase project (not checked in), including a configured OAuth web
client if you want Google sign-in to work, for the Google Services
plugin to run.

### Cloud Functions

```
cd functions
npm install
npm run build   # tsc
npm test        # jest
npm run deploy  # firebase deploy --only functions (requires firebase-tools + a linked project)
```
