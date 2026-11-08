# Local lesson rewrite batches

The checked-in 296-record seed can be divided into editorial work packets without Firebase access:

```bash
cd functions
npm run prepare-rewrite-batches -- --output /secure/local/wiccan-rewrite-staging
```

The command performs no network request and has no Firestore write option. It preserves seed order, document IDs, complete original fields, and `draft` status. The default maximum is 25 records, producing eleven 25-record packets and one 21-record packet. Every packet has a deterministic SHA-256 and every original record has its own source-snapshot SHA-256. The destination must be new; the command refuses to overwrite an existing directory.

Each entry intentionally starts with `proposedFields: null`, `expectedUpdateTime: null`, and two validation blockers. A completed rewrite remains a local proposal until it is bound to the corresponding live draft and update time, backed up, source-reviewed, validated, and converted into the existing reviewed change-set format. These local packets cannot be passed directly to the Firestore apply command.

This lets editorial rewriting proceed before Firebase is connected while preventing the local seed from being mistaken for a live inventory or an approved production change set. `functions/sourcePassages.json` is currently empty, so every historical rewrite remains source-blocked until an independent passage review is recorded.

## Narrative v2 rewrites (November 2026)

The November 7, 2026 standard (`docs/CONTENT_VOICE_AND_LESSON_STANDARD.md`) teaches practical modern Wiccan practice, and historical context is allowed only after a passage is verified. Batches are being rewritten to that standard one packet at a time:

| Batch | Status |
| --- | --- |
| 01 | Narrative v2 complete (`disposition: narrative_v2_rewrite_complete_editorial_review_pending`) |
| 02 | Narrative v2 complete (`disposition: narrative_v2_rewrite_complete_editorial_review_pending`) |
| 03 | Narrative v2 complete (`disposition: narrative_v2_rewrite_complete_editorial_review_pending`) |
| 04 | Narrative v2 complete (`disposition: narrative_v2_rewrite_complete_editorial_review_pending`) |
| 05–12 | Earlier source-adaptation proposals; narrative v2 pending |

Every narrative v2 entry follows the same rules:

- The document ID, seed order, `sourceId`, `similarityGroupId` and `draft` status are preserved.
- User-facing text is original modern practice with no historical claims. The unverified historical citations, source locator, URL, tradition and passage fields are removed, which leaves `citations` empty for now.
- Removing historical attribution does not waive the standard's other citation rule. Botanical, material-safety and similar claims in a lesson, such as plant allergies, infusion spoilage or which stones tolerate water, need verified references added during editorial review, before migration.
- The entry uses one current method category, with intentions moved to tags.
- Each lesson has a 250–450-word numbered method of at least seven steps.
- The batch test requires each record's only `findPracticalLessonViolations` issue to be the expected missing-passage blocker.
- Each rewrite must pass `screenGeneratedCandidate` against every other batch's proposals, and `npm run verify-rewrite-curriculum` across all 296.

Batch 01 is spread across all ten categories, two or three lessons each. It still needs human editorial, safety and duplicate review before any migration, and nothing here writes to Firestore.
