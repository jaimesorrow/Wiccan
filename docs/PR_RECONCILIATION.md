# PR and package/toolchain reconciliation — 2026-10-02

Base: `eeb63a082b20be8e38fa5b6f4f29d7e351b153d6`.
GitHub inventory: 41 PRs and 29 surviving branches, including post-PR branch updates.
PRs #40 and #41 were open at the initial audit and merged while this branch was
being prepared. Their exact source changes are preserved. The final branch also
contains main commit `6e2e2b33f0a2813d4ff7dd6eb2bd9696d6764eaf`; the PR now focuses on the requested package-name
migration and remaining toolchain consistency fixes.
Main receives this package/toolchain reconciliation only when this PR is merged.

## Final identity and toolchain

Every Android flavor, Kotlin namespace and Kotlin package prefix is
`com.jaimesorrow.wiccan`, matching the registered production Firebase client.
Dev/staging suffixes are removed; variants replace each other on a device.
Separate dev/staging Firebase projects still need configurations registering this package.
The production Firebase project remains `wiccan-android`.
Backend `ANDROID_PACKAGE_NAME` remains explicitly configured per project and fails
closed when absent; no production default is introduced.

Java 17; Gradle 9.8.0; AGP 9.4.1 built-in Kotlin; Kotlin 2.4.20;
KSP 2.3.12; Hilt 2.60.1; Room 2.8.5; Billing 9.1.0;
compile SDK 37 / target SDK 36; ktlint integration 14.2.0 / engine 1.0.1.
Functions deployment, CI, local `.nvmrc` and lockfile root metadata all use Node 22.
Functions dependency updates and ESLint flat configuration come from #40.
The unused legacy Kotlin Android plugin alias is removed.

All Kotlin source sets, imports and script/document paths are moved together.
Room schema files move with the database class; database name, schema contents
and identity hashes stay unchanged. Periodic WorkManager registration uses UPDATE
so stored worker class names are replaced on upgrade. Existing home-screen widgets
may need to be removed and re-added after their provider class package changes;
verify an upgrade installation on a device before release.

## Validation in this environment

- Package/toolchain consistency check: passes, including all Kotlin package declarations.
- Five injected faults rejected: CI Node drift, namespace drift, lockfile Node drift,
  dev application-ID suffix, and mismatched dev Firebase package.
- Existing validator regression tests: 8 pass.
- Structural validation and all 31 release preflight checks: pass.
- Shell syntax checks: pass.
- Fresh dependency install and full Android/Functions build matrices: blocked by
  unavailable environment proxy/dependency downloads. Local runtime provides
  Java 21 and Node 24 rather than the required Java 17 / Node 22.
  No combined-branch compilation, Android tests, Functions tests or device tests
  are claimed as passing here.

The historical build evidence in BUILD_VALIDATION.md belongs to its named source
branch. It does not validate this combination or the Kotlin package migration.
Keep this consolidation in draft until both toolchains and upgrade behavior pass.

## PR inventory

“Merged” records GitHub merge state and main history; it does not imply every old
file should remain byte-for-byte after subsequent fixes. Closed unmerged proposals
are recorded explicitly rather than silently restoring obsolete identities,
duplicate theme classes, or outdated toolchain versions.

| PR | Title | Disposition |
|---|---|---|
| [#1](https://github.com/jaimesorrow/Wiccan/pull/1) | Add Android foundation and server-side daily content engine | Merged into main |
| [#2](https://github.com/jaimesorrow/Wiccan/pull/2) | Add Nyxwell foundation: data layer, ContentRepository, and daily-selection Cloud Function | Merged into main |
| [#3](https://github.com/jaimesorrow/Wiccan/pull/3) | Sessions 3-7: content generation, billing, notifications, widget, auth | Merged into main |
| [#4](https://github.com/jaimesorrow/Wiccan/pull/4) | Session 8: Archive (Book of Shadows) screen | Merged into main |
| [#5](https://github.com/jaimesorrow/Wiccan/pull/5) | Session 9: Settings — account & subscription | Merged into main |
| [#6](https://github.com/jaimesorrow/Wiccan/pull/6) | Session 10: Paywall (Upgrade) screen | Merged into main |
| [#7](https://github.com/jaimesorrow/Wiccan/pull/7) | Session 11: Firestore security rules | Merged into main |
| [#8](https://github.com/jaimesorrow/Wiccan/pull/8) | Session 12: Error-handling foundation (shared AppResult/AppError) | Merged into main |
| [#9](https://github.com/jaimesorrow/Wiccan/pull/9) | Session 13: Adaptive app icon | Merged into main |
| [#10](https://github.com/jaimesorrow/Wiccan/pull/10) | Session 14: Splash screen (AndroidX SplashScreen, auth-gated) | Merged into main |
| [#11](https://github.com/jaimesorrow/Wiccan/pull/11) | Session 15: Crash reporting & analytics (Crashlytics + 6 events) | Merged into main |
| [#12](https://github.com/jaimesorrow/Wiccan/pull/12) | Session 16: CI pipeline (GitHub Actions) | Merged into main |
| [#13](https://github.com/jaimesorrow/Wiccan/pull/13) | Session 17: App Check & admin function lockdown | Merged into main |
| [#14](https://github.com/jaimesorrow/Wiccan/pull/14) | Foundation repair: Wiccan rebrand, API 36, daily-assignment race fix, timezone semantics | Merged into main |
| [#15](https://github.com/jaimesorrow/Wiccan/pull/15) | Sessions 21+24+19+23+18+25/43+29: Navigation through content-safety hardening | Merged into main |
| [#16](https://github.com/jaimesorrow/Wiccan/pull/16) | Sessions 45/44 + 31-35: Room migrations, backup safety, environment separation | Merged into main |
| [#17](https://github.com/jaimesorrow/Wiccan/pull/17) | Sessions 46-50 + 36-40: release preflight and Play Console readiness docs | Merged into main |
| [#18](https://github.com/jaimesorrow/Wiccan/pull/18) | Audit remediation: CI, subscription cancellation, App Check, local-9am notifications, 16KB check, ops docs | Merged into main |
| [#19](https://github.com/jaimesorrow/Wiccan/pull/19) | Adopt approved Wiccan content voice and lesson format | Merged into main |
| [#20](https://github.com/jaimesorrow/Wiccan/pull/20) | Audit remediation: device schema, kill switches, product flavors | Merged into main |
| [#21](https://github.com/jaimesorrow/Wiccan/pull/21) | Security, concurrency and architecture remediation | Closed unmerged: service-layer refactor and historical remediation remain separate; current security behavior retained |
| [#22](https://github.com/jaimesorrow/Wiccan/pull/22) | Ground Lesson of the Day in curated ancient sources | Merged into main |
| [#23](https://github.com/jaimesorrow/Wiccan/pull/23) | Add dynamic six-palette Wiccan theme foundation | Closed unmerged: theme foundation superseded by merged #24; inherited #21 refactor remains separate |
| [#24](https://github.com/jaimesorrow/Wiccan/pull/24) | Six-palette dynamic Wiccan visual identity + Wheel-of-the-Year theming | Merged into main |
| [#25](https://github.com/jaimesorrow/Wiccan/pull/25) | Set prod applicationId to match wiccan-android Firebase project | Merged into main |
| [#26](https://github.com/jaimesorrow/Wiccan/pull/26) | Production Firebase configuration and release integration | Closed unmerged: production identity / config and newer toolchain retained from #27/#29/#39 |
| [#27](https://github.com/jaimesorrow/Wiccan/pull/27) | Fix production package and Firebase identity configuration | Merged into main |
| [#28](https://github.com/jaimesorrow/Wiccan/pull/28) | Add production Firebase and hardened Play Billing integration | Closed unmerged: corrected production integration merged through #29 |
| [#29](https://github.com/jaimesorrow/Wiccan/pull/29) | Add verified production Firebase and hardened Play Billing integration | Merged into main |
| [#30](https://github.com/jaimesorrow/Wiccan/pull/30) | Prevent repeated daily lessons and link ancient source records | Merged into main |
| [#31](https://github.com/jaimesorrow/Wiccan/pull/31) | Add the 150-entry ancient-source content seed pool | Merged into main |
| [#32](https://github.com/jaimesorrow/Wiccan/pull/32) | Fix broken secret-scan check in release-preflight.sh | Closed unmerged: secret-scan fix superseded by main's tracked-file, fail-closed scan |
| [#33](https://github.com/jaimesorrow/Wiccan/pull/33) | Build the signed release bundle in CI | Merged into main |
| [#34](https://github.com/jaimesorrow/Wiccan/pull/34) | Fix import ordering that broke ktlint on main | Merged into main |
| [#35](https://github.com/jaimesorrow/Wiccan/pull/35) | Add the emulator suite and its end-to-end backend check | Merged into main |
| [#36](https://github.com/jaimesorrow/Wiccan/pull/36) | Add a run skill that can actually render the UI | Merged into main |
| [#37](https://github.com/jaimesorrow/Wiccan/pull/37) | Fix fresh-checkout builds and validate current main | Merged into main |
| [#38](https://github.com/jaimesorrow/Wiccan/pull/38) | Add a repo validation gate and wire it into CI | Merged into main |
| [#39](https://github.com/jaimesorrow/Wiccan/pull/39) | Modernize the toolchain: Gradle 9, AGP 9, Kotlin 2.4, Billing 9.1 | Merged into main |
| [#40](https://github.com/jaimesorrow/Wiccan/pull/40) | Modernize the Cloud Functions dependencies | Merged into main during preparation; retained in this branch |
| [#41](https://github.com/jaimesorrow/Wiccan/pull/41) | Fix AGP 9 source lint and validate latest main build | Merged into main during preparation; retained in this branch |

## Surviving branch inventory

Comparisons use the pinned base above. Diverged commits can contain equivalent
changes that were cherry-picked or replaced; an ahead count alone is not proof of
missing functionality. Main's modern identity, billing hardening, theme system,
emulator suite, signing workflow and scan fixes are retained.

| Branch | Head | Relation to main | Reconciliation |
|---|---|---|---|
| `add-ancient-content-seed` | `c9f6c73bae33` | behind | Already in main history |
| `claude/build-clean` | `46f2742b9f14` | diverged (1 ahead) | Import-order correction superseded by #34 |
| `claude/ci-release-signing` | `c70f915e5595` | behind | Already in main history |
| `claude/final-sync` | `0f6d8c26a123` | ahead (1 ahead) | Included (#40) |
| `claude/fix-main-ktlint-after-palette-merge` | `9ef16eb44ca3` | behind | Already in main history |
| `claude/modernize` | `24394c514af1` | behind | Already in main history |
| `claude/run-skill` | `50814f6a09a8` | behind | Already in main history |
| `claude/salvage-emulator-suite` | `494927681e59` | behind | Already in main history |
| `claude/validate-main` | `a9e7b474b561` | behind | Already in main history |
| `claude/vm-setup-app-build-y2mudu` | `c65d791772de` | behind | Already in main history |
| `claude/wiccan-android-firebase-lxivy1` | `a59c1a2bb207` | diverged (3 ahead) | Firebase setup/emulator changes represented by #35; current config retained |
| `claude/wiccan-ci-release-fix-b7gdd6` | `83f1a5d53416` | diverged (1 ahead) | Tracked-file fail-closed scan already on main |
| `codex/fix-main-build-2026-10-02` | `5bd0cc7943ba` | ahead (1 ahead) | Included (#41) |
| `codex/validate-main-build` | `3c22d1cf4880` | behind | Already in main history |
| `content-format-update` | `7ded2ff94ce6` | behind | Already in main history |
| `copilot/fix-github-actions-job` | `b9f74cca377a` | diverged (5 ahead) | Production setup and Billing migration superseded by #29/#39 |
| `copilot/fix-github-actions-job-again` | `c9f6c73bae33` | behind | Already in main history |
| `copilot/foundation-core-content-engine` | `88cb6f1fd923` | behind | Already in main history |
| `copilot/make-app-public` | `12f31b6f8459` | behind | Already in main history |
| `copilot/monthly-subscription-inquiry` | `a012ac66d1e5` | behind | Already in main history |
| `feature/ancient-grounded-lessons` | `c06e23386cc0` | behind | Already in main history |
| `feature/dynamic-wiccan-themes` | `8c8d2c80f070` | diverged (53 ahead) | Theme superseded by #24; inherited service refactor remains unmerged |
| `fix/config-identity-audit` | `88f939e3f77b` | behind | Already in main history |
| `fix/unique-lessons-source-provenance` | `00408237b1f4` | behind | Already in main history |
| `main` | `dae3b74ff95d` | ahead (2 ahead) | Already in main history |
| `release/production-integration-clean` | `c0d3b2d8ab24` | diverged (24 ahead) | Corrected integration merged in #29 |
| `release/production-integration-v2` | `b18ee6d8f62f` | behind | Already in main history |
| `release/production-readiness` | `57071439b8e1` | diverged (8 ahead) | Production/Google sign-in/signing changes superseded by #29/#33; modern toolchain retained |
| `security-architecture-remediation` | `2963bf7a8afd` | diverged (51 ahead) | Obsolete identity/toolchain rejected; optional services/refactor remains unmerged |

## Explicit remaining work

The closed #21/#23 service-layer extraction and its historical remediation report
are not merged by this package/toolchain PR. The current ports/adapters business
logic is retained. This inventory is not a claim that every rejected proposal or
planned feature is accepted or complete. PR #31's structured Android lesson fields,
device validation, signing, live Firebase/Play checks, legal URLs, dev/staging
project setup and editorial approval remain tracked follow-ups.

## Publication freshness

Final integration base: `6e2e2b33f0a2813d4ff7dd6eb2bd9696d6764eaf`. Compared its changes against the initial
base: only the #40/#41 paths changed, and all are already represented here.
The branch was updated with that main commit without reverting any main changes.


## Follow-up verification of PR #42

The package/toolchain gate now has 11 repeatable isolated regression tests,
including accepting a separately registered dev project with the same package
and rejecting source-package drift, missing Room migration schemas, legacy
Kotlin plugin use and missing configuration. CI runs these tests before the gate.
Both Actions runs for head `b3942c1daca91259818cc52f72ee77b3e17f50fe`
([run 36977704794](https://github.com/jaimesorrow/Wiccan/actions/runs/36977704794),
[run 36977704832](https://github.com/jaimesorrow/Wiccan/actions/runs/36977704832))
ended in `startup_failure`; both job lists are empty. The local proxy still
refuses connections. These observations establish infrastructure blockers,
not a passing or failing compile result.

Compared both moved Room schemas against their original main blob hashes:
contents are identical. Source imports, resource class references and relative
manifest class names use the new namespace. Database and DataStore filenames
are preserved. An installed-app upgrade and live widget pass remain necessary.

## Conflict resolution after PR #43

Merged main `6a375a0ed89c1aa384fdc86c8788761cb718b82f` using Git's command-line
three-way merge of the exact pinned source trees. This carries the audited branch
histories and current sign-in availability changes into the Firebase package
namespace. All six auth tests are preserved, including the newly added repository
test moved into the renamed source tree. Billing's current-version comment, CI
concurrency cancellation, package.json runtime selector, and environment
alias/project separation checks are retained. The earlier inventories above are
snapshots of their stated audit times; PR #43 subsequently retained the historical
branch commits without restoring rejected source implementations.

Conflict resolution checks run separately from PR #43's historical build matrix.
Dependency download and device verification limits still apply to this migration.

The resolved tree passes 25 package/toolchain regression tests, eight validator
regression tests, the direct consistency gate, structure validation, all 31
release preflight checks, shell syntax checks and Git whitespace checks. All 134
Kotlin files from current main are retained under the migrated package, with only
the intentional periodic worker policy update beyond package substitutions.
Both Room schemas remain byte-identical. The six auth tests are retained but
were not executed here because Android dependencies cannot be downloaded.

PR #42 was subsequently merged into main as `b13dc64b97157bc08a4b2bac919b542c42971803`.
The remaining follow-up strengthens the package gate against per-flavor ID
overrides and build-type suffixes, and records the migration's validation scope.
