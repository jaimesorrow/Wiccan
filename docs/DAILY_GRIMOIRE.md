# Daily delivery and Grimoire upgrade

Today assigns published, eligible content through the authenticated, App
Check-enforced `getTodayContent` callable. Users answer no lesson-preference or
experience questions. Sign-in and the existing Terms/Privacy acceptance remain.
Difficulty is absent from the content model, seed, mappings and editorial flow;
free/premium tiers are preserved independently.

## Assignment and history

The device sends its `timezone` (IANA zone or Android fixed UTC offset), never a
user ID, tier or calendar date. Fixed offsets such as `GMT+05:30` are normalized
for both assignment and notification dates. The
server freezes the request instant and computes the local date. Clients that
omit the new argument retain the stored profile-zone/UTC fallback. The response
adds `deliveredDate` and `timezone` to the existing flat content payload, so old
clients can continue reading it. Deploy the updated callable before releasing
the updated Android client, which requires assignment provenance for caching.

`userContentHistory/{uid}_{YYYY-MM-DD}` remains the single daily assignment.
Server randomness chooses a candidate; a Firestore transaction checks the
existing assignment, the current publication/provenance and user tier, legacy
history, and per-content/per-similarity reservations before committing. Every new
assignment also reads and writes a per-user usage document, serializing requests
across different dates and mixed legacy/editorial grouping metadata. Same-day
callers receive the winner; different dates cannot reserve a delivered ID or
similarity group again. Previously delivered entries stay in history even when
the pool is exhausted. A missing same-day content document returns an unavailable
assignment error and leaves its history intact.

Editorial `similarityGroupId` identifies rewordings of the same lesson. Explicit
groups take priority; source ID plus normalized citation is a legacy fallback.
The seed keeps the original 150 IDs and adds 146 companion lessons, for 296
drafts total. Companion lessons share their source entry's identity, and eight
reviewed overlapping pairs share identities across their clusters. Editors
must reuse a group for substantially reworded versions. This is metadata-based
exclusion, not semantic similarity detection for content with no identifying
metadata.

The transaction writes history and the existing
`userContentUsage/{uid}/content/{contentId}` reservation, plus an additive
`userContentUsage/{uid}/similarityGroups/{sha256}` marker where metadata exists.
Only server code can access reservations or write history. History snapshots
retain group and source/citation metadata for future duplicate checks. Legacy
rows remain readable and protect against repeats without requiring a backfill.
The transaction resolves their content metadata when available. The history
scan is proportional to each user's delivered collection; monitor Firestore
read cost as the published pool grows.

Exhaustion uses `resource-exhausted` with `reason: NO_NEW_CONTENT`. Android shows
an explicit no-new-lesson state linking to Grimoire and clears the daily cache;
the background worker finishes without retrying exhausted content. It never
silently recycles an old entry.

## Grimoire, Room and deployment

Grimoire is the visible rename of the existing archive collection. Internal
`archive` routes, the `LIBRARY` tab enum, Firestore collection/document names,
and Room table/database names stay stable. History lists newest first, supports
the existing category grouping, and opens the same detail view used by Today.
An additive descending history index matches the existing timeline query;
retain the ascending index and deploy `firestore.indexes.json` before relying
on that query in production.

Room schema 3 adds nullable reflection, practice, source/rights and assignment
owner/timezone columns with `ALTER TABLE`. Existing rows remain intact; schema
1-to-2 remains registered. Old cache rows with no owner are withheld until an
authenticated server refresh, while every previously received entry remains in
Firestore history. The cache is scoped to the active account and local date;
resume and local midnight trigger refreshes. The database class/schema path,
Android components and installed application ID remain
`com.jaimesorrow.wiccan`; generated resources use namespace `com.wiccan.app`.

Existing Firestore content documents may retain obsolete difficulty keys; readers
ignore them. The explicit admin seed upsert removes that key from seeded content
without changing publication status. No live seeding, migration, deployment,
history deletion or PR merge is performed by this change.

The existing rule allowing authenticated reads of all published content is
unchanged. Free/premium assignment eligibility is checked server-side and inside
the transaction; direct published-document reads were already not tier-gated.

## Verification

Run the repository's existing Android build, unit-test, formatting and lint
commands plus `:app:connectedProdDebugAndroidTest` with an emulator. Room tests
exercise schema 2-to-3 and 1-to-3 while preserving rows. Compose tests cover
Grimoire navigation, accessible labels, empty/grouped history, lesson detail and
the no-new-lesson state. Functions unit tests cover daily selection and dates.

The instrumented tests parse exported schema JSON with Android's parser and
open historical database files through the production Room class, which validates
the upgraded schema. This avoids a serializer ABI incompatibility in the existing
`MigrationTestHelper` dependencies without changing application library versions.

With Node 22 and Java 21 for the Firestore emulator:

```sh
npm --prefix functions ci
npm --prefix functions run build
npm --prefix functions test -- --runInBand
npm --prefix functions run lint
npm --prefix firestore-tests ci
npm --prefix firestore-tests test
```

The isolated `demo-wiccan` emulator verifies the real production transactions,
same/different-date races, legacy history, similarity exclusion, exhaustion,
eligibility revalidation and security rules. It never contacts a live project.

Manual release verification: create a real account and enter Today without
preference questions; repeat same-day opens on two devices; cross a local date
boundary or change timezone; confirm Today automatically appears in Grimoire;
upgrade a schema-1/schema-2 install with history and a widget; review structured
lesson sections and rights/safety notes; verify free/premium account behavior
and the exhausted pool after deploying rules, indexes and Functions to staging.
Live Firebase/Play configuration and editorial approval of draft content remain
separate release prerequisites.

## Local results — 2026-10-06

Validated from latest main `b13dc64` using the existing Java 17 / Node 22
toolchain. Functions build, ESLint and 113 unit tests pass. The isolated
Firestore emulator passes 14 security tests and 18 production transaction tests.
Android passes 105 unit/Compose tests, debug APK assembly, Android-test
compilation and formatting. Direct AndroidJUnitRunner execution on the API 29
software emulator reports `OK (3 tests)` for schema 1→2, 1→3 and 2→3 upgrades.

The release AAB and lint pass; lint has zero errors and 64 nonblocking warnings.
All eight bundled native libraries pass the alignment check. Release tasks used
two workers and 1 GiB Gradle/Kotlin heaps to fit this workspace's memory limit;
application toolchain/dependency versions remain unchanged. The AAB is unsigned
because no upload keystore is configured.

The 24 toolchain regression tests, eight validator tests, repository structure
and identity checks, 31 release-preflight checks, seed field comparison, secret
scan and whitespace checks pass. Hosted CI remains a separate execution after
push. No production Firebase operation or data deletion was run.

The first Gradle-connected attempt reached the emulator before its package
service was ready and incorrectly reported success without running tests. The
actual direct runner results were checked explicitly after boot. The original
test helper then failed before migration on its serializer ABI; the updated
tests validate the exported historical databases through production Room and
all three now pass.
