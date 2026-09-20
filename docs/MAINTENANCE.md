# Maintenance

Recurring operational tasks this codebase needs, and how each is
currently handled (automated, manual, or not yet built).

## Dependency updates

- **Automated**: `.github/dependabot.yml` opens weekly PRs for npm
  (`functions/`), Gradle (root + `app/`), and GitHub Actions dependencies.
  Firebase packages are grouped into one PR (`firebase-*`,
  `@google-cloud/*`, `@googleapis/*`) so a coordinated Firebase SDK bump
  doesn't arrive as a dozen separate PRs.
- **Manual**: review and merge each Dependabot PR — CI (`.github/workflows/ci.yml`)
  runs the full test suite against it, but a passing CI run doesn't mean
  "safe to auto-merge" for a major version bump (see Billing Library
  below for why).

## Content pool

- `npm run generate-draft -- --tier <free|premium> [--sourceId <id>] [--type <spell|potion|lesson>] [--count <n>]`
  generates drafts via Claude from `functions/sources.json`'s curated
  sources. Prints the resolved Firebase project before running (see
  `functions/.env.example`'s Environments guidance) so an operator always
  knows which environment they're about to write into.
- `npm run approve-draft -- --contentId <id>` publishes one draft, after
  `approveDraft`'s safety/completeness gate passes.
- **Growing `sources.json`** is a manual, deliberate curation task — see
  the file's own doc comment: never auto-populated from a crawl. The
  365-entry launch target referenced in code comments is still just 1
  source; this is ongoing content work, not a code task.

## Play Billing / Kotlin toolchain

- `billing-ktx` is currently pinned to `9.1.0` (`gradle/libs.versions.toml`).
  That release requires Kotlin 2.3.x metadata, so this project's Android
  Gradle script must keep the Kotlin JVM target on the newer
  `kotlin { compilerOptions { ... } }` DSL rather than the removed
  `android.kotlinOptions { jvmTarget = "17" }` form.
- If Kotlin, KSP, Hilt, or AGP is bumped again, verify the versions as a
  compatible set before merging and rerun the full Android suite:
  `./gradlew clean ktlintCheck :app:testProdDebugUnitTest :app:assembleProdDebug
  :app:bundleProdRelease`.

## Room schema migrations

- Every migration ever shipped lives in `app/src/main/kotlin/com/wiccan/app/data/local/Migrations.kt`'s
  `WICCAN_DATABASE_MIGRATIONS` array — **never remove an entry**, even
  one that looks obsolete: a device restoring an old backup, or one that
  simply hasn't updated in a long time, can still be on any prior schema
  version and needs the full migration chain to reach the current one.
- Each new migration should get a corresponding test in
  `app/src/androidTest/kotlin/com/wiccan/app/data/local/WiccanDatabaseMigrationTest.kt`
  following `MIGRATION_1_2`'s pattern (build the prior version from its
  exported schema JSON in `app/schemas/`, run the migration, assert the
  result) — see `docs/RELEASE_CHECKLIST.md` for why this currently
  compiles but has never executed against a real SQLite engine.

## Runtime kill switches

- `systemConfig/global` (Firestore, no client access — `firestore.rules`) holds two ops-only emergency flags, both fail-open (default `true`, i.e. normal operation, if the doc or field doesn't exist):
  - `dailyContentEnabled` — stops `getTodayContent` (the interactive callable) and `sendDailyNotificationsJob` (which also selects content) from serving any content at all. Flip to `false` if a bad content batch needs to be pulled from delivery immediately.
  - `notificationsEnabled` — stops just the daily push job, independent of content selection (e.g. an FCM incident). Flip to `false` to silence the hourly job without touching `getTodayContent`.
- To flip either: edit the `systemConfig/global` document directly in the Firebase Console (or via the Admin SDK) — there is no CLI script for this, since it's meant to be a rare, deliberate, auditable manual action, not an automated one.
- `FirestoreSystemConfigRepository`/`FakeSystemConfigRepository` (`functions/src/systemConfigPort.ts`) are the read path; nothing in this codebase writes to `systemConfig/global` — creating/editing it is an operator action, not a code path.

## Policy versions

- `PolicyVersions.TERMS`/`PolicyVersions.PRIVACY`
  (`app/src/main/kotlin/com/wiccan/app/domain/model/PolicyVersions.kt`)
  gate the `RequiresPolicyAcceptance` root state and are checked
  server-side by `acceptTerms` (`functions/src/policyVersions.ts`,
  `functions/src/acceptTerms.ts`). **Bump the relevant constant whenever
  the actual hosted Terms/Privacy page changes materially** — that's what
  forces already-signed-in users back through the acceptance screen
  before they can continue.

## Environments

- See the README's Environments section and `functions/.env.example` for
  the dev/staging/production separation. Routine maintenance: keep each
  environment's `.env.<project-id>` secrets current, and never let a
  secret drift into being shared across environments.

## Native library / 16KB page-size compliance

- `scripts/check-16kb-alignment.sh` runs in CI on every push against the
  release `.aab`. If it ever fails after adding a new dependency, that
  dependency (or a transitive one) shipped a non-compliant native
  library — check the failing library's own release notes/issue tracker
  for a fixed version before working around it any other way.

## What's not automated yet

- No automated dependency **major-version** upgrade testing beyond CI's
  existing test suite — a major bump (Room, Hilt, AGP, Kotlin, Compose
  BOM) should get a manual full-app smoke test on a real device before
  merging, not just a green CI run.
- No scheduled Firestore backup/export (see `docs/DISASTER_RECOVERY.md`).
- No automated monitoring/alerting beyond what Firebase Console itself
  surfaces (Crashlytics, Cloud Functions error rates, App Check metrics)
  — setting up proactive alerts (e.g. Cloud Monitoring alert policies on
  function error rate or `sendDailyNotifications`'s `failed` count) is
  real project-specific ops work against a real project, not something
  this repo configures on its own.
