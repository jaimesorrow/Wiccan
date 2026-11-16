# Wiccan content narrative reference packet

Reviewed against `main` at `7b4d9f3` on November 16, 2026.

This directory preserves the November 15 content review packet as historical editorial evidence. These files are references only. They are not migration payloads, do not replace the current validators, and must not be used as proof of the current live Firestore state without a new read from `wiccan-android`.

The authoritative forward-looking narrative remains [`docs/CONTENT_VOICE_AND_LESSON_STANDARD.md`](../../CONTENT_VOICE_AND_LESSON_STANDARD.md). The coordinated target schema and screen order remain [`docs/PRACTICAL_MAGIC_LESSON_TEMPLATE.md`](../../PRACTICAL_MAGIC_LESSON_TEMPLATE.md). If this packet conflicts with either current document, the current documents win.

## Preserved files

| File | Role | Status recorded by the source |
| --- | --- | --- |
| `REWRITES_VOICED_150.pdf.xz.part-*` | Byte-preserving archive of the final voiced review copy for 150 practical lessons | All 150 are marked draft; the PDF says nothing was published or written to Firestore from this artifact |
| `REWRITES_PLAIN_150.pdf.xz.part-*` | Byte-preserving archive of the pre-voice comparison copy | Superseded for tone by the voiced copy; retained for audit history |
| `PUBLISHED_38.pdf.xz` | Byte-preserving archive of the Firestore snapshot of 38 practices | The PDF says it was read from live Firestore on November 15, 2026, after rights confirmation |
| `PUBLISHED_54.pdf.xz` | Byte-preserving archive of the Firestore snapshot of 54 practices | The PDF says it was read from live Firestore on November 15, 2026 |
| `VOICE_NOTES.md` | Voice-pass rules, counts, validation results, and unresolved source review | Records a completed voice pass over 150 drafts and unresolved citation/source-passage gates |
| `REWRITE_NOTES.md` | Rewrite methods, safety decisions, record inventory, and remaining work | Records the 150 practical rewrites and explicitly states that the rewrite operation itself performed no Firestore writes |
| `APP_COPY_VOICE_REFERENCE.md` | Approved user-facing app-copy boundary and anchor wording | Records the implemented moonlit voice while keeping consequential language plain |

Two uploaded files named `REWRITES.pdf` were byte-for-byte identical. Only one canonical copy is retained. Both had SHA-256 `870166fa5544ebb18bd29284844f1ff02747d6403c41576b0c354da131e6d44e`.

The PDFs are compressed because the reference packet exceeds the connected repository transfer limit. Decompression recreates the exact reviewed source bytes. Reassemble each split rewrite archive in part-number order before decompressing it. Verify the reconstructed PDF against the source hashes below.

## Narrative review

The packet and current standard agree on the core product direction:

- practical modern Wiccan activities rather than general history essays;
- method-based categories, including candle spells, herbal rituals, cord and knot work, lunar practices, charms, crystals, divination, altars, elemental practices, and potions;
- warm, mystical, mature, and clear prose;
- non-coercive, self-directed or consent-based practices;
- no guaranteed supernatural, medical, romantic, financial, or legal outcomes;
- activity-specific safety instructions and potion disclosure handling;
- historical material used only with accurate tradition labels and verified evidence;
- Grimoire as the app's archive name.

The supplied app-copy rewrite is also implemented on the reviewed `main` baseline. Its key sign-in, Today, Grimoire, error, notification, and email strings are present in the Android and Functions sources. It is the current interface-copy reference and supersedes older login wording where the exact phrases differ.

`Book of Shadows` still appears in some lesson prose. That is treated as an in-lesson Wiccan journal term, not as the app navigation name. The app feature and shared UI remain **Grimoire**. A future strict terminology cleanup must be reviewed separately rather than silently changing published lesson text.

## Completion status

| Written task | Evidence | Status |
| --- | --- | --- |
| Practical rewrite of the selected 150 records | Plain and voiced review copies plus rewrite notes | Complete in the reference packet |
| Voice pass over the selected 150 records | Voiced copy and voice notes | Complete in the reference packet |
| Safety pass over the selected 150 records | Voice notes report explicit safety additions and zero schema/safety validation issues at the recorded baseline | Complete for that review baseline; revalidate before any new write |
| Publication represented by the supplied snapshots | Two disjoint Firestore snapshots: 38 plus 54 | 92 records documented as published on November 15, 2026 |
| Publication of the rest of the 150 | No supplied live snapshot | 58 records not evidenced as published |
| Source passage, citation, and rights verification | Voice notes report 357 citation issues across 143 records; the current `functions/sourcePassages.json` is empty | Not complete in the repository |
| Repository rewrite staging | Twelve checked-in batches contain 296 draft entries | Complete as staging; every batch remains `firebaseReady: false` |
| Current Functions regression tests | 370 passing tests on November 16, 2026 | Passing |

The two publication PDFs are historical evidence, not a current production audit. The checked-in rewrite batches deliberately remain draft staging even where a supplied PDF describes the corresponding live document as published. Do not change those staging statuses merely to mirror these PDFs.

## Required follow-up

1. Read the current live Firestore inventory before claiming the 92 records are still published or before calculating a current publication total.
2. Populate and review the source-passage registry, resolve citation and rights gaps, and retain edition-specific evidence.
3. Revalidate safety, duplicates, schema compatibility, and publication eligibility against current code.
4. Review the 58 voiced records not represented in the publication snapshots.
5. Keep Firestore publication, repository migration, Functions deployment, and Android release as separate controlled actions.

## Integrity hashes

These are hashes of the original reconstructed source files, not the compressed archive containers.

| File | SHA-256 |
| --- | --- |
| `REWRITES_VOICED_150.pdf` | `870166fa5544ebb18bd29284844f1ff02747d6403c41576b0c354da131e6d44e` |
| `PUBLISHED_38.pdf` | `fd6e19b719f7dea92bd61946b653a6d5f61a41144659624f2e74a50898fcc3ba` |
| `PUBLISHED_54.pdf` | `32f46f85dbc429bb03262e7f87775e92fa05e585e5cc01a7ab2e5e21aa63e90a` |
| `VOICE_NOTES.md` | `3277035c32ae561e93871961d3f5f9b9b891a0ccef4fa4cd646d0e59f8b4c989` |
| `REWRITES_PLAIN_150.pdf` | `016168d038e9ffb98cff383a2f21eb2d584a99e25cd011885d9b823e16bc35ee` |
| `REWRITE_NOTES.md` | `c895d71ca4d9ea6e83b7e6376d1ce6dec92a44749da02586460a6cdf2e49484f` |
