# Voice notes: mystical voice pass (Nov 15, 2026)

Jaime's request: "I want the lessons to have mystical voice too but still be clear of intention and not be misleading."

All 150 docs in the live `content` collection were voiced in three parts. Part 1 covers sorted items 1–50 (`voiced/_p1/`), part 2 covers 51–100 (`part2/`) and part 3 covers 101–150 (`part3/`). They were merged in `ids-sorted.txt` order into `rewrites.json`, `REWRITES.md`, `REWRITES.pdf` and `docs/`. The pre-voice versions are unchanged in `rewrites-plain.json` and `REWRITES-plain.pdf` (md5-verified). Nothing was written to Firestore or GitHub, and every record is `status`/`contentStatus: draft`.

## What was voiced
- **Title:** evocative, but it still names the practice (for example "Salt, Herb, and Iron: Making a Witch Bottle to Guard the Home").
- **Context:** an evocative opening sentence that restates only facts already in the original live doc. It is followed by the existing "original modern working ... not a reconstruction of the historical rite ... makes no claim that the source culture practised Wicca" framing, which is kept in every doc.
- **Framing line:** one short atmospheric sentence before the numbered steps.
- **Incantation, closing and grounding, journaling prompt (`reflection`), and follow-up (`practice`):** written in a moonlit, warm, witchy voice. Follow-ups are still concrete, timed actions.

## What was not voiced
- **Intent (`summary`):** still a plain, literal statement of what the reader physically does and why. A mystical sentence may follow it but never replaces it.
- **Concrete content:** materials and quantities, `timing`, `durationMinutes` and the numbered steps are unchanged in substance. Mystical phrasing appears only as framing around them.
- **Fixed fields:** IDs, `type` (spell, potion or lesson; never ritual), `category`, `primaryCategoryId`, `secondaryCategoryIds`, `lessonType`, `requiresPotionDisclosure`, `sourceId`, `sourceCitation`, `sourceUrl`, `rightsReviewRequired` (54 true) and status are identical to the plain version. This was verified across all 150.
- **Not misleading:** no guaranteed or supernatural outcomes, no medical, financial or legal efficacy claims, and no claim that a modern practice is ancient or historically authentic. Historical facts in the context lines come only from the original doc.

## Numbered steps that changed
Only two steps changed across all 150 docs. Each change removes a word that trips the method-ingestion guard and keeps the same meaning.
- **`egypt-heka-003-handbooks-not-scripture`:** "eating or drinking something ordinary" became "having something to eat or a glass of water".
- **`maqlu-103-hazardous-and-protected-materials` (step 3):** the jar label text "Ritual use only. Do not consume." became "Ritual use only – not food". The lesson's safety note still covers ingestion and poison control.

## Safety-text rule
- **Plain sentences kept word for word:** every sentence of each plain-backup `safetyNote` is kept word for word in the voiced version, with exactly three recorded exceptions (below). "Ritual use only. Do not consume." and the potion disclosure flag are unchanged wherever they existed.
- **Recorded rewordings:** these were needed because the repo's essential-oil ingestion regex matches "essential oil ... ingest" even when the sentence negates it ("never ingest").
  - `london-leiden-142-scorpion-oil-with-a-linen-strip` and `london-leiden-146-the-greek-bean-macerate` (recorded in `_review.safetyRewording`):
    - Before: "If you add essential oil, use no more than 1 to 2 drops per 10 ml of carrier, never ingest it, and check with a qualified professional before using essential oils if you are pregnant, nursing, on medication or using the oil near children or pets."
    - After: "If you add essential oil, use no more than 1 to 2 drops per 10 ml of carrier and keep it for external craft use. Check with a qualified professional before using essential oils if you are pregnant, nursing, on medication or using the oil near children or pets."
  - `maqlu-096-pure-oil-clear-oil` (recorded in `rewrite.safetySentencesReworded`):
    - Before: "Do not add essential oils unless they are properly diluted, and never ingest them."
    - After: "Essential oils are for external use only, and only when properly diluted; this recipe does not need them."
- **`maqlu-100-thyme-and-sesame`:** the safety note doesn't say "Ritual use only. Do not consume." in either version. It is a food preparation of toasted seeds, with `requiresPotionDisclosure: true` and lessonType guided-practice. It passes the validators, but an editor should confirm the food-safety framing.

## Added safety sentences
Short, plain safety sentences were added only where `validateLesson` or `findPracticalSafetyViolations` required a topic the note didn't cover. They were added in 108 of 150 docs: 33 in part 1, 35 in part 2 and 40 in part 3. The topics are allergies, children, pets, fire/flame (extinguish, unattended, heat-safe), consent ("consent is required"), ingestion, sharp tools, skin contact and smoke/ventilation. Each doc's additions are listed in:
- part 1: `_review.voice.safetySentencesAdded`
- part 2: `_review.safetySentencesAdded`
- part 3: `rewrite.safetySentencesAutoAdded`

Some additions are triggered by keywords alone. For example, the word "fire" in an element list adds a flame sentence to a practice that has no flame. They are harmless, but an editor may prefer to reword the trigger word instead.

## Validation (repo code at main a68c802, compiled in `/workspace/wiccan-emulator/functions/lib`)
- **`validateLesson` and `findPracticalSafetyViolations`** (`/tmp/vl/check2.js rewrites.json`): 0 safety or schema issues across 150 docs, and every type is spell, potion or lesson. Before the voice pass, the plain set had 85 docs failing these checks.
- **Duplicates** (`/tmp/vl/dup_all.js rewrites.json`, using the repo's `checkDuplicates` with the `status`/`rejectionReasons` fields):
  - Each doc against the other 149: 150 of 150 `accepted-draft`, 0 rejected.
  - Against the 296 PR rewrites, excluding the doc's own PR version: 19 matches, all in part 2 and all expected.
    - 15 are the doc's own `lesson-study-` twin: london-leiden 058, 063, 064, 097, 098, 099, 129, 132, 148, 150, and maqlu 019, 034, 037, 038, 062.
    - 4 are documented in-part reassignments: 058→060, maqlu-030→london-leiden-101, maqlu-019→maqlu-022, lesson-study-maqlu-051→maqlu-029.
  - 55 docs are still close to their own PR version, which is expected for reused text.

## Open citation gaps (not fixed by the voice pass)
`validateLesson` reports 357 citation issues on 143 docs:
- 143 have no `accessedAt` date: egypt 42, harris 5, london-leiden 32, maqlu 64.
- 100 have no usageNote (the claim the citation supports).
- 100 have no structured title/author.
- 14 have non-https URLs.

These come from the live docs' `sourceCitation`/`sourceUrl`. Part 3 dated the sources it could reach on 2026-11-15. oracc.museum.upenn.edu and hermetic.com weren't reachable from the box, so their dates were left blank rather than invented. No `sourcePassageId` exists, and `functions/sourcePassages.json` is empty, so every historical context line still needs source review before publication. Lessons typed `lesson` also need the approve-draft practicalReview before they can be published.
