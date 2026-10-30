# Firestore migration execution report — 2026-10-30

Target project: `wiccan-android`

## Production execution

Execution began directly with the established `backup-content` workflow, without a preliminary production inventory. The managed runtime exposed no Google Cloud identity, Application Default Credentials, Firebase login, secret binding, or outbound GCP identity. Backup authentication failed with `NO_ADC_FOUND` before any Firestore document was read; the secure temporary backup directory remained empty.

Because the required backup artifact, immutable upload manifest, and emulator restore proof could not be created, no production migration, import, configuration write, or publication was attempted. A tested Firebase deployment command was attempted separately and failed at Firebase authentication before deployment. No production state was changed.

## Repository implementation

- Reconciled the batch publication merged in PR #98 with update-time and authored-content hash preconditions.
- Publication now updates `status`, `contentStatus`, `publishedAt`, and `updatedAt` transactionally and creates a server-only immutable `contentPublicationAudit` row.
- Automatic eligible publication revalidates each draft and derives an exact operation digest; blocked drafts remain unchanged.
- Canonical practical fields, timestamps, categories, source passage evidence, rights, disclosures, duplicate review, safety review, and hash-bound human editorial approval remain mandatory.
- Reviewed seed imports create both compatibility status fields and server timestamps while retaining document IDs.
- Read-only tooling can report all delivery-related root and nested collection counts once credentials exist, without exposing user or payment document contents.

## Content disposition

- Canonical seed: 296 unique drafts—200 lessons, 59 spells, and 37 potions.
- Rewrite curriculum: 296 records and 296 distinct IDs; packet structure and lexical duplicate checks passed.
- Registered verified source passages: 0.
- Locally publication-eligible records: 0.
- Source-blocked records: 296.
- Safety/editorial-review-required records: 296.
- Legacy rights flags: 106.
- Potion/disclosure-requiring records: 37.
- Production imported, migrated, or published: 0 because backup and authentication prerequisites were unavailable.

The historical 146-record gap was not assumed to remain current. Exact production existence checks can occur only as part of the authenticated, backup-protected import transaction.

## Validation results

- TypeScript build: passed.
- ESLint: passed.
- Functions Jest: 36 suites passed, 1 emulator-only suite skipped; 315 tests passed and 21 skipped.
- Firestore rules: 21/21 passed.
- Firestore repository integration: 21/21 passed, including atomic publication, audit creation, idempotent retry, and stale-version rejection.
- Rewrite curriculum: 296/296 valid structural packets, no lexical duplicate concerns in that curriculum check.
- Android Gradle: blocked before dependency resolution because the runtime has no Android SDK; this is not a successful Android build.
- Firebase deployment: failed authentication before any deployment.

## Remaining external blockers

1. Attach an authorized short-lived Google identity for `wiccan-android`.
2. Provide an existing private GCS bucket with public-access prevention for immutable backup upload.
3. Supply human-verified source passages and exact hash-bound editorial, rights, safety, and duplicate approvals.
4. Provide an Android SDK for compilation.
5. Re-run backup, upload, restore drill, reviewed migration/import, automatic eligible publication, deployment, and post-operation subscriber delivery verification.
