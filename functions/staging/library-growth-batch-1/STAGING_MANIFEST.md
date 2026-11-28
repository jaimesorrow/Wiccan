# Staging manifest — library-growth-batch-1

Draft staging for issue #195 (grow the content library toward 300+). This batch grows `functions/sourcePassages.json` so `generateContentDraftsJob` / `runContentAgentCycle` can later draft from verified passages. **`systemConfig/global.contentGenerationEnabled` stays off.** No production Firestore writes, no Storage uploads, no merge.

## Counts

- Previously checked-in verified passages: **11**
- New visually verified passages in this batch: **50**
- Register total after this PR: **61**
- Repo-documented live published inventory (not a live Firestore count): about **104** published items as of `docs/V1_1_PLAN.md` (2026-11-21); the 296-entry `ancientContentSeed.json` remains draft.
- Images: **50/50 generated**. Unique original 4:3 black-and-gold art (Midnight Blue `#0B132B` / gold `#E8BC61`), 1152×864, saved as JPEG quality 90 to keep the PR reviewable (PNG would have been ~70–90 MB). Files live in `functions/staging/library-growth-batch-1/illustrations/{passageId}.jpg`. These are **not** production `illustrationUrl` values and are not uploaded to Storage in this PR. `sourcePassages.json` has no image field; the filename is the join key. One first-pass lunar image misread “vessel” as a ship and was regenerated as a ritual water-bowl.

## Pre-1931 edition rule (Jaime, 4:37 PM AKST, 27 Nov 2026)

Only magical texts first published **before 1931**. A translation or edition counts only if **that edition itself** was published before 1931. Every new record below records `editionDate` and the edition used. All 50 new records, and the 11 already in the register, have `editionDate` in {1904, 1908, 1912}. Nothing published in 1931 or later is quoted.

## Sources used in this batch

| sourceId (existing `sources.json`) | Work first published | Edition / translation used | editionDate | Rights basis (summarized) |
| --- | --- | --- | --- | --- |
| `london-leiden-demotic-magical-papyrus` | Demotic Magical Papyrus of London and Leiden, English edition 1904 | F. Ll. Griffith and Herbert Thompson, *The Demotic Magical Papyrus of London and Leiden*, Oxford: Clarendon Press, 1904. Internet Archive `demoticmagicalpa01grifuoft` (NOT_IN_COPYRIGHT). Printed page P = PDF page P+12 = IA leaf n(P+11). URL form: `https://archive.org/details/demoticmagicalpa01grifuoft/page/n{P+11}/mode/1up` | **1904** | US publication 1904 (pre-1931). Griffith d. 1934; H. Thompson d. 1944; UK/EU life+70 already expired. Existing register rights block reused. |
| `mesopotamian-maqlu` | R. C. Thompson, *Semitic Magic, its Origins and Development*, London: Luzac, 1908 | Same 1908 English edition only. Internet Archive `semiticmagicitso0000thom`. p. xxv = IA n30; p. xxvi = n31; p. xxvii = n32; p. 151 = n224. URL form: `https://archive.org/details/semiticmagicitso0000thom/page/n{n}/mode/1up`. **Not** ORACC, Abusch, or any post-1930 English. | **1908** | US/UK publication 1908. R. C. Thompson d. 4 May 1941; UK/EU life+70 expired end of 2011. |

Existing Metternich records (Budge, *Legends of the Gods*, **1912**) remain in the register; this batch adds none from that book. Harris has a `sources.json` entry but no visually verified pre-1931 English translation was located (Budge *Egyptian Magic* 1899 only footnotes Chabas), so **no Harris passages** are added.

`sources.json` is **not** changed. Key of Solomon (Mathers 1889) is pre-1931 and rights-clearable, but adding a `historical-grimoire` lesson source would break checked-in tests that currently require the lesson-generation pool to be `ancient-grimoire` only. Left as an open policy question.

## Review status

Each new record has `review.passageInspected`, `sourceFidelityApproved`, and `safetyApproved` set true, with:

- `reviewedBy`: `Beta (AI inspection against the printed page images of the cited pre-1931 edition, staged for Jaime Sorrow's editorial review), 2026-11-27`
- `reviewedAt`: `2026-11-27T16:45:00-09:00`

These flags exist so `findPassageViolations` can pass, following the same pattern as earlier Grok Bot AI-inspected records. **Jaime still reviews every passage before generation is turned on and before any `approveDraft`.** Images do not need per-image review (Jaime, 4:31 PM AKST, 27 Nov 2026), but they must remain original, item-specific, and never reused.

## New passages

| id | editionDate | locator / textTitle (short) | fetched URL | suggested seasonalWindow (#224, on generated content only) | illustration |
| --- | --- | --- | --- | --- | --- |
| `ll-col02-03-09-darkness-and-light` | 1904 | P. London-Leiden, col. II, 3-9 (Griffith & Thompson 1904, p. 27) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n38/mode/1up | none suggested | `illustrations/ll-col02-03-09-darkness-and-light.jpg` |
| `ll-col02-10-13-setem-is-my-name` | 1904 | P. London-Leiden, col. II, 10-13 (Griffith & Thompson 1904, p. 27) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n38/mode/1up | none suggested | `illustrations/ll-col02-10-13-setem-is-my-name.jpg` |
| `ll-col02-14-20-anubis-lotus` | 1904 | P. London-Leiden, col. II, 14-20 (Griffith & Thompson 1904, p. 29) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n40/mode/1up | none suggested | `illustrations/ll-col02-14-20-anubis-lotus.jpg` |
| `ll-col02-21-28-thoth-vessel` | 1904 | P. London-Leiden, col. II, 21-28 (Griffith & Thompson 1904, p. 31) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n42/mode/1up | none suggested | `illustrations/ll-col02-21-28-thoth-vessel.jpg` |
| `ll-col04-29-30-good-dispatch` | 1904 | P. London-Leiden, col. III, 29-30 and col. IV heading on the same page (Griffith & Thompson 1904, p. 39) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n50/mode/1up | none suggested | `illustrations/ll-col04-29-30-good-dispatch.jpg` |
| `ll-col04-01-08-imuthes-table` | 1904 | P. London-Leiden, col. IV, 1-8 (Griffith & Thompson 1904, pp. 39 and 41) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n50/mode/1up | none suggested | `illustrations/ll-col04-01-08-imuthes-table.jpg` |
| `ll-col04-09-11-imuthes-invocation` | 1904 | P. London-Leiden, col. IV, 9-11 (Griffith & Thompson 1904, p. 41) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n52/mode/1up | none suggested | `illustrations/ll-col04-09-11-imuthes-invocation.jpg` |
| `ll-col05-01-02-foreleg-north` | 1904 | P. London-Leiden, col. V, 1-2 (Griffith & Thompson 1904, p. 45) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n56/mode/1up | none suggested | `illustrations/ll-col05-01-02-foreleg-north.jpg` |
| `ll-col05-03-08-white-lamp-dream` | 1904 | P. London-Leiden, col. V, 3-8 (Griffith & Thompson 1904, p. 45) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n56/mode/1up | none suggested | `illustrations/ll-col05-03-08-white-lamp-dream.jpg` |
| `ll-col05-09-23-murai-lamp-invocation` | 1904 | P. London-Leiden, col. V, 9-23 (Griffith & Thompson 1904, pp. 47 and 49) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n58/mode/1up | none suggested | `illustrations/ll-col05-09-23-murai-lamp-invocation.jpg` |
| `ll-col07-27-34-lamp-compulsion` | 1904 | P. London-Leiden, col. VII, 27-34 (Griffith & Thompson 1904, p. 63) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n74/mode/1up | none suggested | `illustrations/ll-col07-27-34-lamp-compulsion.jpg` |
| `ll-col08-01-07-darkness-separate-light` | 1904 | P. London-Leiden, col. VIII, 1-7 (Griffith & Thompson 1904, pp. 63 and 65) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n74/mode/1up | none suggested | `illustrations/ll-col08-01-07-darkness-separate-light.jpg` |
| `ll-col08-12-16-pasash-inquiry` | 1904 | P. London-Leiden, col. VIII, 12-16 (Griffith & Thompson 1904, p. 65) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n76/mode/1up | none suggested | `illustrations/ll-col08-12-16-pasash-inquiry.jpg` |
| `ll-col09-33-35-osiris-hawks` | 1904 | P. London-Leiden, col. IX, 33-35 (Griffith & Thompson 1904, p. 75) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n86/mode/1up | none suggested | `illustrations/ll-col09-33-35-osiris-hawks.jpg` |
| `ll-col10-01-08-spirits-anubis-lamp` | 1904 | P. London-Leiden, col. X, 1-8 (Griffith & Thompson 1904, p. 75) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n86/mode/1up | none suggested | `illustrations/ll-col10-01-08-spirits-anubis-lamp.jpg` |
| `ll-col10-09-19-dark-chamber-vessel` | 1904 | P. London-Leiden, col. X, 9-19 (Griffith & Thompson 1904, p. 77) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n88/mode/1up | none suggested | `illustrations/ll-col10-09-19-dark-chamber-vessel.jpg` |
| `ll-col10-20-22-vessel-alone-lunar` | 1904 | P. London-Leiden, col. X, 20-22 (Griffith & Thompson 1904, p. 79) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n90/mode/1up | { kind: "moon", phase: "waxing" } or full (text: 4th–15th lunar day, moon fills the uzat) | `illustrations/ll-col10-20-22-vessel-alone-lunar.jpg` |
| `ll-col10-23-28-bark-of-phre` | 1904 | P. London-Leiden, col. X, 22-28 (Griffith & Thompson 1904, p. 79) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n90/mode/1up | none suggested | `illustrations/ll-col10-23-28-bark-of-phre.jpg` |
| `ll-col10-29-30-opener-of-earth` | 1904 | P. London-Leiden, col. X, 29-30 (Griffith & Thompson 1904, p. 81) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n92/mode/1up | none suggested | `illustrations/ll-col10-29-30-opener-of-earth.jpg` |
| `ll-col11-01-04-giving-favour-thoth` | 1904 | P. London-Leiden, col. XI, 1-4 (Griffith & Thompson 1904, p. 81) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n92/mode/1up | none suggested | `illustrations/ll-col11-01-04-giving-favour-thoth.jpg` |
| `ll-col14-14-20-bronze-bowl-anubis` | 1904 | P. London-Leiden, col. XIV, 14-20 (Griffith & Thompson 1904, p. 101) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n112/mode/1up | none suggested | `illustrations/ll-col14-14-20-bronze-bowl-anubis.jpg` |
| `ll-col14-21-32-lamp-censer-dismissal` | 1904 | P. London-Leiden, col. XIV, 21-32 (Griffith & Thompson 1904, p. 103) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n114/mode/1up | none suggested | `illustrations/ll-col14-21-32-lamp-censer-dismissal.jpg` |
| `ll-col16-10-14-lamp-that-is-lighted` | 1904 | P. London-Leiden, col. XVI, 10-14 (Griffith & Thompson 1904, p. 111) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n122/mode/1up | none suggested | `illustrations/ll-col16-10-14-lamp-that-is-lighted.jpg` |
| `ll-col16-15-17-invocation-before-phre` | 1904 | P. London-Leiden, col. XVI, 15-17 (Griffith & Thompson 1904, p. 111) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n122/mode/1up | none suggested | `illustrations/ll-col16-15-17-invocation-before-phre.jpg` |
| `ll-col16-18-21-rise-early-pure` | 1904 | P. London-Leiden, col. XVI, 18-21 (Griffith & Thompson 1904, p. 111) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n122/mode/1up | no seasonalWindow in the #224 schema (dawn purity; not a sabbat or moon phase). Note only. | `illustrations/ll-col16-18-21-rise-early-pure.jpg` |
| `ll-col16-22-30-new-lamp-east` | 1904 | P. London-Leiden, col. XVI, 22-30 (Griffith & Thompson 1904, p. 113) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n124/mode/1up | no seasonalWindow in the #224 schema (east / sunrise placement). Note only. | `illustrations/ll-col16-22-30-new-lamp-east.jpg` |
| `ll-col17-01-02-boel-lamp-method` | 1904 | P. London-Leiden, col. XVII, 1-2 (Griffith & Thompson 1904, p. 113) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n124/mode/1up | none suggested | `illustrations/ll-col17-01-02-boel-lamp-method.jpg` |
| `ll-col17-16-21-dismissal-open-tai` | 1904 | P. London-Leiden, col. XVII, 16-21 (Griffith & Thompson 1904, p. 117) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n128/mode/1up | none suggested | `illustrations/ll-col17-16-21-dismissal-open-tai.jpg` |
| `ll-col17-21-26-new-lamp-purity` | 1904 | P. London-Leiden, col. XVII, 21-26 (Griffith & Thompson 1904, p. 117) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n128/mode/1up | none suggested | `illustrations/ll-col17-21-26-new-lamp-purity.jpg` |
| `ll-col17-27-29-boel-lake-of-heaven` | 1904 | P. London-Leiden, col. XVII, 27-29 (Griffith & Thompson 1904, p. 117) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n128/mode/1up | none suggested | `illustrations/ll-col17-27-29-boel-lake-of-heaven.jpg` |
| `ll-col18-10-24-fair-face-anubis` | 1904 | P. London-Leiden, col. XVIII, 10-24 (Griffith & Thompson 1904, p. 121) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n132/mode/1up | none suggested | `illustrations/ll-col18-10-24-fair-face-anubis.jpg` |
| `ll-col18-25-32-light-table-cakes` | 1904 | P. London-Leiden, col. XVIII, 25-32 (Griffith & Thompson 1904, p. 123) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n134/mode/1up | none suggested | `illustrations/ll-col18-25-32-light-table-cakes.jpg` |
| `ll-col21-01-09-osiris-vessel` | 1904 | P. London-Leiden, col. XXI, 1-9 (Griffith & Thompson 1904, p. 135) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n146/mode/1up | none suggested | `illustrations/ll-col21-01-09-osiris-vessel.jpg` |
| `ll-col23-21-26-divine-opposite-moon` | 1904 | P. London-Leiden, col. XXIII, 21-26 (Griffith & Thompson 1904, p. 147) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n158/mode/1up | { kind: "moon", phase: "full" } (15th day, opposite the moon, uzat) | `illustrations/ll-col23-21-26-divine-opposite-moon.jpg` |
| `ll-col23-27-30-moon-fills-uzat` | 1904 | P. London-Leiden, col. XXIII, 27-30 (Griffith & Thompson 1904, p. 149) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n160/mode/1up | { kind: "moon", phase: "full" } (moon fills the uzat) | `illustrations/ll-col23-27-30-moon-fills-uzat.jpg` |
| `ll-col28-01-10-bronze-vessel-natron` | 1904 | P. London-Leiden, col. XXVIII, 1-10 (Griffith & Thompson 1904, pp. 163 and 165) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n174/mode/1up | none suggested | `illustrations/ll-col28-01-10-bronze-vessel-natron.jpg` |
| `ll-col28-11-15-vegetable-oil-light` | 1904 | P. London-Leiden, col. XXVIII, 11-15 (Griffith & Thompson 1904, p. 165) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n176/mode/1up | none suggested | `illustrations/ll-col28-11-15-vegetable-oil-light.jpg` |
| `ll-col29-01-05-sun-inquiry-brick` | 1904 | P. London-Leiden, col. XXIX, 1-5 (Griffith & Thompson 1904, p. 165) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n176/mode/1up | no seasonalWindow in the #224 schema (solar / noon inquiry). Note only. | `illustrations/ll-col29-01-05-sun-inquiry-brick.jpg` |
| `ll-col29-06-20-lotus-pure-light` | 1904 | P. London-Leiden, col. XXIX, 6-20 (Griffith & Thompson 1904, p. 167) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n178/mode/1up | none suggested | `illustrations/ll-col29-06-20-lotus-pure-light.jpg` |
| `ll-col29-21-25-east-window-uzat` | 1904 | P. London-Leiden, col. XXIX, 21-25 (Griffith & Thompson 1904, p. 169) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n180/mode/1up | no seasonalWindow in the #224 schema (sunrise/east; not a sabbat or moon phase). Note only. | `illustrations/ll-col29-21-25-east-window-uzat.jpg` |
| `th-maqlu-iii-1-witch-that-roameth` | 1908 | Thompson, Semitic Magic (London, 1908), p. xxv, quoting Maqlu Tablet III, 1 ff. | https://archive.org/details/semiticmagicitso0000thom/page/n30/mode/1up | none suggested | `illustrations/th-maqlu-iii-1-witch-that-roameth.jpg` |
| `th-maqlu-v-1-sesame-undo-spell` | 1908 | Thompson, Semitic Magic (London, 1908), p. xxvi, quoting Maqlu Tablet V, 1 ff. | https://archive.org/details/semiticmagicitso0000thom/page/n31/mode/1up | none suggested | `illustrations/th-maqlu-v-1-sesame-undo-spell.jpg` |
| `th-maqlu-i-knot-loosed` | 1908 | Thompson, Semitic Magic (London, 1908), p. xxvii, quoting Maqlu Tablet I | https://archive.org/details/semiticmagicitso0000thom/page/n32/mode/1up | none suggested | `illustrations/th-maqlu-i-knot-loosed.jpg` |
| `th-maqlu-i-earth-gilgamish-loosed` | 1908 | Thompson, Semitic Magic (London, 1908), p. xxvii, quoting Maqlu Tablet I | https://archive.org/details/semiticmagicitso0000thom/page/n32/mode/1up | none suggested | `illustrations/th-maqlu-i-earth-gilgamish-loosed.jpg` |
| `th-maqlu-i-sappan-purify-me` | 1908 | Thompson, Semitic Magic (London, 1908), p. xxvii, quoting Maqlu Tablet I | https://archive.org/details/semiticmagicitso0000thom/page/n32/mode/1up | none suggested | `illustrations/th-maqlu-i-sappan-purify-me.jpg` |
| `th-maqlu-i-marduk-lord-of-magic` | 1908 | Thompson, Semitic Magic (London, 1908), p. xxvii, quoting Maqlu Tablet I | https://archive.org/details/semiticmagicitso0000thom/page/n32/mode/1up | none suggested | `illustrations/th-maqlu-i-marduk-lord-of-magic.jpg` |
| `th-maqlu-i-48-close-the-pass` | 1908 | Thompson, Semitic Magic (London, 1908), p. 151, quoting Maqlu Tablet I, 48 ff. | https://archive.org/details/semiticmagicitso0000thom/page/n224/mode/1up | none suggested | `illustrations/th-maqlu-i-48-close-the-pass.jpg` |
| `th-maqlu-i-figures-of-my-witch` | 1908 | Thompson, Semitic Magic (London, 1908), p. 151, quoting Maqlu Tablet I (ed. Tallqvist), with Thompson's notes on mutilated lines | https://archive.org/details/semiticmagicitso0000thom/page/n224/mode/1up | none suggested | `illustrations/th-maqlu-i-figures-of-my-witch.jpg` |
| `th-maqlu-k10333-threshold` | 1908 | Thompson, Semitic Magic (London, 1908), p. xxvi n. 2, quoting K. 10333 | https://archive.org/details/semiticmagicitso0000thom/page/n31/mode/1up | none suggested | `illustrations/th-maqlu-k10333-threshold.jpg` |
| `ll-col14-08-13-anubis-vessel-names` | 1904 | P. London-Leiden, col. XIV, 8-13 (Griffith & Thompson 1904, p. 101) | https://archive.org/details/demoticmagicalpa01grifuoft/page/n112/mode/1up | none suggested | `illustrations/ll-col14-08-13-anubis-vessel-names.jpg` |

### Per-item illustration prompts

Shared style prefix (applied to every prompt, never reused as a finished image):

> Original digital illustration, not a photograph and not a historical manuscript scan, in the Midnight Grimoire style of the Wiccan app: deep midnight-blue-black background the colour of #0B132B, antique gold #E8BC61 line work and gold-leaf highlights, high contrast, like an illuminated plate. Ornate but restrained gold filigree at the edges containing no writing. No letters, no words, no numbers, no hieroglyphs, no cuneiform, no watermarks, no signatures, no typography of any kind. Unique composition for this subject only. Cinematic 4:3 frame.

Unique subject, one per passage:

- `ll-col02-03-09-darkness-and-light`: A bronze vessel of still water in a dark chamber, a clean bar of gold light dividing dense darkness from a luminous surface on the water.
- `ll-col02-10-13-setem-is-my-name`: A jackal-headed Anubis silhouette in gold linen, standing before a dark inquiry vessel, calm and formal, no face copied from any statue photograph.
- `ll-col02-14-20-anubis-lotus`: Anubis as a gold figure seated upon a blooming lotus above black water, faint underworld stars behind him.
- `ll-col02-21-28-thoth-vessel`: An ibis-headed Thoth in gold leaning over a bronze water-vessel, a reed and scribal palette at his side, the water holding a point of light.
- `ll-col04-29-30-good-dispatch`: A small crocus blossom and a pale alum crystal on a gold dish, with a thin departing thread of light leaving the frame.
- `ll-col04-01-08-imuthes-table`: A four-footed olive-wood table in a quiet chamber, four bricks stacked before it, a clay censer of olive charcoal glowing gold.
- `ll-col04-09-11-imuthes-invocation`: A gold priestly healer-figure approaching a cloth-covered table in a clean, still room, a thin column of incense.
- `ll-col05-01-02-foreleg-north`: A ritual lamp set toward the north beside a stylized bull's-foreleg emblem in gold, a dark horizon line.
- `ll-col05-03-08-white-lamp-dream`: A white lamp standing on a single brick, faint gold dream-smoke rising into a sleeping chamber.
- `ll-col05-09-23-murai-lamp-invocation`: A golden lamp flame addressed as a living presence, rays of spoken light radiating but forming no letters.
- `ll-col07-27-34-lamp-compulsion`: A lamp burning steadily in deep darkness, gold rays pressing outward like a summons, the room otherwise empty.
- `ll-col08-01-07-darkness-separate-light`: Two halves of a chamber, one of dense shadow and one of antique gold light, divided by a perfectly clean vertical line.
- `ll-col08-12-16-pasash-inquiry`: A seeker seated before a lamp of inquiry, abstract unreadable gold marks hovering like unspoken names, never forming letters.
- `ll-col09-33-35-osiris-hawks`: Osiris standing formal and tall in gold, flanked by two hawks with spread wings, a dark field behind them.
- `ll-col10-01-08-spirits-anubis-lamp`: A lamp surrounded by many faint spirit-lights, a jackal standard of Anubis rising behind the flame.
- `ll-col10-09-19-dark-chamber-vessel`: A closed dark room with a single vessel of water catching a coin of gold light, walls almost swallowing the glow.
- `ll-col10-20-22-vessel-alone-lunar`: A lone vessel under a waxing-to-full moon, the moon filling like a stylized watching eye, no glyphs.
- `ll-col10-23-28-bark-of-phre`: The solar bark of Ra gliding on a gold river of sky, a sun disk at the prow, night water below.
- `ll-col10-29-30-opener-of-earth`: A gold guardian figure opening a door in the earth, soil parting like gates, a warm light in the threshold.
- `ll-col11-01-04-giving-favour-thoth`: Thoth with an ibis head and moon disk, extending an open gold palm of favour toward the viewer.
- `ll-col14-08-13-anubis-vessel-names`: Anubis standing before a vessel, a corona of unspoken gold name-rings around his head, abstract and unreadable.
- `ll-col14-14-20-bronze-bowl-anubis`: A bronze bowl of water seen from above-oblique, Anubis's jackal profile appearing only as a gold reflection in the surface.
- `ll-col14-21-32-lamp-censer-dismissal`: A lamp and a clay censer standing side by side, a ribbon of incense smoke drifting away as a farewell.
- `ll-col16-10-14-lamp-that-is-lighted`: A newly kindled lamp, the first gold flame just catching on a clean wick, darkness receding.
- `ll-col16-15-17-invocation-before-phre`: A worshipper with raised arms facing the rising sun-bark of Ra, dawn gold flooding the horizon.
- `ll-col16-18-21-rise-early-pure`: First dawn light entering a scrupulously clean chamber, an empty gold basin of pure water on the floor.
- `ll-col16-22-30-new-lamp-east`: A new lamp placed toward an east window, sunrise gold pouring across the sill onto the flame.
- `ll-col17-01-02-boel-lamp-method`: A tall ritual lamp with an almost architectural, still flame, formal and columnar against black.
- `ll-col17-16-21-dismissal-open-tai`: Heavy curtains or doors opening in a dark sanctuary, a vessel's light departing outward into night.
- `ll-col17-21-26-new-lamp-purity`: A spare new lamp on immaculate white linen, one perfect flame, extreme cleanliness and stillness.
- `ll-col17-27-29-boel-lake-of-heaven`: A celestial lake of gold water set in the night sky, a lamp flame mirrored perfectly in its surface.
- `ll-col18-10-24-fair-face-anubis`: Anubis with a fair, calm, gracious gold face, frontal and kind, a faint lotus at his shoulder.
- `ll-col18-25-32-light-table-cakes`: A modest offering table set with round cakes and a single lamp, humble and careful, gold on black.
- `ll-col21-01-09-osiris-vessel`: Osiris in green-gold, standing above a vessel of still water at his feet, tall atef-like crown suggested without hieroglyphs.
- `ll-col23-21-26-divine-opposite-moon`: A divine gold figure standing opposite a full moon, the moon like a watching eye across a dark court.
- `ll-col23-27-30-moon-fills-uzat`: A full moon swelling to fill a stylized protective eye shape in gold, no hieroglyph strokes that could be read as writing.
- `ll-col28-01-10-bronze-vessel-natron`: A bronze vessel surrounded by white natron crystals sparkling like frost in gold light.
- `ll-col28-11-15-vegetable-oil-light`: A simple lamp fueled with clean vegetable oil, a soft generous gold flame and a small oil cruse beside it.
- `ll-col29-01-05-sun-inquiry-brick`: A brick altar under a high sun disk, inquiry at noon, sharp gold shadows on the bricks.
- `ll-col29-06-20-lotus-pure-light`: A lotus opening in a column of pure gold light, water beads on the petals, no figures.
- `ll-col29-21-25-east-window-uzat`: An east-facing window at morning, sunlight filling a stylized eye motif in the pane, no letters.
- `th-maqlu-iii-1-witch-that-roameth`: A cloaked and anonymous night figure walking empty Mesopotamian streets under watching stars, not a portrait of any person, gold on deep black.
- `th-maqlu-v-1-sesame-undo-spell`: Sesame seeds and a leafy protective plant scattered over a loosened knot of gold cord, the knot coming apart.
- `th-maqlu-i-knot-loosed`: A gold ritual knot coming undone, salt crystals and dripping tallow, a desert waste opening behind the broken knot.
- `th-maqlu-i-earth-gilgamish-loosed`: A heroic gold figure standing on cracked earth, bindings breaking into dust around his feet, night sky above.
- `th-maqlu-i-sappan-purify-me`: A walled city with two gates, sunrise gold at the east and sunset gold at the west, a box, a pot, and a purifying herb borne toward water.
- `th-maqlu-i-marduk-lord-of-magic`: A regal Mesopotamian divine figure, Marduk, holding a staff of light, a smaller messenger approaching across a gold court.
- `th-maqlu-i-48-close-the-pass`: A mountain pass shut by a looming gold wall, desert beyond, a lone figure holding the pass closed.
- `th-maqlu-i-figures-of-my-witch`: Anonymous clay or wax ritual figures laid on a platter, no living likeness, gold firelight, formal and still.
- `th-maqlu-k10333-threshold`: A house threshold in gold line, three marks of protective light on the sill (not letters), night outside, warmth within.

A machine-readable copy is in `illustration_prompts.json`. Generated PNGs, when present, are in `illustrations/`. Do not invent a live Storage URL in passage records.

## Dropped or excluded in this batch

### Already in the register (not duplicated)

- London-Leiden col. XXVII.1 (existing `ll-col27-01-bark-of-phre`); a different bark-of-Phre procedure from col. X.23–28 is included as `ll-col10-23-28-bark-of-phre`.
- London-Leiden col. VIII.8–9 names (existing `ll-col08-08-09-names`); new `ll-col08-01-07-darkness-separate-light` is cut before those lines.

### Safety / scope (attested in the 1904 book, not staged)

- Col. XII, XV, XXI, XXV: erotic procedures.
- Col. XIII: separation / hostile splitting.
- Col. XIX: dog-bite, poison, throat (medical).
- Col. XX: scorpion / medical.
- Col. XXIII.1–20: Typhon, catalepsy, death.
- Col. XXIV: sleep, catalepsy, opium, scammony, mandragora.
- Hoopoe-blood and chick-gall ointments (including the ointment immediately after the moon/uzat lines used in `ll-col23-27-30-moon-fills-uzat`).
- Verso XVII: erotic dreams.
- Verso XVIII: lizard-blood / milk of a male child.
- Col. IV blood-amulet material omitted from `ll-col04-29-30-good-dispatch` (crocus/alum + dismissal charm only).
- Col. XXIX.26–30 blood ointment omitted from `ll-col29-21-25-east-window-uzat`.
- Thompson 1908 p. xxvi hostile Maqlû curse language of the “seize / destroy / abyss of fire” type: dropped. Undoing, loosening, purification, and appeal texts are kept with a strict no-harm modern-instruction scope. Some included Maqlû quotations still contain historically hostile lines (tongue cut off, gag the mouth); the `scope.rationale` forbids turning those lines into a modern curse.

### Failed the pre-1931 edition rule, or not visually verified in a pre-1931 edition

- **Betz, *The Greek Magical Papyri in Translation*, 1986** — out of scope (edition 1931+).
- **Modern Maqlû English (Abusch, ORACC English, or any post-1930 edition)** — out of scope. ORACC may be used only as dating evidence for the ancient tablets, never as the quoted English.
- **Storms 1948**, **Pingree/Attrell Picatrix** — out of scope.
- **Goodwin 1852**, *Fragment of a Graeco-Egyptian Work upon Magic* (Cambridge Antiquarian Society; BM Papyrus XLVI Greek): the edition is pre-1931, but the downloaded IA PDF had almost no text layer and page images were not lined up in time for visual verification. **Held for a later batch**, not quoted from OCR.
- **Conybeare, Testament of Solomon, 1898/99**: pre-1931, but a verified manuscript-catalogue URL was not locked in this pass.
- **Budge, *Egyptian Magic*, 1899**: inspected for a Harris translation; only a Chabas footnote was found, not a usable English text of the Harris Magical Papyrus.
- **Grendon 1909** Anglo-Saxon charms: pre-1931 publication, but UK/EU life+70 depends on an unverified death date; not used.
- A bibliographic-only Thompson footnote on K. 9496 (no quoted ritual wording) was drafted and **dropped** as too thin to generate from.

## Sourcing plan for later batches (all must be pre-1931 editions)

- More visually verified London-Leiden 1904 pages not yet sliced, and further Metternich pages from Budge 1912 (lessons only).
- Goodwin 1852 (BM XLVI) once page images are visually quoted and a BM catalogue URL is attached.
- Conybeare Testament of Solomon 1898/99, once a manuscript catalogue URL is verified.
- Mathers, *The Key of Solomon the King*, 1889 (IA `b24884431`, public-domain mark) — **only if** Jaime accepts adding a `historical-grimoire` / early-modern source to `sources.json` and updating the tests that currently keep the checked-in lesson pool ancient-only.
- Cockayne, *Leechdoms, Wortcunning, and Starcraft of Early England*, 1864–66; Scot, *The Discoverie of Witchcraft*, Nicholson 1886 reprint of the 1584 text — same policy question, plus work-by-work rights notes.
- Do **not** use Betz 1986, modern Maqlû English, or any 1931+ edition.

## Open questions for Jaime / Grok Bot

1. May historical-grimoire sources (Mathers 1889, and later Scot / Cockayne if cleared) be added to `sources.json`? That requires a deliberate change to tests that currently require every checked-in lesson-generation source to be `ancient-grimoire` with `era === "ancient"`.
2. `seasonalWindow` lives on generated content documents, not on source passages. Should the suggested moon/full (and the dawn/sun notes, which do not fit the #224 schema) be applied when drafts are generated from the tagged passages?
3. `Content.illustrationUrl` is stored and mapped on Android only when it starts with `https://`. The Compose UI still largely uses bundled `WiccanLineArt`. Staging paths are not live URLs. After review, should these PNGs be uploaded to Storage and written onto generated drafts as `illustrationUrl`?
4. New review flags are Beta AI inspection against printed page images, pending Jaime's human review. Generation remains off until he says otherwise.
5. A pre-1931 English translation of the Harris Magical Papyrus still has not been visually verified. Leave Harris out until one is found.

## What this PR does not do

- Does not enable `contentGenerationEnabled`.
- Does not merge, deploy, force-push, or write production Firestore/Storage.
- Does not touch issue #86.
- Does not redesign seasonal tags (#224).
- Does not update the live published count in `docs/CONTENT_PRODUCTION_RUNBOOK.md`; that figure should be revised when Jaime merges and when generation actually runs.

