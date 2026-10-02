# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Wiccan is a Kotlin/Jetpack Compose Android app (Kotlin namespace `com.wiccan.app`, production application ID `com.jaimesorrow.wiccan`)
backed by Firebase Cloud Functions (Node/TypeScript, `functions/`). It
delivers one piece of witchcraft content — a spell, potion, or lesson —
per day on a subscription. See `README.md` for the module/package
structure, data model, and core mechanics (daily selection, billing,
content generation, auth) — it's accurate and worth reading first.

Two genuinely separate codebases share this repo (Kotlin/Gradle app,
TypeScript/npm functions). Changing one almost never requires touching
the other except where a callable's request/response shape or a
Firestore document schema is the contract between them — check both
sides when changing either.

## Commands

### Android (`./gradlew ...` from repo root)

The app has three product flavors — `dev`, `staging`, `prod` — one per
Firebase environment. **Only `prod` has a registered `google-services.json`
checked in** (`app/src/prod/`; CI substitutes a placeholder), so generic flavor-less task names
(`assembleDebug`, `testDebugUnitTest`) are ambiguous/will fail on
`dev`/`staging`. Always use the flavor-qualified form:

```
./gradlew :app:assembleProdDebug              # debug APK
./gradlew :app:testProdDebugUnitTest          # unit tests
./gradlew :app:compileProdDebugAndroidTestKotlin  # instrumented tests compile (no emulator to run them here)
./gradlew :app:bundleProdRelease              # release AAB
./gradlew ktlintCheck                         # style (all flavors — flavor-agnostic)
./gradlew :app:lintProdRelease                # Android lint
```

Run a single test class/method with the standard Gradle test filter, e.g.:

```
./gradlew :app:testProdDebugUnitTest --tests "com.wiccan.app.ui.theme.SeasonalThemeResolverTest"
./gradlew :app:testProdDebugUnitTest --tests "*.SeasonalThemeResolverTest.mid-period dates*"
```

After any native-library-affecting dependency bump, also run
`./scripts/check-16kb-alignment.sh` (checks the release AAB for Android
15+ 16KB page-size compliance).

Requires `ANDROID_HOME`/`sdk.dir`. Dev/staging need their own registered
Firebase client configurations; prod has one at `app/src/prod/google-services.json`.

### Cloud Functions (from `functions/`)

```
npm run build          # tsc
npm test               # jest
npm run lint           # eslint src (flat config: eslint.config.mjs)
npm run generate-draft -- --tier <free|premium> ...   # admin CLI, prints resolved project first
npm run approve-draft -- --contentId <id>
```

Run a single Jest test file/case: `npx jest src/__tests__/sendDailyNotifications.test.ts -t "skips a device"`.

## Architecture

**Server is the source of truth, always.** This is the one idea that
explains most of the non-obvious code in both halves of the repo: daily
content selection, subscription tier, the notifications-enabled flag,
terms/privacy acceptance, and device registration are all decided or
verified by a Cloud Function against Firestore/the Play Developer API —
never trusted from client-sent state. Firestore rules enforce this at the
data layer too: most collections are `allow read, write: if false` except
for the Admin SDK, with the client only ever getting read access to its
own already-computed results (`firestore.rules`).

### Android app (`app/src/main/kotlin/com/wiccan/app/`)

- **MVI per screen**: each feature under `ui/<feature>/` has a
  `<Feature>Contract.kt` (`UiState` + sealed `UiEvent` + sealed
  `UiEffect`), a `<Feature>ViewModel` with a single `onEvent(event)`
  dispatcher, and a `<Feature>Screen.kt` Composable. ViewModels expose
  `StateFlow<UiState>` plus a `Flow<UiEffect>` (a `Channel`) for
  one-shot things like snackbars. Screens that need to be previewable
  without a live ViewModel/Hilt split into a public `Screen(viewModel = hiltViewModel())`
  wrapper and an `internal fun <Feature>ScreenContent(state, effects, onEvent)`
  (see `TodayScreen.kt`) — follow that split for new screens with
  previews.
- **`AppResult<T>`/`AppError`** (`core/result/`, `core/error/`) is the
  shared error taxonomy: repositories return `AppResult<T>`, ViewModels
  pattern-match on it and forward `AppError` to the UI via an effect
  (`toUserMessage()` renders it). Don't introduce a different error type
  for a new repository — classify into the existing `AppError` cases.
- **Local-only settings vs. server-backed state**: a purely
  device-local preference (theme mode, notifications-toggle UI cache)
  gets its own small DataStore-backed `*Preferences` class in
  `data/local/` (see `ThemePreferences`, `NotificationPreferences`) and
  is injected directly into ViewModels — it does not need a domain
  `Repository` interface. Anything that talks to Firebase/the network
  goes through a `domain/repository/XRepository` interface + `data/repository/XRepositoryImpl`.
- **Visual identity**: the app renders in one of six `WiccanPalette`
  values (`ui/theme/WiccanPalette.kt`) — Near Black, Midnight Blue, Royal
  Plum, Deep Forest, Warm Earth, Light/Ivory — selected by
  `WiccanThemeMode` (automatic Wheel-of-the-Year via
  `SeasonalThemeResolver`, Follow System, or a locked palette),
  persisted via `ThemePreferences`. **Screens must never hardcode a
  `Color(0x...)`** — always read `MaterialTheme.colorScheme.*` /
  `MaterialTheme.typography.*` so they automatically follow whichever
  palette is active; add new colors to `WiccanPaletteColors.kt` +
  `WiccanColorSchemes.kt` for all six palettes, never inline. Typography
  (`Type.kt`) uses downloadable Google Fonts (Cinzel for
  display/headings, Manrope for everything else) — there is no emulator
  in most sandboxes to screenshot it; use the `@Preview`s in
  `WiccanThemePreviews.kt`.
- **The Glance home-screen widget** (`widget/`) renders in its own
  composition, not the app's — it can't read `MaterialTheme.colorScheme`
  and instead resolves the active palette directly via
  `resolveWiccanPalette(...).toColorScheme()`, reached through a Hilt
  `EntryPoint` (`WiccanWidgetEntryPoint`) since Glance doesn't construct
  widgets through Hilt.
- **Product flavors** (`dev`/`staging`/`prod`) map 1:1 to the three
  Firebase environments in `.firebaserc`; each needs its own
  `google-services.json` under `app/src/<flavor>/`.

### Cloud Functions (`functions/src/`)

- **Ports-and-adapters per resource**: most Firestore-backed concerns
  are a `xPort.ts` interface, a `firestoreXRepository.ts` implementation,
  and a `testUtils/fakeXRepository.ts` in-memory fake used by that
  resource's Jest tests — follow this shape for a new resource rather
  than querying Firestore directly from a callable.
  `functions/src/index.ts` wires callables/scheduled functions/triggers
  to their concrete repositories; keep boundary concerns (App Check
  enforcement, input validation, auth checks) in `index.ts` and business
  logic in the pure port implementations, matching the existing split.
- **App Check is enforced on every user-facing callable** — new
  callables need it too (see any existing callable in `index.ts` for the
  pattern).
- **Runtime kill switches**: `systemConfig/global` (Firestore, no client
  access) holds `dailyContentEnabled`/`notificationsEnabled`, both
  fail-open (`!== false`) if the doc/field is missing — read via
  `SystemConfigPort`/`FirestoreSystemConfigRepository`. There is no
  write path in code; flipping them is a deliberate manual operator
  action (see `docs/MAINTENANCE.md`).
- **`sources.json`** (curated public-domain source material for content
  generation) is manually maintained, never auto-populated — see its own
  doc comment before treating a low source count as a bug.

## Known standing constraints

- **No authenticated Firebase project access, signing keystore, Android emulator, or
  Play Console session exists in a fresh sandbox.** `dev`/`staging`
  flavors, `WiccanDatabaseMigrationTest`, RTDN/purchase-verification
  field shapes, and any on-device screenshot are consequently
  unverifiable here — say so rather than fabricating a result. See
  `docs/STATUS.md` for the current, honestly-tracked list of what's
  fixed vs. what's blocked on external infrastructure.
- **Play Billing Library is 9.1.0**, on Gradle 9.8 / AGP 9.4.1 / Kotlin
  2.4.20 / KSP 2.3.12 / Hilt 2.60.1 / Room 2.8.5 / compileSdk 37. Getting
  there required moving all of them together — each one forced the next;
  the chain is recorded in `docs/MAINTENANCE.md`. Two AGP 9 rules follow
  from it: the `kotlin.android` plugin must stay **removed** (AGP 9 has
  built-in Kotlin support), and `jvmTarget` lives in the top-level
  `kotlin { compilerOptions { } }` block, not `android.kotlinOptions`.
- `docs/MAINTENANCE.md` also documents the seasonal theme's exact
  date-to-palette mapping and reasoning, and everything else that's a
  recurring operational task rather than a one-off.
