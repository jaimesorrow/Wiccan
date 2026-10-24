# Task 1 implementation status

Updated: 2026-10-24. This report distinguishes completed repository work from production evidence.

## Completed repository work

- Reverified GitHub `main` at `532088cb85a2cf2eafbcefa03437226d9e1d8332` and retained draft PR #78 as the review vehicle.
- Audited the checked-in seed: 296 records, all drafts; 200 lessons, 59 spells, and 37 potions. None passes the new standard because the approved exact-passage register is empty.
- Implemented ten practical categories, backward-compatible lesson aliases, separate `tradition = Wiccan / Modern Witchcraft` and `sourceTradition`, structured citations, adaptation notes, safety metadata, and Room migration 4→5.
- Added a ten-candidate historical-source registry. Two editions have edition-level rights evidence, but zero candidates are production-approved because no exact passage has completed every review.
- Added exact and lexical duplicate checks, deterministic concept fingerprints, passage-reuse checks, configurable semantic thresholds, an explicit semantic-provider coverage field, and source/existing-content originality screening.
- Added typed root `content/{id}` backup, private Cloud Storage upload with generation precondition, emulator restore verification, update-time guarded atomic migration, deterministic change-set digests, document comparison, and atomic conflict-aware rollback.
- Restored the official Gradle wrapper JAR and executable wrapper mode required by CI.
- Updated the manual content generator to accept verified ancient, medieval, or early-modern grimoire passages, store drafts only, and keep historical tradition separate from the modern Wiccan lesson tradition. No schedule or autonomous publishing path was enabled.
- Added a local-only rewrite staging command for the checked-in curriculum. It produces twelve deterministic packets (eleven groups of 25 and one group of 21), preserves all 296 IDs and complete original draft fields, records per-record and per-batch SHA-256 digests, and has no network or write option.
- Bound five expanded representative rewrites to actual checked-in draft IDs for editorial review. Their numbered instructional sections contain 273–293 words and cover ritual closure, candle work, cord work, sacred-space preparation, and water cleansing.

## Validation evidence

- Functions: TypeScript build passed; ESLint passed; 35 suites and 276 tests passed, with one emulator-dependent suite skipped by the unit command.
- Dependency audit: zero known npm vulnerabilities at `--audit-level=moderate` after resolving the vulnerable transitive `body-parser` version.
- Firebase emulator: 19 Firestore security-rule cases, 19 transaction/repository integration tests, and 36 end-to-end Auth/Functions/Firestore/Pub/Sub checks passed; no production data was contacted.
- Checked-in content audit: 296 inspected; 0 qualified; 296 source-blocked; 293 records in duplicate-concern coverage; 158 pairwise lexical concerns; 37 disclosure-triggering records.
- Repository/toolchain structural checks: 43 release-preflight checks and all Python validation suites passed.
- Android: repository and release-preflight checks passed. Compilation could not start because this execution environment has no Android SDK and the Gradle distribution download was unavailable during the final run. This is an unexecuted check, not a pass.
- GitHub Actions: the rebased commit triggered CI, but GitHub started no runner steps and annotated every job with: "recent account payments have failed or your spending limit needs to be increased." This is an account billing block, not a test result. The same repository checks were executed locally as listed above.

## Approval gates and blockers

- Firebase/Google Cloud access is not connected. The live root `content` count, production statuses, update times, delivery history, `settings/disclosures`, IAM, rules, indexes, and private backup are unverified.
- The five staged lessons are editorial examples. They are not bound to verified live document IDs or update times and cannot be considered migrated records.
- `functions/sourcePassages.json` remains empty. No production change set can pass validation until exact passage, date, edition, translator, commercial-use rights, scope, fidelity, and safety reviews are recorded.
- Semantic duplicate screening has an interface and thresholds but no configured provider; reports state `semanticExecuted: false`.
- GitHub reports 33 open Dependabot alerts on the default branch, but the available integration token cannot read alert details (`403 Resource not accessible by integration`). Local Functions and Firestore npm audits are clean; the remaining repository-wide alerts require a maintainer with security-alert access to triage.
- No production Firestore write, backup upload, content publication, schedule, deployment, merge, subscription change, or Play release occurred.

## Next gated sequence

1. Connect an authorized identity to `wiccan-android` and run the read-only audit.
2. Review the live inventory, source registry, taxonomy, schema, and shared validators.
3. Complete exact-passage reviews and bind five representative rewrites to eligible, undelivered live draft IDs.
4. Capture and privately upload the root-content backup, restore-test it in the emulator, and generate the exact dry-run change set.
5. Obtain explicit approval for the change-set SHA-256 before any production migration.
6. Complete Task 1 verification before enabling Task 2 production generation or Firestore writes.

The operator prompt for reviewed lesson authoring is [CHATGPT_DAILY_LESSON_PROMPT.md](CHATGPT_DAILY_LESSON_PROMPT.md). The production connection and migration commands are in [FIRESTORE_MIGRATION_RUNBOOK.md](FIRESTORE_MIGRATION_RUNBOOK.md).
