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

`functions/ancientContentSeed.json` contains **296 draft entries: 200 actual
lessons, 59 spells, and 37 potions**. The original 150 entries contained only
54 lessons; total content count must never be used as the lesson count.
Those original entries and IDs are preserved. An additional 146 newly authored,
**unreviewed lesson drafts** provide original historical-reading commentary
against the cited records already documented in that pool. These additions
need human historical, cultural, safety, and rights review before publication;
structural validation is not editorial approval.

Every entry follows the canonical structure in
`docs/CONTENT_VOICE_AND_LESSON_STANDARD.md` (`intro`, `body`, `reflection`,
optional `practice`, optional `safetyNote`) and carries `sourceCitation` and
`sourceUrl`. The additional lessons discuss interpretation and evidence rather
than providing procedures for the historical spells or hazardous preparations.
The existing spells and potions remain historical records with safety notes.
The four ancient sources remain manually curated; no new source or invented
citation was introduced to meet the count.

`functions/src/contentSeed.ts` validates the file, rejects duplicate IDs,
titles and bodies, and applies structural and source gates. Tests check the
actual `type == "lesson"` count and substantive bodies for the additions.
The generator also supplies `reflection` and `practice` as separate fields,
so new generated lessons use the same representation as the checked-in pool.

### Validate, import, and verify production content

```
cd functions
npm run seed-content
npm run seed-content -- --project wiccan-android --commit
npm run verify-lessons -- --project wiccan-android
```

The first command validates the local file without accessing Firebase. The
other two require Application Default Credentials with permission to read
production Firestore; importing also requires permission to create documents.
Authenticate in your own trusted environment, for example using
`gcloud auth application-default login`, or point
`GOOGLE_APPLICATION_CREDENTIALS` at an existing authorized credential file.
Never put credential files in this repository or paste private keys into chat.

These tools explicitly target **`wiccan-android`**, require that exact
`--project` argument for remote actions, and refuse to use
`FIRESTORE_EMULATOR_HOST` as production verification. They do not rely on
`.firebaserc` aliases or an implicit default project.

Document IDs are deterministic. The importer creates missing draft documents
inside Firestore transactions and **preserves every field of every existing
document**, including edited drafts and published bodies. It reports preserved
entries whose text differs from the checked-in copy. A seed ID attached to an
existing document with a different type or source aborts its transaction;
read-only preflight also refuses a missing seed ID that would duplicate an
existing title or body under another ID. Resolve a collision rather than
overwriting production data. No import
publishes or deletes anything.

Remote verification reports the actual lesson count, total content count,
lesson counts by status and tier, outstanding rights review, missing manifest
lesson IDs, additional lesson IDs, duplicate titles/bodies, and structural/source problems. It requires
all 200 manifest lesson IDs and at least 200 actual lessons. Additional
production lessons are retained and reported; the tool never shrinks a
production collection to fit a target. A collection of 200 mixed spells,
potions, and lessons fails the lesson-count check.

**Draft count is separate from published content available to subscribers.**
After reviewing an individual draft and clearing any outstanding rights issue,
publication remains an explicit per-document action:

```
npm run approve-draft -- --project wiccan-android --contentId <id>
```

Approval refuses a lesson without a structured reflection question or curated
ancient source, and refuses any draft with `rightsReviewRequired: true`.
Clearing that flag is an editorial decision after confirming rights or
rewriting the affected content; the importer does not clear it.

### Rights and editorial review

106 entries carry `rightsReviewRequired: true`: the original 54 rights flags
and 52 conservatively inherited flags on new commentaries. Retained source
citations do not independently establish reuse permission. New commentary is
original prose, but all 146 additions still require manual editorial review.

### Reader levels

`difficulty` is assigned in the file and must be read as-is rather than
derived from `safetyNote`, `type`, or material hazard. It is independent of
`tier`. The full pool contains 100 beginner, 100 intermediate, and 96 advanced
entries, with 100 free and 196 premium entries. Among the **200 lessons**,
79 are beginner/free, 67 intermediate/premium, and 54 advanced/premium.
The historical original 150 entries retain their 50/50/50 reader-level split.

### Verification limits

The checked-in count and tests describe local content. No live count for
`wiccan-android` is asserted until the authenticated read-only verification
command succeeds. Importing drafts cannot by itself establish that 200 lessons
are published or ready for daily delivery.

## Existing published lessons

This code change governs newly generated and newly approved lessons. It does **not** automatically rewrite or unpublish existing Firestore documents that were published before this policy existed.

Before relying on the policy for production, audit existing published `content` documents where `type == "lesson"`. Any lesson that is not traceable to an approved ancient `sourceId` should be moved back to `draft`, rewritten against a curated ancient source, reviewed, and approved again.
