# Lesson schema v2 — practical Wiccan lessons

Wiccan is a guided learning app for practical Wiccan practice: each day a
user receives one structured lesson that teaches a specific practice (a
candle spell, herbal ritual, cord work, lunar practice, charm, stone work,
divination, altar work, elemental practice, or a potion/infusion). Lessons
are original, mature and spiritually respectful; history may inspire a
lesson, but no lesson claims that modern Wicca existed in an ancient culture
or that a modern ritual reconstructs an ancient one. No lesson promises a
supernatural result.

Code: `functions/src/lessons/` (`categories.ts`, `canonicalLesson.ts`,
`validateLesson.ts`, `duplicates.ts`, `repoInventory.ts`).

## Schema before this change (still stored)

`Content` (`functions/src/types.ts`) — the shape the app and
`getTodayContent` read today: `id, type (spell|potion|lesson), tier, tradition,
category (8 intention categories or the earlier practical IDs), title, intro,
body, reflection, practice, materials (string[]), safetyNote, tags, status
(draft|published), sourceId, sourceCitation, sourceUrl, rightsReviewRequired,
similarityGroupId`, plus optional additive fields from `PracticalLessonFields`.

## Canonical lesson (v2)

```text
id, title, slug, tradition, summary, introduction, lesson, incantation,
reflection, practice, safetyNote, primaryCategoryId, secondaryCategoryIds,
lessonType, requiresPotionDisclosure, difficulty, durationMinutes,
tools[{name, quantity, required, substitution}],
correspondences{elements, colors, herbs, stones, moonPhases, days, symbols},
tags, citations[{title, author, edition?, url?, accessedAt, usageNote, rightsNote?}],
contentStatus (draft|review|rejected|archived|published), createdAt, updatedAt
```

`lessonType`: guided-practice, spell, ritual, potion, infusion, bath, oil,
spray, jar. The last six always set `requiresPotionDisclosure`, which renders
`settings/disclosures.potionSafetyDisclosure`.

Stored v2 drafts also carry the legacy fields (`legacyFieldsFor`) so the
current app keeps working until it reads the canonical model directly.

## Categories (method, not outcome)

| ID | Label |
| --- | --- |
| candle-spells | Candle Spells |
| herbal-rituals | Herbal Rituals |
| cord-knot-work | Cord & Knot Work |
| lunar-practices | Lunar Practices |
| charms-talismans | Charms & Talismans |
| crystals-stones | Crystals & Stones |
| divination | Divination |
| altars-sacred-space | Altars & Sacred Space |
| elemental-practices | Elemental Practices |
| potions-infusions | Potions & Infusions |

Intentions (protection, love, abundance, rest, focus...) are tags.

## Old → new mapping (`toCanonicalLesson`)

| Old | New |
| --- | --- |
| document ID | unchanged — never derived from title or slug |
| intro | introduction |
| body | lesson |
| materials: string[] (`"None"` dropped) | tools[{name, quantity:"", required:true, substitution:""}] |
| category: earlier practical ID (candle-magic, herbs-botanicals, cord-knot-magic, moon-phases-timing, elemental-magic, ...) | the matching method category |
| category: intention (Protection, HealingAndWellness, ...) | method inferred from the text, intention kept as a tag; a fallback is reported when the text names no method |
| type potion / lessonType tea, bath, oil, ... | lessonType + requiresPotionDisclosure = true |
| status | contentStatus |
| sourceCitation, sourceUrl | one citation, reported for review (no usageNote/accessedAt) |
| correspondences: string[] | correspondences.symbols |
| (missing) summary, durationMinutes, slug | slug derived from title; summary and duration stay empty and are reported |

Mapping never invents content. An incomplete record fails validation and is
excluded from delivery rather than crashing a reader. The dry-run mapping and
audit of the 296 checked-in records is `docs/LEGACY_LESSON_AUDIT.md`.

## Validation (`validateLesson`)

Rejects: missing required fields or `"null"` text; unknown primary/secondary
categories; secondary repeating primary; unsupported lessonType, difficulty or
status; duration outside 1–240 minutes; a guided lesson with fewer than three
steps; potion-like lessons without the disclosure; non-food preparations
without "Ritual use only. Do not consume."; essential-oil ingestion;
guaranteed or supernatural-outcome promises; coercive or non-consensual
instructions; medical treatment claims; missing safety guidance when the
lesson involves fire, smoke, skin contact, ingestion, children, animals,
allergens or sharp tools; affecting another person without consent wording;
invented ancient-Wiccan lineage or sayings; citations without title/author,
the supported claim (`usageNote`) or an access date; quotations without a
citation. Negated phrases ("does not guarantee") are allowed.

## Duplicate detection (`duplicates.ts`)

Normalization: Unicode NFKD, lowercase, punctuation removed, plurals
singularized, decorative words removed, and synonym groups collapsed (cord /
string / thread; candle / taper / votive; cleansing / clearing /
purification; charm / talisman / amulet; tea / infusion / brew; grounding /
centering / steadying; threshold / doorway; ...). Colours, herbs, stones,
moon phases, weekdays and numbers are replaced by placeholders, so changing
only those never makes a lesson new.

Concept signature, derived from the content (not tags): primary/secondary
method, intended purpose, primary and supporting tools, the ordered ritual
actions (verb + object per step), setting, timing, symbolic structure,
incantation theme and follow-up action.

Exact checks: document ID, slug, normalized title, normalized summary,
content hash and concept-signature hash.

Scores (deterministic; no embeddings required):

| Check | Method | Default rejection |
| --- | --- | --- |
| Title | character-trigram Dice on normalized titles | ≥ 0.85 |
| Summary | TF-IDF cosine | ≥ 0.82 |
| Guided instructions | TF-IDF cosine | ≥ 0.78 |
| Combined text | TF-IDF cosine | ≥ 0.82 |
| Ritual-action sequence | max(LCS sequence, set Jaccard) | ≥ 0.75 |
| Concept signature | weighted field similarity | ≥ 0.80 |

Two or more checks at ≥ 85% of their threshold also reject. A candidate must
differ from its closest match in at least four areas (method, action
sequence, skill, setting, structure, tool use, timing, reflection, follow-up,
safety, context, purpose). Thresholds are configurable but the defaults are
the floor for production batches.

## Commands (from `functions/`)

```bash
npm run content:validate                    # validate lessons/drafts/*.json; nonzero on any issue
npm run content:duplicates                  # check drafts against the whole repo inventory
npm run content:duplicates -- --legacy-audit  # also print the legacy practicality audit (JSON)
```

Nothing in this module writes to Firestore.
