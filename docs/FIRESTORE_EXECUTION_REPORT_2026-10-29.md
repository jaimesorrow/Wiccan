# Firestore migration execution report — 2026-10-29

Target project: `wiccan-android`

## Production execution

The managed workspace exposed no Google Cloud identity, Application Default
Credentials, Firebase login, private backup bucket, or configured secret. The
Firebase CLI failed authentication before any production Firestore read or
write. Consequently no production backup, migration, import, publication,
configuration write, deployment, or delivery smoke test was performed. This is
the required fail-closed outcome: the repository requires a private
public-access-prevention-enforced backup, immutable generation, successful
emulator restore drill, exact change-set digest, and hash-bound editorial review
before production writes.

## Content results

- Checked-in seed: 296 records (200 lessons, 59 spells, 37 potions).
- Rewrite packets: 296 distinct IDs; all pass packet structure and lexical
  duplicate checks.
- Production imported: 0 (production authentication/backup unavailable).
- Production migrated: 0 (same blocker).
- Production published: 0.
- Local seed disposition: 296 drafts; 0 eligible under the publication standard.
- Source/editorial blockers: 296 lack an exact registered passage plus dated,
  hash-bound fidelity, scope, rights, safety, and duplicate approval. The
  checked-in packets explicitly prohibit treating their prose as those approvals.
- The previously identified 146 seed-only IDs remain uncreated in production;
  no existence claim can be made without authenticated document checks.

## Implementation and validation

- Added `publish-eligible`, a dry-run-first controlled publication command with
  recorded blockers, bounded retries, revalidation, and idempotent transactional
  status/timestamp writes.
- Corrected Firestore security-test auth fixtures to include the verified-email
  claim required by production rules.
- TypeScript build and lint passed.
- Jest: 36 suites passed, 1 integration suite skipped; 314 tests passed and 19
  skipped.
- Firestore repository integration suite: 20/20 passed against the emulator,
  including idempotent publication status and timestamp persistence.
- Firestore emulator security suite: 8/8 passed.
- Rewrite curriculum validation: 296/296 records, 296 distinct IDs, no invalid
  packets and no lexical duplicate concerns.
- Android build: not executed to completion because the managed image lacked an
  Android SDK. A JDK was supplied, after which Gradle stopped specifically at
  SDK discovery; this is environment/tooling evidence, not a successful build.

## Remaining production sequence

Attach an authorized Google Cloud identity and an existing private
public-access-prevention-enforced bucket, then follow
`FIRESTORE_MIGRATION_RUNBOOK.md`: backup, immutable upload, restore drill,
reviewed migration dry run/apply, before/after comparison, publication dry
run/apply, Firebase deployment, and post-deployment subscriber delivery test.
Independent editors must resolve the recorded source, rights, safety, and
duplicate blockers; automation must not fabricate those approvals.
