# Ancient Lesson Sourcing

`Lesson of the Day` content is historical-source-grounded. It must teach from curated ancient material rather than generic modern occult prose presented as ancient tradition.

## Historical framing

Wiccan is a modern product. Ancient Egyptian, Mesopotamian, Greek, Roman, and other pre-modern religious-magical traditions must be presented on their own terms. The app must not describe them as "ancient Wicca" or claim an unbroken Wiccan lineage from antiquity.

A modern reflection may be inspired by ancient material, but it must be labeled as a modern reflection or adaptation rather than an authentic reconstruction.

## Enforcement

The content pipeline enforces this in three places:

1. `functions/sources.json` contains manually curated source metadata, provenance, and `groundedFacts`.
2. `functions/src/contentSource.ts` rejects any `lesson`-eligible source whose `era` is not `ancient`.
3. `functions/src/approveDraft.ts` refuses to publish a lesson unless its `sourceId` resolves to a curated, ancient, lesson-eligible source with grounded historical facts.

The generation prompt treats `groundedFacts` as the boundary for historical claims. It may explain them in original language, but it must not invent ancient quotations, dates, deities, ritual steps, lineages, or unsupported claims.

## Initial ancient source set

### Ancient Egyptian heka and amulets

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

`functions/ancientContentSeed.json` holds 150 hand-reviewed entries written
against the four ancient sources above: 54 lessons, 59 spells, 37 potions.
Entries have no reader experience label; their existing `tier` access remains
50 free entries and 100 premium entries.

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
npm run seed-content -- --commit  # upsert into the selected project
```

Document IDs are deterministic, so a re-run updates the same 150 documents
rather than creating duplicates. Every entry is seeded as `status: "draft"`,
and an already-published document keeps its status - seeding fills the pool
and never publishes. Publication stays a per-document `approve-draft` action.
The upsert removes the retired `difficulty` field on those same seed documents;
it preserves their other fields and every user's delivered-content history.
Confirm the printed project ID before committing a write.

### Rights review

54 entries carry `rightsReviewRequired: true`: they either retain a short
phrase from a modern translation or derive from museum prose whose reuse terms
have not been confirmed (the two Metropolitan Museum amulet entries). Under the
copyright policy above, clear those rights or rewrite the phrasing before any
of those entries is approved for publication.

### Editorial similarity groups

`similarityGroupId` is a stable editorial identity for a lesson or narrow
concept. Rewordings, alternate titles, and substantially overlapping entries
share that identity, including entries with different types or tiers. A new
title or billing tier must not create a new identity for the same lesson.

All seed entries have an explicit identity. The reviewed overlapping pairs
share one: water transferred over cippi (1/4), nightmare preparations (13/49),
dawn handwashing (22/34), divination dismissal (56/130), foreign-language
healing formulas (59/71), personal traces (65/127), white-wool knot release
(80/86), and the aromatic torch bundle (115/145). Other entries retain
individual identities. These groups identify substantial overlap, not an
entire tradition, category, manuscript, or museum article. For example, the
wedjat/heart-scarab lesson and the lesson on an amulet's sources of power use
the same article but have different identities.

When generating a revision, the admin CLI accepts
`--similarityGroupId <existing-group>` and `--sourceCitation <precise-passage>`.
Editors should review every new draft for overlap and retain or assign its
identity before publication. Source citations, links, rights notes, safety
notes, reflection, and optional practice remain part of the stored lesson.

## Existing published lessons

This code change governs newly generated and newly approved lessons. It does **not** automatically rewrite or unpublish existing Firestore documents that were published before this policy existed.

Before relying on the policy for production, audit existing published `content` documents where `type == "lesson"`. Any lesson that is not traceable to an approved ancient `sourceId` should be moved back to `draft`, rewritten against a curated ancient source, reviewed, and approved again.
