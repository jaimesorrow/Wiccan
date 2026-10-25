# Live Firestore content audit — 2026-10-25

Project identity was authenticated through the Firebase CLI and verified as `wiccan-android`. This audit inspected root `content`, root `userContentHistory`, and `settings/disclosures` through read-only Firestore REST requests. It performed no Firestore writes.

## Verified inventory

- Root `content` documents: **150**.
- Status: **150 draft**, **0 published**.
- Types: **54 lessons**, **59 spells**, **37 potions**.
- Root `userContentHistory` rows: **0**.
- Distinct delivered lesson IDs observed there: **0**.
- `settings/disclosures`: **absent**.
- Live IDs absent from the checked-in 296-record seed: **0**.
- Checked-in seed IDs absent from live Firestore: **146**.

The reported 296-record production count is therefore not correct for the live root collection at the time of inspection. The repository has 296 records; production has 150. The 146 missing IDs must not be created implicitly as part of an update to existing Firestore lessons.

## Existing distribution

| Existing category | Count |
| --- | ---: |
| Protection | 45 |
| BanishingAndRelease | 31 |
| HealingAndWellness | 31 |
| DivinationAndClarity | 21 |
| HomeAndHearth | 12 |
| MoonAndSeasonal | 6 |
| ProsperityAndAbundance | 3 |
| LoveAndRelationships | 1 |

All 150 records require the new practical taxonomy, structured lesson fields, exact-passage evidence, and editorial review. The automated audit reports **0/150** as publication-ready because `functions/sourcePassages.json` contains no approved passages.

## Backup and batch binding

A typed, gzip-compressed local backup was captured outside the repository with permissions `0600`:

- Local artifact: `/tmp/wiccan-firestore-20261025/content-backup.json.gz`
- SHA-256: `e3d2b7d4817f3015fe4a576c8855398ca3a4574b3f56ef9cc5e95eb0a9c13e02`
- Documents: **150**

A second raw Firestore REST artifact preserves the API's exact typed value representation. The local backup has not been uploaded to Cloud Storage because the project currently exposes no Storage bucket. It consequently does not yet satisfy the production apply command's private-bucket and immutable-generation requirement.

The 150 live documents were bound to six write-disabled batches of 25 in checked-in-seed order. Each entry records its live update time, original fields, and source snapshot digest. Two existing editorial proposals were bound to their live IDs in Batch 1; both remain blocked by source review. These private artifacts remain outside Git because they contain a production data export.

## Readiness blockers

1. **150/150 source blocked:** no exact passage has an approved registry record.
2. **150/150 editorial review pending:** fidelity, scope, rights, safety, and duplicate decisions are not hash-bound.
3. **37 disclosure-triggering records:** the expected global disclosure document is absent.
4. **Six duplicate concerns:** exact and lexical checks ran; semantic screening has no configured provider.
5. **Backup durability:** a local recoverable backup exists, but no private Cloud Storage bucket is available for the required immutable off-workspace copy.
6. **No publication action exists in this work:** migration must preserve `draft`; publication remains a separate approval and operation.

## Write status

No lesson, setting, rule, index, history record, subscription, authentication record, or Grimoire document was modified. The next content update must use the six live-bound batches and reject any document whose update time changes after this audit.
