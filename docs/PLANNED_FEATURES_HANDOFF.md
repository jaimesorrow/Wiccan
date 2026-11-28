# Planned experience features — implementation handoff

Updated: 2026-11-27

This is the continuation checklist for the signed-in Wiccan experience requested
in requirements 1 through 7.3. It records what is on `main`, what is being added
on `feat/planned-experience-pages`, and what the next agent must do. Do not
duplicate an existing screen. Preserve the current server-verified subscription
contract and fail closed when entitlement cannot be verified.

Status key: `[x]` complete, `[-]` partial/in progress, `[ ]` not started.

## Mandatory PR handoff rule

Every pull request opened for this project must contain a detailed `Handoff`
section. It must mark finished work as `[x] Finished`, partial work as
`[-] In progress`, and unstarted work as `[ ] Not started`. It must identify
changed files, tests run, tests not run, backend/Firebase/Google Cloud/Google
Play actions, deployment order, rollback concerns, blockers, and the exact next
safe action. A PR is not ready for review if another agent cannot continue from
its description without repeating the audit.

## Required order

Work in this order because later screens reuse earlier data and entitlement
work: Home/tides, daily illustration data, Grimoire, Spell Crafting, Flow Mode,
Cabinet, Coven altar/presence, then the moderated forum.

## 1. Sign-up username

- [x] Show username only during account creation.
- [x] Validate a 3–24 character community username.
- [x] Reserve it case-insensitively with the `reserveUsername` callable.
- [x] Require it before profile completion, including older accounts.
- [x] Release the owned reservation during account deletion.
- [x] Cover repository and onboarding behavior with tests.

Primary code: `ui/auth/SignInScreen.kt`, `ui/auth/OnboardingScreen.kt`,
`data/repository/AuthRepositoryImpl.kt`, `functions/src/firestoreUserProfileRepository.ts`.

## 2. Home and Cosmic Timing

- [x] Center the Wiccan logo/wordmark.
- [x] Welcome card, lunar calendar row, moon phase/sign/illumination and timing
  guidance.
- [x] Monthly sabbat calendar and eight sabbat calculations.
- [x] Cosmic Timing card with planetary hour, moon transit, illumination, VoC
  and alignment guidance.
- [x] Keep approximate coordinates on-device.
- [x] Finished on branch: rename the heading to `Year of Sabbats`, localize dates and
  explain ordinary days between sabbats.
- [x] Finished on branch: real NOAA CO-OPS tide predictions, nearest-station selection,
  offline cache, states and attribution. New code is under `data/tides`,
  `data/repository/TideRepositoryImpl.kt`, `domain/model/CoastalTide.kt`,
  `domain/repository/TideRepository.kt` and `ui/today/HomeTideViewModel.kt`.
- [ ] Run Android unit tests, ktlint and a device test. The current environment
  could not download Gradle 9.9.2. Live NOAA responses were manually confirmed
  on 2026-11-27.
- [ ] Device-test inland, Alaska, denied-location, no-network, stale-cache and
  timezone-boundary behavior.

## 3. Daily lesson

- [x] Parchment reading card, hero/fade, candle, quill/ink pot, botanical
  dividers, quill-style title and readable sepia body.
- [x] `illustrationUrl` model, Firestore mapping, HTTPS loader, Room 6→7
  migration and category fallback.
- [x] Today bottom navigation and fail-closed subscription/trial lock.
- [x] Time-limited closed-test grant CLI and server fields.
- [ ] Each publishable item needs its own unique `illustrationUrl` with
  `illustrationOriginal: true` (see PR #234). Bundled `illustrationKey` is not a
  publication path.
- [ ] Supply and rights-review a unique image for every publishable lesson.
  Do not insert invented or unlicensed URLs. Existing seed entries generally
  fall back to category art.
- [ ] Run the grant CLI with real tester emails and Application Default
  Credentials. The tool grants existing accounts; it does not create shared
  credentials.
- [ ] Device-test remote-image failure, offline image behavior, trial purchase,
  restore, expiry and tester-grant expiry.

## 4. Personal Grimoire

- [x] Finished on branch: concise, accurate etymology: English `grimoire` came through French
  `grimoire`, altered from Old French `gramaire` (grammar/learning), ultimately
  from Latin `grammatica` and Greek. Do not claim only a vague shared root.
- [x] Finished on branch: search state/events and normalized filtering across title, category,
  type and tradition, with clear and no-results behavior.
- [x] Timeline/Categories tabs, all current shelves, populated-first sorting,
  legacy categories and collapse/expand behavior.
- [ ] Add dedicated per-user downloaded-content Room storage. The current
  `CachedDailyContent` table caches only one Today entry and must not be reused.
- [ ] Add download/remove/progress/error UI, full payload versioning, image disk
  caching, storage cleanup on sign-out/delete and a Room migration.
- [ ] Decide and document post-expiry behavior for already downloaded entries.
  Current implementation interprets “free” as no separate add-on fee while a
  trial/subscription is active; it must not silently bypass the mandatory
  subscription contract.
- [ ] Add ViewModel, DAO/migration and offline/reinstall/account-switch tests.

Primary code: `ui/archive/*`, `domain/repository/ArchiveRepository.kt`,
`data/repository/ArchiveRepositoryImpl.kt`, `data/local/*`.

## 5. Spell Crafting and Flow Mode

- [ ] Add a Spell Crafting destination and history/instruction screen.
- [ ] Add the requested wide spell-map artwork using owned/licensed assets.
- [ ] Add an editable, length-adaptive Spell Card and local persistence for
  personal spells.
- [ ] Add Save to Personal Grimoire and define optional cloud sync/rules.
- [ ] Add Flow Mode destination with ordered ritual steps, Previous/Advance,
  lifecycle-safe timer and restoration after process recreation.
- [ ] Add microphone permission UX, voice/chant detection, waveform and
  transcription. Update Privacy, Data Safety and Play disclosures before
  release.
- [ ] Add optional botanical/crystal/moon-phase correspondence suggestions.
  Frame them as traditional correspondences; never promise amplification or a
  result.
- [ ] Keep base spell editing/saving free within the permitted app shell. Gate
  Flow Mode and correspondence suggestions with server-verified premium access,
  failing closed and opening the existing paywall.
- [ ] Add Room models/DAO/migrations, repositories, ViewModels and unit/UI tests.

Suggested packages: `ui/spellcraft`, `ui/flow`, `domain/model/PersonalSpell`,
and a dedicated personal-spell repository.

## 6. My Cabinet

- [ ] Add a parchment `My Cabinet` destination; it does not exist today.
- [ ] Herbs & Botanicals selector, reviewed illustration, significance,
  documented historical/modern applications and inventory selection.
- [ ] Crystals & Stones selector, reviewed illustration, significance,
  documented correspondences/applications and inventory selection.
- [ ] Tools & Vessels inventory with accessible checkbox controls.
- [ ] Use a curated reviewed catalog; lesson categories and category fallback
  art are not a Cabinet database.
- [ ] Persist inventory offline per user and clean it on account deletion.
- [ ] Keep claims educational and avoid medical or guaranteed metaphysical
  effects.
- [ ] Resolve the free-access shell conflict. The current app has a mandatory
  subscription gate around the signed-in experience; Cabinet cannot become
  accessible to non-subscribers through a UI-only change.
- [ ] Add selector, persistence, offline, accessibility and claim-safety tests.

Suggested packages: `ui/cabinet`, `domain/model/CabinetItem`, and a dedicated
catalog/inventory repository.

## 7. Coven and Community Altar

- [ ] Add a premium Coven destination and fail-closed server-enforced access.
- [ ] Add Community Altar visuals and a user-controlled ambient audio player
  with play/mute/volume, lifecycle release and no forced background playback.
- [ ] Add a server-owned active Global Spell Focus and idempotent `Ignite Your
  Spark & Set Intention` action.
- [ ] Derive participation counts and activity from real server data; never
  invent counts or activity.
- [ ] Add privacy-safe ephemeral presence with heartbeat/TTL or RTDB
  `onDisconnect`. Never expose uid or exact coordinates.
- [ ] Render aggregate/randomized coarse-region golden dots on the globe. Exact
  city activity requires explicit user consent.
- [ ] Add offline/loading/error/retry/accessibility behavior and abuse/rate-limit
  tests.

## 7.3 Community forum

- [x] Unique concealed community username foundation.
- [ ] Add posts, replies, pagination, server timestamps and soft deletion.
- [ ] Enforce premium read/write access on the server, not only in navigation.
- [ ] Add report, block and mute before enabling user-generated content.
- [ ] Add moderation/admin queue, prohibited-content policy, spam/rate limits,
  audit trail, appeal/contact path and account-deletion handling.
- [ ] Update Terms, Privacy, Data Safety and retention disclosures, with the
  required coordinated policy-version rollout.
- [ ] Add Firestore indexes/rules or callable-only access and emulator tests.

Suggested packages: `ui/coven`, `data/community`, `domain/community`, and
`functions/src/community`.

## Navigation and release integration

- [ ] Add destinations only when the screen is functional; do not add dead tabs.
- [ ] Keep Home, Today, Grimoire and Settings behavior stable while adding new
  destinations.
- [ ] Update English, Spanish and Brazilian Portuguese resources together.
- [ ] Update Play listing claims only after each feature is actually shipped.
- [ ] Run `./scripts/validate-repo.sh`, Android unit/UI tests, Functions build,
  lint/tests, Firestore emulator tests and a real-device navigation/accessibility
  pass before requesting merge.
- [ ] Do not deploy Functions/rules/policy versions ahead of the matching Android
  build.

## Backend and Firebase implementation instructions

Use callable Functions for community mutations so premium checks, accepted
policy versions, moderation, rate limits, server timestamps and aggregate
updates cannot be bypassed by a modified client. Require Firebase Auth and App
Check. Keep client access default-deny until rules and emulator tests ship.

### Proposed data model

- `communityFocus/{focusId}`: operator-authored `title`, `summary`, `startsAt`,
  `endsAt`, `status`, `imageRef`, `participantCount`, `createdAt`, `updatedAt`.
- `communityFocus/{focusId}/participants/{uid}`: one idempotent participation
  document per user/focus with server timestamp, optional reviewed intention
  category, privacy-safe region cell and activity opt-in. Never exact location.
- `communityActivity/{eventId}`: server-generated sanitized activity with
  `focusId`, username snapshot, optional coarse region label, action type,
  `createdAt`, `expiresAt`. No free text in the first release. Configure TTL on
  `expiresAt`, but also exclude expired records in queries because TTL deletion
  is asynchronous.
- `forumPosts/{postId}` and `forumPosts/{postId}/replies/{replyId}`: server-owned
  author uid/username snapshot/timestamps plus body, status, moderation state,
  report count and soft-delete fields. Never accept author/count/timestamp values
  from the client.
- `users/{uid}/blockedUsers/{blockedUid}`: private block records.
- `contentReports/{reportId}`: private reporter, target type/id, reason enum,
  limited note, timestamps and review status. Deny client reads.
- `moderationActions/{actionId}`: append-only trusted audit log; deny all client
  access.
- `users/{uid}/personalSpells/{spellId}` only if cloud sync is approved;
  otherwise keep spells in per-user Room storage.
- `cabinetCatalog/{itemId}` only if the catalog must update without an app
  release. Publish only reviewed records with citations, rights and schema
  version. User inventory belongs under `users/{uid}/cabinet/{itemId}` or Room.

### Callable Functions

- `joinCommunityFocus`: require auth, App Check, current policies and verified
  non-expired premium/trial/closed-test access. Transactionally create only when
  absent and increment once. Derive username/region server-side.
- `createForumPost` and `createForumReply`: validate and normalize Unicode,
  trim, enforce length, reject links initially, rate-limit and use server
  timestamps.
- `deleteOwnForumPost`: soft-delete while preserving moderation/audit records.
- `reportCommunityContent`: validate target/reason and prevent duplicate spam.
- `blockCommunityUser`/`unblockCommunityUser`: reject self-block and keep the
  list private.
- Scheduled cleanup: remove stale activity/presence and reconcile aggregates.
- Moderation trigger: quarantine at a defined threshold for human review; never
  permanently delete solely from untrusted report counts.

Each callable needs tests for success, unauthenticated, missing App Check,
expired tier, stale policy, malformed input, rate limiting, idempotency and
cross-account access. Put code under `functions/src/community/` and export it
from `functions/src/index.ts`.

### Rules, indexes and retention

- Default deny every new collection.
- Never trust a client premium boolean. Use server-controlled entitlement and
  expiry, or callable-only mutations.
- Rules are not filters; queries must include the same visibility/status
  constraints the rules require.
- Deny direct writes to counts, activity, reports, moderation, usernames,
  author uid and timestamps.
- Permit private user data only where `request.auth.uid` matches the path and
  allowed-field/schema checks pass.
- Keep rule `get()`/`exists()` calls within access limits and cover every allowed
  and denied path with emulator tests.
- Add likely composite indexes to `firestore.indexes.json`: forum status +
  moderation state + created time; reply status + created time; activity focus +
  expiry + created time; report status + created time. Finalize against actual
  queries.
- Configure TTL for ephemeral activity/presence fields and document the field.

### Presence and media

Preferred presence uses Firebase Realtime Database because Firestore has no
native connection-presence primitive. Android writes
`presence/{focusId}/{uid}/{connectionId}`, registers `onDisconnect().removeValue()`
before setting online state, and re-registers after every reconnect. RTDB rules
allow a user to touch only their uid path. A trusted Function aggregates coarse
region cells; clients read aggregates, never uid records. Add stale-heartbeat
cleanup. Never store exact coordinates.

Put only owned/licensed lesson/Cabinet art in Cloud Storage and keep path,
checksum, alt text, rights/source and review status. Do not ship forum image
uploads initially. Bundle reviewed altar audio when practical; otherwise use an
immutable versioned Storage object with checksum and local fallback. Audio must
have visible play/mute/volume and stop/release with lifecycle changes.

## Firebase Console checklist

Target production project: `wiccan-android`. Confirm `.firebaserc` and the active
CLI project before every deploy. Use separate dev/staging Firebase projects.

- [ ] Register each Android flavor against its own Firebase project and keep
  each `google-services.json` in the documented flavor directory.
- [ ] Enable required Auth providers and verify authorized/action-handler
  domains. Test username reservation/release in emulators and staging.
- [ ] Create Firestore and RTDB only after deliberately choosing their regions.
- [ ] Add rules and emulator tests with every collection; add indexes to source
  control and wait until indexes report Ready before dependent Functions.
- [ ] Configure TTL on the correct collection group and `expiresAt` field.
- [ ] Confirm Blaze billing before Functions/RTDB load and add budget/quota
  alerts.
- [ ] Store secrets in Secret Manager through supported commands; never commit or
  paste credentials.
- [ ] Deploy new Functions to staging first and inspect auth/App Check errors,
  retries, duplicate writes, latency and rate limits.
- [ ] Configure App Check Play Integrity using Play App Signing SHA fingerprints.
  Monitor valid traffic before enforcing Firestore, RTDB or callable Functions.
- [ ] Test RTDB `onDisconnect` under crash, airplane mode, process death, account
  switch and entitlement expiry.

Run production commands individually and verify the printed project:

```bash
firebase use production
```

```bash
firebase deploy --only firestore:indexes --project wiccan-android
```

```bash
firebase deploy --only functions:joinCommunityFocus,functions:createForumPost,functions:createForumReply,functions:reportCommunityContent --project wiccan-android
```

```bash
firebase deploy --only firestore:rules,database --project wiccan-android
```

Do not run production deploys until staging, policy compatibility and the
matching Android build have passed.

## Google Cloud Console checklist

- [ ] Confirm the billing account, budgets and quota alerts for the Google Cloud
  project behind `wiccan-android`.
- [ ] Audit IAM. Remove stale principals/keys, do not grant Owner to agents, and
  use narrowly scoped deploy/runtime roles.
- [ ] Prefer Workload Identity Federation or short-lived CI credentials instead
  of downloaded service-account JSON keys.
- [ ] Review Secret Manager access so each runtime identity can read only the
  secrets it uses.
- [ ] Review Cloud Functions/Cloud Run service accounts, region, ingress,
  min/max instances, concurrency, memory and timeout after load testing.
- [ ] Review Cloud Scheduler jobs for target, region, timezone and retry policy.
- [ ] Add alerts for permission failures, callable error rate, moderation job
  failures and unusual database writes/cost.
- [ ] Configure Firestore TTL in the Cloud console and record the collection
  group/field in this file and the deployment PR.
- [ ] Ensure exact location, user text, audio and tokens are not written to logs,
  Analytics, Crashlytics or BigQuery. Redact request bodies from errors.
- [ ] Document retention before enabling Analytics/Crashlytics exports for new
  community, location or microphone events.

## Google Play Console and Android developer checklist

### Build and signing

- [ ] Increment `versionCode`; never reuse one uploaded to any track.
- [ ] Build a signed AAB through the existing release workflow, never with a
  debug key.
- [ ] Register Play App Signing SHA-1/SHA-256 fingerprints in Firebase for
  Google Sign-In and App Check.
- [ ] Run repository validation, release lint, bundle build, 16 KB alignment and
  real-device smoke/accessibility tests.
- [ ] Do not claim unshipped features in the listing or screenshots.

### Permissions, privacy and Data safety

- [ ] Keep coarse location optional and foreground-only. Before permission,
  explain that it selects planetary hours and the nearest NOAA station on-device.
  The tide code sends NOAA only a public station id/date, not raw coordinates.
- [ ] When voice support is added, declare `RECORD_AUDIO`, show a prominent
  disclosure before the runtime request, and say whether audio is on-device,
  transmitted, stored or immediately discarded.
- [ ] Update Data safety for username, posts/replies, intentions, reports,
  blocks, moderation data, approximate region, audio/transcripts, personal
  spells and all third-party SDK collection/sharing.
- [ ] Keep Data safety and Privacy Policy wording consistent.
- [ ] Update account-deletion disclosures and behavior for forum/personal data.
- [ ] Re-run the content-rating questionnaire and disclose UGC/chat.

### UGC requirements before forum release

- [ ] Publish clear Terms and Community Guidelines and require acceptance before
  posting.
- [ ] Provide in-app reporting for every contribution.
- [ ] Provide in-app blocking.
- [ ] Operate ongoing moderation with response and appeal handling.
- [ ] Apply an appropriate age screen and accurate content rating.
- [ ] Do not release an unmoderated forum in closed testing; UGC rules still
  apply to test releases.

### Subscription and closed-test validation

- [ ] Keep entitlement decisions consistent across client, Functions, rules,
  paywall, Terms and listing. Flow, suggestions and Coven require verified
  premium/trial status on client and server.
- [ ] Add QA accounts as Play license testers. Closed-track testers are not
  automatically license testers and non-license testers can incur real charges.
- [ ] Publish to Internal testing first and install through Play with the opted-in
  account.
- [ ] Use Play Billing Lab/license testing for trial, renewal, grace, hold,
  cancellation, restore, refund and expiry. Verify RTDN, acknowledgement and
  server entitlement after every state.
- [ ] Test at least one non-license account before production so the app does not
  accidentally depend on accelerated test behavior.
- [ ] Keep time-limited server closed-test grants separate from purchases and
  never generate fake purchase tokens.

### Rollout and rollback

1. Pass Emulator Suite and staging Firebase.
2. Pass Internal testing with license testers and App Check monitoring.
3. Pass Closed testing with real opt-in, policy forms and live moderation.
4. Review crash, auth, billing, abuse and cost metrics.
5. Start the smallest practical staged production rollout.
6. Expand only while billing, backend and moderation metrics remain healthy.

Before upload, keep the previous build available, make new destinations remotely
disableable where practical, and keep Functions/rules backward-compatible until
all active Play tracks have compatible clients.

## Policy and deployment order

Community, UGC, microphone/transcription, cloud-synced spells and new retention
language require coordinated legal review. Batch policy edits so users do not
re-accept repeatedly.

1. Finalize behavior and the data map.
2. Update native Terms/Privacy/Community Guidelines and hosted pages together.
3. Update Play Data safety, content rating, app access and deletion answers.
4. Bump app, Functions and rules policy constants in one PR; run parity tests.
5. Upload the compatible Android build to Internal testing before backend
   enforcement.
6. Deploy indexes, then Functions, then rules/RTDB/hosting in the documented
   compatible order.
7. Verify old-track clients remain usable or remove them from active tracks
   before enforcement.

Never enforce a new policy version or App Check/rules requirement before the
Android build that can satisfy it is available on every active track.

## Current branch verification

- [x] `git diff --check` after the Home/tide implementation.
- [x] English, Spanish and Brazilian Portuguese XML parsed after the Home/tide
  implementation.
- [x] NOAA station-catalog and prediction endpoints returned valid live data in
  a manual read-only check on 2026-11-27.
- [ ] Full Gradle validation remains required on a runner with Gradle 9.9.2.

