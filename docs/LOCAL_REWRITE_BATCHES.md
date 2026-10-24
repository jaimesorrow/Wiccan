# Local lesson rewrite batches

The checked-in 296-record seed can be divided into editorial work packets without Firebase access:

```bash
cd functions
npm run prepare-rewrite-batches -- --output /secure/local/wiccan-rewrite-staging
```

The command performs no network request and has no Firestore write option. It preserves seed order, document IDs, complete original fields, and `draft` status. The default maximum is 25 records, producing eleven 25-record packets and one 21-record packet. Every packet has a deterministic SHA-256 and every original record has its own source-snapshot SHA-256. The destination must be new; the command refuses to overwrite an existing directory.

Each entry intentionally starts with `proposedFields: null`, `expectedUpdateTime: null`, and two validation blockers. A completed rewrite remains a local proposal until it is bound to the corresponding live draft and update time, backed up, source-reviewed, validated, and converted into the existing reviewed change-set format. These local packets cannot be passed directly to the Firestore apply command.

This lets editorial rewriting proceed before Firebase is connected while preventing the local seed from being mistaken for a live inventory or an approved production change set. `functions/sourcePassages.json` is currently empty, so every historical rewrite remains source-blocked until an independent passage review is recorded.
