# Main integration validation — 2026-10-02

Validated `codex/fix-main-build-2026-10-02` against main commit
`eeb63a082b20be8e38fa5b6f4f29d7e351b153d6`, including merged PRs #37, #38
and #39. The branch contains that commit and retains the Gradle 9 / AGP 9 /
Kotlin 2.4 / Billing 9 modernization. This records automated build results,
not release approval or completion of every planned product feature.

## Fixes found during validation

- ktlint plugin 12.1.1 reported success under AGP 9 while checking only
  Gradle Kotlin scripts. Plugin 14.2.0 restores checks of all five populated
  Android source sets (133 Kotlin files). The lint engine stays at 1.0.1,
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
| `:app:testProdDebugUnitTest` | PASS — 80 tests, 17 suites, no failures/errors/skips |
| `:app:assembleProdDebug` | PASS — debug APK produced |
| `:app:compileProdDebugAndroidTestKotlin` | PASS — compilation only |
| `:app:bundleProdRelease` | PASS — unsigned release AAB produced |
| `:app:lintProdRelease` | PASS — 0 errors, 64 warnings |
| `scripts/check-16kb-alignment.sh` | PASS — all 8 libraries have 16 KB LOAD alignment |
| Functions `npm ci`, build and lint | PASS |
| Functions Jest | PASS — 98 tests across 14 suites |
| Validator regression tests | PASS — 8 isolated tests |
| CI/release workflow syntax | PASS — Actionlint 1.7.12 |
| Run driver SDK setup | PASS — 6 isolated fresh/existing configuration checks |
| Fresh SDK manager Java compatibility | PASS — official CLI revision 23.0 runs on Java 17 |

Android used Java 17.0.20.1, Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20 and
Android SDK 37.0 / Build Tools 37.0.0 (`compileSdk` 37, `targetSdk` 36).
Functions used Node 20.20.2. Android validation started with `clean`.
A temporary local Gradle init script routed Maven Central and Robolectric
runtime downloads through
`https://maven-central.storage-download.googleapis.com/maven2/` to avoid
artifact rate limiting. Repository repositories were not changed. Gradle
workers were limited to two for the shared build environment.

Reproduce from the repository root with Java 17, Node 20 and an installed SDK:

```bash
./gradlew clean
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

## Earlier backend emulator evidence

The 2026-10-01 isolated `demo-wiccan-validation` Firebase emulator run
passed 18 business-flow assertions. It first verified App Check rejection,
then temporarily disabled enforcement only in gitignored generated
`functions/lib/index.js` for authenticated business flows, following the
run skill. A rebuild restored enforcement afterward. No Functions source
changed in the current main modernization or this PR; this emulator run
was not repeated on 2026-10-02. It does not verify live Play purchases,
FCM delivery or production App Check attestation.

## Remaining limits

- Hosted GitHub Actions ends in `startup_failure` before jobs execute,
  including the current main run. The API does not establish the account
  cause. These are local results, not successful hosted CI.
- The release AAB has no signature files. Signing, Play/Firebase setup and
  live flows remain release prerequisites in `RELEASE_CHECKLIST.md`.
- Android lint reports 64 nonblocking warnings, including 35 spelling
  notices and dependency/API/configuration suggestions. Kotlin and Gradle
  also report deprecations. Passing checks do not mean zero warnings.
- Device/Room migration execution, dev/staging Firebase configurations,
  hosted legal pages and editorial/rights approval of draft content remain
  outstanding.
- PR #31 left Android support for structured `reflection`, `practice` and
  `difficulty` as follow-up work; the model, cache and detail view omit
  those fields.
- `branch-current` must be required by repository rules to block stale
  merges. A later base-branch change requires another validation run.
