# Staged original images for published Wiccan content

Backfill of an original black-and-gold grimoire illustration for every currently published lesson, spell, and potion.

**This folder is staging only.** Nothing here has been uploaded to Firebase Storage or written to production Firestore.

## Progress

- Published items in production (read-only query, 2026-11-27): **104** (12 lessons, 73 spells, 19 potions). Older backups listed 92; 12 more have been published since 2026-11-21.
- Images generated this run: **47**
- Remaining: **57**
- Format: 1024×1024 WebP, one unique image per Firestore document ID
- Files: `content-staging/images/<id>.webp`
- Box copy: `/workspace/wiccan-image-backfill/images/<id>.webp`
- Mapping: `content-staging/mapping.csv` (also `/workspace/wiccan-image-backfill/mapping.csv`)

## Schema field (provisional)

The brief asked to guess `imagePath`. The live app and PR #231 actually use **`illustrationUrl`** (an HTTPS URL). After a later production upload (not done here), set `illustrationUrl` on the matching `content/{id}` document. `imagePath` is flagged as a provisional guess and is not a live field.

Provisional Storage object path (not uploaded):

`gs://wiccan-android.firebasestorage.app/content-images/<id>.webp`

## Remaining IDs

- london-leiden-149-the-sixteen-thread-band
- london-leiden-150-the-wax-ape-and-the-oxyrhynchus-fish
- maqlu-012-saying-it-again-three-and-seven
- maqlu-017-the-patient-speaks-in-first-person
- maqlu-019-the-three-movements-of-the-night
- maqlu-022-water-closes-what-fire-opened
- maqlu-027-why-it-is-called-burning
- maqlu-037-invocation-of-nusku-protective-night-light
- maqlu-044-you-are-my-reflection
- maqlu-051-a-ritual-in-the-royal-court
- maqlu-054-girra-and-nusku
- maqlu-057-materials-chosen-for-how-they-die
- maqlu-062-the-figurine-as-legal-substitute
- maqlu-068-ban-ban-the-circle-around-the-bed
- maqlu-069-black-liquid-on-the-fate-goddess
- maqlu-070-coloured-twine-around-the-bed
- maqlu-072-ea-has-unbound-the-sinews
- maqlu-074-fourteen-sherds-from-the-crossroad
- maqlu-075-i-have-enclosed-the-ford
- maqlu-076-i-trample-down-my-foe
- maqlu-077-o-nusku-these-are-the-figurines-of-my-sorcerer
- maqlu-078-peeled-away-like-garlic
- maqlu-079-salt-charged-at-the-head-of-the-bed
- maqlu-080-seven-knots-of-white-wool
- maqlu-081-smearing-the-door-and-doorposts
- maqlu-083-the-clay-finger
- maqlu-084-the-reed-cross
- maqlu-085-the-witch-who-roams-the-streets
- maqlu-086-three-knots-of-white-wool
- maqlu-087-three-sticks-of-cornel
- maqlu-088-two-cornel-sticks-at-the-outer-gate
- maqlu-089-washing-over-her-likeness-in-flour
- maqlu-090-chaff-wall-plaster-and-soot
- maqlu-091-handwash-water-of-eight-materials
- maqlu-093-juniper-and-kukru
- maqlu-094-kukru-and-soapwort
- maqlu-095-parched-roasted-flour
- maqlu-096-pure-oil-clear-oil
- maqlu-100-thyme-and-sesame
- maqlu-109-asafoetida-at-the-head-of-the-bed
- maqlu-110-be-off-be-off
- maqlu-112-burning-the-shurpu-at-the-river
- maqlu-113-evil-udug-demon-to-your-steppe
- maqlu-115-i-am-raising-the-torch
- maqlu-116-i-call-upon-you-the-sulphur-torch
- maqlu-118-melt-dissolve
- maqlu-119-pure-sulphur-daughter-of-the-great-heavens
- maqlu-120-river-is-my-head-sulphur-my-physique
- maqlu-121-struck-three-times-with-cornel
- maqlu-124-the-pure-oven-daughter-of-anu
- maqlu-126-two-loaves-thrown-to-two-dogs
- maqlu-127-whoever-you-are-witch-who-took-my-clay
- maqlu-128-you-o-water-quenching-the-crucible
- maqlu-140-mixed-fumigants-for-the-patient
- maqlu-143-sulphur-and-amurru-salt
- maqlu-144-sulphur-with-three-plants
- maqlu-145-the-aromatic-torch-bundle

## Generated this run

- egypt-heka-005-heka-magic-as-a-god
- egypt-heka-006-isis-mistress-of-magic
- egypt-heka-009-naming-the-body-part-by-part
- egypt-heka-014-spells-for-childbirth
- egypt-heka-020-thoth-the-god-of-spells
- egypt-heka-031-back-you-apophis
- egypt-heka-032-back-you-fish-and-crocodiles
- egypt-heka-033-conjuration-for-the-head-four-times
- egypt-heka-035-don-t-fear-bastet
- egypt-heka-039-isis-isis-come-to-your-horus
- egypt-heka-041-sun-come-to-your-daughter
- egypt-heka-042-thoth-s-protection-at-the-bed
- egypt-heka-043-to-be-said-on-water-or-on-land
- egypt-heka-045-you-who-are-in-the-hole
- egypt-heka-046-your-mouth-has-been-shut-by-the-sun
- egypt-heka-047-barley-bread-kneaded-with-salt
- egypt-heka-048-garlic-and-beer-for-the-house
- egypt-heka-071-conjuration-in-the-speech-of-keftiu
- egypt-heka-111-be-spewed-out-you-poison
- egypt-heka-114-holding-the-scorpion-s-mouth-shut
- egypt-heka-117-isis-has-spun-nephthys-has-woven
- egypt-heka-122-the-clay-scorpion-on-turquoise
- egypt-heka-123-the-faience-lion-on-red-linen
- egypt-heka-125-the-tamarisk-falcon-for-snakebite
- harris-036-drive-away-the-lions-of-meru
- harris-073-first-spell-of-enchanting-all-that-is-in-the-water
- harris-082-the-clay-egg-at-the-prow
- london-leiden-016-the-number-seven-in-egyptian-practice
- london-leiden-050-oil-recited-over-seven-times-for-fever
- london-leiden-056-how-to-dismiss-what-you-called
- london-leiden-058-names-from-everywhere
- london-leiden-060-tests-and-prognoses
- london-leiden-061-the-child-medium
- london-leiden-097-the-divination-oil-of-seven
- london-leiden-102-from-petition-to-compulsion
- london-leiden-108-what-the-erotic-spells-actually-demanded
- london-leiden-129-anise-stalks-with-eggshell
- london-leiden-130-ape-s-dung-to-send-them-away
- london-leiden-131-ass-s-dung-with-an-amulet-of-nephthys
- london-leiden-132-copper-sulphate-fumigation
- london-leiden-134-crocodile-egg-shell-on-the-flame
- london-leiden-135-crocus-powder-with-alum
- london-leiden-136-eye-paint-for-the-bark-of-phre
- london-leiden-138-garlic-and-rue-for-a-dog-bite
- london-leiden-142-scorpion-oil-with-a-linen-strip
- london-leiden-146-the-greek-bean-macerate
- london-leiden-147-the-sa-wr-and-ilkh-stones
