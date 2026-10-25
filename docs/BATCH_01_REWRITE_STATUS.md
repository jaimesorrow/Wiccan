# Rewrite batch 1 status

Batch 1 covers the first 25 checked-in draft IDs in `functions/ancientContentSeed.json`. Every ID and draft status is preserved. This is an editorial artifact only; `writes` and `firebaseReady` are both `false` in the machine-readable packet at `functions/rewriteBatches/batch-01.editorial.json`.

## Result

- Records dispositioned: **25/25**.
- Full practical rewrites completed: **7/25**.
- Source, rights, or scope holds: **18/25**.
- Firebase-ready records: **0/25**.
- Published records: **0**.
- Firestore reads or writes: **0**.

The completed editorial rewrites are:

1. `maqlu-010-nine-tablets-one-night` → **The Words-and-Actions Ledger**.
2. `maqlu-012-saying-it-again-three-and-seven` → **Three Deliberate Repetitions**.
3. `london-leiden-016-the-number-seven-in-egyptian-practice` → **The Seven-Point Boundary**.
4. `maqlu-017-the-patient-speaks-in-first-person` → **The First-Person Threshold**.
5. `maqlu-019-the-three-movements-of-the-night` → **Boundary, Removal, Washing**.
6. `maqlu-022-water-closes-what-fire-opened` → **Dawn Water Closure**.
7. `maqlu-024-what-maqlu-was` → **The Circle Around the Vessel**.

The five new Maqlū proposals cite exact Ritual Tablet line ranges and use the ORACC/CMAwRo text. Archived ORACC licensing evidence states CC BY-SA 3.0 terms, but the live rights page was unavailable and the ShareAlike effect on product distribution still requires independent review. All seven proposals remain drafts with `rightsReviewRequired: true` and are not approved migration patches.

## Holds for the remaining 18 records

The packet records a proposed practical concept and primary category for every held record. The holds are intentional:

- Borghouts's *Ancient Egyptian Magical Texts* is a modern copyrighted translation; free access to a PDF does not establish commercial reuse rights.
- Museum object or essay pages do not supply an approved exact primary passage by themselves.
- ORACC-linked *Maqlu* records need an exact line-level passage and reuse review.
- Wikipedia and university overview pages are secondary sources and cannot serve as the required primary-source foundation.
- Medical and pregnancy-related source concepts need an especially strict scope review and may only become symbolic, non-medical adaptations.

No citation was invented to force a held record through validation. The held original documents remain recoverable and unchanged.

## Publication boundary

“Editorial processing complete” means each of the 25 records now has a documented disposition. It does **not** mean the batch is ready for Firebase. A Firebase-ready batch still requires an authenticated live-document binding, backup and restore test, an approved source-passage record, independent hash-bound editorial review, duplicate screening against the live curriculum, and a validated change-set digest.
