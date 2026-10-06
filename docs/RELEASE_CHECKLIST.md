# Release checklist (technical preflight)

Everything on this list is either verified by this repo's own build/test
commands, or explicitly marked as needing a real device, a real Firebase
project, or a human decision this sandbox can't make safely. Nothing here
is guessed at — where a step can't be verified without real infrastructure,
it says so instead of pretending.

## Signing

- [ ] Use **Play App Signing** for the production listing. Google holds the app-signing key; your locally generated keystore should be treated as the **upload key**, not as the irreplaceable app-signing key.
- [ ] Generate and securely back up the upload keystore (`keytool -genkeypair -v -keystore upload.keystore.jks -alias wiccan-upload -keyalg RSA -keysize 2048 -validity 10000`) **outside** this repo.
- [ ] Copy `app/keystore.properties.example` to `app/keystore.properties` and fill in the upload key's real `storeFile`/`storePassword`/`keyAlias`/`keyPassword`. `app/build.gradle.kts` picks this up automatically when present; otherwise release bundles are unsigned.
- If an upload key used with Play App Signing is lost or compromised, request an upload-key reset in Play Console. The unrecoverable-key warning applies only if you deliberately self-manage the actual app-signing key instead of using Play App Signing.

### Building the signed bundle in CI (no local Android SDK needed)

`.github/workflows/release.yml` produces the uploadable `.aab` on a GitHub
runner, so cutting a release never depends on a local toolchain. The
keystore exists only as a repository secret and in the runner's temp
directory — never in the repo, never in the build log.

No machine with `keytool`? **Google Cloud Shell**
(<https://shell.cloud.google.com>) runs in the browser and already has
`keytool` and `base64`, so the upload key can be created without
installing anything and without routing it through a third party:

```bash
keytool -genkeypair -v -keystore upload.keystore.jks -alias wiccan-upload \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 upload.keystore.jks    # the value for RELEASE_KEYSTORE_BASE64
```

Download the `.jks` and back it up before closing Cloud Shell — its home
directory is not a backup.

Then add four repository secrets under **Settings → Secrets and variables
→ Actions**:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | the `base64 -w0` output above |
| `RELEASE_KEYSTORE_PASSWORD` | the store password |
| `RELEASE_KEY_ALIAS` | `wiccan-upload`, or whatever `-alias` you used |
| `RELEASE_KEY_PASSWORD` | the key password |

Run it from the **Actions** tab, or push a `v*` tag.

- The workflow refuses to publish an unsigned bundle. This matters because
  `app/build.gradle.kts` deliberately falls back to *unsigned* when no
  signing config is present, so a missing or mistyped secret would
  otherwise produce a bundle that looks successful and is rejected only
  after upload. The gate looks for `META-INF/<ALIAS>.RSA` inside the
  `.aab`: `jarsigner`'s exit code is `0` even for an unsigned bundle, so it
  cannot be the gate on its own.
- Both CI and the release workflow use the authoritative prod source-set
  config at `app/src/prod/google-services.json`. CI additionally copies a
  fake root fallback; that fallback does not override the prod config.
- [ ] `versionCode` in `app/build.gradle.kts` must strictly increase on
  every Play submission; bump it before each run (see Versioning below).
- [ ] After the first upload, add the **Play App Signing** certificate's
  SHA-1 to Firebase. Google re-signs the app with its own key, so the
  certificate on users' devices is not the upload key — Google sign-in and
  App Check both fail in production if only the upload key's fingerprint
  is registered.

## Minification (R8/ProGuard)

- **Current state: `isMinifyEnabled = false`.** `app/proguard-rules.pro` is empty.
- This is a deliberate decision to leave in place for this pass: turning on R8 shrinking/obfuscation can break reflection-heavy code paths (Firebase, Hilt-generated code, Room) in ways that only surface at runtime on a real device — something this sandbox has no emulator to catch. Flipping it on blindly and shipping wrong keep rules would be worse than leaving it off.
- [ ] Before enabling: set `isMinifyEnabled = true`, run a full manual smoke test on a real device/emulator (sign-in, Today content load, Grimoire, Paywall, widget, notifications, sign-out, delete account), and add keep rules for anything that breaks. Most AndroidX/Firebase libraries ship their own consumer ProGuard rules, so a clean run is plausible but not guaranteed — that's exactly what the smoke test is for.

## Versioning

- `versionCode`/`versionName` live in `app/build.gradle.kts` (`defaultConfig`). Bump both on every Play submission — `versionCode` must strictly increase, Play rejects a re-upload otherwise.
- [ ] Decide a versioning scheme (this checklist doesn't impose one) and document it once chosen.

## 16KB page size (Android 15+)

- [x] `scripts/check-16kb-alignment.sh` passed on the 2026-10-02 Billing 9.1.0 release bundle: all eight native libraries have 16 KB LOAD alignment. Re-run for each release bundle.

## Product flavors (dev/staging/prod)

- The app builds three product flavors, one per environment (see the README's Environments section) — `dev`/`staging`/`prod`, matching `.firebaserc`'s aliases. Production uses the verified source-specific config at `app/src/prod/google-services.json` for Firebase project `wiccan-android` and application ID `com.jaimesorrow.wiccan`. CI still copies `app/google-services.ci.json` as a non-production fallback, but the prod source-set config is authoritative for the prod variant. `dev`/`staging` still need their own real Firebase projects/configs before those variants can be treated as connected environments.
- [ ] Once real dev/staging Firebase projects exist, add their `google-services.json` files and confirm `./gradlew :app:assembleDevDebug` / `:app:assembleStagingDebug` build cleanly.

## Automated checks (branch integration run on 2026-10-02; rerun before release)

See [BUILD_VALIDATION.md](BUILD_VALIDATION.md) for exact results and limits.
The baseline is Java 17, Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20 and
Billing 9.1.0, with `compileSdk` 37 / `targetSdk` 36; the SDK package is
`platforms;android-37.0`. Hosted Actions reports `startup_failure` before
jobs execute; its exact cause is unconfirmed. Local Android tests: 86
passed across 18 suites. Functions on Node 22: fresh `npm ci`, build, lint and 98
tests across 14 suites passed. Ktlint plugin 14.2.0 restores Android source
coverage with engine 1.0.1. Debug and release builds, instrumented-test
compilation and 16 KB alignment pass. Release lint reports zero errors and
62 nonblocking warnings. The release bundle is unsigned.

- [x] `bash scripts/release-preflight.sh` — verifies package/Firebase identity, App Check wiring, protected writes, RTDN retry behavior, WorkManager initialization, and obvious committed server-secret patterns
- [x] `python3 scripts/test-check-toolchain.py` and `python3 scripts/check-toolchain.py` — 21 regression tests and package/runtime consistency
- [x] `python3 scripts/test-validate-repo.py` — eight isolated failure-path regression checks
- [x] `./gradlew ktlintCheck :app:testProdDebugUnitTest`
- [x] `./gradlew :app:assembleProdDebug :app:compileProdDebugAndroidTestKotlin`
- [x] `./gradlew :app:bundleProdRelease :app:lintProdRelease` — release bundle and lint gate (bundle is unsigned without `keystore.properties`)
- [x] `./scripts/check-16kb-alignment.sh` — all eight bundled libraries pass
- [x] `cd functions && npm ci && npm run build && npm test && npm run lint`

## Needs a real device/emulator (not verified here)

- [ ] `WiccanDatabaseMigrationTest` (Room migration 1→2) actually run via `connectedAndroidTest` — compiles clean here, never executed against a real SQLite engine.
- [ ] Deep links (`wiccan://today`, `wiccan://premium`) via `adb shell am start -a android.intent.action.VIEW -d "wiccan://today"`.
- [ ] Auto Backup exclusions (`data_extraction_rules.xml`/`backup_rules.xml`): back up a real install, restore it, confirm no stale session/content survives.
- [ ] A real Play Billing sandbox purchase, to confirm `verifyPurchase`'s acknowledgement/expiry field shapes behave as expected against a live purchase (they compile clean against `@googleapis/androidpublisher`'s real types, but have never been exercised against a real purchase).
- [ ] `handlePlayRtdn` end-to-end: Play Console → Pub/Sub → this function, once Real-time developer notifications are wired up for the real project.

## Needs a real Firebase project (not verified here)

- [ ] Deploy `firestore.rules`/`firestore.indexes.json` and confirm the app reads/writes correctly against them (rules are logically reviewed, never deployed).
- [ ] Confirm the `users/{uid}/devices` collection-group read (`listDevicesWithToken`) and the `titleExists` title-projection query need no manual composite index — both should be index-free for an unfiltered `.get()`/`.select()`, but that's only confirmable against a real project.
- [x] Production Firebase CLI alias is set to `wiccan-android` in `.firebaserc`.
- [ ] Create/assign real dev and staging Firebase project IDs; those two aliases intentionally remain placeholders so development commands cannot accidentally target production.
- [ ] Create the production Functions runtime configuration/service-account access required for Google Play Developer API calls; do not commit service-account JSON or private keys.
