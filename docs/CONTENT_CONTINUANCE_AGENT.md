# Historical-grimoire content continuance agent

The existing operator-run agent uses the same practical narrative as the rewrite packets. It accepts only a fully reviewed pre-modern passage, asks for one distinct performable Wiccan/Modern Witchcraft adaptation, emits the ten-category schema, labels original incantations and all modern changes, applies shared safety validation, and creates drafts only. Trusted code binds source tradition, structured citation, slug, provenance, rights-review requirement, and `draft` status; the model cannot set approvals or publication eligibility.

The CLI remains preview-only unless an operator supplies both `--project wiccan-android` and `--commit`. Continuous draft generation is available as an operator-controlled process:

```bash
npm run content-agent -- --project wiccan-android --commit --continuous \
  --intervalMinutes 1440 --maxBatches 30 --tier free \
  --passageId <approved-passage-id>
```

Omit `--maxBatches` only for a deliberately supervised long-running process. Every cycle reports `contentStatus: draft` and `published: false`. A cycle that creates no unique-titled drafts stops rather than repeatedly spending API quota. No Cloud Scheduler job or autonomous publication path is enabled by this change.

Continuous generation cannot run until the source registry contains an eligible reviewed passage and the operator supplies the API key and production credentials. This is an evidence gate, not a generator fallback: the agent must stop instead of inventing a source or technique.
