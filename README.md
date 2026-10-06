# Wiccan

Wiccan is a Kotlin Android app that delivers one piece of witchcraft
content — a spell, a potion, or a lesson — per day on a subscription.
Content is original writing inspired by public-domain folklore and
grimoires.

## Structure

```
app/                         Android app module (Kotlin, package com.jaimesorrow.wiccan)
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
  ui/paywall/                  Play subscription purchase and restore flow

functions/                    Cloud Functions (Node/TypeScript)
  ancientContentSeed.json       296 ancient-source content drafts (200 lessons)
                                 (seed pool, all status=draft)
  src/contentSeed.ts            Seed loading + validation gates
  src/scripts/seedContentCli.ts Admin CLI: create missing seed IDs in Firestore
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
- **Seed content** ships in `functions/ancientContentSeed.json`: 200 lessons,
  59 spells and 37 potions from four curated ancient sources. The 146 new
  companion lessons require editorial/source review; all 296 entries are
  drafts. `npm run seed-content` validates it;
  `-- --commit --project wiccan-android` creates missing IDs while preserving
  existing documents. `npm run verify-lessons -- --project wiccan-android`
  verifies the live lesson pool. See `docs/ANCIENT_LESSON_SOURCING.md`.
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

## Validating the repo

One command answers "is this repo whole, consistent, and buildable?":

```
./scripts/validate-repo.sh              # structure + both build matrices
./scripts/validate-repo.sh --structure  # structure only, seconds, no toolchain
python3 scripts/check-toolchain.py     # package IDs and toolchain consistency
python3 scripts/test-check-toolchain.py  # isolated package/runtime drift tests
python3 scripts/test-validate-repo.py   # isolated validation failure checks
```

It layers cheapest-first: structural invariants, then
`scripts/release-preflight.sh`, then the same build matrix CI runs.
Functions dependencies are installed with `npm ci` from the lockfile.
Hosted Actions must still execute its own run.

The structural layer catches what a successful build cannot: the
applicationId disagreeing with a `google-services.json`, a Play Billing
package-name fallback creeping back into `functions/src` (which would let
a dev/staging call hit the production Play listing), merge-conflict
markers, committed build output or key material, and workflows pointing at
scripts that don't exist. CI runs the isolated validation regression tests
and `--structure` before the expensive steps.

## Building

### GitHub Codespaces

Open the repository in GitHub Codespaces and select the checked-in
`.devcontainer` configuration. It installs Java 17, Node 22, Android SDK 37 and
the required build tools, then creates a local `local.properties` pointing at
the Codespace SDK. The first build downloads Gradle and Maven dependencies:

```bash
./gradlew :app:assembleProdDebug
./gradlew :app:testProdDebugUnitTest
```

The production release bundle can be built for inspection with
`./gradlew :app:bundleProdRelease`; it remains unsigned until you provide the
upload keystore through the release workflow secrets. Never put Firebase service
account keys or signing keystores in the Codespace repository.

For Google Play Billing setup, Firebase lesson verification and outstanding Play
release setup, see [the production setup guide](docs/PRODUCTION_SETUP.md).

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

The current toolchain uses Java 17, Gradle 9.8.0, AGP 9.4.1 and Kotlin
2.4.20, with Play Billing 9.1.0. Install the Android SDK platform with
`sdkmanager "platforms;android-37.0"` (`compileSdk` 37, `targetSdk` 36).
Set `sdk.dir` in a local `local.properties` file or the `ANDROID_HOME`
environment variable. The production Firebase file includes the registered
web OAuth client ID verified in Firebase on 2026-10-06 UTC, allowing the
Google Services plugin to generate `default_web_client_id`. Google and
Email/Password providers are enabled. Confirm that the installed build's
signing certificate is registered with Firebase, and verify Google sign-in
on a device before treating authentication as release-ready.

### Cloud Functions

Use Node 22, matching `functions/package.json` and the deployed Functions
runtime. CI reads the Node version from that file.

```
cd functions
# Use Node 22, matching Firebase deployment and CI.
npm ci
npm run build   # tsc
npm test        # jest
npm run lint    # eslint
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
- The admin CLI scripts print the resolved Firebase project ID before doing anything, specifically
  so an operator never mutates the wrong environment's content pool by
  running a command against whatever project `firebase use` happened to be
  pointed at.
- Production content imports, verification and approval require an explicit
  `--project wiccan-android`. Approve one reviewed draft with
  `npm run approve-draft -- --project wiccan-android --contentId <id>`.
- The Android app has matching `dev`/`staging`/`prod` product flavors.
  All flavors install as `com.jaimesorrow.wiccan`, matching the Firebase
  package name and Kotlin namespace. Variants replace each other on a device.
  Production uses `app/src/prod/google-services.json`; dev/staging need
  separate project configs registering that same package. `app/google-services.ci.json` remains a deliberately
  fake build fallback and is not the production Firebase configuration.

Package and runtime consistency: `python3 scripts/check-toolchain.py`; regression
checks: `python3 scripts/test-check-toolchain.py`. See
[the branch integration audit](docs/BRANCH_INTEGRATION_AUDIT.md) for incorporated
changes and the retained draft package migration.
