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
- **Compatibility audit (2026-10-24)**: Android, npm, Gradle, Firebase CLI,
  and GitHub Actions versions were compared with their current stable release
  channels. Compatible updates were applied together and both npm workspaces
  audit with zero known vulnerabilities. Three newer majors are deliberately
  held: Firebase Rules Unit Testing 6/Firebase JS 13 require Node 24.12 while
  the deployed Cloud Functions runtime remains on Firebase-supported Node 22;
  TypeScript 7 is outside the supported ranges of `ts-jest` and
  `typescript-eslint`; and `@types/node` remains on major 22 to model the
  deployed runtime. Revisit those together when their runtime/tooling chain is
  supported rather than overriding peer requirements.
- The October 24 refresh also moves `typescript-eslint` to 8.72.0 and
  `body-parser` to 2.4.0. The latter fixes GHSA-27r3-jxmh-p828, a high-severity
  denial-of-service issue in compressed request parsing.
- The Firestore emulator scripts invoke the pinned current Firebase CLI with
  `npx --yes firebase-tools@15.34.1`. Keeping the CLI out of the committed
  dependency graph avoids shipping its large, unrelated command-line
  dependency tree while preserving reproducible CI behavior.

## Content pool

- `npm run content-agent -- --tier <free|premium> [--sourceId <id>] [--type <spell|potion|lesson>] [--count <n>]`
  previews a generation request without calling Claude or writing to
  Firestore. The eligible pool contains only sources classified as an
  `ancient-grimoire`; museum overviews and other historical context are
  excluded even when they discuss ancient material.
- `npm run content-agent -- --project wiccan-android --commit --tier <free|premium> [...]`
  calls Claude and writes review-required drafts. The exact production
  project and `--commit` are both mandatory; generated content is never
  auto-published. `npm run generate-draft` remains an alias. The recommended
  operating cadence is one manually initiated batch of 30–60 drafts per
  month, followed by source, duplication, safety, and rights review.
- Every committed agent draft is server-stamped with
  `generatedBy = ancient-grimoire-content-agent`,
  `generationSourceKind = ancient-grimoire`, and `generatedAt`. The repository
  also forces `status = draft` and `rightsReviewRequired = true`, even if a
  caller supplies weaker values. These fields provide an audit trail; they do
  not replace the manual source, safety, duplication, and rights review.
- `npm run approve-draft -- --project wiccan-android --contentId <id>` publishes one draft, after
  `approveDraft`'s safety/completeness gate passes.
- **Growing `sources.json`** is a manual, deliberate curation task — see
  the file's own doc comment: never auto-populated from a crawl. The pool
  contains three generation-eligible ancient grimoires/manuscripts and one
  historical-context record retained for the checked-in seed. Growing the
  content pool toward the 365-entry launch target remains ongoing editorial
  work.

## Play Billing Library version

- **Resolved.** `billing` is now 9.1.0, reached by taking the whole
  toolchain forward together rather than bumping it alone. The chain, in
  the order each link forced the next: Billing 9.1.0 needs Kotlin >= 2.3;
  Kotlin 2.3+ metadata is unreadable to room-compiler 2.6.1 (it throws
  "unexpected jvm signature V"), so Room moved to 2.8.5; newer KSP needs a
  newer AGP than 8.7.3; Hilt 2.59+ refuses to apply below AGP 9.0.0; and
  AGP 9 needs Gradle 9. The current set is Gradle 9.8.1, AGP 9.4.1,
  Kotlin 2.4.21, KSP 2.3.12, Hilt 2.60.1, Room 2.8.5, compileSdk 37.
- The earlier note here said the KSP/Hilt classloader conflict could not
  be resolved without KSP's compatibility table. That diagnosis was
  incomplete: the crash was Room's annotation processor, not KSP's or
  Hilt's, and the stack trace named it
  (`androidx.room.compiler.processing...typeNameFromJvmSignature`).
  Reading the trace rather than the symptom is what unblocked it.
- Two AGP 9 migrations came with this and are worth knowing before any
  future build-file edit: the `org.jetbrains.kotlin.android` plugin must
  be **removed** (AGP 9 has built-in Kotlin support and fails the build if
  both are applied), and `android.kotlinOptions` is gone — `jvmTarget`
  now lives in the top-level `kotlin { compilerOptions { } }` block.
- Firebase BOM 34 dropped the `-ktx` artifacts; the Kotlin extensions are
  in the main libraries now, so `firebase-firestore-ktx` and friends are
  just `firebase-firestore`. There was no source change — this app never
  imported from the `.ktx` packages.

## Room schema migrations

- Every migration ever shipped lives in `app/src/main/kotlin/com/jaimesorrow/wiccan/data/local/Migrations.kt`'s
  `WICCAN_DATABASE_MIGRATIONS` array — **never remove an entry**, even
  one that looks obsolete: a device restoring an old backup, or one that
  simply hasn't updated in a long time, can still be on any prior schema
  version and needs the full migration chain to reach the current one.
- Each new migration should get a corresponding test in
  `app/src/androidTest/kotlin/com/jaimesorrow/wiccan/data/local/WiccanDatabaseMigrationTest.kt`
  following `MIGRATION_1_2`'s pattern (build the prior version from its
  exported schema JSON in `app/schemas/`, run the migration, assert the
  result) — see `docs/RELEASE_CHECKLIST.md` for why this currently
  compiles but has never executed against a real SQLite engine.

## Seasonal theme mapping

- `app/src/main/kotlin/com/jaimesorrow/wiccan/ui/theme/SeasonalThemeResolver.kt`
  maps the eight-spoke Wheel of the Year to the six `WiccanPalette`
  values, on one continuous mood arc starting and ending at the year's
  darkest point (Yule) and peaking at its brightest (Litha), then
  returning through warmer harvest/autumn tones rather than mirroring
  the spring ascent exactly:

  | Sabbat | ~Date | Palette | Why |
  |---|---|---|---|
  | Yule | Dec 21 | Near Black | Longest night — the year's darkest, most minimal point. |
  | Imbolc | Feb 1 | Midnight Blue | First light returning, still cold and quiet. |
  | Ostara | Mar 20 | Deep Forest | Spring equinox — new growth, herbs and earth waking. |
  | Beltane | May 1 | Royal Plum | Fire festival — blossoming, vivid, mystical. |
  | Litha | Jun 21 | Light / Ivory | Summer solstice — the year's brightest point. |
  | Lughnasadh | Aug 1 | Warm Earth | First harvest — grain fields, candlelight. |
  | Mabon | Sep 22 | Royal Plum | Autumn equinox — dusk, orchard/wine harvest; mirrors Beltane descending. |
  | Samhain | Oct 31 | Midnight Blue | The veil thins; mirrors Imbolc descending. |

  In the week before each sabbat the resolved color scheme blends between
  dark palettes. A dark/light transition switches the complete color scheme
  at the midpoint of that week: independently blending dark and light text
  roles can reduce contrast below 4.5:1. Manual light/dark changes likewise
  switch complete schemes without animating individual roles. The rest of
  each ~6-7 week span shows a pure palette. The mapping is deterministic (`java.time.LocalDate`
  in, `SeasonalResolution` out) — no clock/timezone side effects beyond
  the caller's own `today` parameter, and no network dependency.
- A user's explicit Settings choice (a locked palette, or Follow System)
  always overrides this — see `WiccanThemeMode`.
- **Typography**: `Type.kt` uses Android's downloadable Google Fonts
  provider (`androidx.compose.ui:ui-text-google-fonts`) for Cinzel
  (display/headings) and Manrope (everything else), rather than bundled
  font files, since this sandbox has no way to source licensed font
  binaries to embed. The provider's certificates
  (`res/values-v23/font_certs.xml`) are copied verbatim from Google's own
  Jetchat Compose sample — never fabricated. This requires Google Play
  Services to actually download the fonts (falls back to the system font
  otherwise); the app already depends on Play Services for Firebase/Play
  Billing, so this adds no new class of device incompatibility.
- **Visual validation**: `WiccanPaletteScreenshotTest` renders all six
  palettes on the JVM with Robolectric and Roborazzi. Run
  `.claude/skills/run-wiccan/driver.sh screenshots` to write PNGs under
  `app/build/outputs/roborazzi/`. These exercise colors, contrast and layout
  using the real type scale with system font glyphs; they cannot verify
  Cinzel/Manrope downloads or typeface appearance. `WiccanThemePreviews.kt`
  also provides `@Preview` composables for Android Studio. A real-device
  visual pass remains required before release, including the actual fonts
  and app screens; the JVM specimens do not replace that review.

## Runtime kill switches

- `systemConfig/global` (Firestore, no client access — `firestore.rules`) holds two ops-only emergency flags, both fail-open (default `true`, i.e. normal operation, if the doc or field doesn't exist):
  - `dailyContentEnabled` — stops `getTodayContent` (the interactive callable) and `sendDailyNotificationsJob` (which also selects content) from serving any content at all. Flip to `false` if a bad content batch needs to be pulled from delivery immediately.
  - `notificationsEnabled` — stops just the daily push job, independent of content selection (e.g. an FCM incident). Flip to `false` to silence the hourly job without touching `getTodayContent`.
- To flip either: edit the `systemConfig/global` document directly in the Firebase Console (or via the Admin SDK) — there is no CLI script for this, since it's meant to be a rare, deliberate, auditable manual action, not an automated one.
- `FirestoreSystemConfigRepository`/`FakeSystemConfigRepository` (`functions/src/systemConfigPort.ts`) are the read path; nothing in this codebase writes to `systemConfig/global` — creating/editing it is an operator action, not a code path.

## Policy versions

- `PolicyVersions.TERMS`/`PolicyVersions.PRIVACY`
  (`app/src/main/kotlin/com/jaimesorrow/wiccan/domain/model/PolicyVersions.kt`)
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
