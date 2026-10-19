# Checked-in content audit

Audit scope: `functions/ancientContentSeed.json` on repository main snapshot `05bc7393721d24fbb04f7c73da8b9ba7ba707ccb`. This is not a live Firestore inventory.

## Verified file totals

| Measure | Count |
| --- | ---: |
| Records | 296 |
| Draft | 296 |
| Lesson | 200 |
| Spell | 59 |
| Potion | 37 |
| Legacy rights-review flag | 106 |
| Exact registered passage ID | 0 |
| New practical primary category | 0 |
| Hash-bound practical editorial review | 0 |

Source labels in the file:

| Source ID | Records | New-standard finding |
| --- | ---: | --- |
| `ancient-egypt-heka-amulets` | 84 | Historical-context source, not an ancient grimoire passage; blocked |
| `mesopotamian-maqlu` | 138 | Broad source record exists; exact passage, edition/translation rights, and scope review are not in the passage register; blocked |
| `london-leiden-demotic-magical-papyrus` | 64 | Broad source record exists; exact passage, edition/translation rights, and scope review are not in the passage register; blocked |
| `harris-magical-papyrus` | 10 | Broad source record exists; exact passage, edition/translation rights, and scope review are not in the passage register; blocked |

Legacy category totals are 88 Protection, 62 Healing & Wellness, 60 Banishing & Release, 42 Divination & Clarity, 24 Home & Hearth, 12 Moon & Seasonal, 6 Prosperity & Abundance, and 2 Love & Relationships. They have not been automatically mapped. Reclassification requires inspecting the central source-supported technique.

## Duplicate review

Exact normalized titles: 0 duplicate groups. Exact normalized bodies: 0 duplicate groups.

The seed contains 139 repeated `similarityGroupId` groups covering 293 records; the largest group contains four records. These are editorial duplicate concerns, not automatic deletion or merge decisions. The current documentation says many companion lessons intentionally share an identity, so each group still needs review of technique, learning objective, and sequence.

## New-standard disposition

| Disposition | Count |
| --- | ---: |
| Ancient passage verified in the new register | 0 |
| Rewritten and fully reviewed | 0 |
| Source-blocked | 296 |
| Exact edition/translation rights unresolved in the new register | 296 |
| Safety review required | 296 |
| Potion disclosure required at minimum | 37 |
| Eligible under the new standard | 0 |

The 106 legacy rights flags are preserved. They do not imply that the remaining 190 records have verified commercial-use rights for an exact edition or translation.

No record was deleted, merged, renumbered, reclassified, rewritten, approved, or published during this file audit. Live statuses, update times, existing published records, and Grimoire delivery history require the read-only Firestore audit described in [READ_ONLY_AUDIT_ACCESS.md](READ_ONLY_AUDIT_ACCESS.md).
