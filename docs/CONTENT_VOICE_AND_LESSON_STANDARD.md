# Wiccan content voice and practical lesson standard

Updated: November 7, 2026

This is the editorial standard for daily lessons, the content generator, validators, migrations, and the shared Today/Grimoire detail screen.

## Narrative

Wiccan teaches practical modern Wiccan practice. Each daily lesson guides one specific activity: a candle spell, herbal ritual, cord or knot practice, lunar practice, charm, crystal working, divination exercise, altar practice, elemental practice, or ritual preparation.

Historical grimoires and magical manuscripts may provide inspiration or context only after the exact passage, edition, rights, and factual claim have been verified. Preserve the source's real tradition. Never call an ancient or pre-modern practice Wicca, imply an ancient Wiccan lineage, or create a ritual first and attach a citation afterward.

Lessons use original, warm, mature, inclusive prose. They are mystical but clear, practical rather than theatrical, and accessible without becoming childish. Magical results are framed as spiritual, symbolic, devotional, meditative, or intention-focused practice. Never guarantee supernatural, romantic, medical, financial, or legal outcomes.

## Required lesson hierarchy

1. Type, distinct title, tradition, and practical category chips.
2. Brief summary of what the user will learn and perform.
3. Introduction with only the context necessary to understand the practice.
4. Tools and honest substitutions.
5. Complete numbered guided method with preparation, performance, closure, and safe handling.
6. Optional original incantation, clearly distinguished from historical quotations.
7. One experience-specific reflection question.
8. One small follow-up practice.
9. Activity-specific safety note.
10. Populated correspondences only when supported or clearly labeled as modern associations.
11. Citations for historical, scientific, botanical, material-safety, or medical claims.
12. Shared potion disclosure whenever `requiresPotionDisclosure` is true.

Do not turn the lesson into a general history article. The numbered practice is the core. Omit optional sections cleanly when they do not apply.

## Practical categories

New content uses exactly one primary method-based category:

- Candle Spells — `candle-spells`
- Herbal Rituals — `herbal-rituals`
- Cord & Knot Work — `cord-knot-work`
- Lunar Practices — `lunar-practices`
- Charms & Talismans — `charms-talismans`
- Crystals & Stones — `crystals-stones`
- Divination — `divination`
- Altars & Sacred Space — `altars-sacred-space`
- Elemental Practices — `elemental-practices`
- Potions & Infusions — `potions-infusions`

Grounding, protection, cleansing, confidence, rest, abundance, love, and similar purposes are tags, not primary navigation categories. Legacy IDs remain readable during migration but are not valid choices for newly generated lessons.

## Safety and consent

Keep practices self-directed or explicitly consent-based. Reject coercive love work, attempts to control another person, fear-based claims, medical treatment claims, and certainty about another person's thoughts or the future.

Add specific precautions for flame, smoke, ventilation, plants, oils, skin contact, baths, water, stones, cords, allergies, pregnancy or nursing, medications, children, animals, sharp tools, outdoor collection, storage, and disposal when relevant.

Never recommend ingesting essential oils. Clearly label non-food preparations: `Ritual use only. Do not consume.` Any potion, tea, bath, oil, spray, jar, or skin-contact lesson sets `requiresPotionDisclosure = true` and renders `settings/disclosures.potionSafetyDisclosure` alongside its specific safety note.

## Originality

A changed title, color, crystal, herb, moon phase, room, number of knots, incantation, paragraph order, or intention does not make a new lesson. Compare the practical method, action sequence, tools, setting, symbolic structure, follow-up practice, wording, source passage, and similarity group.

The generator inventories all draft and published records available to it. It screens every candidate against that inventory and candidates accepted earlier in the same run. Exact title, slug, authored-content, and concept fingerprints are atomically reserved before a draft is created. A failed screen remains rejected; the generator creates a genuinely different concept or stops when the safe concept pool is exhausted.

## Status and publication

Generated lessons are drafts requiring human source, safety, rights, scope, and duplicate review. The generator cannot publish. Firestore publication, migration, rules deployment, Functions deployment, and Android release remain separate operator actions.

Preserve document IDs, creation metadata, delivered history, legitimate source decisions, similarity identities, and editorial annotations. Do not delete or merge existing content automatically.
