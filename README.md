# Wiccan

Wiccan is a Kotlin Android app that delivers one piece of witchcraft
content — a spell, a potion, or a lesson — per day on a subscription.
Content is original writing inspired by public-domain folklore and
grimoires.

## Structure

```
app/                         Android app module (Kotlin, package com.jaimesorrow.wiccan)
                               Generated Android namespace: com.wiccan.app
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
  ancientContentSeed.json       296 ancient-source drafts: 200 lessons,
                                 59 spells and 37 potions
  src/contentSeed.ts            Seed loading + validation gates
  src/scripts/seedContentCli.ts Guarded admin import + inventory verification
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
  user can't reset or game their delivery history. A transaction assigns one
  unseen eligible entry per user-local date, honoring existing history and
  excluding editorial similarity groups as well as previously delivered IDs.
  An exhausted pool returns an explicit no-new-content state; it never recycles
  old entries. Free/premium selection remains server-controlled.
- **Grimoire** automatically lists every delivered entry, newest first, with
  category grouping and the same full detail view. New accounts reach Today
  after sign-in and the existing legal acceptance gate, with no lesson-preference
  or experience questionnaire.
- **Billing** uses two allow-listed premium subscription products
  (`wiccan_premium_monthly` and `wiccan_premium_yearly`) verified
  server-side via the Google Play Developer API; the client's own purchase
  state is never trusted as the tier source of truth. The current paywall
  launches the monthly offer; restore/server verification recognizes both,
  and a daily job re-checks active subscriptions for lapses.
- **Seed content** ships in `functions/ancientContentSeed.json`: 296 entries
  written from four curated ancient sources — 200 lessons, 59 spells and
  37 potions — without difficulty labels. Existing free/premium tiers remain
  independent. `npm run seed-content` validates locally; an authenticated
  `-- --project wiccan-android --commit` creates missing drafts, preserves
  existing edited/published content, retires old difficulty metadata, and
  verifies the live lesson inventory. See `docs/ANCIENT_LESSON_SOURCING.md`.
- **Content generation** draws only from the hand-curated `sources.json`
  entries classified as ancient grimoires or ancient magical manuscripts,
  using each source's vetted facts as the boundary — never quoted or closely
  paraphrased. Preview mode performs no write; production requires explicit
  `--project wiccan-android --commit`. Every generated draft is marked for
  rights review and needs a separate approval before delivery.
- **Auth** uses Android Credential Manager for Google sign-in and
  Firebase Auth for email/password. `users/{uid}` creation is a 1st-gen
  Auth trigger, never a client-side write.

## Content voice and lesson format

[Content Voice and Practical Lesson Standard](docs/CONTENT_VOICE_AND_LESSON_STANDARD.md)
defines the updated editorial target.
[Practical Lesson and Grimoire Templates](docs/PRACTICAL_MAGIC_LESSON_TEMPLATE.md)
provides the fill-in format, nine practical categories, source-review requirements,
and compatibility notes.

Every lesson must teach one source-verified Wiccan or witchcraft method.
Preserve the historical source tradition and label modern adaptations. The
core is a numbered method, supported by tools, documented timing, a
source-dependent formula, reflection, a completion prompt, safety notes,
and exact source/edition/rights references. Do not invent missing ritual
steps or incantations.

This is an authoring target. The current seed and runtime still use the
legacy category/schema contract; coordinated Android and pipeline updates
and a reviewed migration are required before applying it to live content.
Source inspection, rights and safety approval remain necessary even when
structural validation passes.

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

### Android app

```
./gradlew :app:assembleProdDebug
./gradlew :app:testProdDebugUnitTest
# With the Firebase suite already running, install an emulator-routed build:
./gradlew :app:installProdDebug -PuseFirebaseEmulators=true
# Before launching, clear saved Firebase Auth state from the previous mode:
adb shell pm clear com.jaimesorrow.wiccan
```

Clear app data every time you switch between emulator routing and normal
Firebase, including when switching back. Both builds replace the same app;
clearing its data signs you out and removes local settings and cached content.
Use a test device and sign in again against the selected backend.

The app has three product flavors (`dev`/`staging`/`prod`, one per
environment - see Environments below). Production uses the verified
`app/src/prod/google-services.json` for Firebase project `wiccan-android`
and application ID `com.jaimesorrow.wiccan`. Building `dev`/`staging`
as connected environments still requires each environment's own real
`google-services.json` under `app/src/dev/` or `app/src/staging/`.

The current toolchain uses Java 17, Gradle 9.8.1, AGP 9.4.1 and Kotlin
2.4.21, with Play Billing 9.1.0. Install the Android SDK platform with
`sdkmanager "platforms;android-37.0"` (`compileSdk` 37, `targetSdk` 36).
Set `sdk.dir` in a local `local.properties` file or the `ANDROID_HOME`
environment variable. The production Firebase file includes the registered
web OAuth client ID verified in Firebase on 2026-10-06 UTC, allowing the
Google Services plugin to generate `default_web_client_id`. Google and
Email/Password providers are enabled. Confirm that the installed build's
signing certificate is registered with Firebase, and verify Google sign-in
on a device before treating authentication as release-ready.

The opt-in emulator build sends Auth, Firestore, and Functions to the local
ports declared in `firebase.json`. Release builds cannot enable that flag;
see `docs/FIREBASE_SETUP.md` for the three-terminal test flow.

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
- Mutating admin CLI commands require the explicit `--project wiccan-android`
  target instead of inheriting whichever project `firebase use` happens to
  select. Content generation additionally requires `--commit`; without it,
  `npm run content-agent` only validates and previews the request.
- The Android app has matching `dev`/`staging`/`prod` product flavors.
  All flavors install as `com.jaimesorrow.wiccan`, matching the Firebase
  package name. The generated Android namespace is `com.wiccan.app`; existing
  Kotlin class packages and manifest component names remain stable for upgrades.
  Variants replace each other on a device.
  Production uses `app/src/prod/google-services.json`; dev/staging need
  separate project configs registering that same package. `app/google-services.ci.json` remains a deliberately
  fake build fallback and is not the production Firebase configuration.

Package and runtime consistency: `python3 scripts/check-toolchain.py`; regression
checks: `python3 scripts/test-check-toolchain.py`. See
[the branch integration audit](docs/BRANCH_INTEGRATION_AUDIT.md) for incorporated
changes and the retained draft package migration.
