# Ancient Lesson Sourcing

AI-authored content is ancient-grimoire-grounded. Every generated lesson,
spell, and potion must start from an editor-approved ancient grimoire or
ancient magical manuscript rather than generic modern occult prose.

## Historical framing

Wiccan is a modern product. Ancient Egyptian, Mesopotamian, Greek, Roman, and other pre-modern religious-magical traditions must be presented on their own terms. The app must not describe them as "ancient Wicca" or claim an unbroken Wiccan lineage from antiquity.

A modern reflection may be inspired by ancient material, but it must be labeled as a modern reflection or adaptation rather than an authentic reconstruction.

## Enforcement

The content pipeline enforces this in four places:

1. `functions/sources.json` contains manually curated source metadata,
   provenance, `groundedFacts`, and a `sourceKind` classification.
2. `functions/src/generateDraft.ts` permits every generated content type only
   when `era == ancient` and `sourceKind == ancient-grimoire`.
3. Model output is structurally validated before a Firestore write and is
   always saved with `status = draft` and `rightsReviewRequired = true`.
4. `functions/src/approveDraft.ts` refuses publication while rights review is
   outstanding and applies its safety/completeness/source checks.

The generation prompt treats `groundedFacts` as the boundary for historical
claims. It may explain them in original language, but it must not introduce
material from modern occult books or unlisted sources, or invent ancient
quotations, dates, deities, ritual steps, lineages, or unsupported claims.

## Initial ancient source set

### Ancient Egyptian heka and amulets

This record is classified as `ancient-historical-context`, not an ancient
grimoire. It remains provenance for existing seed drafts but the AI content
agent cannot generate from it.

Editorial references:

- British Museum — *Heka: magic in ancient Egypt*: https://www.britishmuseum.org/our-work/international/international-touring-exhibitions/heka
- Metropolitan Museum of Art — *Ancient Egyptian Amulets*: https://www.metmuseum.org/essays/egyptian-amulets

Grounded lesson themes include the historical role of heka, amulet materials and symbolism, textual amulets, spoken words, and ritual action. These beliefs are described historically and are not presented as scientifically proven protection or medical treatment.

### Maqlû

Editorial reference:

- ORACC Corpus of Mesopotamian Anti-witchcraft Rituals (CMAwR), including the Maqlû ritual tablet Q002713: https://oracc.museum.upenn.edu/cmawro/maqlu/Q002713/html

Grounded lesson themes include incantation, purification, symbolic work with figurines, fire, and invocation of divine powers in ancient Mesopotamian anti-witchcraft ritual. Historical descriptions of burning, illness, or hostile magic are not converted into harmful modern instructions.

### London-Leiden Demotic Magical Papyrus

Editorial reference:

- British Museum EA10070: https://www.britishmuseum.org/collection/object/Y_EA10070-2

Grounded lesson themes include a third-century CE Egyptian manuscript containing spells, ritual recipes, divinatory procedures, invocations, Demotic writing, Old Coptic glosses, and Greek recitation. Unsafe, poisonous, coercive, or medical recipes may be discussed historically but are never turned into modern instructions.

### Harris Magical Papyrus

Editorial reference:

- British Museum EA10042: https://www.britishmuseum.org/collection/object/Y_EA10042-8

Grounded lesson themes include New Kingdom Egyptian written ritual literature, hymns, incantations, and the preservation of ritual speech in papyrus collections.

## Copyright and translation policy

Ancient works and historical facts are not treated as permission to copy a modern translation. Wiccan uses independently written factual summaries and does not reproduce or closely paraphrase copyrighted modern translations, museum prose, or scholarly editions.

If a direct quotation is ever added, its edition and rights status must be verified separately before publication.

## Safety and cultural integrity

Historical accuracy does not make an ancient practice safe to reproduce. Lessons must not turn historical descriptions into instructions involving poison, unsafe ingestion, coercion, self-harm, harm to others, dangerous fire use, or unsupported medical treatment.

The app should distinguish:

- what the ancient source or artifact attests;
- what scholars infer or debate;
- what is symbolic or religious belief;
- what is a modern reflection inspired by the material.

When evidence is fragmentary or uncertain, say so rather than filling gaps.

## The checked-in seed pool

`functions/ancientContentSeed.json` holds 296 entries written against the four
ancient sources above: 200 lessons, 59 spells and 37 potions. Of these, 84 are
grounded in the historical-context heka/amulet record and therefore predate the
stricter grimoire-only generation boundary; they are not silently relabeled as
grimoire-derived. The seed preserves the original 150-entry pool and adds 146
companion lesson drafts. Entries have
no reader-experience label; `tier` remains a separate access property with
100 free entries and 196 premium entries.

Every entry follows the canonical structure in
`docs/CONTENT_VOICE_AND_LESSON_STANDARD.md` (`intro`, `body`, `reflection`,
optional `practice`, optional `safetyNote`) and carries `sourceCitation` and
`sourceUrl` so the citation block can be rendered with the entry. Spells and
potions are written as historical records with a safety note, never as
instructions to perform; the four ancient sources' `eligibleTypes` were
widened to `lesson`, `spell` and `potion` to reflect that.

`functions/src/contentSeed.ts` validates the file and applies the same
structural gates as `approveDraft`. `functions/src/__tests__/contentSeed.test.ts`
runs those gates against the real checked-in file in CI, so an edit that breaks
the pool fails the build.

### Seeding

```
cd functions
npm run seed-content              # validate only, writes nothing
npm run seed-content -- --project wiccan-android --commit
```

Document IDs are deterministic. The guarded importer preflights duplicate
titles/bodies, creates missing entries as `status: "draft"`, and preserves the
status and authored fields of existing drafts or published documents. For
existing seed IDs it only synchronizes `similarityGroupId` and removes the
retired `difficulty` field. It then performs a read-only inventory check for
all 200 lesson IDs. Publication stays a per-document `approve-draft` action,
and no delivered-content history is changed. The remote path refuses any
project other than the explicit `wiccan-android` target.

### Rights review

106 entries carry `rightsReviewRequired: true`: the original 54 rights flags
plus 52 conservatively inherited flags on companion lessons. Retained source
citations do not establish reuse permission. Clear each flag through an
editorial rights review or rewrite before approving the entry for publication.

### Editorial similarity groups

`similarityGroupId` is a stable editorial identity for a lesson or narrow
concept. Rewordings, alternate titles, and substantially overlapping entries
share that identity, including entries with different types or tiers. A new
title or billing tier must not create a new identity for the same lesson.

All seed entries have an explicit identity. Each companion lesson shares its
source entry's identity. The reviewed overlapping pairs also share one across
their complete companion clusters: water transferred over cippi (1/4),
nightmare preparations (13/49), dawn handwashing (22/34), divination dismissal
(56/130), foreign-language healing formulas (59/71), personal traces (65/127),
white-wool knot release (80/86), and the aromatic torch bundle (115/145).
Other entries retain individual identities. These groups identify substantial
overlap, not an entire tradition, category, manuscript, or museum article. For
example, the wedjat/heart-scarab lesson and the lesson on an amulet's sources
of power use the same article but have different identities.

When generating a revision, the admin CLI accepts
`--similarityGroupId <existing-group>` and `--sourceCitation <precise-passage>`.
Editors should review every new draft for overlap and retain or assign its
identity before publication. Source citations, links, rights notes, safety
notes, reflection, and optional practice remain part of the stored lesson.

Previewing is the default and performs no paid API call or database write:

```
cd functions
npm run content-agent -- --tier free --type lesson --count 3
npm run content-agent -- --project wiccan-android --commit --tier free --type lesson --count 3
```

## Existing published lessons

This code change governs newly generated and newly approved lessons. It does **not** automatically rewrite or unpublish existing Firestore documents that were published before this policy existed.

Before relying on the policy for production, audit existing published `content` documents where `type == "lesson"`. Any lesson that is not traceable to an approved ancient `sourceId` should be moved back to `draft`, rewritten against a curated ancient source, reviewed, and approved again.
