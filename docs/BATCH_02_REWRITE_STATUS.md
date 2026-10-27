# Batch 02 rewrite status

Date: 2026-10-25

Batch 02 contains the second 25 live Firestore lesson IDs in checked-in seed order. Editorial rewriting is complete for all 25 records. This artifact is write-disabled and every proposal preserves `status: draft` and `contentStatus: draft`.

## Validation result

- Proposed rewrites: 25/25
- Main instructional length: 250–450 words for every lesson
- Numbered methods: sequential and actionable
- Practical categories: valid for every lesson
- Automated safety/schema validation: passed except for the intentional missing-approved-source-passage blocker
- Lexical duplicate concerns within Batch 02: 0
- Lexical duplicate concerns across Batches 01–02: 0
- Firestore writes performed: 0

## Category distribution

| Category ID | Count |
| --- | ---: |
| `protection-cleansing-warding` | 9 |
| `altars-sacred-space` | 4 |
| `moon-phases-timing` | 4 |
| `elemental-magic` | 2 |
| `herbs-botanicals` | 2 |
| `potions-infusions` | 2 |
| `candle-magic` | 1 |
| `divination` | 1 |

## Source and production status

The rewrites preserve each existing record's claimed historical locator as a research lead. Those locators are not treated as approved evidence. Exact passages, edition and translation provenance, reuse rights, source fidelity, and safety still require independent approval. Until those reviews are bound to the authored content hash, each proposal remains `source_review_pending` and is not Firebase-ready.

The private live-bound packet was updated outside the repository with the 25 proposals and their observed production update times. Its internal batch digest is:

`aee62a24890562ea5ce8e0b781ec158a0e7da1e9bd215eb90e26a7fb9511ba75`

A durable private backup and restore test remain required before any production content write. Publishing is a separate operation and is not enabled by these artifacts.
