# Revised Wiccan content — original 150 records (REVIEW ONLY)

Project: `wiccan-android`; source: `functions/ancientContentSeed.json` and the six checked-in `rewriteBatches/batch-01..06.editorial.json` packets.

**NOT READY FOR FIRESTORE WRITES, PUBLICATION, OR IMPORT.** This is a prepared editorial artifact, not an authorized production changeset.

- 150 existing record IDs; 54 lesson, 59 spell, 37 potion types; all status/contentStatus = draft.
- The October 25 read-only audit found these 150 document IDs already present in root `content`. Do NOT create them again or overwrite their versions.
- The record IDs, original `type`, original `tier`, and original checked-in `similarityGroupId` are preserved. The retired `difficulty` field is omitted.
- Practical rewrites replace the historical-essay narrative; structured `summary`, `sourceTradition`, `primaryCategoryId`, `introduction`, `lesson` (numbered method), `tools`, `incantation`, `reflection`, `practice`, `safetyNote`, `citations`, `adaptationNotes`, and other fields are included.
- All 150 have `rightsReviewRequired: true` until the exact passage, translation/edition rights, historical fidelity, modern Wiccan scope, safety, and duplicate reviews are completed.
- `sourcePassageId` entries are unapproved candidate references: `functions/sourcePassages.json` is empty. They MUST NOT be treated as valid source approvals.
- Other production prerequisites remain: secure immutable backup plus restore test, exact Firestore update-time binding, source & editorial hash approvals, disclosure settings, dry-run, reviewed patch SHA approval.
- After review, use `docs/FIRESTORE_MIGRATION_RUNBOOK.md` to produce protected patch sets for the existing 150 records. Publication is separate and must use the guarded approval process.
- The 146 other checked-in seed entries are NOT in this artifact. No additional Firestore documents are created by this branch.

This file is staged on a draft PR and must not be merged as a declaration that the content is approved.
