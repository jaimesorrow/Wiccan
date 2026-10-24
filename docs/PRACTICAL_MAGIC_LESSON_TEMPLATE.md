# Wiccan — Practical Lesson and Grimoire Templates

Updated: October 20, 2026

Use this authoring target for the existing drafts and future lessons. It follows the October 19 Practical Magic Content and App Update Plan. Square brackets mark content to fill from inspected sources; remove optional sections only when their omission is justified in the editorial record.

## 1. Editorial basis

Every lesson teaches one distinct, performable Wiccan or witchcraft practice traceable to an inspected passage in an identifiable ancient grimoire or magical manuscript. Verify both ancient-source eligibility and Wiccan/witchcraft scope. Preserve the source's actual tradition and distinguish contemporary Wiccan applications from its historical identity.

Inspect the passage before writing. Retain exact locators, manuscript/catalogue identity, dating evidence, edition/translator, accessible location, and commercial-use rights findings. A broad museum overview, invented ritual with an attached citation, or an Original content attribution does not satisfy this requirement.

Use original, serious, clear explanatory prose. Identify source-supported instructions, uncertain evidence, and every modern adaptation. Do not invent missing materials, quantities, correspondences, timing, formulas, or an ancient Wiccan lineage. Hold practices that cannot be adapted safely while preserving their documented central method.

## 2. User-facing Lesson of the Day template

```text
LESSON OF THE DAY

[Distinct title naming the actual practice]
[Brief summary of the method and its spiritual or symbolic purpose]

Tradition: [Actual historical source tradition]
Contemporary application: [Wiccan/witchcraft application and adaptation, if applicable]
Category: [One practical category]
Secondary categories: [Optional; same approved list]
Time: [Honest practical estimate, when useful; identify prescribed timing separately]

INTRODUCTION
[Two or three sentences explaining what the reader will perform and the
specific teaching focus. Keep historical context concise.]

TOOLS AND MATERIALS
[Documented materials and quantities, if the passage provides them.]
[Clearly identify modern safety substitutions and what they change.]
[Use no-tools wording only when the supported method needs none.]

TIMING AND CORRESPONDENCES (when supported)
[State the passage's actual requirements and references.]
[Label modern associations or adaptations explicitly.]
[Omit this section when the source supplies none and no reviewed adaptation applies.]

THE LESSON — NUMBERED METHOD
[Approximately 250–450 words across the main explanation and method.
Do not pad incomplete evidence to meet the range.]

[Briefly explain the documented purpose and basis for the method.]

1. [Preparation that the passage actually prescribes, or labeled modern safety preparation.]
2. [First source-supported operation; clear enough to carry out.]
3. [Remaining documented operations in their actual order.]
[Continue only for steps the evidence supports.]

[Explain documented closing actions, if any. Identify modern cleanup,
storage, or disposal guidance separately. Keep departures from the source visible.]

INCANTATION OR FORMULA (source-dependent)
[Include only when the passage supports a formula suitable for this lesson.]
[Use a legally reusable excerpt with exact attribution, or identify a paraphrase.]
[If absent, omit this section and record the editorial reason. Do not invent a rhyme.]

REFLECTION
[One question specific to this practice and the reader's experience?]

PRACTICE
[One concise completion or observation prompt referring to the method above.]

SAFETY NOTE
[Activity-specific precautions for the materials and route of use.]
[Address relevant flame, smoke, plants, oils, water, stones, cords,
allergies, pets, children, or health concerns.]
[State whether a preparation is non-ingestible and how it may be used.]

SOURCE AND ADAPTATION NOTES
[Text title; manuscript/catalogue reference; historical origin and dating.]
[Exact chapter, page, column, folio, line, or spell number inspected.]
[Edition, translator/editor, publication date, and accessible source link.]
[Rights/attribution information appropriate for display.]
[Explain documented instructions, uncertainty, and modern changes.]

POTION SAFETY DISCLOSURE (when required)
[Render the shared disclosure from settings/disclosures.potionSafetyDisclosure
when requiresPotionDisclosure is true, alongside the preparation-specific safety note.]
```

The numbered method is the core lesson. The separate Practice section is a completion prompt, not a substitute for the main instructions. The same detail template applies to spells, potions, Today, and saved Grimoire entries; adapt the type label.

Omit unsupported timing or formulas rather than supplying conventional occult details. Label a shortened, flameless, substituted, or otherwise changed practice as an adaptation and explain its effect on the source method. Hold the lesson when the adaptation removes its defining technique.

## 3. Nine practical categories

Choose exactly one primary category by the technique taught. Optional secondary categories use this same list.

| Display name | Target ID |
| --- | --- |
| Candle Magic | `candle-magic` |
| Herbs & Botanicals | `herbs-botanicals` |
| Potions & Infusions | `potions-infusions` |
| Crystals & Stones | `crystals-stones` |
| Cord & Knot Magic | `cord-knot-magic` |
| Moon Magic & Timing | `moon-phases-timing` |
| Elemental Magic | `elemental-magic` |
| Altars & Sacred Space | `altars-sacred-space` |
| Divination | `divination` |

Protection, love, cleansing, abundance, and similar aims may remain as optional intention tags. Remove the old intention-based navigation groups. Do not convert a legacy intention category by a single lookup: inspect the actual supported method. Keep categories empty when qualifying source material has not been verified.

## 4. Editorial record and compatibility

These are authoring targets, not a claim that the current Android app or deployed Firestore already supports them. Extend the existing `content` contract compatibly; do not create a competing collection by default.

```text
id: [preserve existing document ID]
type: lesson | spell | potion
tier: free | premium
title: [distinct practice title]
slug: [stable readable slug]
tradition: [actual source tradition]
summary: [brief method and purpose]
introduction: [context and focus]
tools: [documented materials; explicitly labeled adaptations]
lesson: [source-grounded explanation and numbered method]
incantation: [source-supported formula or paraphrase; otherwise null]
reflection: [one question]
practice: [one completion or observation prompt]
safetyNote: [specific precautions; required wherever relevant]
primaryCategoryId: [exactly one of the nine target IDs]
secondaryCategoryIds: [optional list from the same nine IDs]
lessonType: [approved type; validate against the reviewed implementation enum]
requiresPotionDisclosure: [boolean under the established project policy]
durationMinutes: [honest estimate when applicable]
correspondences: [only sourced or explicitly labeled adaptations]
tags: [method and optional intention tags]
citations: [structured source records; finalize the shape during schema review]
contentStatus: draft
createdAt: [preserve original creation metadata]
updatedAt: [change only when the record changes]

Retain:
sourceId, sourceCitation, sourceUrl,
rightsReviewRequired, rightsNotes, similarityGroupId,
editorial annotations, curriculum placement, and unrelated metadata.
```

Map legacy `intro`, `body`, `materials`, and `status` deliberately to the new names, while keeping old documents readable. Define which representation is authoritative before implementing dual fields. Keep source/rights decisions intact. Document absent incantations, unresolved passages, scope decisions, and adaptation reviews in the editorial record; proposed new metadata names require schema review.

Do not require difficulty or experience labels. Keep free/premium access independent.

Set `requiresPotionDisclosure = true` for potion, tea, bath, oil, spray, jar, and skin-contact lesson types under the established project policy. The shared text comes from `settings/disclosures`, field `potionSafetyDisclosure`; do not replace it with per-lesson text or omit specific precautions.

## 5. Source and publication review

A draft is ready for editorial acceptance only after all applicable checks pass:

- The editor inspected the exact passage and verified its ancient dating, edition/translation, accessible location, and commercial-use rights.
- The practice passes Wiccan/witchcraft scope review with accurate attribution and explicit adaptations.
- One distinct method is sufficient to perform the practice; historical explanation supports the instructions.
- The primary category matches the method, not an intention or incidental object.
- Source fidelity, missing evidence, supported formulas, adaptations, reflection, and practical completion are checked.
- Activity-specific safety and shared potion disclosure are correct. Verify specialist guidance rather than inventing doses or safety claims.
- No essential-oil ingestion, coercive targeting, unsupported medical claims, or guaranteed outcomes are introduced.
- Wording and central concept have been compared with other lessons. A changed color, ingredient, or intention alone does not make a new lesson.

Keep unresolved items blocked for review. A populated citation or a passing structural validator does not prove historical accuracy or publication readiness.

All migrated records remain drafts. Production writes require a verified inventory, secure backup with restore instructions, a reviewed change set, conflict protection, and approval. Publication and rules/Functions deployments are separate actions.

## 6. Grimoire template

The user-facing collection is **Grimoire**, with **Timeline** and **Categories** views.

Each delivered-entry row contains:

- Title and content type.
- Practical primary category when reviewed and available.
- Source tradition.
- Date delivered.
- A link to the same complete detail layout used by Today.

Timeline sorts newest first. Categories groups received entries under the nine practical categories with collapsible sections. Retired categories require a compatible, reviewed migration or fallback presentation; saved entries must remain reachable.

Do not rewrite stored history merely to rename the UI. Preserve content IDs, delivery dates, source/similarity metadata, curriculum order, and account ownership. Keep one trusted daily assignment stable across retries, devices, widgets, and notifications. Exclude previously delivered IDs and editorial similarity groups; show an explicit exhausted state without recycling old lessons.

Empty copy: "Your Grimoire is empty. Open Today to receive your first lesson."

## 7. Implementation state — October 20, 2026

The checked-in seed at commit `3ea0f2c6cbed90b8dc353d85b9126122ca73b31d` has 296 draft records: 200 lessons, 59 spells, and 37 potions. It still uses the eight legacy intention categories. Structured reflection and practice are already separate fields in the current code; the older template's snapshot caveat is obsolete.

The nine-category practical schema and this complete source-verification template still require coordinated implementation in the generator, validators, importer, Kotlin mapping/models, Room, and shared Compose detail UI. Updating this template does not rewrite or publish database content.
