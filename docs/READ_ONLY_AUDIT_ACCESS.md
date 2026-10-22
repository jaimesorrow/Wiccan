# Read-only production audit access

The repository can be updated through GitHub, but this workspace currently has no Google/Firebase identity. The production inventory therefore remains unverified and no live lesson was edited.

Open the [Google Cloud IAM page for `wiccan-android`](https://console.cloud.google.com/iam-admin/iam?project=wiccan-android) and grant the workspace's connected Google identity the narrow `Cloud Datastore Viewer` role (`roles/datastore.viewer`). Do not grant Editor or Owner for the audit.

The workspace also needs that Google identity attached through the ChatGPT/Codex cloud-environment connection. After the connection reports ready, run:

```bash
cd functions
npm run audit-content -- --project wiccan-android --output /tmp/wiccan-live-audit.json
```

The audit command is read-only. It counts the actual `content` documents, inspects all statuses and types, reads delivery-history IDs to identify previously delivered records, reports duplicate concerns and source blockers, and does not update Firestore.

Production content changes require a separate reviewed change set, a backup manifest with a tested restore, conflict checks against each document update time, and explicit approval of the exact change-set digest. Publication is a later, separate action.
