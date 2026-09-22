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
- `users/{uid}` — server-controlled account/profile data. Device messaging
  registrations live under `users/{uid}/devices`.

## Core mechanics

- **Daily selection** runs entirely server-side (`getTodayContent`) so a
  user can't reset or game their delivery history. It's idempotent for
  repeat calls the same day, tier-filters candidates, and falls back to
  allowing repeats (with monitoring) once a user exhausts the published
  pool for their tier.
- **Billing** uses two allow-listed premium subscription products
  (`wiccan_premium_monthly` and `wiccan_premium_yearly`) verified
  server-side via the Google Play Developer API; the client's own purchase
  state is never trusted as the tier source of truth. The current paywall
  launches the monthly offer; restore/server verification recognizes both,
  and a daily job re-checks active subscriptions for lapses.
- **Content generation** draws only from a hand-curated `sources.json` of
  public-domain sources, using each source as thematic inspiration only —
  never quoted or closely paraphrased. Every generated draft needs a
  separate `approveDraft` action before it's eligible for delivery.
- **Auth** uses Android Credential Manager for Google sign-in and
  Firebase Auth for email/password. `users/{uid}` creation is a 1st-gen
  Auth trigger, never a client-side write.

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

## Building

### Android app

```
./gradlew :app:assembleProdDebug
./gradlew :app:testProdDebugUnitTest
```

The app has three product flavors (`dev`/`staging`/`prod`, one per
environment - see Environments below). Production uses the verified
`app/src/prod/google-services.json` for Firebase project `wiccan-android`
and application ID `com.jaimesorrow.wiccan`. Building `dev`/`staging`
as connected environments still requires each environment's own real
`google-services.json` under `app/src/dev/` or `app/src/staging/`.

Requires the Android SDK (`compileSdk`/`targetSdk` 36) — set `sdk.dir` in
a local `local.properties` file or the `ANDROID_HOME` environment
variable. The current production Firebase file has no OAuth client, so
Google Sign-In intentionally reports "not configured" until the real Play
App Signing SHA certificates and Google Auth/OAuth configuration are added
and Firebase provides a refreshed production config.

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
  `production`). Production is pinned to `wiccan-android`; dev and
  staging intentionally remain placeholders until separate projects are
  created. Switch the CLI target explicitly with `firebase use <alias>`
  before any deploy or admin operation.
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
- The Android app has matching `dev`/`staging`/`prod` product flavors.
  Production installs as `com.jaimesorrow.wiccan` and uses
  `app/src/prod/google-services.json`. Dev/staging use application-ID
  suffixes and need their own source-specific Firebase configs once those
  projects exist. `app/google-services.ci.json` remains a deliberately
  fake build fallback and is not the production Firebase configuration.
