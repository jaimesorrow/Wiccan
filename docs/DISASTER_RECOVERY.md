# Disaster recovery

What to do when something load-bearing is gone or corrupted. Grounded in
this repo's actual architecture — three Firestore collections plus one
subcollection, Cloud Functions, and one external dependency (Google Play
Billing) — not a generic template.

## What exists to lose

| Data | Where | Source of truth? |
|---|---|---|
| User profiles (`tier`, `acceptedTermsVersion`, etc.) | `users/{uid}` | Yes — Firestore is authoritative |
| Registered devices (FCM token, time zone) | `users/{uid}/devices/{deviceId}` | Yes, but re-derivable: the app re-registers on next launch |
| Delivery history | `userContentHistory` | Yes — this is what makes daily selection non-repeating |
| Content pool (drafts + published) | `content` | Yes for drafts (generation costs real Claude API spend); published items are also recoverable by regenerating + re-approving, but that changes their content |
| Subscription audit records | `subscriptions/{uid}` | No — Google Play is authoritative. This is a cache/audit trail, rebuildable via `recheckLapsedSubscriptionsJob` or a manual `purchases.subscriptionsv2.get` call per user |
| Firebase Auth accounts | Firebase Auth | Yes — no code-level backup; relies on Firebase's own durability |

**The one truly irreplaceable asset is `userContentHistory`.** If it's
lost, `selectTodayContent`'s repeat-avoidance logic has nothing to avoid
repeating — users would start seeing already-seen content as if new. There
is no code-level backup of it today (see "Gaps" below).

## Scenario: Firestore data loss or corruption (a collection, or the whole project)

1. **Stop the bleeding.** Disable `sendDailyNotificationsJob` and
   `recheckLapsedSubscriptionsJob` in Firebase Console (or
   `firebase functions:delete` them temporarily) so nothing writes further
   state on top of a corrupted read.
2. **Restore from a Firestore backup**, if one exists (Firestore's
   [scheduled backups](https://firebase.google.com/docs/firestore/backups)
   or a manual export via `gcloud firestore export`) — **this repo does
   not currently configure either.** See "Gaps" below; until it does,
   there is nothing to restore from, and this step is unavailable.
3. **If no backup exists:** `users/{uid}` documents can be reconstructed
   from Firebase Auth (uid, email) plus `tier` re-derived by re-running
   `recheckLapsedSubscriptionsJob`'s logic per user against the Play
   Developer API (source of truth for entitlement). `userContentHistory`
   **cannot** be reconstructed once lost — accept that affected users may
   see repeat content until the pool naturally cycles.
4. **Re-enable the scheduled jobs** once the collection(s) are confirmed
   consistent.

## Scenario: A bad Cloud Functions deploy breaks a critical path

1. `firebase functions:log` to identify which function and error.
2. Roll back: `firebase deploy --only functions` from the last known-good
   commit (`git checkout <sha> -- functions && cd functions && npm run
   build && firebase deploy --only functions`), or use the Cloud Console's
   Cloud Functions revision history to roll back a single function without
   a full redeploy.
3. Every callable in `functions/src/index.ts` is independent — a bad
   deploy of `sendDailyNotificationsJob` doesn't require rolling back
   `getTodayContent`. Roll back only the broken function(s) if the
   deploy tooling allows targeting a single one.

## Scenario: A generated draft or published content item is wrong/unsafe

- A **draft** (not yet published) is simply deleted from Firestore
  directly (`content/{id}`) — nothing else references it yet.
- A **published** item already delivered to users cannot be un-delivered
  (their `userContentHistory` row already names it), but can be pulled
  from future delivery by setting its `status` back to `"draft"` in
  Firestore, or deleting it outright if it should never be shown again.
  `approveDraft`'s safety gate (see `functions/src/approveDraft.ts`)
  prevents this specific class of problem for *new* content going
  forward, but doesn't retroactively fix something already published
  before the gate existed.

## Scenario: The signing keystore is lost

**There is no recovery.** If Play App Signing was used at first upload
(Google holds the app signing key), you can still publish updates using a
new upload key after proving ownership to Google — see [Play's upload key
reset process](https://support.google.com/googleplay/android-developer/answer/9842756).
If a self-managed key was used instead, a lost keystore means the app can
never be updated again under its existing Play Store listing; a new
listing (with a new applicationId) would be the only option. This is
exactly why `docs/RELEASE_CHECKLIST.md` recommends Play App Signing.

## Scenario: The Anthropic API key or Play service account credentials leak

1. Revoke the leaked credential immediately (Anthropic Console for the API
   key; Google Cloud IAM for the service account key).
2. Issue a new one, update `functions/.env.<project-id>` (never commit
   it), redeploy functions.
3. Because each environment has its own credentials (see the README's
   Environments section and `functions/.env.example`), a leak in one
   environment never requires rotating another's.

## Gaps (not addressed by this repo today)

- **No configured Firestore backup/export schedule.** This is the single
  biggest disaster-recovery gap — set up [scheduled Firestore
  backups](https://firebase.google.com/docs/firestore/backups) on the
  real project once it exists; this repo can't configure a backup
  schedule on a project that doesn't exist yet.
- **No automated restore-drill.** Even once backups exist, this repo has
  no tooling to test a restore; that's a manual Firebase Console/`gcloud`
  operation until someone builds one.
