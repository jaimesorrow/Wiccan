---
name: steward
description: Repo-specific conventions for driving a Wiccan PR to green — which validation commands actually apply, what must never be fabricated or guessed, and standing toolchain constraints. Read before acting on CI failures or review comments on a PR you opened or drive for its author.
---

# Wiccan PR stewardship

This repo is two genuinely separate codebases sharing one PR pipeline: an
Android app (Kotlin/Gradle) and Firebase Cloud Functions
(TypeScript/npm). Figure out which one(s) a PR's diff actually touches
before picking commands — don't run both toolchains' full suites out of
habit, and don't skip one that's actually in scope.

## Validation commands (use the flavor-qualified/exact forms)

**Android** — the app has `dev`/`staging`/`prod` product flavors and only
`prod` has a usable `google-services.json` checked in. Generic
flavor-less task names (`assembleDebug`, `testDebugUnitTest`) are
ambiguous and will fail on `dev`/`staging` — always qualify:

```
./gradlew :app:assembleProdDebug
./gradlew :app:testProdDebugUnitTest
./gradlew :app:compileProdDebugAndroidTestKotlin
./gradlew :app:bundleProdRelease
./gradlew ktlintCheck
./gradlew :app:lintProdRelease
./scripts/check-16kb-alignment.sh   # after any native-lib-affecting dependency change
```

**Functions** (from `functions/`):

```
npm run build
npm test
npm run lint
```

A PR touching both `app/` and `functions/` needs both suites green, not
just the one that happens to compile first.

## Never fabricate, never guess

- No real Firebase project, signing keystore, Play Console listing, or
  Android emulator/device exists in this sandbox. Never invent a
  project ID, API key, or credential to make something "work" — leave
  it as documented scaffolding (`.firebaserc`, `keystore.properties.example`)
  and say so plainly in the PR/comment instead of papering over it.
- `WiccanDatabaseMigrationTest` and any Android instrumented test compile
  here but have never executed against a real device — don't report them
  as "passing," only as "compiles clean."
- There is no way to capture a real on-device screenshot here. Visual
  changes are validated via the `@Preview` composables in
  `ui/theme/WiccanThemePreviews.kt` (or new previews alongside a changed
  screen) — never claim a fabricated screenshot exists.
- Don't guess at a Compose/Material3/Firebase/Play SDK's exact method
  signature, default parameter, or certificate/config value from
  training-data recall alone when it's easy to verify — check the real
  Maven metadata, the real decompiled artifact in `~/.gradle/caches`, or
  the library's own docs/source first (this is how the Google Fonts
  provider setup and the WorkManager-initializer lint fix were done —
  verified against real sources, not recalled). If verification isn't
  possible and real uncertainty remains, say so and propose the
  smallest safe next step rather than shipping a guess.

## Repo-specific gotchas worth knowing before touching code

- **Never hardcode a `Color(0x...)` in Compose UI.** The app renders in
  one of six palettes (`WiccanPalette`); every screen reads
  `MaterialTheme.colorScheme.*`/`MaterialTheme.typography.*` so it
  follows whichever palette is active. A new color goes into
  `WiccanPaletteColors.kt` + `WiccanColorSchemes.kt` for all six
  palettes, not inline at the call site.
- **Firestore rules default to `allow read, write: if false`** for
  server-controlled collections (`users/{uid}/devices`, `systemConfig/*`,
  etc.) — a fix that needs the client to read/write more of Firestore is
  very likely solving the wrong layer; the correct fix almost always
  routes through a new or existing Cloud Function callable instead of
  loosening `firestore.rules`.
- **Play Billing Library is pinned to 7.1.1 on purpose** — bumping to
  9.x needs Kotlin ≥2.3.0, which hits a real KSP/Hilt Gradle-plugin
  classloader conflict. Don't re-attempt this bump without first reading
  the exact blocker and next steps in `docs/MAINTENANCE.md`.
- **`docs/STATUS.md`** is the canonical, actively-maintained tracker of
  what's actually fixed vs. still blocked on external infrastructure. A
  PR that changes that picture (fixes a flagged gap, or introduces a new
  known limitation) should update it in the same PR — don't let it drift
  out of sync with what the code actually does.
- **`functions/sources.json`** (content-generation source material) is
  manually curated and deliberately small — a low count is not a bug to
  "fix" by auto-populating it.
