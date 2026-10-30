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
3. Capture the root `content` collection into a typed, gzip-compressed artifact with mode `0600`. Upload that exact artifact to an existing private bucket that enforces public-access prevention, then record its `gs://` URI, object generation, SHA-256, completion time, and every covered content ID.
4. Restore-test the exact artifact against the Firestore emulator and create a restore-tested manifest. The migration rejects an untested manifest.
5. Review the actual record count, statuses, update times, delivery-history matches, duplicates, source blockers, and proposed category mapping.
6. Build a change set whose entries preserve the existing document ID and expected Firestore update time.
7. Run the migration without `--commit`; retain the comparison report.
8. Resolve every conflict and validation failure. Unsupported records remain unchanged drafts.
9. Approve the SHA-256 digest of the exact final change set.
10. Run with `--commit`, the exact approval digest, and the restore-tested backup manifest.
11. Re-run the audit and compare IDs, statuses, histories, and field hashes. Publication is a separate operation.

```bash
cd functions
npm run audit-content -- --project wiccan-android --output /secure/path/before-audit.json
npm run backup-content -- \
  --project wiccan-android \
  --output /secure/path/content-backup.json.gz \
  --report /secure/path/content-backup-report.json
npm run upload-content-backup -- \
  --project wiccan-android \
  --artifact /secure/path/content-backup.json.gz \
  --bucket <private-pap-enforced-bucket> \
  --object wiccan/content-backup.json.gz \
  --approval <exact-backup-sha256> \
  --manifest /secure/path/upload-manifest.json
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 npm run restore-test-content-backup -- \
  --artifact /secure/path/content-backup.json.gz \
  --manifest /secure/path/upload-manifest.json \
  --output /secure/path/restore-tested-manifest.json
npm run seed-content -- --project wiccan-android \
  --changeset /secure/path/reviewed-changes.json \
  --output /secure/path/dry-run-report.json
npm run seed-content -- --project wiccan-android \
  --changeset /secure/path/reviewed-changes.json \
  --backup /secure/path/restore-tested-manifest.json \
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

## Controlled publication

Publication is a separate, fail-closed operation after the migration comparison
passes. The command scans drafts in document-ID order and writes a mode-0600
result report. It validates the complete lesson structure, registered passage,
hash-bound editorial/safety/rights/duplicate review, and potion-disclosure flag.
The default is a read-only dry run:

```bash
npm run publish-eligible -- --project wiccan-android \
  --output /secure/path/publication-dry-run.json
npm run publish-eligible -- --project wiccan-android --commit \
  --output /secure/path/publication-apply.json
```

Commit mode re-reads and revalidates each candidate, retries transient failures,
and uses an idempotent transaction to set both `status` and `contentStatus` to
`published` with `publishedAt` and `updatedAt` server timestamps. Blocked drafts
remain unchanged and their reasons are recorded. Do not edit review flags or the
source register merely to increase the published count.

The apply path rejects missing IDs, duplicate IDs, non-draft or previously delivered documents, stale update times, protected-field changes, unregistered passages, invalid categories, incomplete reviews, and safety violations. Transaction retries preserve conflict detection. The comparison command fails on added or removed IDs, status changes, delivery-history changes, or content changes outside the reviewed set.

## Rollback

Stop the migration if any post-write audit count, ID, status, or history invariant changes unexpectedly. Keep the application kill switch in its current state and do not publish the revised lessons.

Use the secure pre-migration backup and the committed change report to restore only the affected root content document IDs. Before restoration, capture their current update times and verify that no later editor has changed them. Restore the exact pre-migration fields and preserved status; do not recreate missing IDs and do not touch similarly named nested `userContentUsage/{uid}/content` collections. Run the audit again and compare hashes to the before report.

Generate the rollback plan from the restore-tested artifact, reviewed change set, and post-migration audit. Review its digest before adding `--commit --approval <digest>`:

```bash
npm run rollback-content -- \
  --project wiccan-android \
  --artifact /secure/path/content-backup.json.gz \
  --manifest /secure/path/restore-tested-manifest.json \
  --changeset /secure/path/reviewed-changes.json \
  --afterAudit /secure/path/after-audit.json \
  --output /secure/path/rollback-dry-run.json

npm run rollback-content -- \
  --project wiccan-android \
  --artifact /secure/path/content-backup.json.gz \
  --manifest /secure/path/restore-tested-manifest.json \
  --changeset /secure/path/reviewed-changes.json \
  --afterAudit /secure/path/after-audit.json \
  --approval <exact-rollback-plan-sha256> \
  --commit \
  --output /secure/path/rollback-apply-report.json
```

A broad collection-group import is not an acceptable shortcut because the project also uses nested collections named `content`. A rollback must be scoped to the reviewed root document IDs.

## Production state

Firebase CLI access was connected on 2026-10-25 and a read-only Firestore audit verified 150 root `content` documents, all drafts. A typed local backup was captured with SHA-256 `e3d2b7d4817f3015fe4a576c8855398ca3a4574b3f56ef9cc5e95eb0a9c13e02`. The project listed no Cloud Storage bucket, so private upload, immutable object generation, and emulator restore validation remain incomplete. No content write, publication, rules deployment, or index deployment has occurred. See [LIVE_FIRESTORE_CONTENT_AUDIT_2026-10-25.md](LIVE_FIRESTORE_CONTENT_AUDIT_2026-10-25.md).
