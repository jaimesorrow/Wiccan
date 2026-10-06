# Production preparation validation — 2026-10-02

This work builds on main commit `6a375a0ed89c1aa384fdc86c8788761cb718b82f`.
The Android app remains native Kotlin/Jetpack Compose, with package
`com.jaimesorrow.wiccan` and Firebase project `wiccan-android`. Google Play
Billing is the subscription provider selected by the owner.

## Changes

- Bind purchases to a hashed Firebase account ID and reserve token ownership
  atomically. Preserve pseudonymous anti-replay hashes after deletion, without
  retaining a raw UID or token in the ownership registry.
- Propagate restoration/verification failures, retry owned purchases on
  foreground resume, and use the same regular monthly offer for display and
  purchase. Disable purchase while its actual price/offer is unavailable.
- Guard subscription updates against old tokens and deleted accounts. Cancel
  auto-renew before erasing the account; retain retryable data if cancellation fails.
- Restrict direct Firestore reads to previously delivered, published content;
  deny listing the content pool. A create-only admin backfill supports legacy
  delivery history before deploying the new rules.
- Map, cache and render structured reflection, practice and difficulty fields
  in Android Today/Archive. Room schema version 3 preserves older cached rows
  through migrations from versions 1 and 2.
- Preserve the original 150 content drafts and add 146 original companion lesson
  drafts, yielding 200 actual lessons and 296 content documents total. New drafts
  require source/editorial review. Publication rejects outstanding rights flags;
  generation emits lesson sections as separate fields.
- Add create-only Firebase seeding and a read-only live lesson inventory, requiring
  an explicit production project and refusing emulator redirection.
- Pin the official Gradle distribution checksum and strengthen release validation
  and native page-alignment checks.

## Validation

The final results are recorded here after checks complete. Earlier validation
evidence remains in [BUILD_VALIDATION.md](BUILD_VALIDATION.md).

## Account and release limits

No authenticated Firebase/Play Console access or signing keystore was supplied
for this work. No production deployment, lesson write/count verification, Play
product creation, payout configuration or signed release upload is claimed.
The production Firebase Android config is present; its OAuth client list is
empty, so Google sign-in still needs Firebase provider/certificate configuration.

Live subscription purchase/renewal/RTDN and production Play Integrity checks
require the internal test track and the real backend. Hosted legal/support pages,
store assets and editorial/rights review remain owner prerequisites. Follow
[PRODUCTION_SETUP.md](PRODUCTION_SETUP.md) to complete them.
