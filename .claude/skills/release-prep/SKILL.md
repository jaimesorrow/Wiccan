---
name: release-prep
description: Checklist for preparing or validating a Wiccan release — the full automated validation matrix across both toolchains, what's already verified vs. what genuinely needs a real device/Firebase project, and keeping docs/RELEASE_CHECKLIST.md and docs/STATUS.md honest. Use when asked to prepare, cut, validate, or sign off on a release — not for routine feature PRs.
---

# Wiccan release preflight

Full detail lives in `docs/RELEASE_CHECKLIST.md` and `docs/STATUS.md`;
this skill is the sequence to actually run plus the discipline for
keeping those docs true. Also see `docs/PLAY_CONSOLE_READINESS.md` for
store-listing-side items.

## 1. Run the automated matrix (verifiable in any sandbox)

```
./gradlew clean ktlintCheck :app:testProdDebugUnitTest :app:assembleProdDebug \
  :app:compileProdDebugAndroidTestKotlin :app:bundleProdRelease :app:lintProdRelease
./scripts/check-16kb-alignment.sh

cd functions && npm run build && npm test && npm run lint
```

All of these must be green before anything else matters. Don't skip
`lintProdRelease` — it isn't in every historical validation invocation
in this repo's git history, but it's part of the real bar now and has
caught a genuine bug before (a missing `WorkManagerInitializer` manifest
removal).

## 2. Confirm signing state honestly

`isMinifyEnabled = false` is a **deliberate, still-current** decision —
flipping on R8 needs a full manual smoke test on a real device (sign-in,
Today content load, Archive/Library, Paywall, widget, notifications,
sign-out, delete account) that no sandbox here can run. Don't flip it on
speculatively. Release signing needs a real keystore
(`app/keystore.properties`, gitignored) — its absence means
`bundleProdRelease` builds unsigned, which is expected, not a bug to
"fix" by fabricating credentials.

## 3. Bump versioning

`versionCode`/`versionName` in `app/build.gradle.kts` — `versionCode`
must strictly increase or Play rejects the upload.

## 4. Never claim what can't be verified here

The following need a real device/emulator or a real Firebase project and
must be reported as "not verified in this environment," never as
passing, unless you actually have one:

- `WiccanDatabaseMigrationTest` executed via `connectedAndroidTest`
  (compiles clean; never run against real SQLite here).
- Deep links (`wiccan://today`, `wiccan://premium`) via `adb`.
- Auto Backup exclusion behavior (backup → restore → confirm no stale
  session/content).
- A real Play Billing sandbox purchase exercising `verifyPurchase`'s
  acknowledgement/expiry field shapes.
- `handlePlayRtdn` end-to-end (Play Console → Pub/Sub → function).
- `firestore.rules`/`firestore.indexes.json` deployed and exercised
  against a live project; the `users/{uid}/devices` collection-group
  query and `titleExists` projection query's index-free-ness.
- Anything gated on `.firebaserc`'s placeholder project IDs or a real
  `functions/.env.<project-id>`.

## 5. Keep the docs in sync

If this pass fixed something `docs/STATUS.md` or
`docs/RELEASE_CHECKLIST.md` listed as open/blocked, update that doc in
the same PR — don't let the "what's actually done" record drift from
what the code does. If it surfaced a new gap, add it rather than letting
it go untracked.
