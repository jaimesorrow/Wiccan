# Wiccan repository audit - FINAL consolidated report

Prepared by Reviewbot, 2026-11-15 ~01:30 AKST. Audit base `6fc5824`; re-verified against current `main` **`a68c802`** (PR #149 "Bump the stable toolchain to November 2026" was merged at 01:05 AKST, after the original audit). The only remote changes made were the 55 branch deletions you authorized. Nothing was pushed, merged or commented on.

## 1. Summary
- The repo is lean. Real tooling turned up **7 removable items** across `functions/` and `app/`: knip, tsc, eslint, depcheck-equivalent knip deps check, vulture, kotlinc warnings, Android lint, rg reference scans.
- The repo has no committed build outputs, OS junk, logs, commented-out code, TODO/FIXMEs or unused npm dependencies. Android lint reports 0 UnusedResources.
- **PR #150** (cloud agent, head `a7f3387`) covers 3 of the removals and is **correct**. It builds and tests green both as-is and when merged onto current main.
- Those **4 removals are now included in #150** as well, in head `ca6f400` (on top of `a68c802`): the unused Kotlin class, the invalid tsconfig key, the obsolete `values-v23` folder, and the redundant null-safe calls. They were applied from the local patch. Local recheck of that commit: Functions 368 passed / 25 skipped, `tsc` and eslint clean, ktlint clean, 207 unit tests, prodDebug assemble, and instrumented-test compilation.
- **Branches:** 55 fully merged branches were deleted; 4 branches with unmerged work are kept. Each tip SHA is recorded for restore.
- **PR #151's report**, after its 10:06Z (01:06 AKST) correction, broadly agrees with this one. The differences are listed in section 7.

## 2. Structure overview
| Area | Tech | Entry points | Purpose |
|---|---|---|---|
| `app/` | Kotlin 2.4, AGP 9.5.1, Gradle 9.9.1, compileSdk 37, Hilt, Room (v5), Compose, Glance, WorkManager | `WiccanApplication`, `MainActivity`, `WiccanMessagingService`, widget receivers, `DailyContentWorker` | Android app (`com.jaimesorrow.wiccan`, namespace `com.wiccan.app`); flavors dev/staging/prod |
| `functions/` | TypeScript, Node 22, firebase-functions 7, jest, eslint | `src/index.ts` -> `lib/index.js`; 17 operator CLIs in `src/scripts` run via npm scripts | Cloud Functions: daily content, billing/RTDN, notifications, account deletion, content pipeline/agent |
| `firestore-tests/` | node:test + Firebase emulator | `npm test` | Firestore rules + emulator integration tests |
| `hosting/` | static HTML/CSS | Firebase Hosting | Public legal/support/account-deletion pages |
| `scripts/` | Python, bash, Node | CI | Toolchain/identity checks, repo validation, 16KB alignment, emulator e2e |
| `brand/` | PNG/PDF + Python generator | `build_brand_assets.py` | Approved artwork and generated exports |
| `docs/` | Markdown (29) | - | Runbooks, standards, research reports |
| `.github/`, `.devcontainer/`, `.claude/` | YAML, Docker, Markdown | CI / Codespaces / Claude Code | CI, dev environment, agent skills |

## 3. Branch cleanup result
- **Deleted: 55.** These were 31 fully merged, 14 equivalent (only merge commits ahead) and 10 squash-merged with an identical patch-id on main.
  - Each tip was re-fetched and re-verified immediately before deletion, and none was skipped.
  - The names and tip SHAs, with a restore command, are in `/workspace/wiccan-audit/deleted-branches.txt`.
- **Count reconciliation:** the earlier "6 unmerged" was a miscount. There were 60 non-main branches = 55 + 5.
- `deps/stable-toolchain-2026-11` (PR #149) was **not** deleted by me. It disappeared when #149 was merged at 01:05 AKST.
- **Current remote branches (7):**
  - `main`
  - `claude/wiccan-android-firebase-lxivy1` (kept): 1 commit pushed after #25 merged; adds `scripts/signing-fingerprints.sh` (not on main) and a RELEASE_CHECKLIST edit.
  - `codex/firestore-content-production-readiness` (kept): 1 commit, "harden production publication workflow", 15 files; PR #97 closed unmerged. Possibly superseded by #98/#101 (unverified).
  - `feature/approved-wiccan-brand-identity-20261108` (kept): 6 commits (brand emblem component, launcher vector drawables, BRAND_IMPLEMENTATION.md); PR #113 closed unmerged. Probably replaced by #114.
  - `codex/wiccan-production-subscriptions-lessons` (kept): 11 commits; squash-merged as 14982f8 via #46, but 6 small files still differ from that squash.
  - `cursor/dead-code-audit-ee70`: the cloud agent's PR #150 (open).
  - `cursor/repo-audit-report-ee70`: the cloud agent's PR #151 (open).

## 4. Removals
### 4a. Covered by PR #150 (correct; verified)
| Item | Evidence |
|---|---|
| `functions/src/testUtils/fakeSystemConfigRepository.ts` (deleted) + fix of its mention in `docs/MAINTENANCE.md` | knip unused file; rg: no importer |
| `functions/src/contentSimilarity.ts` `lexicalSimilarity()` (old L100-108) | rg: only its definition |
| `gradle/libs.versions.toml` library aliases `build-httpclient`, `build-logback-core`, `build-logback-classic` | rg: alias keys appear only in the catalog. `app/build.gradle.kts:155-157` uses the **version** keys `libs.versions.buildHttpClient` / `buildLogback`, which #150 keeps. My earlier note calling these aliases "used" was wrong: only the version entries are used. Build green after removal (section 6). |

### 4b. Now included in PR #150 (`ca6f400`). These were pending after `a7f3387` and were applied from `/workspace/wiccan-audit/cleanup-remaining-after-150.patch` (7 files, +8/-21)
| Item | Evidence | Confidence |
|---|---|---|
| `app/src/main/kotlin/.../domain/model/UserContentHistory.kt` (delete) + README model list line | Only reference in all Kotlin/XML is its own declaration. Grimoire uses `HistoryEntry` via `HistoryMappers.kt`. #151 (corrected) agrees. | High |
| `functions/tsconfig.json` `"compileOnRoot": false` | Not a TypeScript option; silently ignored | High |
| `app/src/main/res/values-v23/font_certs.xml` -> `values/font_certs.xml` (+2 doc/KDoc path refs in MAINTENANCE.md, Type.kt) | Android lint ObsoleteSdkInt: minSdk 24 makes the v23 qualifier pointless | High |
| `data/repository/AuthRepositoryImpl.kt:59-63` redundant `?.` (5 sites; `user?.isEmailVerified != true` -> `!user.isEmailVerified`) | kotlinc "Unnecessary safe call" x5 (smart-cast already non-null). After the patch, kotlinc reports no non-deprecation warnings. | High |
The original `/workspace/wiccan-audit/cleanup.patch` (against 6fc5824) is superseded. It overlaps #150 and no longer applies to main.

## 5. Medium / low / unclear
**Medium**
- `functions/src/types.ts:59` `UserContentHistory` interface: unused, but it is the only server-side description of the history document. Keep or delete by preference.
- `functions/src/lessons/canonicalLesson.ts:217` `legacyFieldsFor()`: see Issue 1.

**Low (keep)**
- About 60 knip "unused exports" in `functions/`: all are used inside their own file, so only the `export` keyword is extra.
- `SAFE_DP` in `brand/scripts/build_brand_assets.py:32`: vulture flags it as unused, but it documents the adaptive-icon safe zone.
- npm `generate-draft` duplicates `content-agent`: a documented alias.
- `app/proguard-rules.pro`: inert because `isMinifyEnabled=false`.
- `WidgetUpdater` and `WiccanWidgetUpdater` overlap; both have callers, so consolidating them would be a refactor.

**Framework-invoked (look unused, but are not)**
- Hilt modules, Room converters, `@Preview` composables, the 17 operator CLIs (wired through npm scripts), and `functions/lessons/drafts/.gitkeep`.

**Unclear purpose (owner decision)**
- 13 docs that nothing links to. Most are point-in-time research or audit artifacts:
  - `CHATGPT_DAILY_LESSON_PROMPT`, `CONTENT_CONTINUANCE_AGENT`, `DAILY_GRIMOIRE`
  - `DUPLICATE_AND_ORIGINALITY_CONTROLS`, `ERMAN_BERLIN_3027_SOURCE_EVIDENCE_REVIEW`, `FIVE_BOUND_REWRITE_REVIEW`
  - `HISTORICAL_SOURCE_REGISTRY_REPORT`, `LOCAL_REWRITE_BATCHES`, `ORACC_MAQLU_SOURCE_EVIDENCE_REVIEW`
  - `PRACTICAL_CATEGORY_MAPPING_PROPOSAL`, `READ_ONLY_AUDIT_ACCESS` (reads stale: "no Firebase identity is connected yet")
  - `STAGED_SOURCE_EVIDENCE_REVIEW` (linked only from FIVE_BOUND)
  - `LEGACY_LESSON_AUDIT` (generated output)

  Archive or delete them if the history isn't needed.
- dev/staging flavors and `.firebaserc` placeholders: scaffolding for Firebase projects that don't exist yet.
- `.devcontainer/`: no doc mentions it, but Codespaces uses it automatically.

## 6. Issues found
1. **`legacyFieldsFor` question:** `canonicalLesson.ts:217` has no caller, yet `docs/CONTENT_SCHEMA_V2.md:40` says stored v2 drafts carry the legacy fields via `legacyFieldsFor`. Either the doc is wrong and the function is dead, or a call is missing in the draft-write path, which would be a bug. Needs an owner decision; not removed.
2. **Missing Room schema v4:** `app/schemas/.../4.json` was never committed (1, 2, 3 and 5 exist; DB version 5; `MIGRATION_3_4` and `MIGRATION_4_5` exist). Migration tests can't validate against a v4 snapshot. Still missing on main.
3. **firebase-tools pin mismatch: RESOLVED by #149.** On 6fc5824, `ci.yml` used 15.33.0 and the package.json files 15.34.1. On a68c802, every pin is 15.36.0 (ci.yml, both package.json files, docs).
4. **`lintProdDebug` failure:** still fails on main a68c802 with 2 errors (ViewModelConstructorInComposable) in `app/src/test/.../ui/OnboardingFlowButtonsTest.kt:134`. CI only runs `lintProdRelease`, which passes, so this goes unnoticed.
5. **New since #149, minor:** `functions/package-lock.json` on main fails `npm ci` under npm 11 / Node 24 ("Missing: @firebase/app@0.16.2 from lock file"). It works under Node 22 / npm 10.9.9, which CI uses. Regenerating the lock with npm 11 would avoid surprises for developers on newer Node.

## 7. Verification
All runs are local on the box, with JDK 17 (CI's Android JDK), Android SDK 37, and Node 22.23.5 / npm 10.9.9 for the post-#149 trees.
| Tree | Functions build+lint+jest | Android: ktlintCheck, testProdDebugUnitTest, assembleDebug (dev/staging/prod) + assembleProdDebug, compileProdDebugAndroidTestKotlin, lintProdRelease | Repo scripts |
|---|---|---|---|
| main 6fc5824 (baseline) | pass (368 pass / 25 skipped emulator-only) | pass, 207 unit tests 0 failures | pass |
| my original patch on 6fc5824 | pass | pass, 207/0 | pass (4 python checks + validate-repo --structure) |
| **#150 head a7f3387** | **pass (368/25)** | **BUILD SUCCESSFUL, 207/0, dev/staging/prod debug APKs built** | - |
| **#150 merged onto main a68c802** | **pass (368/25)** | **BUILD SUCCESSFUL, 207/0, 3 debug APKs** | - |
| main + #150 + pending patch | pass (368/25) | BUILD SUCCESSFUL, 207/0; no non-deprecation kotlinc warnings | pass |
| main a68c802 `lintProdDebug` | - | FAIL (pre-existing, Issue 4) | - |

**Not run:**
- the CI e2e emulator job (`scripts/e2e-emulator.mjs`)
- the Firestore rules emulator tests
- instrumented tests (no emulator)
- `bundleProdRelease` + the 16KB alignment check
- `npm audit`

**#150 vs #151 vs this report**
- #151's report was re-fetched at head `1e9ffa2` (corrected 01:06 AKST).
- **Agreements:**
  - The same 3 removals.
  - `UserContentHistory.kt` is unused. This was fixed in its correction; the original said it was used.
  - Schema v4 is missing.
  - The knip exports are internal-only.
  - The framework-invoked caveats.
- **Material differences:**
  - (a) #151 does not flag `compileOnRoot`, the obsolete `values-v23`, or the `lintProdDebug` failure.
  - (b) #151 lists the redundant safe calls but leaves them; it counts 4 where kotlinc reports 5.
  - (c) #151 states "No tracked file was left because its role could not be stated" and gives the 13 unlinked docs a purpose. This report lists them as owner decisions.
  - (d) #151 treats `legacyFieldsFor` as a compatibility contract to keep. This report raises it as a possible missing call.
  - (e) #151 adds items this report did not list: `@types/jest` (keep), the widget-updater overlap, the empty proguard rules (keep), and the narrow comment in `instrumented-tests.yml`. These are reasonable and non-blocking.
  - (f) #151 was written against 6fc5824, before #149 merged, so its #149 notes are now historical.

## 8. Every file on main (a68c802, 472 tracked files), grouped by directory
"Used by" lists are auto-derived from reference scans; for Kotlin, any file referencing a declared name. Hilt and Manifest wiring is noted where a scan cannot see it. Items marked REMOVED or now in #150 refer to sections 4a/4b. The four items that this report originally called pending are included in #150 (`ca6f400`).

**(repo root)**
- `.editorconfig` - Editor/ktlint formatting rules (ktlintCheck in CI).
- `.firebaserc` - Firebase CLI project aliases (prod wiccan-android; dev/staging placeholders).
- `.gitignore` - Keeps build output, node_modules, local Firebase config, keystores, .DS_Store out of git.
- `.nvmrc` - Pins local Node 22; check-toolchain.py asserts it matches functions engines.
- `CLAUDE.md` - Coding-agent architecture rules and commands for this repo.
- `README.md` - Product overview, module map, build/test commands.
- `build.gradle.kts` - Root Gradle build: plugin aliases + buildscript security floors for lint classpath.
- `firebase.json` - Firebase deploy config: functions, rules/indexes, hosting headers, emulator ports.
- `firestore.indexes.json` - Firestore composite indexes (deployed).
- `firestore.rules` - Firestore security rules (deployed; tested by firestore-tests/ and functions/tests).
- `gradle.properties` - Gradle/AndroidX JVM and build flags.
- `gradlew` - Gradle wrapper launcher (Unix); used by CI and devs.
- `gradlew.bat` - Gradle wrapper launcher (Windows).
- `settings.gradle.kts` - Gradle settings; includes :app and repositories.

**.claude/skills/content-voice/**
- `SKILL.md` - Claude Code skill: editorial voice rules for lessons.

**.claude/skills/release-prep/**
- `SKILL.md` - Claude Code skill: release validation checklist.

**.claude/skills/run-wiccan/**
- `SKILL.md` - Claude Code skill: build/run/screenshot the app in a container.
- `driver.sh` - Driver script for the run-wiccan skill (existence checked by validate-repo.sh).

**.claude/skills/steward/**
- `SKILL.md` - Claude Code skill: which checks apply to a PR.

**.claude/skills/wiccan-theme/**
- `SKILL.md` - Claude Code skill: theme/palette guardrails for UI edits.

**.devcontainer/**
- `Dockerfile` - Codespaces/devcontainer image (Android + Node toolchain).
- `devcontainer.json` - Codespaces/VS Code devcontainer definition (auto-discovered).
- `post-create.sh` - Devcontainer post-create setup; runs check-toolchain.py.

**.github/**
- `dependabot.yml` - Dependabot update schedule for Gradle, npm, Actions.

**.github/workflows/**
- `ci.yml` - CI: structure checks, Android unit/assemble/lint/bundle, Functions build/test/lint, emulator e2e.
- `instrumented-tests.yml` - Runs androidTest (Room migration + launch smoke) on an emulator.
- `release.yml` - Builds signed prod release bundle on tags/dispatch.

**app/**
- `build.gradle.kts` - Android app module build: flavors, Compose, Hilt, Room, Firebase, ktlint, dependency floors.
- `google-services.ci.json` - Placeholder Firebase config CI copies to app/google-services.json.
- `keystore.properties.example` - Template for gitignored release-signing properties.
- `proguard-rules.pro` - R8 rules file (44 bytes) referenced by release build type; inert today because isMinifyEnabled=false (low).

**app/schemas/...WiccanDatabase//**
- (directory) - Room schema exports 1,2,3,5.json (4.json missing - see Issues); used by migration tests. 4 files.

**app/src/androidTest/kotlin/com/jaimesorrow/wiccan/data/local/**
- `WiccanDatabaseMigrationTest.kt` - Instrumented Room migration test against exported schemas.

**app/src/androidTest/kotlin/com/jaimesorrow/wiccan/ui/**
- `AppLaunchSmokeTest.kt` - Instrumented app-launch smoke test.

**app/src/debug/kotlin/com/jaimesorrow/wiccan/appcheck/**
- `DebugAppCheckModule.kt` - Hilt module: debug App Check provider (debug builds).

**app/src/dev/res/values/**
- `strings.xml` - Dev-flavor app name override.

**app/src/main/**
- `AndroidManifest.xml` - Manifest: activity, FCM service, widget receivers, permissions.

**app/src/main/kotlin/com/jaimesorrow/wiccan/**
- `MainActivity.kt` - Single activity: splash, theme, root gate navigation (manifest entry).
- `WiccanApplication.kt` - Hilt Application: WorkManager factory, App Check, analytics/crash init (manifest). Used by: DailyContentWorker.kt, WiccanWorkerFactory.kt, WiccanPaletteScreenshotTest.kt +1.

**app/src/main/kotlin/com/jaimesorrow/wiccan/analytics/**
- `AnalyticsConsentManager.kt` - Applies analytics/crash consent to Firebase. Used by: WiccanApplication.kt, PolicyAcceptanceViewModel.kt, SettingsViewModel.kt +6.
- `AnalyticsLogger.kt` - Typed analytics event logging. Used by: AuthRepositoryImpl.kt, SubscriptionRepositoryImpl.kt, DeleteAccountUseCase.kt +7.

**app/src/main/kotlin/com/jaimesorrow/wiccan/appcheck/**
- `AppCheckProviderInstaller.kt` - Interface the debug/release App Check modules implement. Used by: DebugAppCheckModule.kt, WiccanApplication.kt, ReleaseAppCheckModule.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/billing/**
- `WiccanBillingClient.kt` - Play Billing client wrapper (offers, purchase flow). Used by: SubscriptionRepositoryImpl.kt, SubscriptionOfferTest.kt, SubscriptionRepositoryImplTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/core/auth/**
- `CurrentUserProvider.kt` - Abstraction for the signed-in uid. Used by: FirebaseCurrentUserProvider.kt, ArchiveFirestoreDataSource.kt, ContentRepositoryImpl.kt +8.
- `FirebaseCurrentUserProvider.kt` - Firebase Auth implementation of CurrentUserProvider. Used by: AuthModule.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/core/config/**
- `FirebaseEmulatorConfig.kt` - Points Firebase SDKs at emulators when -PuseFirebaseEmulators. Used by: AuthModule.kt, FirebaseModule.kt.
- `PolicyUrls.kt` - Hosted Terms/Privacy/delete-account URLs. Used by: LegalDocuments.kt, SettingsScreen.kt, LegalDocumentsTest.kt +1.

**app/src/main/kotlin/com/jaimesorrow/wiccan/core/error/**
- `AppError.kt` - App-wide error taxonomy. Used by: AppErrorMapper.kt, ThrowableToAppError.kt, AppResult.kt +29.
- `AppErrorMapper.kt` - AppError -> user message. Used by: ArchiveDetailScreen.kt, SignInScreen.kt, PaywallScreen.kt +3.
- `ThrowableToAppError.kt` - Classifies exceptions into AppError. Used by: ArchiveRepositoryImpl.kt, AuthRepositoryImpl.kt, ContentRepositoryImpl.kt +2.

**app/src/main/kotlin/com/jaimesorrow/wiccan/core/result/**
- `AppResult.kt` - Success/failure result type for repositories. Used by: ArchiveRepositoryImpl.kt, AuthRepositoryImpl.kt, ContentRepositoryImpl.kt +34.

**app/src/main/kotlin/com/jaimesorrow/wiccan/core/time/**
- `LocalCalendarProvider.kt` - Device-local date/timezone for cache validity and requests. Used by: ContentRepositoryImpl.kt, ContentRepositoryImplTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/core/util/**
- `ContextExt.kt` - Context.findActivity() helper for billing/credentials.

**app/src/main/kotlin/com/jaimesorrow/wiccan/data/local/**
- `AnalyticsConsentPreferences.kt` - DataStore for analytics consent. Used by: AnalyticsConsentManager.kt, AnalyticsConsentManagerTest.kt.
- `CachedDailyContentDao.kt` - Room DAO for cached daily lesson. Used by: WiccanDatabase.kt, ContentRepositoryImpl.kt, DatabaseModule.kt +1.
- `CachedDailyContentEntity.kt` - Room entity for cached daily lesson. Used by: CachedDailyContentDao.kt, WiccanDatabase.kt, ContentMappers.kt +2.
- `Converters.kt` - Room type converters (string lists). Used by: WiccanDatabase.kt.
- `Migrations.kt` - Room migrations 1->5. Used by: WiccanDatabaseMigrationTest.kt, DatabaseModule.kt, WiccanDatabaseMigrationJvmTest.kt.
- `NotificationPreferences.kt` - DataStore for notification toggle. Used by: ThemePreferences.kt, NotificationRepositoryImpl.kt, NotificationRepositoryImplTest.kt.
- `ThemePreferences.kt` - DataStore for theme mode. Used by: WiccanThemeMode.kt, SettingsViewModel.kt, ThemeViewModel.kt +4.
- `WiccanDatabase.kt` - Room database (version 5). Used by: WiccanDatabaseMigrationTest.kt, Migrations.kt, DatabaseModule.kt +1.

**app/src/main/kotlin/com/jaimesorrow/wiccan/data/mapper/**
- `ContentMappers.kt` - Callable/Room payload <-> Content mapping. Used by: ArchiveRepositoryImpl.kt, ContentRepositoryImpl.kt, ContentMappersTest.kt +2.
- `HistoryMappers.kt` - Firestore history docs -> HistoryEntry. Used by: ArchiveRepositoryImpl.kt, ContentMappersTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/data/remote/**
- `AccountFunctionsDataSource.kt` - Callables: acceptTerms, deleteAccount, profile. Used by: AuthRepositoryImpl.kt, AuthRepositoryImplTest.kt.
- `ArchiveFirestoreDataSource.kt` - Firestore reads for Grimoire history. Used by: ArchiveRepositoryImpl.kt.
- `ContentFunctionsDataSource.kt` - getTodayContent callable. Used by: ContentRepositoryImpl.kt, ContentRepositoryImplTest.kt.
- `FcmTokenProvider.kt` - Fetches FCM registration token. Used by: NotificationRepositoryImpl.kt, NotificationRepositoryImplTest.kt.
- `GoogleCredentialDataSource.kt` - Credential Manager Google sign-in. Used by: AuthRepositoryImpl.kt, AuthRepositoryImplTest.kt.
- `NotificationFunctionsDataSource.kt` - Device/notification registration callables. Used by: NotificationRepositoryImpl.kt, NotificationRepositoryImplTest.kt.
- `SubscriptionFunctionsDataSource.kt` - verifyPurchase callable. Used by: SubscriptionRepositoryImpl.kt, SubscriptionRepositoryImplTest.kt.
- `UserTierDataSource.kt` - Observes users/{uid} tier. Used by: SubscriptionRepositoryImpl.kt, UserTierDataSourceTest.kt, SubscriptionRepositoryImplTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/data/repository/**
- `ArchiveRepositoryImpl.kt` - ArchiveRepository over Firestore. Used by: RepositoryModule.kt.
- `AuthRepositoryImpl.kt` - AuthRepository over Firebase Auth/Firestore. The five redundant safe calls at lines 59-63 are removed in #150 (`ca6f400`). Used by: RepositoryModule.kt, AuthRepositoryImplTest.kt.
- `ContentRepositoryImpl.kt` - ContentRepository: callable + Room cache. Used by: RepositoryModule.kt, ContentRepositoryImplTest.kt.
- `NotificationRepositoryImpl.kt` - NotificationRepository: FCM token registration. Used by: RepositoryModule.kt, NotificationRepositoryImplTest.kt.
- `SubscriptionRepositoryImpl.kt` - SubscriptionRepository: billing + verification. Used by: RepositoryModule.kt, SubscriptionRepositoryImplTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/di/**
- `AuthModule.kt` - Hilt: FirebaseAuth + CurrentUserProvider bindings.
- `DatabaseModule.kt` - Hilt: Room database + DAO. Used by: Migrations.kt.
- `FirebaseModule.kt` - Hilt: Firestore, Functions, Messaging, Crashlytics, Analytics.
- `RepositoryModule.kt` - Hilt: repository interface bindings.

**app/src/main/kotlin/com/jaimesorrow/wiccan/domain/model/**
- `AccountStatus.kt` - Onboarding gate states. Used by: AuthRepositoryImpl.kt, AuthRepository.kt, RootViewModel.kt +3.
- `AuthState.kt` - Signed-in/out state. Used by: AuthRepositoryImpl.kt, AccountStatus.kt, AuthRepository.kt +3.
- `Content.kt` - Daily content domain model. Used by: CachedDailyContentEntity.kt, ContentMappers.kt, ArchiveFirestoreDataSource.kt +19.
- `ContentCategory.kt` - Content categories. Used by: Migrations.kt, ContentMappers.kt, HistoryMappers.kt +11.
- `ContentStatus.kt` - Content status enum. Used by: ContentMappers.kt, Content.kt, WiccanThemePreviews.kt +4.
- `ContentTier.kt` - Free/premium tier enum. Used by: AnalyticsLogger.kt, ContentMappers.kt, UserTierDataSource.kt +16.
- `ContentType.kt` - Spell/potion/lesson enum. Used by: AnalyticsLogger.kt, ContentMappers.kt, HistoryMappers.kt +11.
- `HistoryEntry.kt` - Grimoire history row model. Used by: HistoryMappers.kt, ArchiveRepositoryImpl.kt, ArchiveRepository.kt +6.
- `PolicyVersions.kt` - Required Terms/Privacy versions (mirrors functions/src/policyVersions.ts). Used by: AuthRepositoryImpl.kt, AuthRepository.kt, LegalDocuments.kt +1.
- `RootState.kt` - Top-level app gate state. Used by: MainActivity.kt, AccountStatus.kt, RootViewModel.kt +2.
- `SubscriptionOffer.kt` - Play subscription offer model. Used by: WiccanBillingClient.kt, SubscriptionRepositoryImpl.kt, SubscriptionRepository.kt +2.
- `UserContentHistory.kt` - UNUSED data class (only reference is its declaration). Removed in #150 (`ca6f400`), along with its README mention.
- `WiccanThemeMode.kt` - Theme mode (automatic/system/locked palette). Used by: ThemePreferences.kt, SettingsContract.kt, SettingsScreen.kt +11.

**app/src/main/kotlin/com/jaimesorrow/wiccan/domain/repository/**
- `ArchiveRepository.kt` - Archive (Grimoire) port. Used by: ArchiveRepositoryImpl.kt, RepositoryModule.kt, ArchiveDetailViewModel.kt +2.
- `AuthRepository.kt` - Auth port. Used by: AuthRepositoryImpl.kt, RepositoryModule.kt, DeleteAccountUseCase.kt +6.
- `ContentRepository.kt` - Daily content port. Used by: ContentRepositoryImpl.kt, RepositoryModule.kt, TodayViewModel.kt +4.
- `NotificationRepository.kt` - Notification port. Used by: NotificationRepositoryImpl.kt, RepositoryModule.kt, WiccanMessagingService.kt +4.
- `SubscriptionRepository.kt` - Subscription port. Used by: ContentRepositoryImpl.kt, SubscriptionRepositoryImpl.kt, RepositoryModule.kt +6.

**app/src/main/kotlin/com/jaimesorrow/wiccan/domain/usecase/**
- `DeleteAccountUseCase.kt` - Account deletion orchestration (Settings). Used by: SettingsViewModel.kt, SettingsScreenButtonsTest.kt, SettingsViewModelTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/notification/**
- `WiccanMessagingService.kt` - FCM service: token refresh, daily push (manifest). Used by: AndroidManifest.xml.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/archive/**
- `ArchiveContract.kt` - Grimoire list UI state/intents. Used by: ArchiveScreen.kt, ArchiveViewModel.kt, ArchiveViewModelTest.kt +1.
- `ArchiveDetailContract.kt` - Grimoire detail UI state. Used by: ArchiveDetailScreen.kt, ArchiveDetailViewModel.kt, ArchiveDetailViewModelTest.kt.
- `ArchiveDetailScreen.kt` - Grimoire detail screen. Used by: WiccanNavGraph.kt, OnboardingFlowButtonsTest.kt.
- `ArchiveDetailViewModel.kt` - Grimoire detail ViewModel. Used by: ArchiveDetailScreen.kt, WiccanDestinations.kt, WiccanNavGraph.kt +2.
- `ArchiveEntryRow.kt` - Grimoire list row composable. Used by: ArchiveScreen.kt.
- `ArchiveScreen.kt` - Grimoire list screen. Used by: ContentDetailView.kt, WiccanNavGraph.kt, SettingsScreen.kt +5.
- `ArchiveViewModel.kt` - Grimoire list ViewModel. Used by: ArchiveScreen.kt, ArchiveViewModelTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/auth/**
- `OnboardingScreen.kt` - Profile/email-verification onboarding screen. Used by: MainActivity.kt, OnboardingFlowButtonsTest.kt.
- `OnboardingViewModel.kt` - Onboarding ViewModel. Used by: OnboardingScreen.kt, OnboardingFlowButtonsTest.kt.
- `RootViewModel.kt` - Computes RootState gate. Used by: MainActivity.kt, PolicyAcceptanceViewModel.kt, ThemeViewModel.kt +1.
- `SignInContract.kt` - Sign-in UI state/intents. Used by: SignInScreen.kt, SignInViewModel.kt, SignInScreenLayoutTest.kt +1.
- `SignInScreen.kt` - Sign-in screen. Used by: MainActivity.kt, SignInScreenLayoutTest.kt.
- `SignInViewModel.kt` - Sign-in ViewModel. Used by: SignInScreen.kt, SignInViewModelTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/brand/**
- `WiccanBrand.kt` - Brand emblem/logo composables. Used by: MainActivity.kt, ArchiveScreen.kt, OnboardingScreen.kt +8.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/content/**
- `ContentDetailView.kt` - Shared lesson body renderer (Today + Grimoire). Used by: ArchiveDetailScreen.kt, TodayScreen.kt, ContentDetailViewTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/legal/**
- `LegalDocumentScreen.kt` - In-app legal document screen. Used by: MainActivity.kt, WiccanNavGraph.kt, OnboardingFlowButtonsTest.kt.
- `LegalDocuments.kt` - Versioned native legal texts. Used by: MainActivity.kt, PolicyUrls.kt, OnboardingScreen.kt +8.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/navigation/**
- `WiccanDestinations.kt` - Navigation routes. Used by: MainActivity.kt, WiccanNavGraph.kt, WiccanWidget.kt +2.
- `WiccanNavGraph.kt` - App shell nav graph. Used by: MainActivity.kt, GrimoireNavigationTest.kt, WiccanAppShellNavigationTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/notification/**
- `NotificationSetupEffect.kt` - Notification permission/registration effect. Used by: MainActivity.kt.
- `NotificationSetupViewModel.kt` - Notification setup ViewModel. Used by: NotificationSetupEffect.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/paywall/**
- `PaywallContract.kt` - Paywall UI state/intents. Used by: PaywallScreen.kt, PaywallViewModel.kt, PaywallViewModelTest.kt.
- `PaywallScreen.kt` - Paywall screen. Used by: MainActivity.kt, OnboardingFlowButtonsTest.kt.
- `PaywallViewModel.kt` - Paywall ViewModel. Used by: PaywallScreen.kt, OnboardingFlowButtonsTest.kt, PaywallViewModelTest.kt.
- `SubscriptionSetupEffect.kt` - Billing connection/restore effect. Used by: MainActivity.kt.
- `SubscriptionSetupViewModel.kt` - Subscription setup ViewModel. Used by: SubscriptionSetupEffect.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/policy/**
- `PolicyAcceptanceContract.kt` - Policy acceptance UI state. Used by: PolicyAcceptanceScreen.kt, PolicyAcceptanceViewModel.kt, OnboardingFlowButtonsTest.kt +1.
- `PolicyAcceptanceScreen.kt` - Terms/Privacy acceptance screen. Used by: MainActivity.kt, ArchiveScreen.kt, OnboardingScreen.kt +11.
- `PolicyAcceptanceViewModel.kt` - Policy acceptance ViewModel. Used by: PolicyAcceptanceScreen.kt, OnboardingFlowButtonsTest.kt, PolicyAcceptanceViewModelTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/settings/**
- `SettingsContract.kt` - Settings UI state/intents. Used by: SettingsScreen.kt, SettingsViewModel.kt, SettingsViewModelTest.kt.
- `SettingsScreen.kt` - Settings screen. Used by: MainActivity.kt, WiccanNavGraph.kt, WiccanThemePreviews.kt +1.
- `SettingsViewModel.kt` - Settings ViewModel. Used by: WiccanNavGraph.kt, SettingsScreen.kt, SettingsScreenButtonsTest.kt +1.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/theme/**
- `SeasonalThemeResolver.kt` - Wheel-of-the-Year palette selection. Used by: Theme.kt, WiccanColorSchemeResolver.kt, SeasonalThemeResolverTest.kt.
- `Theme.kt` - WiccanTheme MaterialTheme wrapper. Used by: MainActivity.kt, ThemeViewModel.kt, WiccanPalette.kt +2.
- `ThemeViewModel.kt` - Theme mode ViewModel. Used by: MainActivity.kt, ThemeViewModelTest.kt.
- `Type.kt` - Typography (Cinzel/Manrope via downloadable fonts). Used by: Theme.kt, WiccanPaletteScreenshotTest.kt.
- `WiccanColorSchemeResolver.kt` - Resolves active palette for app and widget. Used by: SeasonalThemeResolver.kt, Theme.kt, WiccanWidget.kt +1.
- `WiccanColorSchemes.kt` - Palette -> Material ColorScheme.
- `WiccanPalette.kt` - The six palettes enum. Used by: SeasonalThemeResolver.kt, Type.kt, WiccanColorSchemeResolver.kt +9.
- `WiccanPaletteColors.kt` - Raw color tokens per palette. Used by: WiccanBrand.kt, WiccanColorSchemes.kt, BrandAssetsTest.kt.
- `WiccanThemeModeUi.kt` - Theme mode picker UI. Used by: ArchiveScreen.kt, ContentDetailView.kt, LegalDocuments.kt +6.
- `WiccanThemePreviews.kt` - @Preview composables for all palettes (Android Studio only).

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/today/**
- `TodayContract.kt` - Today UI state/intents. Used by: WiccanThemePreviews.kt, TodayScreen.kt, TodayViewModel.kt +2.
- `TodayScreen.kt` - Today (daily lesson) screen. Used by: WiccanNavGraph.kt, WiccanThemePreviews.kt, TodayScreenTest.kt.
- `TodayViewModel.kt` - Today ViewModel. Used by: TodayScreen.kt, FakeContentRepository.kt, TodayViewModelTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/ui/today/components/**
- `ContentTypePill.kt` - Content-type chip. Used by: ArchiveEntryRow.kt, ContentDetailView.kt.
- `HairlineDivider.kt` - Divider composable. Used by: SignInScreen.kt, ContentDetailView.kt, TodaySkeleton.kt.
- `MaterialsCard.kt` - Materials list card. Used by: ContentDetailView.kt.
- `SafetyNote.kt` - Safety disclosure note. Used by: ContentDetailView.kt.
- `TodaySkeleton.kt` - Loading skeleton. Used by: TodayScreen.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/widget/**
- `WiccanThemeDateChangeReceiver.kt` - Refreshes widget palette on date change (manifest). Used by: AndroidManifest.xml.
- `WiccanWidget.kt` - Glance home-screen widget. Used by: WiccanThemeDateChangeReceiver.kt, WiccanWidgetReceiver.kt, WiccanWidgetUpdater.kt +2.
- `WiccanWidgetEntryPoint.kt` - Hilt EntryPoint for widget dependencies. Used by: WiccanWidget.kt.
- `WiccanWidgetReceiver.kt` - Glance widget receiver (manifest). Used by: AndroidManifest.xml.
- `WiccanWidgetUpdater.kt` - Triggers widget refresh (Settings). Used by: SettingsViewModel.kt, SettingsViewModelTest.kt.
- `WidgetUpdater.kt` - Widget refresh interface used by DailyContentWorker. Used by: DailyContentWorker.kt, DailyContentWorkerTest.kt.

**app/src/main/kotlin/com/jaimesorrow/wiccan/worker/**
- `DailyContentWorker.kt` - WorkManager job prefetching daily content. Used by: WiccanApplication.kt, WiccanWorkerFactory.kt, DailyContentWorkerTest.kt.
- `WiccanWorkerFactory.kt` - Hilt-aware WorkManager factory. Used by: WiccanApplication.kt, AndroidManifest.xml.

**app/src/main/res/drawable-nodpi/**
- `wiccan_emblem.png` - Brand emblem bitmap (WiccanBrand.kt).
- `wiccan_full_logo.png` - Full logo bitmap (WiccanBrand.kt).
- `wiccan_full_logo_mask.png` - Logo alpha mask for palette tinting (WiccanBrand.kt).

**app/src/main/res/drawable-xxxhdpi/**
- `splash_emblem.png` - Splash-screen icon (themes.xml windowSplashScreenAnimatedIcon).

**app/src/main/res/layout/**
- `widget_loading.xml` - Widget initial layout (wiccan_widget_info.xml).

**app/src/main/res/mipmap-*/**
- (directory) - Launcher icons (ic_launcher, ic_launcher_round, foreground/monochrome layers) per density + adaptive-icon XML; referenced by manifest. 22 files.

**app/src/main/res/values/**
- `colors.xml` - XML colors for launcher/splash/system bars.
- `strings.xml` - App strings.
- `themes.xml` - Activity + splash themes.

**app/src/main/res/values-v23/**
- `font_certs.xml` - Google Fonts provider certs (Type.kt). Folder qualifier v23 is obsolete (minSdk 24). Moved to `values/` in #150 (`ca6f400`).

**app/src/main/res/xml/**
- `backup_rules.xml` - Auto-backup rules (manifest fullBackupContent).
- `data_extraction_rules.xml` - Android 12+ backup/transfer rules (manifest).
- `wiccan_widget_info.xml` - Glance widget provider metadata (manifest receiver).

**app/src/prod/**
- `google-services.json` - Real prod Firebase client config for prod flavor.

**app/src/release/kotlin/com/jaimesorrow/wiccan/appcheck/**
- `ReleaseAppCheckModule.kt` - Hilt module: Play Integrity App Check provider (release builds).

**app/src/staging/res/values/**
- `strings.xml` - Staging-flavor app name override.

**app/src/test/kotlin/com/jaimesorrow/wiccan/analytics/**
- `AnalyticsConsentManagerTest.kt` - JVM/Robolectric test for AnalyticsConsentManager.
- `AnalyticsLoggerTest.kt` - JVM/Robolectric test for AnalyticsLogger.

**app/src/test/kotlin/com/jaimesorrow/wiccan/billing/**
- `SubscriptionOfferTest.kt` - JVM/Robolectric test for SubscriptionOffer.

**app/src/test/kotlin/com/jaimesorrow/wiccan/data/local/**
- `WiccanDatabaseMigrationJvmTest.kt` - JVM/Robolectric test for WiccanDatabaseMigrationJvm.

**app/src/test/kotlin/com/jaimesorrow/wiccan/data/mapper/**
- `ContentMappersTest.kt` - JVM/Robolectric test for ContentMappers.

**app/src/test/kotlin/com/jaimesorrow/wiccan/data/remote/**
- `UserTierDataSourceTest.kt` - JVM/Robolectric test for UserTierDataSource.

**app/src/test/kotlin/com/jaimesorrow/wiccan/data/repository/**
- `AuthRepositoryImplTest.kt` - JVM/Robolectric test for AuthRepositoryImpl.
- `ContentRepositoryImplTest.kt` - JVM/Robolectric test for ContentRepositoryImpl.
- `FakeCachedDailyContentDao.kt` - Test fake for CachedDailyContentDao.
- `NotificationRepositoryImplTest.kt` - JVM/Robolectric test for NotificationRepositoryImpl.
- `SubscriptionRepositoryImplTest.kt` - JVM/Robolectric test for SubscriptionRepositoryImpl.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/**
- `OnboardingFlowButtonsTest.kt` - JVM/Robolectric test for OnboardingFlowButtons.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/archive/**
- `ArchiveDetailViewModelTest.kt` - JVM/Robolectric test for ArchiveDetailViewModel.
- `ArchiveViewModelTest.kt` - JVM/Robolectric test for ArchiveViewModel.
- `FakeArchiveRepository.kt` - Test fake for ArchiveRepository.
- `GrimoireScreenTest.kt` - JVM/Robolectric test for GrimoireScreen.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/auth/**
- `FakeAuthRepository.kt` - Test fake for AuthRepository.
- `RootViewModelTest.kt` - JVM/Robolectric test for RootViewModel.
- `SignInScreenLayoutTest.kt` - JVM/Robolectric test for SignInScreenLayout.
- `SignInViewModelTest.kt` - JVM/Robolectric test for SignInViewModel.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/brand/**
- `BrandAssetsTest.kt` - JVM/Robolectric test for BrandAssets.
- `BrandPaletteScreenshotTest.kt` - JVM/Robolectric test for BrandPaletteScreenshot.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/content/**
- `ContentDetailViewTest.kt` - JVM/Robolectric test for ContentDetailView.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/legal/**
- `LegalDocumentsTest.kt` - JVM/Robolectric test for LegalDocuments.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/navigation/**
- `GrimoireNavigationTest.kt` - JVM/Robolectric test for GrimoireNavigation.
- `WiccanAppShellNavigationTest.kt` - JVM/Robolectric test for WiccanAppShellNavigation.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/paywall/**
- `FakeSubscriptionRepository.kt` - Test fake for SubscriptionRepository.
- `PaywallViewModelTest.kt` - JVM/Robolectric test for PaywallViewModel.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/policy/**
- `PolicyAcceptanceViewModelTest.kt` - JVM/Robolectric test for PolicyAcceptanceViewModel.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/settings/**
- `SettingsScreenButtonsTest.kt` - JVM/Robolectric test for SettingsScreenButtons.
- `SettingsViewModelTest.kt` - JVM/Robolectric test for SettingsViewModel.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/theme/**
- `SeasonalThemeResolverTest.kt` - JVM/Robolectric test for SeasonalThemeResolver.
- `ThemeViewModelTest.kt` - JVM/Robolectric test for ThemeViewModel.
- `WiccanColorSchemeResolverTest.kt` - JVM/Robolectric test for WiccanColorSchemeResolver.
- `WiccanColorSchemesTest.kt` - JVM/Robolectric test for WiccanColorSchemes.
- `WiccanPaletteScreenshotTest.kt` - JVM/Robolectric test for WiccanPaletteScreenshot.
- `WiccanThemeModeTest.kt` - JVM/Robolectric test for WiccanThemeMode.

**app/src/test/kotlin/com/jaimesorrow/wiccan/ui/today/**
- `FakeContentRepository.kt` - Test fake for ContentRepository.
- `TodayScreenTest.kt` - JVM/Robolectric test for TodayScreen.
- `TodayViewModelTest.kt` - JVM/Robolectric test for TodayViewModel.

**app/src/test/kotlin/com/jaimesorrow/wiccan/worker/**
- `DailyContentWorkerTest.kt` - JVM/Robolectric test for DailyContentWorker.

**brand/**
- `README.md` - Brand asset provenance and regeneration instructions.

**brand/exports//**
- (directory) - Generated icon/logo/Play/preview PNGs from build_brand_assets.py; verified by BrandAssetsTest. 10 files.

**brand/scripts/**
- `build_brand_assets.py` - Regenerates launcher/splash/in-app/Play assets from brand/source.

**brand/source/**
- `01_App_Icon_Approved_Reference.png` - Approved app-icon source artwork (input to build_brand_assets.py).
- `02_Full_Logo_Approved_Reference.png` - Approved full-logo source artwork (input to build_brand_assets.py).
- `Wiccan_Brand_Guidelines.pdf` - Owner-supplied brand guidelines.

**docs/**
- `ANCIENT_LESSON_SOURCING.md` - Content sourcing standard and agent commands.
- `BUILD_VALIDATION.md` - How to validate builds locally/CI.
- `CHATGPT_DAILY_LESSON_PROMPT.md` - UNCLEAR: external ChatGPT authoring prompt; not linked anywhere.
- `CONTENT_CONTINUANCE_AGENT.md` - UNCLEAR (likely keep): content agent operator notes; not linked from other docs.
- `CONTENT_PRODUCTION_RUNBOOK.md` - Content production/deploy runbook.
- `CONTENT_SCHEMA_V2.md` - Canonical lesson schema v2 (claims legacyFieldsFor is used - see Issues).
- `CONTENT_VOICE_AND_LESSON_STANDARD.md` - Editorial voice standard (content-voice skill).
- `DAILY_GRIMOIRE.md` - UNCLEAR (likely keep): daily delivery/Grimoire design notes; not linked.
- `DISASTER_RECOVERY.md` - Backup/restore procedures.
- `DUPLICATE_AND_ORIGINALITY_CONTROLS.md` - UNCLEAR: 9-line summary of similarity controls; not linked.
- `ERMAN_BERLIN_3027_SOURCE_EVIDENCE_REVIEW.md` - UNCLEAR: point-in-time source research report; not linked.
- `FIREBASE_SETUP.md` - Firebase project setup.
- `FIRESTORE_MIGRATION_RUNBOOK.md` - Firestore content migration runbook.
- `FIVE_BOUND_REWRITE_REVIEW.md` - UNCLEAR: point-in-time rewrite review artifact; not linked.
- `HISTORICAL_SOURCE_REGISTRY_REPORT.md` - UNCLEAR: point-in-time registry report; not linked.
- `INCIDENT_RESPONSE.md` - Incident response procedures (linked from STATUS).
- `LEGACY_LESSON_AUDIT.md` - Generated 2026-11-08 legacy lesson audit output (linked from CONTENT_SCHEMA_V2).
- `LOCAL_REWRITE_BATCHES.md` - UNCLEAR (likely keep): how rewrite batches are produced; not linked.
- `MAINTENANCE.md` - Operator maintenance guide (systemConfig, CLIs, fonts, toolchain).
- `ORACC_MAQLU_SOURCE_EVIDENCE_REVIEW.md` - UNCLEAR: point-in-time source research report; not linked.
- `PLAY_CONSOLE_READINESS.md` - Play Console submission readiness.
- `PRACTICAL_CATEGORY_MAPPING_PROPOSAL.md` - UNCLEAR: category mapping proposal (status: proposed); not linked.
- `PRACTICAL_MAGIC_LESSON_TEMPLATE.md` - Lesson template (linked from README).
- `PRODUCTION_SETUP.md` - Production environment setup.
- `PRODUCTION_VALIDATION.md` - Production validation steps.
- `READ_ONLY_AUDIT_ACCESS.md` - UNCLEAR: says no Firebase identity connected yet - likely stale; not linked.
- `RELEASE_CHECKLIST.md` - Release checklist.
- `STAGED_SOURCE_EVIDENCE_REVIEW.md` - Source evidence for FIVE_BOUND_REWRITE_REVIEW (same fate as that doc).
- `STATUS.md` - Project status log.
- `SUBSCRIPTION_ACCESS.md` - Subscription entitlement rules.

**firestore-tests/**
- `.gitignore` - Ignores node_modules in this package (checked by validate-repo.sh).
- `README.md` - How to run rules emulator tests.
- `firestore.rules.test.mjs` - Firestore rules tests (CI firestore-emulator job).
- `package-lock.json` - Locked deps for rules tests.
- `package.json` - Test package + emulator test scripts.
- `with-java-21.sh` - Ensures Java 21 for the emulator (npm test).

**functions/**
- `.env.example` - Template for functions env vars (real .env gitignored).
- `ancientContentSeed.json` - Checked-in 296-record content seed (contentSeed.ts).
- `eslint.config.mjs` - ESLint flat config (npm run lint, auto-discovered).
- `historicalSourceRegistry.json` - Historical source candidate registry (historicalSourceRegistry.ts).
- `jest.config.js` - Jest config.
- `package-lock.json` - Locked npm dependency tree (npm ci).
- `package.json` - Functions deps + npm scripts (build/test/lint/deploy + operator CLIs).
- `sourcePassages.json` - Editor-approved source passages, currently [] by design (sourcePassages.ts).
- `sources.json` - Curated historical sources (contentSource.ts).
- `tsconfig.json` - TypeScript build config (src -> lib). The invalid key "compileOnRoot" is removed in #150 (`ca6f400`).
- `tsconfig.test.json` - ts-jest config for tests.

**functions/lessons/drafts/**
- `.gitkeep` - Keeps canonical draft dir that repoInventory.readCanonicalDrafts scans.

**functions/rewriteBatches//**
- (directory) - batch-01..12.editorial.json: editorial rewrite packets read by verifyRewriteCurriculumCli, repoInventory.ts, rewriteBatchStaging.test.ts. 12 files.

**functions/src/**
- `acceptTerms.ts` - Callable: record Terms/Privacy acceptance. Used by: acceptTerms.test.ts, sendDailyNotifications.test.ts, index.ts +1.
- `accountDeletionPort.ts` - Port: account-deletion data operations. Used by: deleteAccount.ts, firestoreAccountDeletionRepository.ts, fakeAccountDeletionRepository.ts.
- `accountWriteGuard.ts` - Guards writes against concurrent account deletion. Used by: firestoreContentRepository.ts, firestoreNotificationUserRepository.ts, firestoreUserProfileRepository.ts.
- `approveDraft.ts` - Draft safety/completeness checks and approval planning. Used by: approveDraft.test.ts, approveDraftCli.ts, publishEligibleCli.ts.
- `billingIdentity.ts` - Obfuscated Play account id from uid. Used by: firestoreUserRepository.test.ts, verifyPurchase.test.ts, firestoreAccountDeletionRepository.ts +4.
- `callableInput.ts` - Input validation for callables. Used by: callableInput.test.ts, getTodayContentCallable.test.ts, index.ts.
- `claudeDraftGenerator.ts` - Anthropic-backed DraftGenerationPort. Used by: index.ts, generateDraftCli.ts.
- `contentAdminRepositoryPort.ts` - Port: admin content writes/reads. Used by: approveDraft.test.ts, generationOriginality.test.ts, approveDraft.ts +5.
- `contentAgent.ts` - Scheduled content agent (batch draft generation with lease). Used by: contentAgent.test.ts, firestoreContentAgentState.integration.test.ts, firestoreContentAgentStateRepository.ts +3.
- `contentAuditComparison.ts` - Compares before/after content audits. Used by: contentAuditComparison.test.ts, compareContentAuditsCli.ts.
- `contentBackup.ts` - Content backup capture/validation. Used by: contentBackup.test.ts, contentRollback.test.ts, contentRollback.ts +4.
- `contentEligibility.ts` - Publication eligibility rules. Used by: contentEligibility.test.ts, firestoreContentRepository.ts, getTodayContent.ts +1.
- `contentInventory.ts` - Content inventory verification. Used by: contentInventory.test.ts, seedContentImport.ts.
- `contentRepositoryPort.ts` - Port: daily content selection storage. Used by: firestoreContentRepository.integration.test.ts, getTodayContent.test.ts, firestoreContentRepository.ts +4.
- `contentRollback.ts` - Content rollback planning. Used by: contentRollback.test.ts, rollbackContentCli.ts.
- `contentSeed.ts` - Loads/validates ancientContentSeed.json. Used by: contentInventory.test.ts, contentSeed.test.ts, rewriteBatchStaging.test.ts +5.
- `contentSimilarity.ts` - Lexical/fingerprint duplicate detection. Used by: contentSimilarity.test.ts, rewriteBatchStaging.test.ts, generationOriginality.ts +3.
- `contentSource.ts` - Loads curated sources.json. Used by: approveDraft.test.ts, contentAgent.test.ts, contentSeed.test.ts +10.
- `contentUsageBackfill.ts` - Backfills content usage markers. Used by: contentUsageBackfill.test.ts, backfillContentUsageCli.ts, firebaseSecurity.test.mjs.
- `createUserProfile.ts` - Auth onCreate: user profile document. Used by: createUserProfile.test.ts, index.ts.
- `dateUtils.ts` - User-local date computation. Used by: dateUtils.test.ts, firestoreContentRepository.integration.test.ts, callableInput.ts +2.
- `deleteAccount.ts` - Callable: erase account data. Used by: deleteAccount.test.ts, index.ts.
- `draftGenerationPort.ts` - Port: LLM draft generation. Used by: contentAgent.test.ts, claudeDraftGenerator.ts, contentAdminRepositoryPort.ts +3.
- `fcmSenderPort.ts` - Port: FCM sending. Used by: firebaseFcmSender.ts, sendDailyNotifications.ts, fakeFcmSenderPort.ts.
- `firebaseFcmSender.ts` - FCM sender implementation. Used by: index.ts.
- `firestoreAccountDeletionRepository.ts` - Firestore account deletion impl. Used by: firestoreContentRepository.integration.test.ts, firestoreUserRepository.test.ts, index.ts +1.
- `firestoreContentAdminRepository.ts` - Firestore admin content impl. Used by: firestoreContentRepository.integration.test.ts, index.ts, approveDraftCli.ts +2.
- `firestoreContentAgentStateRepository.ts` - Firestore content-agent lease/cursor impl. Used by: firestoreContentAgentState.integration.test.ts, index.ts.
- `firestoreContentRepository.ts` - Firestore daily content impl. Used by: firestoreContentRepository.integration.test.ts, getTodayContentCallable.test.ts, index.ts.
- `firestoreNotificationUserRepository.ts` - Firestore notification users impl. Used by: index.ts.
- `firestoreSubscriptionRecordRepository.ts` - Firestore subscription record impl. Used by: firestoreSubscriptionRecordRepository.test.ts, firestoreUserRepository.test.ts, index.ts +1.
- `firestoreSystemConfigRepository.ts` - Reads systemConfig/global kill switches. Used by: getTodayContentCallable.test.ts, index.ts.
- `firestoreUserProfileRepository.ts` - Firestore user profile impl. Used by: getTodayContentCallable.test.ts, index.ts.
- `firestoreUserRepository.ts` - Firestore users/tier impl. Used by: firestoreUserRepository.test.ts, index.ts, firebaseSecurity.test.mjs.
- `generateDraft.ts` - Draft generation orchestration. Used by: generateDraft.test.ts, contentAgent.ts, generateDraftCli.ts.
- `generationOriginality.ts` - Originality checks for generated drafts. Used by: generationOriginality.test.ts, contentAdminRepositoryPort.ts, generateDraft.ts.
- `getTodayContent.ts` - Daily content assignment logic. Used by: firestoreContentRepository.integration.test.ts, getTodayContent.test.ts, index.ts +1.
- `googlePlayBillingRepository.ts` - Play Developer API subscription verification. Used by: googlePlayBillingRepository.test.ts, index.ts.
- `handleRtdn.ts` - Play RTDN handler logic. Used by: handleRtdn.test.ts, index.ts.
- `historicalSourceRegistry.ts` - Validates historicalSourceRegistry.json. Used by: historicalSourceRegistry.test.ts.
- `index.ts` - Functions entry: exports all deployed functions. Used by: SKILL.md, BUILD_VALIDATION.md, eslint.config.mjs +2.
- `notificationUserRepositoryPort.ts` - Port: notification users. Used by: firestoreNotificationUserRepository.ts, sendDailyNotifications.ts, fakeNotificationUserRepository.ts.
- `originalityScreen.ts` - Source/peer originality screen. Used by: originalityScreen.test.ts.
- `playBillingPort.ts` - Port: Play billing. Used by: tierPolicy.test.ts, firestoreAccountDeletionRepository.ts, googlePlayBillingRepository.ts +6.
- `policyVersions.ts` - Current Terms/Privacy versions. Used by: acceptTerms.test.ts, getTodayContentCallable.test.ts, sendDailyNotifications.test.ts +1.
- `practicalContentAudit.ts` - Practical-content audit (audit/seed CLIs). Used by: auditContentCli.ts, seedContentCli.ts.
- `practicalLesson.ts` - Practical lesson schema/checks. Used by: firestoreContentRepository.integration.test.ts, practicalLesson.test.ts, reviewedContentChanges.test.ts +23.
- `recheckLapsedSubscriptions.ts` - Scheduled lapsed-subscription recheck. Used by: recheckLapsedSubscriptions.test.ts, index.ts.
- `reviewedContentChanges.ts` - Applies reviewed content patches. Used by: contentRollback.test.ts, reviewedContentChanges.test.ts, contentRollback.ts +5.
- `rewriteBatchStaging.ts` - Prepares rewrite batch packets. Used by: rewriteBatchStaging.test.ts, prepareRewriteBatchesCli.ts.
- `seedContentImport.ts` - Seed import preflight/apply. Used by: seedContentImport.test.ts, seedContentCli.ts.
- `sendDailyNotifications.ts` - Scheduled daily push sender. Used by: sendDailyNotifications.test.ts, index.ts.
- `sourcePassages.ts` - Loads sourcePassages.json. Used by: contentAgent.test.ts, generateDraft.test.ts, practicalLesson.test.ts +14.
- `subscriptionAccess.ts` - Entitlement access rules. Used by: subscriptionAccess.test.ts, firestoreContentRepository.ts.
- `subscriptionRecordPort.ts` - Port: subscription records. Used by: firestoreSubscriptionRecordRepository.ts, handleRtdn.ts, recheckLapsedSubscriptions.ts +2.
- `systemConfigPort.ts` - Port + types for kill switches. Used by: firestoreSystemConfigRepository.ts, fakeSystemConfigRepository.ts.
- `tierPolicy.ts` - Subscription tier rule. Used by: tierPolicy.test.ts, firestoreAccountDeletionRepository.ts, googlePlayBillingRepository.ts +3.
- `types.ts` - Shared Firestore types (UserContentHistory interface unused - medium). Used by: contentSeed.test.ts, firestoreContentRepository.integration.test.ts, contentAdminRepositoryPort.ts +23.
- `userProfileRepositoryPort.ts` - Port: user profiles. Used by: acceptTerms.ts, createUserProfile.ts, firestoreUserProfileRepository.ts +1.
- `userRepositoryPort.ts` - Port: users/tier. Used by: firestoreUserRepository.test.ts, firestoreUserRepository.ts, handleRtdn.ts +4.
- `verificationEmail.ts` - Verification email rendering. Used by: verificationEmail.test.ts, index.ts.
- `verifyPurchase.ts` - Callable: verify purchase + sync tier. Used by: verifyPurchase.test.ts, index.ts.

**functions/src/__tests__/**
- `acceptTerms.test.ts` - Jest suite for acceptTerms.
- `approveDraft.test.ts` - Jest suite for approveDraft.
- `callableInput.test.ts` - Jest suite for callableInput.
- `contentAgent.test.ts` - Jest suite for contentAgent.
- `contentAuditComparison.test.ts` - Jest suite for contentAuditComparison.
- `contentBackup.test.ts` - Jest suite for contentBackup.
- `contentEligibility.test.ts` - Jest suite for contentEligibility.
- `contentInventory.test.ts` - Jest suite for contentInventory.
- `contentProject.test.ts` - Jest suite for contentProject.
- `contentRollback.test.ts` - Jest suite for contentRollback.
- `contentSeed.test.ts` - Jest suite for contentSeed.
- `contentSimilarity.test.ts` - Jest suite for contentSimilarity.
- `contentSource.test.ts` - Jest suite for contentSource.
- `contentUsageBackfill.test.ts` - Jest suite for contentUsageBackfill.
- `coverageConfig.test.ts` - Jest suite for coverageConfig.
- `createUserProfile.test.ts` - Jest suite for createUserProfile.
- `dateUtils.test.ts` - Jest suite for dateUtils.
- `deleteAccount.test.ts` - Jest suite for deleteAccount.
- `firestoreContentAgentState.integration.test.ts` - Jest suite for firestoreContentAgentState (emulator integration).
- `firestoreContentRepository.integration.test.ts` - Jest suite for firestoreContentRepository (emulator integration).
- `firestoreSubscriptionRecordRepository.test.ts` - Jest suite for firestoreSubscriptionRecordRepository.
- `firestoreUserRepository.test.ts` - Jest suite for firestoreUserRepository.
- `generateDraft.test.ts` - Jest suite for generateDraft.
- `generationOriginality.test.ts` - Jest suite for generationOriginality.
- `getTodayContent.test.ts` - Jest suite for getTodayContent.
- `getTodayContentCallable.test.ts` - Jest suite for getTodayContentCallable.
- `googlePlayBillingRepository.test.ts` - Jest suite for googlePlayBillingRepository.
- `handleRtdn.test.ts` - Jest suite for handleRtdn.
- `historicalSourceRegistry.test.ts` - Jest suite for historicalSourceRegistry.
- `lessonDuplicates.test.ts` - Jest suite for lessonDuplicates.
- `lessonValidation.test.ts` - Jest suite for lessonValidation.
- `originalityScreen.test.ts` - Jest suite for originalityScreen.
- `practicalLesson.test.ts` - Jest suite for practicalLesson.
- `recheckLapsedSubscriptions.test.ts` - Jest suite for recheckLapsedSubscriptions.
- `reviewedContentChanges.test.ts` - Jest suite for reviewedContentChanges.
- `rewriteBatchStaging.test.ts` - Jest suite for rewriteBatchStaging.
- `seedContentImport.test.ts` - Jest suite for seedContentImport.
- `sendDailyNotifications.test.ts` - Jest suite for sendDailyNotifications.
- `subscriptionAccess.test.ts` - Jest suite for subscriptionAccess.
- `tierPolicy.test.ts` - Jest suite for tierPolicy.
- `verificationEmail.test.ts` - Jest suite for verificationEmail.
- `verifyPurchase.test.ts` - Jest suite for verifyPurchase.

**functions/src/lessons/**
- `canonicalLesson.ts` - Canonical v2 lesson model/mapping (legacyFieldsFor unused - see Issues). Used by: lessonDuplicates.test.ts, lessonValidation.test.ts, duplicates.ts +3.
- `categories.ts` - Practical lesson categories. Used by: lessonValidation.test.ts, canonicalLesson.ts, validateLesson.ts.
- `duplicates.ts` - Lesson fingerprinting/duplicate scoring. Used by: lessonDuplicates.test.ts, repoInventory.ts, contentDuplicatesCli.ts.
- `repoInventory.ts` - Reads repo lesson inventory (content CLIs). Used by: contentDuplicatesCli.ts, contentValidateCli.ts.
- `validateLesson.ts` - Lesson validation rules. Used by: lessonValidation.test.ts, contentValidateCli.ts.

**functions/src/scripts/**
- `approveDraftCli.ts` - Operator CLI, run via npm run approve-draft.
- `auditContentCli.ts` - Operator CLI, run via npm run audit-content.
- `backfillContentUsageCli.ts` - Operator CLI, run via npm run backfill-content-usage.
- `backupContentCli.ts` - Operator CLI, run via npm run backup-content.
- `compareContentAuditsCli.ts` - Operator CLI, run via npm run compare-content-audits.
- `contentDuplicatesCli.ts` - Operator CLI, run via npm run content:duplicates.
- `contentProject.ts` - Shared CLI project/flag guard. Used by: contentProject.test.ts, approveDraftCli.ts, auditContentCli.ts +8.
- `contentValidateCli.ts` - Operator CLI, run via npm run content:validate.
- `generateDraftCli.ts` - Operator CLI, run via npm run content-agent / generate-draft.
- `prepareRewriteBatchesCli.ts` - Operator CLI, run via npm run prepare-rewrite-batches.
- `publishEligibleCli.ts` - Operator CLI, run via npm run publish-eligible.
- `restoreTestContentBackupCli.ts` - Operator CLI, run via npm run restore-test-content-backup.
- `rollbackContentCli.ts` - Operator CLI, run via npm run rollback-content.
- `seedContentCli.ts` - Operator CLI, run via npm run seed-content.
- `uploadContentBackupCli.ts` - Operator CLI, run via npm run upload-content-backup.
- `verifyLessonsCli.ts` - Operator CLI, run via npm run verify-lessons.
- `verifyRewriteCurriculumCli.ts` - Operator CLI, run via npm run verify-rewrite-curriculum.

**functions/src/testUtils/**
- `fakeAccountDeletionRepository.ts` - Test fake/fixture used by jest suites (deleteAccount.test.ts).
- `fakeContentAdminRepository.ts` - Test fake/fixture used by jest suites (approveDraft.test.ts, contentAgent.test.ts, generateDraft.test.ts +1).
- `fakeContentAgentState.ts` - Test fake/fixture used by jest suites (contentAgent.test.ts).
- `fakeContentRepository.ts` - Test fake/fixture used by jest suites (firestoreContentRepository.integration.test.ts, getTodayContent.test.ts, sendDailyNotifications.test.ts).
- `fakeDraftGenerationPort.ts` - Test fake/fixture used by jest suites (contentAgent.test.ts, generateDraft.test.ts).
- `fakeFcmSenderPort.ts` - Test fake/fixture used by jest suites (sendDailyNotifications.test.ts).
- `fakeNotificationUserRepository.ts` - Test fake/fixture used by jest suites (sendDailyNotifications.test.ts).
- `fakePlayBillingPort.ts` - Test fake/fixture used by jest suites (firestoreContentRepository.integration.test.ts, firestoreUserRepository.test.ts, handleRtdn.test.ts +3).
- `fakeSubscriptionRecordRepository.ts` - Test fake/fixture used by jest suites (handleRtdn.test.ts, recheckLapsedSubscriptions.test.ts, verifyPurchase.test.ts).
- `fakeSystemConfigRepository.ts` - REMOVED by #150 (unused test fake).
- `fakeUserProfileRepository.ts` - Test fake/fixture used by jest suites (acceptTerms.test.ts, createUserProfile.test.ts, sendDailyNotifications.test.ts).
- `fakeUserRepository.ts` - Test fake/fixture used by jest suites (handleRtdn.test.ts, recheckLapsedSubscriptions.test.ts, verifyPurchase.test.ts).
- `lessonFixtures.ts` - Test fake/fixture used by jest suites (lessonDuplicates.test.ts, lessonValidation.test.ts).

**functions/tests/**
- `firebaseSecurity.test.mjs` - Emulator security test (npm run test:firebase).

**gradle/**
- `libs.versions.toml` - Version catalog for all Android/Gradle dependencies.

**gradle/wrapper/**
- `gradle-wrapper.jar` - Gradle wrapper bootstrap jar (required by gradlew).
- `gradle-wrapper.properties` - Pinned Gradle distribution URL + SHA-256.

**hosting/**
- `404.html` - Hosting 404 page.
- `content-safety.html` - Hosted content-safety policy page.
- `delete-account.html` - Hosted account-deletion instructions (Play policy URL).
- `index.html` - Hosting landing page.
- `privacy.html` - Hosted privacy policy (PolicyUrls).
- `styles.css` - Shared stylesheet for hosting pages.
- `terms.html` - Hosted terms of service (PolicyUrls).

**scripts/**
- `check-16kb-alignment.sh` - Checks 16KB ELF alignment of native libs in the AAB (CI/release).
- `check-toolchain.py` - Asserts toolchain/identity consistency across config files (CI).
- `e2e-emulator.mjs` - End-to-end callable test against Firebase emulators (CI).
- `release-preflight.sh` - Release invariants (run by validate-repo.sh / release).
- `test-check-16kb-alignment.py` - Tests for check-16kb-alignment.sh (CI).
- `test-check-toolchain.py` - Tests for check-toolchain.py (CI).
- `test-validate-repo.py` - Tests for validate-repo.sh (CI).
- `validate-repo.sh` - Repo structure/preflight/build-matrix checker (CI).
