---
name: run-wiccan
description: Build, run, screenshot and drive the Wiccan Android app and its Firebase Cloud Functions backend. Use when asked to run, start, build, launch, screenshot, or manually verify the app or backend — including seeing the six palettes, exercising a callable, or reproducing a UI change without a device.
---

# Running Wiccan

Two deployable surfaces sharing one repo: an **Android app** (Kotlin/Compose)
and a **Cloud Functions backend** (TypeScript). Both are drivable from a
headless container.

Everything goes through one script. Paths are relative to the repo root.

```bash
.claude/skills/run-wiccan/driver.sh          # usage
.claude/skills/run-wiccan/driver.sh deps     # one-time: Android SDK + firebase CLI
```

| Command | What it does |
|---|---|
| `deps` | Installs the Android SDK (~760MB) and `firebase-tools`, writes `local.properties` |
| `build` | `:app:assembleProdDebug` → APK |
| `screenshots` | **Renders all six palettes to PNG. No emulator needed.** |
| `backend` | Starts the Firebase emulators and smoke-tests a callable |
| `test` | Full check matrix, both toolchains |
| `all` | build + screenshots + backend |

## There is no Android emulator here

`/dev/kvm` does not exist in this container, so an AVD cannot run. **Do not
try to launch one.** To see the UI, use `screenshots` — it renders Compose on
the JVM via Robolectric + Roborazzi and writes real PNGs:

```bash
.claude/skills/run-wiccan/driver.sh screenshots
```

```
app/build/outputs/roborazzi/wiccan-palette-near_black.png
app/build/outputs/roborazzi/wiccan-palette-midnight_blue.png
app/build/outputs/roborazzi/wiccan-palette-royal_plum.png
app/build/outputs/roborazzi/wiccan-palette-deep_forest.png
app/build/outputs/roborazzi/wiccan-palette-warm_earth.png
app/build/outputs/roborazzi/wiccan-palette-light_ivory.png
```

**Open them.** They are 822x1782 and show display/body type, a card, a
filled button and an outlined button against each palette's background —
enough to judge color and contrast. The test fails if any renders flat.

The harness is `app/src/test/kotlin/com/jaimesorrow/wiccan/ui/theme/WiccanPaletteScreenshotTest.kt`.
To screenshot a *different* screen, copy its `PaletteSpecimen` pattern: wrap
your composable in `MaterialTheme(colorScheme = palette.toColorScheme(),
typography = LocallyResolvableTypography)` and call
`composeRule.onRoot().captureRoboImage(filePath = ...)`.

Run just that test directly:

```bash
./gradlew :app:testProdDebugUnitTest --tests '*WiccanPaletteScreenshotTest*'
```

## Backend

```bash
.claude/skills/run-wiccan/driver.sh backend
```

Brings up auth (9099), functions (5001), firestore (8080) and pubsub (8085),
then asserts a callable is live. Expected output:

```
Emulators ready after 15s
  getTodayContent (no auth) -> {"error":{"message":"Unauthenticated","status":"UNAUTHENTICATED"}}
  OK: the callable is live and rejecting unattested calls
```

Emulator UI at <http://127.0.0.1:4000>. Stop with
`pkill -f 'emulator[s]:start'`.

Call a function by hand:

```bash
curl -s -X POST http://127.0.0.1:5001/wiccan-android/us-central1/getTodayContent \
  -H "Content-Type: application/json" -d '{"data":{}}'
```

## Build and test

```bash
.claude/skills/run-wiccan/driver.sh build     # -> app/build/outputs/apk/prod/debug/app-prod-debug.apk
.claude/skills/run-wiccan/driver.sh test      # both toolchains
```

Current baseline: Java 17, Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20 and
Play Billing 9.1.0. The Android SDK package is `platforms;android-37.0`
(`compileSdk` 37, `targetSdk` 36); `driver.sh deps` installs that package.
Ktlint plugin 14.2.0 registers Android source checks under AGP 9, with
engine 1.0.1 pinned to the project's formatting rules.
Cloud Functions uses Node 22, matching `functions/package.json` and CI.
For backend emulators, use Firebase CLI 15 or newer with Java 21 or newer.
CLI 14 calls the removed `functions.config()` API under Functions SDK 7 and
its request workers can fail at startup. Upgrade an existing CLI before
running `backend`, and select Java 21 for that command; keep Java 17 for the
Android checks above.

`test` runs `ktlintCheck`, `:app:testProdDebugUnitTest`,
`:app:assembleProdDebug`, `:app:compileProdDebugAndroidTestKotlin`,
`:app:lintProdRelease`, `:app:bundleProdRelease`,
`scripts/check-16kb-alignment.sh`, `scripts/release-preflight.sh`, then
`npm run build && npm test && npm run lint` in `functions/`.

Always use **flavor-qualified** Gradle tasks. `assembleDebug` fails, because
`dev` and `staging` have no `google-services.json`; only `prod` does.

## Gotchas

These cost real time. All were hit in this container.

- **`ANDROID_PACKAGE_NAME` must be exported when the emulator starts**, not
  in your test process. `deleteUserData` cancels the Play subscription first
  and `createBillingRepository()` throws without it, so account deletion
  fails `INTERNAL` while everything else passes. The functions run inside the
  emulator's process. The driver sets it; if you start emulators by hand, do
  the same.
- **App Check blocks every callable in the emulator.** All 8 are
  `enforceAppCheck: true` and there is no App Check emulator to mint a token,
  so a *valid* ID token still gets `UNAUTHENTICATED`. The emulator log shows
  `{"verifications":{"app":"MISSING","auth":"VALID"}}`. To exercise authed
  flows locally, patch `functions/lib/index.js` (gitignored build output —
  **never `functions/src`**), then `npm run build` to restore.
- **Robolectric instantiates `WiccanApplication` unless told otherwise**, and
  its `onCreate` installs App Check → `IllegalStateException` at
  `FirebaseApp.java:179`. Every Robolectric test here needs
  `@Config(application = android.app.Application::class)`.
- **The app's fonts are downloadable Google Fonts** (Cinzel, Manrope via Play
  Services). Robolectric has no font provider, so using `WiccanTypography`
  directly means Compose never reaches idle. Use
  `LocallyResolvableTypography` from the screenshot test — real type scale,
  system glyphs.
- **`captureToImage()` from `ui-test-junit4` does not work under
  Robolectric.** It calls `forceRedraw`, which needs a real hardware window,
  and dies with `ComposeTimeoutException: Condition still not satisfied after
  2000 ms`. Use Roborazzi's `captureRoboImage` instead.
- **Historical Roborazzi/Kotlin mismatch.** Roborazzi 1.76.0's Kotlin 2.3.0
  metadata failed under the project's former Kotlin 2.0.21 toolchain.
  The current Kotlin 2.4.20 baseline passes the screenshot tests with
  Roborazzi 1.32.2. Verify any later Roborazzi upgrade with those tests;
  the old compiler mismatch does not establish a current upgrade blocker.
- **Roborazzi writes nothing unless recording is on.** The test passes and
  produces zero files. `app/build.gradle.kts` sets
  `systemProperty("roborazzi.test.record", "true")` for all test tasks.
- **`javax.imageio` is not on the Android unit-test classpath** — importing
  it fails with `Unresolved reference 'imageio'` even though tests run on the
  JVM. Use `android.graphics.BitmapFactory`.
- **`setContent` may only be called once per `ComposeTestRule`.** Loop by
  swapping state, not by calling it again.
- **Hosted Actions startup failures.** Recent runs end in `startup_failure`
  with zero jobs. Their API records do not identify the exact cause;
  the earlier billing diagnosis still needs owner confirmation. Record
  local results separately and check the run-page error and owner Actions
  billing/budget settings before retrying.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `SDK location not found` | `.claude/skills/run-wiccan/driver.sh deps` |
| `Could not download kotlin-reflect-…jar` during `lintProdRelease` | Transient artifact fetch. Re-run the task; it passes. |
| `IllegalStateException at FirebaseApp.java:179` in a unit test | Add `@Config(application = android.app.Application::class)` |
| `ComposeTimeoutException` while capturing | Using `captureToImage()`; switch to `captureRoboImage` |
| Screenshot test passes but no PNGs | `roborazzi.test.record` not set |
| `deleteUserData` fails `INTERNAL` in the emulator | `ANDROID_PACKAGE_NAME` was not exported when the emulator started |
| Emulator port already in use | `pkill -f 'emulator[s]:start'` |
