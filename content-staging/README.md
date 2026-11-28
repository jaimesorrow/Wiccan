# Original images for published Wiccan content

Backfill of an original black-and-gold grimoire illustration for every currently published lesson, spell, and potion.

**Generation, Storage upload, and Firestore linking are complete.** Production docs were updated with only `illustrationUrl` and `illustrationOriginal: true`. `systemConfig` and the generator were not changed. This PR was not merged.

## Status (verified 2026-11-27 17:52 UTC-09:00)

- Published items: **104** (12 lessons, 73 spells, 19 potions)
- Images generated and unique SHA-256 hashes: **104 / 104**
- Uploaded to `gs://wiccan-android.firebasestorage.app/content-images/<id>.webp`: **104**
- Firestore docs updated: **104** (0 skipped, 0 failures)
- Public unauthenticated GET of each `illustrationUrl`: **200 image/webp**
- Unauthenticated client write to `content-images/` is denied (Firebase Storage 403)
- Format: 1024×1024 WebP, one unique image per Firestore document ID
- Staging files: `content-staging/images/<id>.webp`
- Mapping: `content-staging/mapping.csv` (status `verified`)

## Schema fields set

Each published `content/<id>` document has:

- `illustrationUrl`: unique HTTPS URL of `content-images/<id>.webp`
- `illustrationOriginal`: `true`

No other fields were changed.
