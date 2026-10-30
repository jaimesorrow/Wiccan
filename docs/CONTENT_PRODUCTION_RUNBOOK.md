# Content production runbook

This workflow targets only Firebase project `wiccan-android`. It is fail-closed: a read-only audit may run without a change approval, but backup upload, migration, publication, and deployment are separate operator-approved operations.

## Current checked-in baseline

- Canonical seed: 296 unique draft IDs (200 lessons, 59 spells, 37 potions).
- Practical review-only set: 150 drafts.
- `sourcePassages.json`: empty. No practical lesson can pass source verification or hash-bound editorial review until a human editor supplies verified evidence.
- Production Android application ID: `com.jaimesorrow.wiccan`.

These are repository facts, not a live Firestore inventory. Never reuse historical live counts in a production report.

## Read-only audit

Use an authorized short-lived Google identity with Firestore read and Storage metadata permissions. Do not use an owner credential.

```bash
cd functions
npm ci
npm run build
npm run audit-content -- --project wiccan-android --output /secure/live-content-audit.json
```

The report includes counts for delivery-related top-level collections, content status/type distributions, update-time preconditions, previously delivered IDs, configuration presence, duplicate concerns, and an actionable violation list. It never prints document bodies from user, subscription, or payment collections.

## Backup and restore drill

Capture locally with owner-only permissions. Upload only to a pre-reviewed private GCS bucket that enforces public-access prevention, versioning, retention, and encryption.

```bash
cd functions
npm run backup-content -- --project wiccan-android \
  --output /secure/wiccan-content-backup.json.gz \
  --report /secure/wiccan-content-backup-report.json
```

After reviewing the report, use its `artifactSha256` as the exact upload approval:

```bash
npm run upload-content-backup -- --project wiccan-android \
  --artifact /secure/wiccan-content-backup.json.gz \
  --bucket "$WICCAN_BACKUP_BUCKET" \
  --object "content/approved-backup.json.gz" \
  --approval "$WICCAN_BACKUP_SHA256" \
  --manifest /secure/wiccan-content-backup-manifest.json
```

Restore only against the emulator, then retain the resulting tested manifest:

```bash
npx firebase-tools@15.34.1 emulators:exec --project demo-wiccan --config ../firebase.json --only firestore \
  "FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node lib/scripts/restoreTestContentBackupCli.js \
  --artifact /secure/wiccan-content-backup.json.gz \
  --manifest /secure/wiccan-content-backup-manifest.json \
  --output /secure/wiccan-content-backup-tested-manifest.json"
```

## Reviewed migration

Dry-run a reviewer-signed change set against current Firestore versions:

```bash
cd functions
npm run seed-content -- --project wiccan-android \
  --changeset /secure/reviewed-content-changes.json \
  --output /secure/content-migration-dry-run.json
```

Production apply requires the exact change-set digest and the restore-tested backup manifest. It preserves published/delivered records and aborts the atomic set on any conflict:

```bash
npm run seed-content -- --project wiccan-android --commit \
  --changeset /secure/reviewed-content-changes.json \
  --approval "$WICCAN_CHANGESET_SHA256" \
  --backup /secure/wiccan-content-backup-tested-manifest.json \
  --output /secure/content-migration-result.json
```

## Publication

Generate a read-only plan for one or up to 25 IDs. Blocked records are reported individually and omitted from the publishable entries:

```bash
cd functions
npm run approve-draft -- --project wiccan-android \
  --operator "named-editor@example.com" \
  --contentId "approved-document-id" \
  --output /secure/content-publication-plan.json
```

Review the plan, its content hashes and update times. Production publication requires explicit approval equal to the report's `planDigest`:

```bash
npm run approve-draft -- --project wiccan-android --commit \
  --plan /secure/content-publication-plan.json \
  --approval "$WICCAN_PUBLICATION_PLAN_SHA256"
```

The commit revalidates every document, publishes the batch atomically, updates both status fields and `updatedAt`, and creates a server-only `contentPublicationAudit` record. A concurrent edit aborts the entire batch. Retrying an already completed plan is a no-op.

## Tests and deployments

```bash
cd functions && npm run build && npm run lint && npm test -- --runInBand
cd ../firestore-tests && npm ci && npm test
cd .. && ./gradlew :app:testProdDebugUnitTest :app:assembleProdDebug :app:compileProdDebugAndroidTestKotlin ktlintCheck --no-daemon
```

Rules, indexes, and functions are deployed separately and only after their exact diff and project are approved:

```bash
npx firebase-tools@15.34.1 deploy --project wiccan-android --only firestore:rules
npx firebase-tools@15.34.1 deploy --project wiccan-android --only firestore:indexes
npx firebase-tools@15.34.1 deploy --project wiccan-android --only functions
```

Do not combine these deploy commands, and do not run them merely because tests pass.
