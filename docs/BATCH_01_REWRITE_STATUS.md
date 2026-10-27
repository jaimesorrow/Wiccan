# Rewrite batch 1 status

Batch 1 covers the first 25 checked-in draft IDs in `functions/ancientContentSeed.json`. Every ID and draft status is preserved. This is an editorial artifact only; `writes` and `firebaseReady` are both `false` in the machine-readable packet at `functions/rewriteBatches/batch-01.editorial.json`.

## Result

- Records dispositioned: **25/25**.
- Full practical rewrites completed: **25/25**.
- Rewrites with exact public-domain or open-source passage research attached: **10/25**.
- Rewrites retaining explicit passage, edition, or rights holds: **15/25**.
- Firebase-ready records: **0/25**.
- Published records: **0**.
- Firestore reads or writes: **0**.

The ten rewrites with attached passage research are:

1. `egypt-heka-002-guarding-children` → **Seven-Knot Household Guardian**.
2. `egypt-heka-007-knots-as-working-technology` → **Four-Strand Working Cord**.
3. `maqlu-010-nine-tablets-one-night` → **The Words-and-Actions Ledger**.
4. `maqlu-012-saying-it-again-three-and-seven` → **Three Deliberate Repetitions**.
5. `egypt-heka-014-spells-for-childbirth` → **Two Tiles of Welcomed Support**.
6. `london-leiden-016-the-number-seven-in-egyptian-practice` → **The Seven-Point Boundary**.
7. `maqlu-017-the-patient-speaks-in-first-person` → **The First-Person Threshold**.
8. `maqlu-019-the-three-movements-of-the-night` → **Boundary, Removal, Washing**.
9. `maqlu-022-water-closes-what-fire-opened` → **Dawn Water Closure**.
10. `maqlu-024-what-maqlu-was` → **The Circle Around the Vessel**.

The five new Maqlū proposals cite exact Ritual Tablet line ranges and use the ORACC/CMAwRo text. Archived ORACC licensing evidence states CC BY-SA 3.0 terms, but the live rights page was unavailable and the ShareAlike effect on product distribution still requires independent review. All seven proposals remain drafts with `rightsReviewRequired: true` and are not approved migration patches.

The three Berlin Papyrus 3027 proposals use Adolf Erman's 1901 public-domain edition and exact page/manuscript locators. They remove child-worn cords, physical use of birth bricks, combustion, and animal offerings. They still require independent fidelity, dating, scope, safety, and duplicate approval.

## Remaining source work

All 25 editorial rewrites are complete. Fifteen rely on the original record's claimed source locator while the precise manuscript passage, edition, translation, or reuse basis remains unresolved. Their structured citations say `Pending` rather than inventing bibliographic facts. These lessons are complete editorial proposals but remain ineligible for Firebase migration until source review replaces each pending value with verified evidence.

The packet records a proposed practical concept and primary category for every held record. The holds are intentional:

- Borghouts's *Ancient Egyptian Magical Texts* is a modern copyrighted translation; free access to a PDF does not establish commercial reuse rights.
- Museum object or essay pages do not supply an approved exact primary passage by themselves.
- ORACC-linked *Maqlu* records need an exact line-level passage and reuse review.
- Wikipedia and university overview pages are secondary sources and cannot serve as the required primary-source foundation.
- Medical and pregnancy-related source concepts need an especially strict scope review and may only become symbolic, non-medical adaptations.

No citation was invented to force a record through validation. The original live documents remain recoverable and unchanged.

## Batch validation

- 25 distinct existing live document IDs.
- 25 preserved `draft` statuses.
- 25 numbered instructional sections between 250 and 450 words.
- 25 compatible `intro`/`introduction`, `body`/`lesson`, and `materials`/`tools` aliases.
- Automated schema and safety validation: no violation other than the intentionally missing approved source-passage registry entry.
- Exact fingerprint, normalized title, same-passage-and-technique, n-gram, and longest-common-procedure duplicate checks: no Batch 1 concern.
- Semantic duplicate provider: not configured; human duplicate review remains required.
- Firestore writes or publications: zero.

Primary category distribution: 10 protection and cleansing; 7 altar and sacred space; 3 crystal and stone; 2 cord and knot; 1 elemental; 1 herbs and botanicals; and 1 moon and timing.

## Publication boundary

“Editorial processing complete” means each of the 25 records now has a documented disposition and is bound privately to its authenticated live document/update time. It does **not** mean the batch is ready for Firebase application. The remaining gates are a durable private backup and restore test, approved source-passage records, independent hash-bound editorial and duplicate reviews, and a validated change-set digest.
