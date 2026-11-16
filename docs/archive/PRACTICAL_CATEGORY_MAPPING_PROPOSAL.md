# Practical category mapping proposal

Status: proposed for editorial approval. No Firestore category value has been migrated.

The current Android and Firestore model uses eight intention-oriented values: `Protection`, `LoveAndRelationships`, `ProsperityAndAbundance`, `HealingAndWellness`, `DivinationAndClarity`, `BanishingAndRelease`, `HomeAndHearth`, and `MoonAndSeasonal`. The checked-in 296-record seed uses them in the following counts.

| Existing value | Records | Proposed treatment |
| --- | ---: | --- |
| `Protection` | 88 | Inspect the central technique. Map to `protection-cleansing-warding` only when protection, cleansing, or warding is the actual working. |
| `LoveAndRelationships` | 2 | No automatic map. Consent review is mandatory; map by technique only. |
| `ProsperityAndAbundance` | 6 | No automatic map. Map by the performed technique, not the desired outcome. |
| `HealingAndWellness` | 62 | No automatic map. Remove medical claims and map by technique only after safety review. |
| `DivinationAndClarity` | 42 | Candidate for `divination`, but only when divination or interpretation is the central practice. |
| `BanishingAndRelease` | 60 | No automatic map. It may become protection, cord work, candle work, elemental work, or remain blocked. |
| `HomeAndHearth` | 24 | No automatic map. It may become altar/sacred space, protection, botanical, or another technique. |
| `MoonAndSeasonal` | 12 | Candidate for `moon-phases-timing` only when lunar timing or a lunar working is central and documented. |

## Proposed final taxonomy

| Firestore ID | Display label | Central technique test |
| --- | --- | --- |
| `candle-magic` | Candle Spells & Rituals | Candle preparation or candle-based ritual is essential to the method. |
| `herbs-botanicals` | Herbal Spells & Botanical Workings | A plant or botanical preparation is the central working. |
| `potions-infusions` | Potions, Oils & Infusions | The user prepares a ritual liquid, oil, bath, water, spray, or jar working. |
| `cord-knot-magic` | Cord & Knot Spells | Knotting, braiding, binding symbolism, or unbinding is central. |
| `crystals-stones` | Crystal & Stone Workings | Stone selection, placement, meditation, or arrangement is central. |
| `moon-phases-timing` | Moon & Lunar Spellwork | Lunar phase, lunar timing, or moon water is essential rather than incidental. |
| `divination` | Divination & Intuitive Practices | Tarot, scrying, pendulum work, dreams, or symbol interpretation is central. |
| `altars-sacred-space` | Altar & Sacred Space Rituals | Preparing an altar, tool set, boundary, or ritual space is the main practice. |
| `elemental-magic` | Elemental Spellwork | Deliberate work with earth, air, fire, water, or their documented correspondences is central. |
| `protection-cleansing-warding` | Protection, Cleansing & Warding | The practical method creates, renews, removes, or closes a protective boundary. |

Each revised lesson receives exactly one primary ID. Secondary IDs use the same list and must describe a substantial secondary technique. A material appearing incidentally does not justify a category.

## Mapping disposition

No checked-in record is safe to map automatically. The current values describe intentions while the proposed values mostly describe techniques. All 296 records also lack a verified passage record and current practical editorial review, so mapping without rewriting would falsely imply that the existing historical prose teaches the named working.

The migration change set must therefore record the proposed primary and secondary IDs per document, alongside the inspected source passage and final numbered method. Unmappable, unsafe, repetitive, or unsupported records stay drafts with their existing IDs and statuses.

Android retains all eight legacy enum values for previously delivered Grimoire entries. The ten proposed IDs are additive, so old cached and remote history remains readable.
