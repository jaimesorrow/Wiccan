# Firestore lesson migration runbook

Target project: `wiccan-android`. This workflow updates only root `content/{id}` draft documents selected in a reviewed change set. It never writes user history, usage markers, authentication, subscriptions, or settings.

## Access

Connect the official Firebase plugin to the Google account that owns the project, or attach a Google Cloud identity to the managed workspace. The operator needs enough access to inspect Firestore, create the approved backup artifact, and update existing draft content. Use the narrowest organization-approved roles; do not add service-account JSON to the repository.

Project console: [Firebase project overview](https://console.firebase.google.com/project/wiccan-android/overview)

IAM: [Google Cloud IAM for `wiccan-android`](https://console.cloud.google.com/iam-admin/iam?project=wiccan-android)

Firestore data: [Firestore console](https://console.firebase.google.com/project/wiccan-android/firestore/databases/-default-/data)

## Required order

1. Verify the authenticated project ID is exactly `wiccan-android`.
2. Run the read phase and retain the generated audit outside the repository.
3. Export a secure backup and record its `gs://` URI, SHA-256, completion time, restore-test time, and every covered content ID in the backup manifest.
4. Review the actual record count, statuses, update times, delivery-history matches, duplicates, source blockers, and proposed category mapping.
5. Build a change set whose entries preserve the existing document ID and expected Firestore update time.
6. Run the migration without `--commit`; retain the comparison report.
7. Resolve every conflict and validation failure. Unsupported records remain unchanged drafts.
8. Approve the SHA-256 digest of the exact final change set.
9. Run with `--commit`, the exact approval digest, and the reviewed backup manifest.
10. Re-run the audit and compare IDs, statuses, histories, and field hashes. Publication is a separate operation.

```bash
cd functions
npm run audit-content -- --project wiccan-android --output /secure/path/before-audit.json
npm run seed-content -- --project wiccan-android \
  --changeset /secure/path/reviewed-changes.json \
  --output /secure/path/dry-run-report.json
npm run seed-content -- --project wiccan-android \
  --changeset /secure/path/reviewed-changes.json \
  --backup /secure/path/backup-manifest.json \
  --approval <exact-change-set-sha256> \
  --commit \
  --output /secure/path/apply-report.json
npm run audit-content -- --project wiccan-android --output /secure/path/after-audit.json
npm run compare-content-audits -- \
  --before /secure/path/before-audit.json \
  --after /secure/path/after-audit.json \
  --changeset /secure/path/reviewed-changes.json \
  --output /secure/path/before-after-report.json
```

The apply path rejects missing IDs, duplicate IDs, non-draft or previously delivered documents, stale update times, protected-field changes, unregistered passages, invalid categories, incomplete reviews, and safety violations. Transaction retries preserve conflict detection. The comparison command fails on added or removed IDs, status changes, delivery-history changes, or content changes outside the reviewed set.

## Rollback

Stop the migration if any post-write audit count, ID, status, or history invariant changes unexpectedly. Keep the application kill switch in its current state and do not publish the revised lessons.

Use the secure pre-migration backup and the committed change report to restore only the affected root content document IDs. Before restoration, capture their current update times and verify that no later editor has changed them. Restore the exact pre-migration fields and preserved status; do not recreate missing IDs and do not touch similarly named nested `userContentUsage/{uid}/content` collections. Run the audit again and compare hashes to the before report.

A broad collection-group import is not an acceptable shortcut because the project also uses nested collections named `content`. A rollback must be scoped to the reviewed root document IDs.

## Production state

No live audit, backup, dry-run, or content write has completed from this workspace yet because no Firebase or Google Cloud identity is connected. Repository work and staged examples are preparation, not evidence about current production contents.
