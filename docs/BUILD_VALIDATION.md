# Main integration validation — 2026-10-01

Validated `codex/validate-main-build` against main commit
`52877319bf63dd6b81c3b4ed8a80500f9ccc6a19`, including merged PRs #35 and #36.
The branch contains that commit; closed, unmerged proposals are not treated
as accepted changes. This records automated build results, not release approval.

## Verified locally

| Check | Result |
|---|---|
| Release identity/security preflight | PASS — 0 failures |
| `ktlintCheck` | PASS |
| `:app:testProdDebugUnitTest` | PASS — 80 tests, 17 suites, no failures/errors/skips |
| `:app:assembleProdDebug` | PASS — debug APK produced |
| `:app:compileProdDebugAndroidTestKotlin` | PASS — compilation only |
| `:app:bundleProdRelease` | PASS — unsigned release AAB produced |
| `:app:lintProdRelease` | PASS — 0 errors, 93 warnings |
| `scripts/check-16kb-alignment.sh` | PASS — all 8 bundled libraries have 16 KB LOAD alignment |
| Functions `npm ci`, build and lint | PASS |
| Functions Jest | PASS — 98 tests across 14 suites |
| Isolated Firebase emulator business flows | PASS — 18 assertions, with the App Check qualification below |
| CI workflow syntax | PASS — Actionlint 1.7.7 |
| PR ancestry check | PASS — current accepted, stale rejected, updated accepted |
| Run driver SDK selection | PASS — 5 isolated fresh/existing configuration checks |

Android used Java 17.0.20.1, Gradle 8.14.3 and Android SDK 36. Functions
used Node 20.20.2. A clean Android build completed through compilation and
APK packaging. Its screenshot test initially failed while fetching a
Robolectric artifact with HTTP 429; the subsequent full matrix passed.
Maven Central was rate-limited from this environment, so a temporary local
Gradle init script routed Maven Central and Robolectric runtime downloads
through `https://maven-central.storage-download.googleapis.com/maven2/`.
Repository dependency versions and repositories were not changed.

Reproduce the normal matrix from the repository root with an installed SDK:

```bash
bash scripts/release-preflight.sh
./gradlew clean ktlintCheck :app:testProdDebugUnitTest :app:assembleProdDebug \
  :app:compileProdDebugAndroidTestKotlin :app:bundleProdRelease :app:lintProdRelease
./scripts/check-16kb-alignment.sh
cd functions && npm ci && npm run build && npm test -- --runInBand && npm run lint
```

All six 822×1782 palette PNGs were generated under
`app/build/outputs/roborazzi/` and opened for inspection. Specimen text and
controls render legibly. The harness uses system glyphs at the real type
scale; actual downloadable fonts, app screens, transitions, widget and
device behavior still require device review.

## Emulator qualification

Firebase CLI 14.22.0 ran Auth, Functions, Firestore and Pub/Sub in the isolated
`demo-wiccan-validation` project. An unchanged-output run verified App Check
rejection. Following the run skill, only gitignored `functions/lib/index.js`
temporarily disabled App Check for the authenticated business-flow run.
`npm run build` restored it afterward; all eight callables enforce App Check
in source and generated output. No Functions source changed.

The 18 passing assertions cover authentication, unauthenticated rejection,
daily selection and idempotence, free/draft filtering, history, terms,
device registration, time zone, notification preferences, collection-group
queries, invalid purchase arguments, the daily kill switch, and account/device
deletion. They do not exercise live Play purchases, FCM delivery or production
App Check attestation.

## Remaining limits

- Hosted GitHub Actions continues to end in `startup_failure` before jobs
  execute. These are local results, not a successful hosted CI run.
- Release bundles are unsigned without a real upload keystore; this bundle
  has no signature files. Signing, Play/Firebase setup and live flows remain
  release prerequisites in `RELEASE_CHECKLIST.md`.
- Android lint has 93 inherited nonblocking warnings: 49 dependency/toolchain
  notices, 35 spelling notices, and 9 API/configuration/icon suggestions. AGP 8.7.3 also warns that its
  tested compile SDK is below 36; Gradle reports deprecated features.
  Passing checks do not mean the repository has zero warnings.
- Device/Room migration execution, dev/staging Firebase configurations,
  hosted legal pages and editorial/rights approval of the draft content pool
  remain outstanding.
- PR #31 explicitly left Android support for structured `reflection`,
  `practice` and `difficulty` as follow-up work. The current Android model,
  cache and detail view omit those fields. This validation does not claim
  every planned product feature is implemented.
- `branch-current` must be required by repository rules to block merging.
  A later base-branch change requires another CI run.
