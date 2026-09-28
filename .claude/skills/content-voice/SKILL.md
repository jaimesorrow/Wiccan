---
name: content-voice
description: Wiccan's editorial standard for writing, reviewing, or generating spell/potion/lesson content — required voice, the Introduction/Lesson/Reflection/Practice/Safety-Note structure, when a safety note is mandatory, and what claims must be qualified. Use before drafting new content by hand, reviewing a generated draft, or changing the generation prompt/validation in functions/src/. Not for UI, theming, or backend infrastructure changes.
---

# Wiccan content voice & lesson standard

Full source of truth: `docs/CONTENT_VOICE_AND_LESSON_STANDARD.md` — read
it before generating or reviewing a non-trivial amount of content; this
skill is the condensed, actionable version for day-to-day drafting.

## Voice

Grounded, mature, historically aware, practical, reflective, subtly
grimoire-inspired. Serious, not theatrical.

**Never**: cheesy fantasy prose, fake antiquated English, sensational
occult language, guaranteed supernatural outcomes, invented historical
claims, medical diagnosis/treatment claims, folklore presented as
scientific fact, fear-based spiritual claims.

## Canonical lesson structure

```
Lesson of the Day — [Title]
Tradition: [Tradition]
Category: [Category]

Introduction   — concise factual/contextual opening
Lesson         — the substantial explanation; clearly separate
                 historical/customary claims from modern practice,
                 symbolism, and personal reflection
Reflection     — one thoughtful question connecting it to the reader's
                 own life/practice — meaningful, never generic filler
Practice       — OPTIONAL: one safe, simple action to try
Safety Note    — REQUIRED whenever materials, ingestion, flame, smoke,
                 skin contact, children, pets, plants, health, or
                 another meaningful risk is involved
```

Don't force Practice or Safety Note when neither actually applies. Spells
and potions follow the same philosophy (see the full doc's "Spells and
potions" section) — distinguish symbolism from established physical
effect, give clear materials/steps, never promise a guaranteed result,
never substitute ritual for medical/legal/financial/emergency care, and
treat "potion" carefully: non-ingestible unless every ingredient and use
is demonstrably food-safe.

## Non-negotiable qualification rules

- Historical/customary claims get qualified ("in some folk-magical
  practices...", "historically, people in different places have...") —
  never stated as universal fact, and never attributed to one culture as
  if it speaks for all similar practices.
- Symbolic/magical claims are framed as symbolic or traditional, never as
  objectively established supernatural fact (e.g. the waxing moon gives
  a *structure* for intention and action, it does not *cause* an
  outcome).
- Respect for different traditions and cultures throughout.

## Where this applies in code

- `functions/src/claudeDraftGenerator.ts`'s system prompt must explicitly
  encode this standard — a change to the prompt is a change to editorial
  policy, treat it with the same care as changing the doc itself.
- Generated content that violates the standard must **fail validation**,
  never be silently published — every generated item stays
  `status = draft` until a human runs `npm run approve-draft`.
- UI (`ContentDetailView` and friends) must render the full section
  hierarchy — type/tradition/category, title, introduction, lesson,
  reflection, optional practice, optional safety note — and use
  restrained line-art for lesson artwork, never physical photography.

## Before calling a draft acceptable

- Reads like the quality anchors in `docs/CONTENT_VOICE_AND_LESSON_STANDARD.md`
  ("The Threshold", "The Waxing Moon", "Salt and Symbolic Boundaries").
- Reflection is specific to this content, not swappable filler.
- Every historical statement is qualified; every symbolic claim reads as
  symbolic, not literal.
- A safety note is present if-and-only-if real risk is involved.
- If the schema needs a new field (e.g. `reflection`, `practice`), that's
  a deliberate migration across Kotlin domain models, Firestore mapping,
  Room, Cloud Functions types, generation, validation, and
  Archive/detail rendering — don't bury structured data inside `body`
  just to dodge the migration.
