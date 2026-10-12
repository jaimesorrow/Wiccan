# Wiccan — Content Voice & Lesson Standard

This document is authoritative for Wiccan editorial content, generated drafts, validation, seed content, previews, and lesson rendering.

## Voice

Wiccan content must feel grounded, mature, historically aware, practical, reflective, and subtly grimoire-inspired. It should be serious rather than theatrical, clear enough for a modern reader, and respectful of different traditions and cultures.

Avoid cheesy fantasy prose, fake antiquated English, sensational occult language, guaranteed supernatural outcomes, invented historical claims, medical diagnosis/treatment claims, presenting folklore as scientific fact, or fear-based spiritual claims.

## Canonical lesson structure

Use this structure whenever it fits:

```text
Lesson of the Day — [Title]

Tradition: [Tradition]
Category: [Category]

Introduction
A concise factual or contextual opening.

Lesson
A substantial, readable explanation of the practice, symbolism, history, or framework. Clearly distinguish historical/customary claims, modern practice, symbolism, and personal reflection.

Reflection
One thoughtful question that connects the lesson to the reader's own practice or life.

Practice
Optional. One safe, simple action the reader can try.

Safety Note
Include whenever materials, ingestion, flame, smoke, skin contact, children, pets, plants, health, or another meaningful risk is involved.
```

Do not force Practice or Safety Note into a lesson when neither is appropriate.

## Preferred production data representation

```text
id
type = lesson
tier = free | premium
tradition
category
title
intro
body
reflection
practice?       // nullable
materials?      // nullable
safetyNote?     // nullable
tags
sourceId
sourceCitation?
sourceUrl?
rightsReviewRequired?
rightsNotes?
similarityGroupId?  // stable across rewordings of the same lesson
status = draft | published | withdrawn
```

If adding `reflection` and `practice` requires a schema migration, implement it deliberately across Kotlin domain models, Firestore mapping, Room, Cloud Functions types, generation, validation, Grimoire/detail rendering, and tests. Do not bury structured metadata inside `body` merely to avoid a migration.

Lessons have no experience-level field or label. Keep source and rights
metadata alongside the historical lesson, and use the same editorial
`similarityGroupId` when revising or substantially rewording it. Distinct
lessons from one broad source need distinct identities. See
`docs/ANCIENT_LESSON_SOURCING.md` for the seed's reviewed overlap groups.

## Quality anchors

### The Threshold

**Tradition:** Folk Practice  
**Category:** Home & Hearth

**Introduction**  
Every dwelling has a threshold. It is both an entrance and a boundary: a place where one space ends and another begins. In traditional folk practice, thresholds were treated with care because they symbolized consent, protection, and passage.

**Lesson**  
A threshold need not be mystical to carry meaning. It is a deliberate point of transition.

When you cross into your home, you may choose to treat that moment with intention. Pause before entering. Notice what you are carrying inward with you—anger, exhaustion, worry, noise from the day.

A simple household rite is to place your hand lightly against the doorframe and consider what you want to bring into your private space. No supernatural claim is required. The value is in establishing a repeated boundary between the outer world and your home.

Historically, people in different places have marked doors and entrances with protective symbols, herbs, iron, salt, prayer, or other customs. Describe such practices carefully and avoid presenting one culture's practice as universal.

**Reflection**  
What do you want your home to feel like when you cross its threshold?

**Practice**  
Tonight, pause at your door before entering. Take one breath and choose one quality you want to bring inside with you.

### The Waxing Moon

**Tradition:** Modern Wiccan Practice  
**Category:** Moon & Seasonal

**Introduction**  
The waxing moon is the period between the new moon and the full moon, when the illuminated portion visible from Earth appears to grow.

**Lesson**  
Within many modern magical traditions, the waxing moon is used symbolically for increase, development, and gradual strengthening.

The important distinction is that Wiccan must not state that the moon forces a desired event to occur. Its visible cycle can instead provide practitioners with a recurring structure for reflection and intentional action.

A waxing-moon practice might focus on learning a skill, rebuilding confidence, saving money, strengthening a habit, tending a relationship, or developing a creative project.

A useful grimoire entry records both intention and action. For example: intention—greater discipline; action—twenty minutes of study each evening; observation—note resistance and progress over the cycle.

**Reflection**  
What in your life should be strengthened slowly rather than forced quickly?

### Salt and Symbolic Boundaries

**Tradition:** Folk Practice  
**Category:** Protection

**Introduction**  
Salt has carried symbolic significance in many cultures because it preserves food, has historically been valuable, and is associated with preservation and cleanliness.

**Lesson**  
In some folk-magical practices, salt is used as a symbol of boundary and protection. State this as a cultural or symbolic practice, not as scientifically established supernatural protection.

A small dish may serve as a deliberate reminder of household boundaries. The meaning is more important than using large quantities.

Avoid scattering large amounts outdoors because excess salt can damage soil and plants.

**Reflection**  
Where would stronger boundaries improve your life?

**Practice**  
If this symbolism is meaningful to you, place a small amount of salt in a dedicated indoor dish and use seeing it as a prompt to reflect on your boundaries.

**Safety Note**  
Keep ritual materials where children and pets cannot ingest them. Avoid applying salt directly to soil or vegetation.

## Spells and potions

Use the same editorial philosophy for spells and potions:

- distinguish symbolism/tradition from established physical effects;
- provide clear materials and steps;
- include safety notes when relevant;
- never promise guaranteed love, wealth, healing, protection, or another supernatural result;
- never substitute ritual for medical, legal, financial, or emergency care;
- do not recommend unsafe ingestion or unidentified plants;
- use “potion” carefully: many entries should be non-ingestible ritual preparations unless every ingredient and use is demonstrably food-safe.

## Generation rule

The draft-generation system may use only an editor-approved source classified
as an ancient grimoire or ancient magical manuscript. All generated content
types must remain traceable to that source's curated facts; modern occult
books, generic model knowledge, and unlisted sources are outside the factual
boundary. Generated content that violates this standard must fail validation
before it is written.

Every generated item is written with `status = draft` and
`rightsReviewRequired = true` until manually reviewed and approved.

## UI rule

Lessons must display the hierarchy clearly:

- type / tradition / category;
- title;
- introduction;
- lesson;
- reflection;
- optional practice;
- optional safety note.

No physical photography is used for lesson artwork. Use restrained line-art matching the Wiccan visual system.

## Acceptance criteria

- Generated lessons read like the quality anchors above.
- Reflection is meaningful rather than generic filler.
- Historical statements are qualified appropriately.
- Symbolic claims are not presented as objective supernatural facts.
- Safety notes appear when risk warrants them.
- UI renders all lesson sections cleanly.
- Grimoire preserves the same content structure.
- Tests cover serialization/migration for new structured fields.
- Generator validation rejects unsafe or sensational content.
