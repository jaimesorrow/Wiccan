# Task 1 implementation status

Updated: 2026-10-24. This report distinguishes completed repository work from production evidence.

## Completed repository work

- Reverified GitHub `main` at `3ea0f2c6cbed90b8dc353d85b9126122ca73b31d` and retained draft PR #78 as the review vehicle.
- Audited the checked-in seed: 296 records, all drafts; 200 lessons, 59 spells, and 37 potions. None passes the new standard because the approved exact-passage register is empty.
- Implemented ten practical categories, backward-compatible lesson aliases, separate `tradition = Wiccan / Modern Witchcraft` and `sourceTradition`, structured citations, adaptation notes, safety metadata, and Room migration 4→5.
- Added a ten-candidate historical-source registry. Two editions have edition-level rights evidence, but zero candidates are production-approved because no exact passage has completed every review.
- Added exact and lexical duplicate checks, deterministic concept fingerprints, passage-reuse checks, configurable semantic thresholds, an explicit semantic-provider coverage field, and source/existing-content originality screening.
- Added typed root `content/{id}` backup, private Cloud Storage upload with generation precondition, emulator restore verification, update-time guarded atomic migration, deterministic change-set digests, document comparison, and atomic conflict-aware rollback.
- Restored the official Gradle wrapper JAR and executable wrapper mode required by CI.
- Updated the manual content generator to accept verified ancient, medieval, or early-modern grimoire passages, store drafts only, and keep historical tradition separate from the modern Wiccan lesson tradition. No schedule or autonomous publishing path was enabled.

## Validation evidence

- Functions: TypeScript build passed; ESLint passed; 31 suites and 246 tests passed, with one emulator-dependent suite skipped by the unit command.
- Dependency audit: zero known npm vulnerabilities at `--audit-level=moderate` after resolving the vulnerable transitive `body-parser` version.
- Firebase emulator: 14 Firestore security-rule cases and 19 transaction/repository integration tests passed against the demo project; no production service was contacted.
- Checked-in content audit: 296 inspected; 0 qualified; 296 source-blocked; 293 records in duplicate-concern coverage; 158 pairwise lexical concerns; 37 disclosure-triggering records.
- Repository/toolchain structural checks: 43 release-preflight checks and all Python validation suites passed.
- Android: dependency resolution succeeded with the checksum-verified pinned Gradle 9.8.1 distribution. Compilation could not start because this execution environment has no Android SDK. This is an unexecuted check, not a pass.

## Approval gates and blockers

- Firebase/Google Cloud access is not connected. The live root `content` count, production statuses, update times, delivery history, `settings/disclosures`, IAM, rules, indexes, and private backup are unverified.
- The five staged lessons are editorial examples. They are not bound to verified live document IDs or update times and cannot be considered migrated records.
- `functions/sourcePassages.json` remains empty. No production change set can pass validation until exact passage, date, edition, translator, commercial-use rights, scope, fidelity, and safety reviews are recorded.
- Semantic duplicate screening has an interface and thresholds but no configured provider; reports state `semanticExecuted: false`.
- No production Firestore write, backup upload, content publication, schedule, deployment, merge, subscription change, or Play release occurred.

## Next gated sequence

1. Connect an authorized identity to `wiccan-android` and run the read-only audit.
2. Review the live inventory, source registry, taxonomy, schema, and shared validators.
3. Complete exact-passage reviews and bind five representative rewrites to eligible, undelivered live draft IDs.
4. Capture and privately upload the root-content backup, restore-test it in the emulator, and generate the exact dry-run change set.
5. Obtain explicit approval for the change-set SHA-256 before any production migration.
6. Complete Task 1 verification before enabling Task 2 production generation or Firestore writes.

The operator prompt for reviewed lesson authoring is [CHATGPT_DAILY_LESSON_PROMPT.md](CHATGPT_DAILY_LESSON_PROMPT.md). The production connection and migration commands are in [FIRESTORE_MIGRATION_RUNBOOK.md](FIRESTORE_MIGRATION_RUNBOOK.md).
