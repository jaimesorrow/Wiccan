# All live lesson rewrite status

Date: 2026-10-25
Project: `wiccan-android`
Collection: root `content`

## Result

The read-only live inventory contains 150 documents, all with draft publication status. All 150 live document IDs are present in the checked-in 296-record seed, and six write-disabled editorial packets now provide one proposed rewrite for each live ID.

- Live IDs covered: 150/150
- Editorial proposals complete: 150/150
- Proposed statuses: 150 draft, 0 published
- Instructional lessons within 250–450 words: 150/150
- Sequential practical methods: 150/150
- Automated schema/safety result: 150 pass with one intentional blocker each
- Remaining per-record blocker: approved exact source passage is not bound in the editor-owned registry
- Lexical duplicate concerns across all 150 proposals: 0
- Production Firestore writes performed: 0

The proposals use original modern incantations, identify the practice as `Wiccan / Modern Witchcraft`, preserve the actual historical tradition separately, and replace hazardous or coercive procedures with disclosed safe adaptations. Existing source locators remain research leads and are not represented as approved evidence.

## Proposed category distribution

| Category ID | Count |
| --- | ---: |
| `protection-cleansing-warding` | 47 |
| `potions-infusions` | 21 |
| `altars-sacred-space` | 19 |
| `candle-magic` | 13 |
| `crystals-stones` | 10 |
| `elemental-magic` | 9 |
| `cord-knot-magic` | 9 |
| `divination` | 9 |
| `herbs-botanicals` | 7 |
| `moon-phases-timing` | 6 |
| **Total** | **150** |

## What remains before Firebase application

1. Verify and approve each exact passage, edition/translation provenance, and reuse rights.
2. Bind passage records and hash-bound fidelity, rights, safety, scope, and duplicate reviews to each proposal.
3. Move the private backup from local temporary storage to durable private storage and restore-test it against the emulator.
4. Re-read live update times and produce the exact before/after change set.
5. Generate the deterministic production digest and obtain approval for that digest.
6. Apply only conflict-free draft updates, preserve IDs/status/timestamps, and run the post-write audit.

The checked-in 296-record seed still contains 146 records that were not found in the live collection. They remain preserved and were not misrepresented as production documents. Publishing is separate from content migration.
