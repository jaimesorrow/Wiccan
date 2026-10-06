# Main integration validation — 2026-10-02

The results below belong to the pre-migration integration branch merged in PR #43.
PR #42's requested Firebase package migration is now on main via `b13dc64b`.
The historical local builds do not validate the migrated package tree. The
later hosted validation below does verify its automated build matrix. See
[PR_RECONCILIATION.md](PR_RECONCILIATION.md) for the package audit and device limits.

Validated `codex/integrate-branches-2026-10-02` against main commit
`6e2e2b33f0a2813d4ff7dd6eb2bd9696d6764eaf`, including merged PRs #40
and #41. All 36 merged PRs are present; the branch audit preserves the
remaining historical heads and carries forward the useful missing changes.
See [BRANCH_INTEGRATION_AUDIT.md](BRANCH_INTEGRATION_AUDIT.md). The branch
retains the Gradle 9 / AGP 9 / Kotlin 2.4 / Billing 9 modernization. This records automated build results,
not release approval or completion of every planned product feature.

## Fixes found during validation

- Preserve all audited branch histories, port missing Google sign-in
  availability checks with six tests, and cancel superseded CI runs.
- Align CI, lockfile runtime metadata, `.nvmrc` and locked run-driver installs
  with the merged Node 22 Functions runtime. Remove the unused Kotlin Android
  plugin alias and add a package/toolchain drift gate, adapted from draft PR #42.
  At that validation point the namespace/flavor migration remained separate.
  PR #42 now carries it as requested; widget upgrade behavior and the changed
  installed flavor identities still require device verification.

- ktlint plugin 12.1.1 reported success under AGP 9 while checking only
  Gradle Kotlin scripts. Plugin 14.2.0 restores checks of all five populated
  Android source sets (134 Kotlin files after the auth regression test).
  The lint engine stays at 1.0.1,
  preserving the existing formatting rules.
- The run driver installed API 36 although main now compiles against 37.
  It now requests Google's `platforms;android-37.0` and `build-tools;37.0.0`,
  reuses an installed SDK manager and preserves unrelated local properties.
- Repository validation could use stale Functions dependencies, ignore an
  install failure, or check an old AAB after bundle compilation failed.
  It now requires `npm ci`, records failures and skips dependent checks.
  Failed Git conflict/secret scans also fail validation.

## Verified locally

| Check | Result |
|---|---|
| Full `scripts/validate-repo.sh` | PASS — structure, 31 preflight checks and both build matrices |
| `ktlintCheck` | PASS — Kotlin source sets and Gradle scripts |
| `:app:testProdDebugUnitTest` | PASS — 86 tests across 18 suites |
| `:app:assembleProdDebug` | PASS — debug APK produced |
| `:app:compileProdDebugAndroidTestKotlin` | PASS — compilation only |
| `:app:bundleProdRelease` | PASS — unsigned release AAB produced |
| `:app:lintProdRelease` | PASS — 0 errors, 62 warnings |
| `scripts/check-16kb-alignment.sh` | PASS — all 8 libraries have 16 KB LOAD alignment |
| Functions `npm ci`, build and lint | PASS |
| Functions Jest | PASS — 98 tests across 14 suites |
| Isolated backend emulator | PASS — 18 business assertions, 3 Auth-trigger assertions, original App Check rejection |
| Validator regression tests | PASS — 8 isolated tests |
| Package/toolchain consistency and regression tests | PASS — 21 isolated tests |
| CI/release workflow syntax | PASS — Actionlint 1.7.12 |
| Run driver SDK setup | PASS — 6 isolated fresh/existing configuration checks |
| Fresh SDK manager Java compatibility | PASS — official CLI revision 23.0 runs on Java 17 |

Android used Java 17.0.20.1, Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20 and
Android SDK 37.0 / Build Tools 37.0.0 (`compileSdk` 37, `targetSdk` 36).
Functions used Node 22.23.3, matching PR #40 and the runtime declared in
`functions/package.json`. CI now reads that declaration directly.
Android validation started with `clean`.
A temporary local Gradle init script routed Maven Central and Robolectric
runtime downloads through
`https://maven-central.storage-download.googleapis.com/maven2/` to avoid
artifact rate limiting. Repository repositories were not changed. Gradle
workers were limited to two for the shared build environment.

Reproduce from the repository root with Java 17, Node 22 and an installed SDK:

```bash
./gradlew clean
python3 scripts/test-check-toolchain.py
python3 scripts/check-toolchain.py
python3 scripts/test-validate-repo.py
./scripts/validate-repo.sh
```

The validator includes identity/security preflight, formatting, Android
unit tests, debug packaging, instrumented-test compilation, release lint,
release packaging, native alignment, and locked Functions installation,
build, tests and lint. CI also runs the validator regression tests.

All six 822×1782 palette PNGs were generated under
`app/build/outputs/roborazzi/` and opened for inspection. Specimen text and
controls render legibly. The harness uses system glyphs at the real type
scale; downloadable fonts, full app screens, transitions, widgets and
device behavior still require device review.

## Hosted validation after the package migration — 2026-10-02

Main `b13dc64b97157bc08a4b2bac919b542c42971803`, containing PR #42's Firebase
package migration, passed [CI run 37000862807](https://github.com/jaimesorrow/Wiccan/actions/runs/37000862807).
The Android job completed on 2026-10-02 at 11:32 UTC; the Functions job also passed.

| Check | Hosted result |
|---|---|
| Package/toolchain gate and structure/preflight | PASS — 21 gate tests, 8 validator tests, 31 preflight checks |
| `ktlintCheck` and `:app:testProdDebugUnitTest` | PASS — includes the migrated auth tests |
| `:app:assembleProdDebug` | PASS |
| `:app:compileProdDebugAndroidTestKotlin` | PASS — compilation only |
| `:app:bundleProdRelease` and `:app:lintProdRelease` | PASS — unsigned bundle |
| Native alignment | PASS — all 8 libraries checked; all 64-bit libraries aligned to 16 KB |
| Functions locked install, build, Jest and lint | PASS — 98 tests across 14 suites |

This supersedes the earlier hosted startup failures and the package migration's
dependency-download blocker. Java 17 and Functions Node 22 remain the supported
build runtimes. Installed-app, widget and Room upgrades still need device testing;
compiling instrumented tests does not execute them.

## Current backend emulator evidence

The 2026-10-02 isolated `demo-wiccan-integration-20261002` Firebase emulator run
passed all 18 business-flow assertions on Node 22.23.3, Firebase CLI 15.32.1
and Java 21.0.12.1, with the merged Admin 14.5 / Functions 7.4 dependencies.
All 12 function definitions initialized. The original App Check rejection was
verified first; enforcement was then temporarily disabled only in the isolated,
generated `functions/lib/index.js` for authenticated business flows, following
the run skill. A rebuild restored all eight enforcement settings, and all 67
Functions source-file hashes remained unchanged. No production project was used.
This does not verify live Play purchases, FCM delivery or production App Check
attestation.

Firebase CLI 14.22 request workers failed because its runtime adapter calls
`functions.config()`, removed by Functions SDK 7. CLI 15 supports the new SDK;
its emulator tooling requires Java 21 or newer, separately from Android's
validated Java 17 baseline. A separate Auth-trigger diagnostic passed all three
assertions: sign-up, automatic profile creation without `acceptTerms`, and server
free-tier/timestamp values. CLI 15's transport initially sent its loopback Auth
callback through the environment proxy despite `NO_PROXY`. A temporary,
loopback-only change to the isolated installed CLI confirmed trigger delivery;
the original CLI file was restored byte-for-byte afterward. All owned emulator
processes stopped and ports closed. Repository code and external proxy handling
were unchanged.

## Remaining limits

- The release AAB has no signature files. Signing, Play/Firebase setup and
  live flows remain release prerequisites in `RELEASE_CHECKLIST.md`.
- Android lint reports 62 nonblocking warnings, including 35 spelling
  notices and dependency/API/configuration suggestions. Kotlin and Gradle
  also report deprecations. Passing checks do not mean zero warnings.
- Device/Room migration execution, dev/staging Firebase configurations,
  hosted legal pages and editorial/rights approval of draft content remain
  outstanding.
- The daily shuffle/Grimoire update adds Android support for structured
  reflection, practice and source/rights metadata, with an additive Room upgrade.
  Difficulty labels are removed from the editorial model and seed pool.
- `branch-current` must be required by repository rules to block stale
  merges. A later base-branch change requires another validation run.
