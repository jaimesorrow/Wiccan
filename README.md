# Wiccan

Wiccan is a Kotlin Android app that delivers one piece of witchcraft
content — a spell, a potion, or a lesson — per day on a subscription.
Content is original writing inspired by public-domain folklore and
grimoires.

## Structure

```
app/                         Android app module (Kotlin, package com.wiccan.app)
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
./gradlew :app:assembleProdDebug
./gradlew :app:testProdDebugUnitTest
```

The app has three product flavors (`dev`/`staging`/`prod`, one per
environment - see Environments below); `prod` is the one that matches the
checked-in `google-services.ci.json` placeholder and what CI builds.
Building `dev`/`staging` requires that environment's own real
`google-services.json` under `app/src/dev/` or `app/src/staging/`.

Requires the Android SDK (`compileSdk`/`targetSdk` 36) — set `sdk.dir` in
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

## Environments

Dev, staging, and production must be three **separate** Firebase projects,
never one project reused with a naming convention — Firestore data, Auth
users, and Cloud Function config all live at the project level, so sharing
one project across environments means a staging bug can touch production
users' data.

- `.firebaserc` declares three project aliases (`default`, `staging`,
  `production`); replace the placeholder project IDs with your real ones
  before deploying anywhere. Switch which one `firebase` commands target
  with `firebase use <alias>`.
- Each project needs its own Cloud Functions config: copy
  `functions/.env.example` to `functions/.env.<project-id>` (Firebase
  Functions v2 loads that file automatically for the matching project) and
  fill in that environment's own `ANTHROPIC_API_KEY` and Play service
  account credentials. Never reuse one environment's secrets in another —
  a leaked or misbehaving key should be revocable without taking every
  environment down.
- The admin CLI scripts (`npm run generate-draft`, `npm run approve-draft`)
  print the resolved Firebase project ID before doing anything, specifically
  so an operator never mutates the wrong environment's content pool by
  running a command against whatever project `firebase use` happened to be
  pointed at.
- The Android app has a matching `dev`/`staging`/`prod` product flavor per
  environment (`app/build.gradle.kts`), each with its own applicationId
  suffix (`prod` has none — it's the real `com.wiccan.app`) so all three
  can be installed side by side on one device. Each flavor needs its own
  `google-services.json` from that environment's Firebase project, placed
  at `app/src/<flavor>/google-services.json` — only `prod` has a
  (CI-only, placeholder) one checked in via `google-services.ci.json`.
