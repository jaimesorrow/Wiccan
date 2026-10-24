# Wiccan — Content Voice and Practical Lesson Standard

Updated: October 20, 2026

This is the editorial target for existing draft rewrites and future lessons, spells, potions, authoring prompts, validators, and shared detail screens. It supersedes the older historical-overview examples, intention categories, and optional standalone reflection exercises in earlier documentation and skill summaries.

The complete fill-in template and ten-category table are in [Practical Lesson and Grimoire Templates](PRACTICAL_MAGIC_LESSON_TEMPLATE.md). Updating these documents does not establish that the live database, generator, validators, or Android schema already implement the target.

## Source and scope

Every daily entry must teach one distinct magical practice traceable to an inspected passage in an identifiable ancient, medieval, or early-modern grimoire or magical primary-source manuscript. Verify both pre-modern-source eligibility and Wiccan/witchcraft scope. Set `tradition` to `Wiccan / Modern Witchcraft`, preserve the actual historical tradition in `sourceTradition`, and label the contemporary application as an adaptation.

Inspect the passage before authoring. Keep its title, manuscript/catalogue identity, documented origin and dating evidence, exact locator, edition and translator/editor, accessible source location, and commercial-use rights findings. Distinguish manuscript date, estimated composition date, and modern edition/translation date. Do not infer antiquity from legendary attribution or treat all historical works as ancient.

An old text does not establish rights to a modern translation or image. A broad catalogue overview, an AI summary, or a populated citation field does not prove passage verification. Do not generate a method first and attach a source afterward.

If source evidence, dating, scope, rights, or a safe faithful adaptation is unresolved, retain a blocked draft and record the reason. Do not fabricate a source, rebrand an unrelated practice, invent an ancient Wiccan lineage, or substitute an unrelated exercise to fill a numeric or category quota.

## Voice

Use clear, original, mature, inclusive explanatory prose. Explain the specific practice substantively. Qualify historical claims, keep uncertainty visible, and frame magical associations as spiritual or symbolic practice.

Exclude theatrical prose, fake antiquated English, stage magic, party activities, free-standing self-help presented as sourced spells, coercive targeting, fear-based claims, invented provenance, guaranteed outcomes, and unsupported medical claims.

## Required lesson hierarchy

1. Distinct title and brief summary.
2. `Wiccan / Modern Witchcraft` lesson tradition plus the actual source tradition, origin and dating; clearly identified contemporary application.
3. Exactly one practical primary category; optional valid secondary categories.
4. Introduction explaining what the reader will perform.
5. Tools/materials and any source-supported timing or correspondences; label adaptations.
6. Main lesson, approximately 250–450 words, centered on a coherent numbered method.
7. Source-supported incantation or formula when appropriate.
8. One specific reflection question.
9. One concise practice/completion prompt pointing back to the method.
10. Activity-specific safety note.
11. Exact source, edition/translation, rights, uncertainty and adaptation notes.
12. Shared potion disclosure when required.

The numbered method is the core lesson. Give enough detail to perform it without a second article. Include documented preparation and sequence; explain documented closing when present and label modern cleanup/storage/disposal guidance. Do not invent ceremonial opening or closing steps.

Do not pad sparse evidence to meet a word range. Do not add conventional colors, moon phases, ingredients, quantities, invocations, or correspondences absent from the evidence. Label any reviewed modern addition clearly.

A formula is source-dependent. Use only suitable, legally reusable wording with attribution, or an identified paraphrase. Omit the section when the source supplies none and record the editorial reason. An original rhyme is not mandatory.

## Practical categories

Use a flat list:

- Candle Magic — `candle-magic`
- Herbs & Botanicals — `herbs-botanicals`
- Potions & Infusions — `potions-infusions`
- Crystals & Stones — `crystals-stones`
- Cord & Knot Magic — `cord-knot-magic`
- Moon Magic & Timing — `moon-phases-timing`
- Elemental Magic — `elemental-magic`
- Altars & Sacred Space — `altars-sacred-space`
- Divination — `divination`
- Protection, Cleansing & Warding — `protection-cleansing-warding`

Choose by the actual source-supported technique. Protection, love, cleansing, abundance and other aims can be optional tags. Retire the intention navigation groups. Review legacy categories per record; do not map an old intention to a new technique automatically. Keep unsupported category slots empty.

## Safety and disclosure

Keep practices self-directed or consent-based. Identify a preparation's permitted route of use and favor non-ingestible preparations during the rewrite. Do not turn unsafe historical recipes into modern instructions.

Provide relevant precautions for flame, smoke, botanicals, oils, stones, water, cords, allergies, children and pets. Avoid essential-oil ingestion, tight bindings, cords near flames, hazardous unattended materials, invented doses, and unsupported ingredient-safety claims. Verify specialized guidance against authoritative sources during actual lesson preparation. Treat unresolved activity risks as review blockers.

Under the established project policy, potion, tea, bath, oil, spray, jar and skin-contact lesson types require `requiresPotionDisclosure = true`. Render `settings/disclosures.potionSafetyDisclosure` in addition to the lesson-specific safety note.

## Data and compatibility

Preserve document IDs, creation metadata, legitimate source/rights decisions, similarity identities, editorial annotations, curriculum placement, and delivered history. Keep the existing `content` collection compatible unless a reviewed architecture decision requires otherwise.

The current legacy contract includes `intro`, `body`, `materials`, `category`, and `status`. Structured reflection and practice already exist. The authoring target adds the practical fields described in the linked template, including `sourceTradition`, `summary`, `primaryCategoryId`, `lessonType`, `requiresPotionDisclosure`, and structured citations. Deliberately map aliases such as `intro/introduction`, `body/lesson`, `materials/tools`, and `status/contentStatus`. Finalize new metadata shapes in schema review rather than silently inventing serializers.

Implement the target across generation, validation, import, Kotlin models/mapping, Room/caching, shared Compose detail rendering and meaningful regression tests before live content migration. Do not bury new structured sections in `body` to avoid the migration. Remove difficulty and experience gates while preserving free/premium access.

## Grimoire and artwork

Use **Grimoire**, with Timeline and Categories views. Today and received entries use the same full detail layout. Preserve saved entries during category transitions, stable assignment ownership/dates, no-repeat reservations, similarity exclusions, and clear exhausted/empty states.

Use restrained Wiccan line-art within the existing six-palette system. Preserve accessibility, themes, widgets, notifications, subscriptions, privacy, and account deletion.

## Review and migration

Review distinct central technique and sequence as well as wording. Changing only a color, ingredient, or intention does not establish a new lesson.

Before live content changes: verify the inventory, create a secure backup with restore instructions, prepare reviewed drafts and a per-record before/after report, preview against current document versions, and obtain approval for the concrete change set. Apply only approved fields with conflict protection and unchanged-record skips.

All migrated content remains draft. Source verification, scope, rights, safety and editorial approval are separate from structural validation. Publication, Cloud Functions deployment, and Firestore rules deployment remain separate actions.
