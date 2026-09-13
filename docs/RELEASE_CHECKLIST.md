# Release checklist (technical preflight)

Everything on this list is either verified by this repo's own build/test
commands, or explicitly marked as needing a real device, a real Firebase
project, or a human decision this sandbox can't make safely. Nothing here
is guessed at — where a step can't be verified without real infrastructure,
it says so instead of pretending.

## Signing

- [ ] Generate a real release keystore (`keytool -genkeypair -v -keystore release.keystore.jks -alias wiccan -keyalg RSA -keysize 2048 -validity 10000`) and store it somewhere durable **outside** this repo — losing it means you can never publish an update to an app already live under it.
- [ ] Copy `app/keystore.properties.example` to `app/keystore.properties` and fill in the real `storeFile`/`storePassword`/`keyAlias`/`keyPassword`. `app/build.gradle.kts` picks this up automatically when present (`bundleProdRelease`/`assembleProdRelease` builds unsigned without it, same as today).
- [ ] Decide Play App Signing vs. self-managed signing at first upload. Play App Signing (Google holds the app signing key, you keep an upload key) is Google's recommended default and is what this checklist assumes; a self-managed key has no recovery path if lost.

## Minification (R8/ProGuard)

- **Current state: `isMinifyEnabled = false`.** `app/proguard-rules.pro` is empty.
- This is a deliberate decision to leave in place for this pass: turning on R8 shrinking/obfuscation can break reflection-heavy code paths (Firebase, Hilt-generated code, Room) in ways that only surface at runtime on a real device — something this sandbox has no emulator to catch. Flipping it on blindly and shipping wrong keep rules would be worse than leaving it off.
- [ ] Before enabling: set `isMinifyEnabled = true`, run a full manual smoke test on a real device/emulator (sign-in, Today content load, Archive/Library, Paywall, widget, notifications, sign-out, delete account), and add keep rules for anything that breaks. Most AndroidX/Firebase libraries ship their own consumer ProGuard rules, so a clean run is plausible but not guaranteed — that's exactly what the smoke test is for.

## Versioning

- `versionCode`/`versionName` live in `app/build.gradle.kts` (`defaultConfig`). Bump both on every Play submission — `versionCode` must strictly increase, Play rejects a re-upload otherwise.
- [ ] Decide a versioning scheme (this checklist doesn't impose one) and document it once chosen.

## 16KB page size (Android 15+)

- [x] `scripts/check-16kb-alignment.sh` inspects every native library bundled in the release `.aab` and fails if a 64-bit ABI's (arm64-v8a, x86_64) ELF LOAD segments aren't 16KB-aligned. Runs in CI on every push. Currently passing — the one bundled native library (`libandroidx.graphics.path.so`, a transitive AndroidX dependency) is already 16KB-aligned on every ABI.

## Product flavors (dev/staging/prod)

- The app builds three product flavors, one per environment (see the README's Environments section) — `dev`/`staging`/`prod`, matching `.firebaserc`'s aliases. Only `prod` (applicationId `com.wiccan.app`, no suffix) has a CI-usable `google-services.json` (the checked-in placeholder, `google-services.ci.json`); `dev`/`staging` need their own real one from that environment's Firebase project before they'll build (`app/src/dev/google-services.json`, `app/src/staging/google-services.json`).
- [ ] Once real dev/staging Firebase projects exist, add their `google-services.json` files and confirm `./gradlew :app:assembleDevDebug` / `:app:assembleStagingDebug` build cleanly.

## Automated checks (already verified by this repo)

- [x] `./gradlew clean ktlintCheck :app:testProdDebugUnitTest :app:assembleProdDebug :app:compileProdDebugAndroidTestKotlin`
- [x] `./gradlew :app:bundleProdRelease` — builds (unsigned without `keystore.properties`)
- [x] `./scripts/check-16kb-alignment.sh` — all bundled native libraries pass
- [x] `cd functions && npm run build && npm test && npm run lint`

## Needs a real device/emulator (not verified here)

- [ ] `WiccanDatabaseMigrationTest` (Room migration 1→2) actually run via `connectedAndroidTest` — compiles clean here, never executed against a real SQLite engine.
- [ ] Deep links (`wiccan://today`, `wiccan://premium`) via `adb shell am start -a android.intent.action.VIEW -d "wiccan://today"`.
- [ ] Auto Backup exclusions (`data_extraction_rules.xml`/`backup_rules.xml`): back up a real install, restore it, confirm no stale session/content survives.
- [ ] A real Play Billing sandbox purchase, to confirm `verifyPurchase`'s acknowledgement/expiry field shapes behave as expected against a live purchase (they compile clean against `@googleapis/androidpublisher`'s real types, but have never been exercised against a real purchase).
- [ ] `handlePlayRtdn` end-to-end: Play Console → Pub/Sub → this function, once Real-time developer notifications are wired up for the real project.

## Needs a real Firebase project (not verified here)

- [ ] Deploy `firestore.rules`/`firestore.indexes.json` and confirm the app reads/writes correctly against them (rules are logically reviewed, never deployed).
- [ ] Confirm the `users/{uid}/devices` collection-group read (`listDevicesWithToken`) and the `titleExists` title-projection query need no manual composite index — both should be index-free for an unfiltered `.get()`/`.select()`, but that's only confirmable against a real project.
- [ ] Fill in `.firebaserc`'s placeholder project IDs and each environment's `functions/.env.<project-id>` (see `functions/.env.example` and the README's Environments section).
