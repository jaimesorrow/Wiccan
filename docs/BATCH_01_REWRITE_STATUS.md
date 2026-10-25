# Rewrite batch 1 status

Batch 1 covers the first 25 checked-in draft IDs in `functions/ancientContentSeed.json`. Every ID and draft status is preserved. This is an editorial artifact only; `writes` and `firebaseReady` are both `false` in the machine-readable packet at `functions/rewriteBatches/batch-01.editorial.json`.

## Result

- Records dispositioned: **25/25**.
- Full practical rewrites completed: **2/25**.
- Source, rights, or scope holds: **23/25**.
- Firebase-ready records: **0/25**.
- Published records: **0**.
- Firestore reads or writes: **0**.

The two completed editorial rewrites are:

1. `london-leiden-016-the-number-seven-in-egyptian-practice` → **The Seven-Point Boundary** (`altars-sacred-space`, secondary `divination`). Its 1904 edition and passage locator have research support, but it still requires independent passage, scope, rights-jurisdiction, safety, and duplicate approval.
2. `maqlu-022-water-closes-what-fire-opened` → **Dawn Water Closure** (`protection-cleansing-warding`, secondary `elemental-magic`). The rewrite is complete, but exact ORACC reuse terms and line-level source review remain unresolved.

Neither rewrite is safe to upload as an approved migration patch yet. Both remain drafts and retain `rightsReviewRequired: true`.

## Holds for the remaining 23 records

The packet records a proposed practical concept and primary category for every held record. The holds are intentional:

- Borghouts's *Ancient Egyptian Magical Texts* is a modern copyrighted translation; free access to a PDF does not establish commercial reuse rights.
- Museum object or essay pages do not supply an approved exact primary passage by themselves.
- ORACC-linked *Maqlu* records need an exact line-level passage and reuse review.
- Wikipedia and university overview pages are secondary sources and cannot serve as the required primary-source foundation.
- Medical and pregnancy-related source concepts need an especially strict scope review and may only become symbolic, non-medical adaptations.

No citation was invented to force a held record through validation. The held original documents remain recoverable and unchanged.

## Publication boundary

“Editorial processing complete” means each of the 25 records now has a documented disposition. It does **not** mean the batch is ready for Firebase. A Firebase-ready batch still requires an authenticated live-document binding, backup and restore test, an approved source-passage record, independent hash-bound editorial review, duplicate screening against the live curriculum, and a validated change-set digest.
