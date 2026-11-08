# Duplicate-safe Wiccan content agent

The content agent generates review-required drafts only. It cannot publish, overwrite, merge, or delete lessons. The CLI is preview-only unless an operator supplies both `--project wiccan-android` and `--commit`.

## Before generation

The agent loads every available draft and published content record and normalizes titles, slugs, summaries, instructions, tools, categories, correspondences, and follow-up practices. It reduces common surface synonyms such as string/thread/yarn to cord and charm/talisman/amulet to a shared comparison form.

Each record receives a concept signature containing:

- Primary and secondary methods
- Intended purpose
- Primary and supporting tools
- Ordered ritual actions
- Setting and timing
- Symbolic structure
- Incantation theme
- Follow-up action

The Claude request receives a compact list of relevant excluded concepts. Deterministic screening after generation remains authoritative; the model cannot certify its own originality.

## Rejection rules

The agent rejects exact or near matches involving normalized title, slug, authored content, concept signature, summary, instruction sequence, ritual actions, or an existing similarity group. Default rejection thresholds are:

- Title: `0.85`
- Summary: `0.82`
- Instructions: `0.78`
- Concept signature: `0.80`
- Ritual actions: `0.75`

Changing only wording, ingredients, colors, moon phase, room, tool subtype, incantation, or intention does not establish a new concept.

Accepted candidates enter the in-memory inventory immediately, preventing duplicates within one batch. Firestore creation atomically reserves hashes for normalized title, slug, authored content, and practical concept. Concurrent agents cannot commit the same accepted concept.

Every committed CLI batch reports accepted drafts, rejected candidates, attempts, exhaustion, closest matches, similarity scores, concept signatures, and rejection reasons. The agent makes at most five attempts per requested draft and stops honestly when it cannot produce the requested distinct count.

## Operation

Preview configuration without an API call or write:

```bash
npm run content-agent -- --dry-run --tier free --passageId <approved-passage-id> --count 10
```

Create drafts after the preview and source review:

```bash
npm run content-agent -- --project wiccan-android --commit --tier free --passageId <approved-passage-id> --count 10
```

The CLI's `--continuous` mode is an explicitly supervised process. It stops when a batch is exhausted or produces no accepted draft.

## Scheduled agent

`generateContentDraftsJob` (`functions/src/index.ts` → `contentAgent.ts`) is the unattended form of the same pipeline. It runs daily at 04:00 UTC and is **off by default**.

- It runs only when `systemConfig/global.contentGenerationEnabled` is exactly `true`.
- Each run takes the next slot in a fixed rotation: every verified passage on the `free` tier, then every passage again on `premium`. It then calls the same `generateDraftBatch` as the CLI with `contentGenerationBatchSize` drafts (1–5, default 3). Every candidate gets the full-inventory duplicate screen and the atomic reservations described above.
- Only passages with no `findPassageViolations` issues and an approved generation source are used. With none, the run records `no-verified-passages` and makes no model call.
- Each run writes a report to `contentGenerationRuns`: status (`completed`, `exhausted`, `failed`, or `no-verified-passages`), passage, tier, accepted drafts, rejected candidates with reasons and closest matches, and attempts. The cursor in `contentAgentState/scheduler` always advances, so an exhausted or failing passage cannot stall the rotation.
- Retries are off. A failed run is recorded and the next day moves on.
- It creates drafts only. Publication still requires `approve-draft`, and there is no automatic publication path.

Enable it in four steps:

1. Add and verify passages.
2. Set the secret: `firebase functions:secrets:set ANTHROPIC_API_KEY`.
3. Deploy the functions.
4. Set `contentGenerationEnabled: true` on `systemConfig/global` in the console. Set it back to `false`, or delete it, to stop the agent.

Historical claims still require a fully reviewed passage record. If evidence, rights, safety, or scope is unresolved, the agent stops instead of inventing a source, claim, method, quantity, formula, or citation.
