# Nyxwell

Nyxwell is a Kotlin Android app that delivers one piece of witchcraft
content — a spell, a potion, or a lesson — per day on a subscription.
Content is original writing inspired by public-domain folklore and
grimoires.

This repository currently contains the **foundation and core
content-delivery engine**: data models, the Room cache, the
`ContentRepository`, and the server-side daily-selection Cloud Function.
There is no UI, billing, notifications, or content-generation pipeline yet
— those are separate follow-up passes.

## Structure

```
app/                         Android app module (Kotlin, package com.nyxwell.app)
  domain/model/               Content, ContentType, ContentTier, ContentStatus, UserContentHistory
  domain/repository/          ContentRepository interface
  data/local/                 Room entity + DAO + database (CachedDailyContent)
  data/remote/                Firebase Functions callable wrapper
  data/mapper/                Payload/entity <-> domain mapping
  data/repository/            Firebase-backed ContentRepository implementation
  di/                         Hilt modules (Firebase, Room, repository binding)
  worker/                     WorkManager daily cache-refresh worker

functions/                   Cloud Functions (Node/TypeScript)
  src/getTodayContent.ts      Core daily-selection logic (pure, unit-tested)
  src/firestoreContentRepository.ts  Firestore-backed implementation
  src/index.ts                 Callable Cloud Function entry point
  src/__tests__/                Jest tests
```

## Data model (Firestore)

- `content` — spells/potions/lessons. Only `status: published` docs are
  eligible for delivery.
- `userContentHistory` — one row per (user, day) delivery, written only by
  the Cloud Function.

## Daily selection

`getTodayContent` (callable Cloud Function) is the **only** way content
gets selected — never a client-side Firestore query, so users can't reset
or game their history. On each call it:

1. Returns today's already-delivered content if this user has one
   (idempotent for repeat calls the same day).
2. Otherwise picks a random published, in-tier candidate the user hasn't
   seen yet, records the delivery, and returns it.
3. Falls back to allowing repeats (and logs a pool-exhaustion event) once
   a user has seen everything published in their tier.

The Android app calls this through `ContentRepository.refreshTodayContent`,
which also mirrors the result into a Room cache (`CachedDailyContent`) so
`observeTodayContent()` works offline. A `DailyContentSyncWorker` refreshes
that cache once a day.

## Building

### Android app

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Requires the Android SDK (`compileSdk`/`targetSdk` 35) — set `sdk.dir` in a
local `local.properties` file or the `ANDROID_HOME` environment variable.
You'll also need a real `app/google-services.json` from your Firebase
project (not checked in) for the Google Services plugin to run.

### Cloud Functions

```
cd functions
npm install
npm run build   # tsc
npm test        # jest
npm run deploy  # firebase deploy --only functions (requires firebase-tools + a linked project)
```
