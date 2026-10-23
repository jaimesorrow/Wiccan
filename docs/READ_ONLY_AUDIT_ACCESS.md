# Firebase audit access (read phase)

This file documents the first, read-only phase of the write-capable migration. The complete workflow is in [FIRESTORE_MIGRATION_RUNBOOK.md](FIRESTORE_MIGRATION_RUNBOOK.md). Production content has not been inventoried or changed from this workspace because no Firebase identity is connected yet.

Connect the official Firebase integration to an account with access to `wiccan-android`, or attach an authorized Google Cloud identity to the managed workspace. The initial inventory needs Firestore read access. The later backup and reviewed draft update need narrowly scoped export and document-update access; do not commit credentials or service-account files.

After the connection reports ready, run the mandatory read phase:

```bash
cd functions
npm run audit-content -- --project wiccan-android --output /tmp/wiccan-live-audit.json
```

The audit command counts the actual root `content` documents, inspects statuses and types, reads delivery-history IDs to identify previously delivered records, reports duplicate concerns and source blockers, and does not update Firestore.

Continue with the secure backup, dry run, exact-digest apply, and post-write audit in the runbook. The update path only modifies validated draft documents and preserves IDs and statuses. Publication is a later, separate action.
