#!/usr/bin/env python3
"""Visually verified Carmina Gadelica 1900 vol. I passages (batch 3)."""
import json
from copy import deepcopy
from pathlib import Path

OUT = Path(__file__).with_name("new_passages_batch3.json")
REVIEWED_AT = "2026-11-27T18:05:00-09:00"
REVIEWED_BY = (
    "Beta (AI inspection against the printed page images of the cited pre-1931 edition, "
    "staged for Jaime Sorrow's editorial review), 2026-11-27"
)

CG_RIGHTS = {
    "status": "verified",
    "basis": (
        "Alexander Carmichael, Carmina Gadelica: Hymns and Incantations with Illustrative Notes on Words, Rites, and Customs, Dying and Obsolete, Volume I, "
        "Edinburgh: printed for the author by T. and A. Constable, and sold by Norman MacLeod, 25 George IV. Bridge, 1900. "
        "US: first published 1900 (before 1931), so public domain. UK and EU: Alexander Carmichael died on 6 June 1912, so life+70 expired at the end of 1982; "
        "the 25-year typographical-arrangement right has long expired. The English wording quoted is Carmichael's 1900 translation, not a later volume's new editorial matter. "
        "Internet Archive item carminagadelicah10carm is a National Library of Scotland digitization; some IA records carry a CC BY-NC-SA notice on the scan images themselves. "
        "This register quotes the public-domain 1900 text only and does not reproduce or commit NLS scan images."
    ),
    "reference": "https://archive.org/details/carminagadelicah10carm",
    "jurisdictions": ["US", "UK", "EU"],
}

CG_DATING = {
    "era": "medieval",
    "status": "verified",
    "evidenceUrl": "https://archive.org/details/carminagadelicah10carm/page/xxix/mode/1up",
    "notes": (
        "The wording quoted is Carmichael's 1900 English of orally collected Highland and Island prayers and charms; no single earlier manuscript of these exact English wordings is claimed. "
        "Carmichael's 1900 introduction (p. xxix) describes a blending of pagan and Christian religion in the poems and relates their music to the old Celtic Church. "
        "The verse-forms (loricae; invocations of Christ, Mary, Michael, Bride, Columba, and the Trinity) belong to medieval Gaelic Christian charm tradition. "
        "The edition used is Volume I, Edinburgh, 1900 (before 1931)."
    ),
}

SCOPE = (
    "Approved use: as cited historical inspiration for a clearly labelled original modern blessing or lesson of protection, devotion, household, fire, labour, travel, or seasonal observance, "
    "following Jaime Sorrow's rule that practical lessons may stand when correctly cited from historical magical texts first published before 1931. "
    "Anything beyond what this passage attests is modern adaptation. Generated content must not instruct harm, coercion, cursing, theft, or the involvement of a child in performing the rite, "
    "even when the historical wording blesses the children of a household. "
    "Must not be presented as Wiccan scripture, as a medical handbook, or as a spell to compel another person's love, favour, or legal defeat."
)

UNCERTAINTY = (
    "Checked by Beta against the printed page image(s) of Alexander Carmichael, Carmina Gadelica, Volume I (Edinburgh: T. and A. Constable for the author, 1900), "
    "Internet Archive item carminagadelicah10carm, on 2026-11-27. Opening small capitals, hanging indents, and Carmichael's square-bracket glosses follow the 1900 typography as closely as plain text allows. "
    "Pending Jaime Sorrow's human editorial review."
)

TEXT_TITLE = "Carmina Gadelica"
MANUSCRIPT_ID = (
    "Carmina Gadelica, Volume I (Edinburgh, 1900): orally collected Highland and Island texts printed with facing Gaelic and English; "
    "no single earlier manuscript of these exact English wordings is claimed"
)
TRADITION = "Scottish Gaelic Christian folk prayer and charm (Highlands and Islands)"
MANUSCRIPT_DATE = (
    "Orally collected in the Highlands and Islands of Scotland in the nineteenth century, and first published in this English wording in 1900; "
    "the verse-forms belong to medieval Gaelic Christian charm and lorica tradition"
)
COMPOSITION_DATE = (
    "Medieval Gaelic Christian charm tradition as orally preserved; the English edition used is Carmichael 1900"
)
EDITION = (
    "Alexander Carmichael, Carmina Gadelica: Hymns and Incantations with Illustrative Notes on Words, Rites, and Customs, Dying and Obsolete, "
    "Volume I, Edinburgh: printed for the author by T. and A. Constable, and sold by Norman MacLeod"
)
TRANSLATOR = "Alexander Carmichael"


def cg_url(page):
    return f"https://archive.org/details/carminagadelicah10carm/page/{page}/mode/1up"


def review():
    return {
        "passageInspected": True,
        "sourceFidelityApproved": True,
        "safetyApproved": True,
        "reviewedBy": REVIEWED_BY,
        "reviewedAt": REVIEWED_AT,
    }


def cg(id, locator, page, text, instructions, formula=None, extra_unc=None):
    rec = {
        "id": id,
        "sourceId": "carmina-gadelica-carmichael-1900",
        "textTitle": TEXT_TITLE,
        "manuscriptId": MANUSCRIPT_ID,
        "tradition": TRADITION,
        "manuscriptDate": MANUSCRIPT_DATE,
        "compositionDate": COMPOSITION_DATE,
        "edition": EDITION,
        "editionDate": "1900",
        "translator": TRANSLATOR,
        "locator": locator,
        "url": cg_url(page),
        "inspectedPassage": text,
        "attestedInstructions": instructions,
        "uncertainties": [UNCERTAINTY] + (extra_unc or []),
        "dating": deepcopy(CG_DATING),
        "rights": deepcopy(CG_RIGHTS),
        "scope": {"status": "approved", "rationale": SCOPE, "evidenceUrl": cg_url(page)},
        "review": review(),
        "formula": formula if formula is not None else text,
    }
    return rec


PASSAGES = []

PASSAGES.append(cg(
    "cg-i-13-prayer-for-protection",
    'Carmina Gadelica, vol. I, "Prayer for Protection," p. 37 (item 13)',
    37,
    "As Thou art the Shepherd over the flock\n"
    "Tend Thou us to the cot and the fold,\n"
    "Sain us beneath Thine own glorious mantle,\n"
    "    Thou Shield of protection, guard us for ever.\n"
    "\n"
    "Be Thou a hard triumphant glave\n"
    "To shield us securely from wicked hell,\n"
    "From the fiends and from the stieve snell gullies,\n"
    "    And from the lurid smoke of the abyss.\n"
    "\n"
    "My soul is in the trustance of the High King,\n"
    "Michael the powerful is in charge of my soul.",
    [
        "The 1900 English text is a prayer asking to be tended as a flock, sained beneath a mantle, shielded, and placed in the charge of Michael.",
        "A modern lesson may use this only as cited historical inspiration for an original protective blessing.",
        "Do not present the 'glave' line as an instruction to take up a weapon, and do not involve a child in performing the rite.",
    ],
))

PASSAGES.append(cg(
    "cg-i-15-rock-of-rocks",
    'Carmina Gadelica, vol. I, "The Rock of Rocks," p. 43 (item 15)',
    43,
    "On the Rock of rocks,\n"
    "The peace of Peter and Paul,\n"
    "Of James and John the beloved,\n"
    "And of the pure, perfect Virgin,\n"
    "    The pure perfect Virgin.\n"
    "\n"
    "The peace of the Father of peace,\n"
    "The peace of the Christ of pasch,\n"
    "The peace of the Spirit of grace,\n"
    "To ourselves and to our children,\n"
    "    Ourselves and our children.",
    [
        "The 1900 English text is a short peace-blessing invoking apostles, the Virgin, and the Trinity.",
        "A modern lesson may use this only as cited historical inspiration for an original blessing of peace.",
        "The historical wording blesses 'our children'; a modern working must not involve a child in performing the rite.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as ON."],
))

PASSAGES.append(cg(
    "cg-i-16-lightner-of-the-stars",
    'Carmina Gadelica, vol. I, "The Light\'ner of the Stars," p. 45 (item 16)',
    45,
    "Behold the Light'ner of the stars,\n"
    "On the crests of the clouds,  [crested mountain clouds]\n"
    "And the choralists of the sky,\n"
    "    Lauding Him.\n"
    "\n"
    "Coming down with acclaim,\n"
    "From the Father above,\n"
    "Harp and lyre of song,\n"
    "    Sounding to Him.\n"
    "\n"
    "Christ, Thou refuge of my love,\n"
    "Why should not I raise Thy fame!\n"
    "Angels and saints melodious\n"
    "    Singing to thee.\n"
    "\n"
    "Thou Son of the Mary of graces,\n"
    "Of exceeding white purity of beauty,\n"
    "Joy were it to me to be in the fields\n"
    "    Of Thy riches.\n"
    "\n"
    "O Christ, my beloved,\n"
    "O Christ of the Holy Blood,\n"
    "By day and by night\n"
    "    I praise Thee.",
    [
        "The 1900 English text is a hymn of praise to Christ as the Light'ner of the stars.",
        "A modern lesson may use this only as cited historical inspiration for an original hymn of praise or night-sky devotion.",
        "The closing epithet 'O Christ of the Holy Blood' is historical Christian praise, not an instruction to use blood in a working.",
    ],
    extra_unc=[
        "The title is printed THE LIGHT'NER OF THE STARS, with an apostrophe. The first word of the verse is printed in small capitals as BEHOLD. "
        "Carmichael's gloss '[crested mountain clouds]' stands after 'clouds,' as in the 1900 English page."
    ],
))

PASSAGES.append(cg(
    "cg-i-18-guardian-angel",
    'Carmina Gadelica, vol. I, "The Guardian Angel," p. 49 (item 18)',
    49,
    "Thou angel of God who hast charge of me\n"
    "From the dear Father of mercifulness,             [mighty]\n"
    "The shepherding kind of the fold of the saints\n"
    "To make round about me this night,\n"
    "\n"
    "Drive from me every temptation and danger,\n"
    "Surround me on the sea of unrighteousness,\n"
    "And in the narrows, crooks, and straits\n"
    "Keep thou my coracle, keep it always.\n"
    "\n"
    "Be thou a bright flame before me,\n"
    "Be thou a guiding star above me,\n"
    "Be thou a smooth path below me,\n"
    "And be a kindly shepherd behind me,\n"
    "To-day, to-night, and for ever.\n"
    "\n"
    "I am tired and I a stranger,\n"
    "Lead thou me to the land of angels,\n"
    "For me it is time to go home\n"
    "To the court of Christ, to the peace of heaven.",
    [
        "The 1900 English text is a night prayer asking a guardian angel to enclose the speaker, keep a coracle in narrows, and be a flame, star, path, and shepherd.",
        "A modern lesson may use this only as cited historical inspiration for an original night blessing or traveller's prayer.",
        "The last stanza is a prayer for a peaceful homecoming, not an instruction to seek death.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as THOU. Carmichael's gloss '[mighty]' stands at the end of the second line."],
))

PASSAGES.append(cg(
    "cg-i-23-the-lustration",
    'Carmina Gadelica, vol. I, "The Lustration," p. 59 (item 23); the main English poem, not the following Variant',
    59,
    "I am bathing my face\n"
    "In the mild rays of the sun,\n"
    "As Mary bathed Christ\n"
    "In the rich milk of Egypt.\n"
    "\n"
    "Sweetness be in my mouth,\n"
    "Wisdom be in my speech,\n"
    "The love the fair Mary gave her Son\n"
    "Be in the heart of all flesh for me.\n"
    "\n"
    "The love of Christ in my breast,\n"
    "The form of Christ protecting me.\n"
    "There is not in sea nor on land\n"
    "That can overcome the King of the Lord's Day.\n"
    "\n"
    "The hand of Bride about my neck,\n"
    "The hand of Mary about my breast,\n"
    "The hand of Michael laving me,\n"
    "The hand of Christ saving me.",
    [
        "The 1900 English text is a morning lustration: bathing the face in sunlight, then asking for sweetness, wisdom, and the protecting hands of Bride, Mary, Michael, and Christ.",
        "A modern lesson may adapt the opening as a modest private morning wash or blessing; do not require or sexualize nudity.",
        "Do not use the line about 'the heart of all flesh' as a spell to compel another person's love or favour.",
    ],
    extra_unc=[
        "The first words of the verse are printed in small capitals as I AM. A four-line 'Variant' printed at the foot of p. 59 is not included in this passage."
    ],
))

PASSAGES.append(cg(
    "cg-i-25-god-guide-me",
    'Carmina Gadelica, vol. I, "God Guide Me," p. 65 (item 25)',
    65,
    "God guide me with Thy wisdom,\n"
    "God chastise me with Thy justice,\n"
    "God help me with Thy mercy,\n"
    "God protect me with Thy strength.\n"
    "\n"
    "God fill me with Thy fulness,\n"
    "God shield me with Thy shade,\n"
    "God fill me with Thy grace,\n"
    "For the sake of Thine Anointed Son.\n"
    "\n"
    "Jesu Christ of the seed of David,\n"
    "Visiting One of the Temple,\n"
    "Sacrificial Lamb of the Garden,\n"
    "Who died for me.",
    [
        "The 1900 English text is a short prayer asking for wisdom, justice, mercy, protection, fulness, and grace.",
        "A modern lesson may use this only as cited historical inspiration for an original prayer of guidance.",
        "The closing Christian epithets, including 'Sacrificial Lamb of the Garden,' are historical devotion, not an instruction to perform a sacrifice.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as GOD. The spelling 'fulness' follows the 1900 page."],
))

PASSAGES.append(cg(
    "cg-i-26-sleep-blessing",
    'Carmina Gadelica, vol. I, "Sleep Blessing," p. 67 (item 26)',
    67,
    "Be Thy right hand, O God, under my head,\n"
    "Be Thy light, O Spirit, over me shining.\n"
    "And be the cross of the nine angels over me down,\n"
    "From the crown of my head to the soles of my feet,\n"
    "    From the crown of my head to the soles of my feet.\n"
    "\n"
    "O Jesu without offence, crucified cruelly,\n"
    "Under ban of the wicked Thou wert scourged,\n"
    "The many evils done of me in the body!\n"
    "That I cannot this night enumerate,\n"
    "    That I cannot this night enumerate.\n"
    "\n"
    "O Thou King of the blood of truth,\n"
    "Cast me not from Thy covenant,\n"
    "Exact not from me for my transgressions,\n"
    "Nor omit me in Thy numbering,\n"
    "    Nor omit me in Thy numbering.\n"
    "\n"
    "Be the cross of Mary and of Michael over me in peace,\n"
    "Be my soul dwelling in truth, be my heart free of guile,\n"
    "Be my soul in peace with thee, Brightness of the mountains,\n"
    "Valiant Michael, accompany thou my soul,\n"
    "    Morn and eve, day and night. May it be so.",
    [
        "The 1900 English text is a night sleep-blessing asking for God's hand and the Spirit's light, a cross over the body, and Michael's accompaniment morn and eve.",
        "A modern lesson may use this only as cited historical inspiration for an original night blessing.",
        "The epithet 'King of the blood of truth' is historical Christian address, not an instruction to use blood.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as BE. The short ethnographic note above the verse on p. 67 is not quoted."],
))

PASSAGES.append(cg(
    "cg-i-27-come-i-this-day",
    'Carmina Gadelica, vol. I, "Come I This Day," p. 69 (item 27)',
    69,
    "Come I this day to the Father,\n"
    "Come I this day to the Son,\n"
    "Come I to the Holy Spirit powerful;\n"
    "Come I this day with God,\n"
    "Come I this day with Christ,\n"
    "Come I with the Spirit of kindly balm.\n"
    "\n"
    "God, and Spirit, and Jesus,\n"
    "From the crown of my head\n"
    "To the soles of my feet;\n"
    "Come I with my reputation,\n"
    "Come I with my testimony,\n"
    "Come I to Thee, Jesu—\n"
    "    Jesu, shelter me.",
    [
        "The 1900 English text is a morning coming-to-God prayer, from the crown of the head to the soles of the feet, ending 'Jesu, shelter me.'",
        "A modern lesson may use this only as cited historical inspiration for an original morning dedication.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as COME. The third line is 'Come I to the Holy Spirit powerful;' without 'this day'."],
))

PASSAGES.append(cg(
    "cg-i-43-resting-prayer",
    'Carmina Gadelica, vol. I, "A Resting Prayer," p. 101 (item 43)',
    101,
    "God shield the house, the fire, the kine,\n"
    "Every one who dwells herein to-night.\n"
    "Shield myself and my beloved group,\n"
    "Preserve us from handling and from harm,\n"
    "Preserve us from foes this night,\n"
    "For the sake of the Son of the Mary Mother,\n"
    "In this place, and in every place wherein they dwell to-night,\n"
    "On this night and on every night,\n"
    "    This night and every night.",
    [
        "The 1900 English text is a night prayer asking God to shield the house, the fire, the kine, and everyone who dwells therein.",
        "A modern lesson may use this only as cited historical inspiration for an original house or night blessing.",
        "Do not expand 'foes' into a curse against a named person.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as GOD."],
))

PASSAGES.append(cg(
    "cg-i-45-blessing-of-house",
    'Carmina Gadelica, vol. I, "Blessing of House," p. 105 (item 45)',
    105,
    "God bless the house,\n"
    "From site to stay,\n"
    "From beam to wall,\n"
    "From end to end,\n"
    "From ridge to basement,\n"
    "From balk to roof-tree,\n"
    "From found to summit,\n"
    "    Found and summit.",
    [
        "The 1900 English text is a short blessing of a house from site to stay and from found to summit.",
        "A modern lesson may use this only as cited historical inspiration for an original house blessing.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as GOD."],
))

PASSAGES.append(cg(
    "cg-i-54-the-new-moon",
    'Carmina Gadelica, vol. I, "The New Moon," p. 123 (item 54); the two English verse blocks, not the ethnographic note above them',
    123,
    "In name of the Holy Spirit of grace,\n"
    "In name of the Father of the City of peace,\n"
    "In name of Jesus who took death off us,\n"
    "Oh ! in name of the Three who shield us in every need,\n"
    "If well thou hast found us to-night,\n"
    "Seven times better mayest thou leave us without harm,\n"
    "    Thou bright white Moon of the seasons,\n"
    "    Bright white Moon of the seasons.\n"
    "\n"
    "May thy laving lustre leave us\n"
    "    Seven times still more blest.\n"
    "\n"
    "    O moon so fair,\n"
    "    May it be so,\n"
    "    As seasons come,\n"
    "    And seasons go.",
    [
        "The 1900 English text is a greeting and blessing of the new moon, asking to be left seven times better without harm.",
        "A modern lesson may use this only as cited historical inspiration for an original new-moon blessing.",
        "Do not treat the ethnographic note on the same page (nodding to the moon, turning silver or rings) as an attested instruction of this passage.",
    ],
    extra_unc=[
        "The first word of the main verse is printed in small capitals as IN. A second, shorter verse is printed below the main rune in a smaller type size; both blocks are quoted here as they appear under the heading THE NEW MOON."
    ],
))

PASSAGES.append(cg(
    "cg-i-61-christmas-chant",
    'Carmina Gadelica, vol. I, "Christmas Chant," pp. 135 and 137',
    135,
    "Hail King! hail King! blessed is He! blessed is He!\n"
    "Hail King! hail King! blessed is He! blessed is He!\n"
    "    Ho, hail! blessed the King!\n"
    "    Ho, hi! let there be joy!\n"
    "\n"
    "Prosperity be upon this dwelling,\n"
    "On all that ye have heard and seen,\n"
    "On the bare bright floor stones,\n"
    "On the shapely standing stone staves,\n"
    "    Hail King! hail King! blessed is He! blessed is He!\n"
    "\n"
    "Bless this house and all that it contains,\n"
    "From rafter and stone and beam,\n"
    "Deliver it to God from pall to cover,\n"
    "Be the healing of men therein,\n"
    "    Hail King! hail King! blessed is He! blessed is He!\n"
    "\n"
    "Be ye in lasting possession of the house,\n"
    "Be ye healthy about the hearth,\n"
    "Many be the ties and stakes in the homestead,\n"
    "People dwelling on this foundation,\n"
    "    Hail King! hail King! blessed is He! blessed is He!\n"
    "\n"
    "Offer to the Being from found to cover,\n"
    "Include stave and stone and beam,\n"
    "Offer again both rods and cloth,\n"
    "Be health to the people therein,\n"
    "    Hail King! hail King! blessed is He! blessed is He!\n"
    "    Hail King! hail King! blessed is He! blessed is He!\n"
    "        Ho, hail! blessed the King!\n"
    "        Let there be joy!\n"
    "\n"
    "    Blessed the King,\n"
    "    Without beginning, without ending,\n"
    "    To everlasting, to eternity,\n"
    "        Every generation for aye,\n"
    "        Ho! hi! let there be joy!",
    [
        "The 1900 English text is a Christmas house-chant asking prosperity, blessing, and health upon a dwelling, with the refrain 'Hail King! hail King! blessed is He!'",
        "A modern lesson may use this only as cited historical inspiration for an original midwinter or house blessing.",
        "Do not present 'healing of men therein' as medical advice.",
    ],
    extra_unc=[
        "The English verse begins on p. 135 and continues on p. 137 (the intervening even page is the facing Gaelic). The first word on p. 135 is printed in small capitals as HAIL."
    ],
))

PASSAGES.append(cg(
    "cg-i-66-hogmanay-bless-the-dwelling",
    'Carmina Gadelica, vol. I, "Hogmanay," p. 157; the four-line house blessing only, not the maledictions printed below it',
    157,
    "May God bless the dwelling,\n"
    "Each stone, and beam, and stave,\n"
    "All food, and drink, and clothing,\n"
    "May health of men be always there.",
    [
        "The 1900 English page prints this four-line blessing of a dwelling, its stones, beams, food, drink, clothing, and the health of those within.",
        "A modern lesson may use this only as cited historical inspiration for an original house blessing at the new year.",
        "The same page then prints hostile 'malison' verses; those lines are not part of this passage and must not be used.",
    ],
    extra_unc=[
        "A short opening verse ('We are come to the door...') and a following block of maledictions are printed on the same page and are not quoted here. "
        "A small left-hand cue above the blessing reads as a note that the lines are sung at the fire."
    ],
))

PASSAGES.append(cg(
    "cg-i-67-blessing-of-the-new-year",
    'Carmina Gadelica, vol. I, "The Blessing of the New Year," p. 159 (item 67)',
    159,
    "God, bless to me the new day,\n"
    "Never vouchsafed to me before,\n"
    "It is to bless Thine own presence\n"
    "Thou hast given me this day, O God.\n"
    "\n"
    "Bless Thou to me mine eye,\n"
    "May mine eye bless all it sees,\n"
    "I will bless my neighbour,\n"
    "May my neighbour bless me.\n"
    "\n"
    "God, give me a clean heart,\n"
    "Let me not from sight of Thine eye,\n"
    "Bless to me my children and my wife,\n"
    "And bless to me my means and my cattle.",
    [
        "The 1900 English text is a New Year blessing of the new day, the eye, the neighbour, a clean heart, and the household's means and cattle.",
        "A modern lesson may use this only as cited historical inspiration for an original new-year or morning blessing.",
        "The historical wording blesses 'my children and my wife'; a modern working must not involve a child in performing the rite.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as GOD. The one-line ethnographic note above the verse is not quoted."],
))

PASSAGES.append(cg(
    "cg-i-73-beltane-blessing",
    'Carmina Gadelica, vol. I, "The Beltane Blessing," pp. 183 and 185 (item 73)',
    183,
    "Bless, O Threefold true and bountiful,\n"
    "Myself, my spouse, and my children,\n"
    "My tender children and their beloved mother at their head.\n"
    "On the fragrant plain, on the gay mountain sheiling,\n"
    "    On the fragrant plain, on the gay mountain sheiling.\n"
    "\n"
    "Everything within my dwelling or in my possession,\n"
    "All kine and crops, all flocks and corn,\n"
    "From Hallow Eve to Bealtane Eve,\n"
    "With goodly progress and gentle blessing,\n"
    "From sea to sea, and every river mouth,\n"
    "    From wave to wave, and base of waterfall.\n"
    "\n"
    "Be the Three Persons taking possession of all to me belonging,\n"
    "Be the sure Trinity protecting me in truth,\n"
    "Oh! satisfy my soul in the words of Paul,\n"
    "And shield my loved ones beneath the wing of Thy glory,\n"
    "    Shield my loved ones beneath the wing of Thy glory.\n"
    "\n"
    "Bless everything and every one,\n"
    "Of this little household by my side,\n"
    "Place the cross of Christ on us with the power of love,\n"
    "Till we see the land of joy,\n"
    "    Till we see the land of joy.\n"
    "\n"
    "What time the kine shall forsake the stalls,\n"
    "What time the sheep shall forsake the folds,\n"
    "What time the goats shall ascend to the mount of mis ,\n"
    "May the tending of the Triune follow them,\n"
    "    May the tending of the Triune follow them.\n"
    "\n"
    "Thou Being who didst create me at the beginning,\n"
    "Listen and attend me as I bend the knee to Thee,\n"
    "Morning and evening as is becoming in me,\n"
    "In Thine own presence, O God of life,\n"
    "    In Thine own presence, O God of life.",
    [
        "The 1900 English text is a Beltane blessing of the household, flocks, and crops from Hallow Eve to Bealtane Eve, asking the Trinity's protection.",
        "A modern lesson may use this only as cited historical inspiration for an original Beltane or household blessing.",
        "The historical wording names spouse and children; a modern working must not involve a child in performing the rite.",
        "Do not reconstruct the ethnographic need-fire or the ordeal of passing between fires described in the note above the verse; that note is not part of this passage.",
    ],
    extra_unc=[
        "The English verse begins on p. 183 (first word printed in small capitals as BLESS) and continues on p. 185. "
        "On p. 185 the line is printed 'the mount of mis ,' with a space before the comma; the final letter is damaged or unprinted on the scanned page. "
        "The facing Gaelic on p. 184 has 'beinn a cheo' (mount of mist). The inspected wording follows the visible English type, not a silent restoration."
    ],
))

PASSAGES.append(cg(
    "cg-i-74-beltane-blessing-flocks",
    'Carmina Gadelica, vol. I, "The Beltane Blessing," pp. 187 and 189 (item 74)',
    187,
    "Mary, thou mother of saints,\n"
    "Bless our flocks and bearing kine,\n"
    "Hate nor scath let not come near us,\n"
    "Drive from us the ways of the wicked.\n"
    "\n"
    "Keep thine eye every Monday and Tuesday\n"
    "On the bearing kine and the pairing queys,\n"
    "Accompany us from hill to sea,\n"
    "Gather thyself the sheep and their progeny.\n"
    "\n"
    "Every Wednesday and Thursday be with them,\n"
    "Be thy gracious hand always about them,\n"
    "Tend the cows down to their stalls,\n"
    "Tend the sheep down to their folds!\n"
    "\n"
    "Every Friday be thou, O Saint, at their head,\n"
    "Lead the sheep from the face of the bens,\n"
    "With their innocent little lambs following them,\n"
    "Encompass them with God's encompassing.\n"
    "\n"
    "Every Saturday be likewise with them,\n"
    "Bring the goats in with their young,\n"
    "Every kid and goat to the sea side,\n"
    "And from the Rock of Aegir on high,\n"
    "With cresses green about its summit.\n"
    "\n"
    "The strength of the Triune our shield in distress,\n"
    "The strength of Christ, His peace and His Pasch,\n"
    "The strength of the Spirit, Physician of health,\n"
    "And of the priceless Father, the King of grace.\n"
    "\n"
    "And of every other saint who succeeded them\n"
    "And who earned the repose of the kingdom of God.\n"
    "\n"
    "Bless ourselves and our children,\n"
    "Bless every one who shall come from our loins,\n"
    "Bless him whose name we bear,\n"
    "Bless, O God, her from whose womb we came.\n"
    "\n"
    "Every holiness, blessing and power,\n"
    "Be yielded to us every time and every hour,\n"
    "In name of the Holy Threefold above,\n"
    "Father, Son, and Spirit everlasting.\n"
    "\n"
    "Be the Cross of Christ to shield us downward,\n"
    "Be the Cross of Christ to shield us upward,\n"
    "Be the Cross of Christ to shield us roundward,\n"
    "Accepting our Beltane blessing from us,\n"
    "    Accepting our Beltane blessing from us.",
    [
        "The 1900 English text is a second Beltane blessing, asking Mary to tend flocks day by day and the Cross of Christ to shield the household.",
        "A modern lesson may use this only as cited historical inspiration for an original Beltane or flock-blessing.",
        "The historical wording blesses children of the household; a modern working must not involve a child in performing the rite.",
        "Do not treat 'Drive from us the ways of the wicked' as a curse against a named person.",
    ],
    extra_unc=[
        "The English verse begins on p. 187 (first word printed in small capitals as MARY) and continues on p. 189. "
        "A row of asterisks stands on p. 189 before the couplet 'And of every other saint who succeeded them'; the couplet is quoted in its printed place."
    ],
))

print("definitions part 1 ok, count so far", len(PASSAGES))

PASSAGES.append(cg(
    "cg-i-82-blessing-of-the-kindling",
    'Carmina Gadelica, vol. I, "Blessing of the Kindling," p. 233 (item 82)',
    233,
    "I will kindle my fire this morning\n"
    "In presence of the holy angels of heaven,\n"
    "In presence of Ariel of the loveliest form,\n"
    "In presence of Uriel of the myriad charms,\n"
    "Without malice, without jealousy, without envy,\n"
    "Without fear, without terror of any one under the sun,\n"
    "But the Holy Son of God to shield me.\n"
    "    Without malice, without jealousy, without envy,\n"
    "    Without fear, without terror of any one under the\n"
    "            sun,\n"
    "      But the Holy Son of God to shield me.\n"
    "\n"
    "God, kindle Thou in my heart within\n"
    "A flame of love to my neighbour,\n"
    "To my foe, to my friend, to my kindred all,\n"
    "To the brave, to the knave, to the thrall,\n"
    "O Son of the loveliest Mary,\n"
    "From the lowliest thing that liveth,\n"
    "To the Name that is highest of all.\n"
    "      O Son of the loveliest Mary,\n"
    "      From the lowliest thing that liveth,\n"
    "      To the Name that is highest of all.",
    [
        "The 1900 English text is a morning blessing spoken when kindling the fire, asking to be without malice or fear, and asking a flame of love toward neighbour, foe, and friend.",
        "A modern lesson may use this only as cited historical inspiration for an original morning or hearth blessing.",
        "The 'flame of love' is charity toward neighbour and foe, not a spell to compel another person's affection.",
    ],
    extra_unc=["The first words of the verse are printed in small capitals as I WILL. The first stanza's refrain folds the word 'sun,' onto its own line, as in the 1900 page."],
))

PASSAGES.append(cg(
    "cg-i-86-blessing-of-the-smooring",
    'Carmina Gadelica, vol. I, "Blessing of the Smooring," p. 241 (item 86)',
    241,
    "I am smooring the fire\n"
    "As the Son of Mary would smoor,\n"
    "Blest be the house, blest be the fire,\n"
    "         Blest be the people all.\n"
    "\n"
    "Who are those down on the floor?\n"
    "John and Peter and Paul.\n"
    "On whom is the vigil to-night?\n"
    "    On the fair gentle Mary and on her Son.\n"
    "\n"
    "The mouth of God said,\n"
    "The angel of God spake,\n"
    "An angel in the door of the house,\n"
    "To guard and to keep us all\n"
    "         Till comes daylight to-morrow.\n"
    "\n"
    "Oh! may the angels of the Holy One of God\n"
    "Environ me all this night,\n"
    "Oh! may the angels of the Anointed One of God\n"
    "Encompass me from harm and from evil,\n"
    "         Oh! encompass me from harm this night.",
    [
        "The 1900 English text is a night blessing spoken when smooring (banking) the fire, blessing house, fire, and people, and asking an angel at the door till daylight.",
        "A modern lesson may use this only as cited historical inspiration for an original night or hearth blessing.",
    ],
    extra_unc=["The first words of the verse are printed in small capitals as I AM. Two other smooring poems in the same volume are not quoted here, to avoid near-duplicate content."],
))

PASSAGES.append(cg(
    "cg-i-88-consecration-of-the-seed",
    'Carmina Gadelica, vol. I, "The Consecration of the Seed," pp. 245-247 (item 88); the sowing stanzas only, ending at the first quatrain of p. 247',
    245,
    "I will go out to sow the seed,\n"
    "In name of Him who gave it growth;\n"
    "I will place my front in the wind,\n"
    "And throw a gracious handful on high.\n"
    "Should a grain fall on a bare rock,\n"
    "It shall have no soil in which to grow;\n"
    "As much as falls into the earth,\n"
    "The dew will make to be full.\n"
    "\n"
    "Friday, day auspicious,\n"
    "The dew will come down to welcome\n"
    "Every seed that lay in sleep\n"
    "Since the coming of cold without mercy,\n"
    "Every seed will take root in the earth,\n"
    "As the King of the elements desired,\n"
    "The braird will come forth with the dew,\n"
    "It will inhale life from the soft wind.\n"
    "\n"
    "I will come round with my step,\n"
    "I will go rightways with the sun,\n"
    "In name of Ariel and the angels nine,\n"
    "In name of Gabriel and the Apostles kind.\n"
    "\n"
    "Father, Son, and Spirit Holy,\n"
    "Be giving growth and kindly substance\n"
    "To every thing that is in my ground,\n"
    "Till the day of gladness shall come.",
    [
        "The 1900 English text is a sowing blessing: going out to sow, walking rightways with the sun, and asking growth for what is in the ground.",
        "A modern lesson may use this only as cited historical inspiration for an original spring or planting blessing.",
        "The ethnographic note on p. 245 (sprinkling seed with cold water three days before sowing, beginning generally on a Friday) is Carmichael's description, not quoted as part of this passage; a modern lesson must not treat it as a required reconstruction.",
    ],
    extra_unc=[
        "The first words of the verse are printed in small capitals as I WILL. The sowing stanzas begin on p. 245 and the closing growth quatrain stands at the top of p. 247. "
        "The rest of p. 247 is a Michaelmas first-cut rune, reserved as a separate passage."
    ],
))

PASSAGES.append(cg(
    "cg-i-88-feast-day-of-michael",
    'Carmina Gadelica, vol. I, p. 247, the Michaelmas first-cut rune printed after the sowing stanzas of "The Consecration of the Seed" (item 88)',
    247,
    "The Feast Day of Michael, day beneficent,\n"
    "I will put my sickle round about\n"
    "The root of my corn as was wont;\n"
    "I will lift the first cut quickly,\n"
    "I will put it three turns round\n"
    "My head, saying my rune the while,\n"
    "My back to the airt of the north,\n"
    "My face to the fair sun of power.\n"
    "\n"
    "I shall throw the handful far from me,\n"
    "I shall close my two eyes twice,\n"
    "Should it fall in one bunch\n"
    "My stacks will be productive and lasting,\n"
    "No Carlin will come with bad times\n"
    "To ask a palm bannock from us,\n"
    "What time rough storms come with frowns\n"
    "Nor stint nor hardship shall be on us.",
    [
        "The 1900 English text is a first-cut rune for the Feast Day of Michael: putting the sickle about the root of the corn, turning the first cut three times round the head, and throwing the handful.",
        "A modern lesson may use this only as cited historical inspiration for an original harvest blessing, not as a rite of augury that must be reconstructed.",
        "Do not treat 'No Carlin will come with bad times' as a curse against a person.",
    ],
))

PASSAGES.append(cg(
    "cg-i-89-reaping-blessing",
    'Carmina Gadelica, vol. I, "Reaping Blessing," p. 249 (item 89)',
    249,
    "God, bless Thou Thyself my reaping,\n"
    "Each ridge, and plain, and field,\n"
    "Each sickle curved, shapely, hard,\n"
    "Each ear and handful in the sheaf,\n"
    "    Each ear and handful in the sheaf.\n"
    "\n"
    "Bless each maiden and youth,\n"
    "Each woman and tender youngling,\n"
    "Safeguard them beneath Thy shield of strength,\n"
    "And gird them in the house of the saints,\n"
    "    Gird them in the house of the saints.\n"
    "\n"
    "Encompass each goat, sheep and lamb,\n"
    "Each cow and horse, and store,\n"
    "Surround Thou the flocks and herds,\n"
    "And tend them to a kindly fold,\n"
    "    Tend them to a kindly fold.\n"
    "\n"
    "For the sake of Michael head of hosts,\n"
    "Of Mary fair-skinned branch of grace,\n"
    "Of Bride smooth-white of ringleted locks,\n"
    "Of Columba of thegraves and tombs,\n"
    "    Columba of the graves and tombs.",
    [
        "The 1900 English text is a blessing of the reaping, the reaping company, and the flocks, in the names of Michael, Mary, Bride, and Columba.",
        "A modern lesson may use this only as cited historical inspiration for an original harvest blessing.",
        "The historical wording blesses maiden, youth, and 'tender youngling' among the company; a modern working must not involve a child in performing the rite.",
        "Do not reconstruct the 'trial of hooks' or death-augury described in the ethnographic note above the verse; that note is not part of this passage.",
    ],
    extra_unc=[
        "The first word of the verse is printed in small capitals as GOD. The first Columba line is printed 'Of Columba of thegraves and tombs,' without a space after 'the'; the refrain is printed 'Columba of the graves and tombs.' Both are quoted as they stand."
    ],
))

PASSAGES.append(cg(
    "cg-i-100-herding-blessing",
    'Carmina Gadelica, vol. I, "Herding Blessing," p. 277 (item 100); the main English poem only, not the short bilingual verses above it',
    277,
    "The keeping of God and the Lord on you,\n"
    "The keeping of Christ always on you,\n"
    "The keeping of Carmac and of Columba on you,\n"
    "The keeping of Cairbre on you going and coming,\n"
    "And the keeping of Ariel the gold-bright on you,\n"
    "    The keeping of Ariel the gold-bright on you.\n"
    "\n"
    "The keeping of Bride the foster-mother on you,\n"
    "The keeping of Mary the yellow-haired on you,\n"
    "Of Christ Jesus, the Son of peace,\n"
    "The King of kings, land and sea,\n"
    "And the peace-giving Spirit, everlasting, be yours,\n"
    "    The peace-giving Spirit, everlasting, be yours.",
    [
        "The 1900 English text is a herding blessing placing the keeping of God, Christ, Carmac, Columba, Cairbre, Ariel, Bride, and Mary upon the flock.",
        "A modern lesson may use this only as cited historical inspiration for an original blessing of care or protection on a journey or at pasture.",
    ],
    extra_unc=["The first word of the main poem is printed in small capitals as THE. Short bilingual verses printed above the main poem on p. 277 are not quoted here."],
))

PASSAGES.append(cg(
    "cg-i-101-herding-blessing-bride",
    'Carmina Gadelica, vol. I, "Herding Blessing," p. 279 (item 101)',
    279,
    "I will place this flock before me,\n"
    "As was ordained of the King of the world,\n"
    "Bride to keep them, to watch them, to tend them,\n"
    "On ben, on glen, on plain,\n"
    "    Bride to keep them, to watch them, to tend them,\n"
    "    On ben, on glen, on plain.\n"
    "\n"
    "Arise, thou Bride the gentle, the fair,\n"
    "Take thou thy lint, thy comb, and thy hair,\n"
    "Since thou to them madest the rich charm,\n"
    "To keep them from straying, to save them from harm,\n"
    "    Since thou to them madest the rich charm,\n"
    "    To keep them from straying, to save them from harm.\n"
    "\n"
    "From rocks, from drifts, from streams,\n"
    "From crooked passes, from destructive pits,\n"
    "From the straight arrows of the slender ban-shee,\n"
    "From the heart of envy, from the eye of evil,\n"
    "    From the straight arrows of the slender ban-shee,\n"
    "    From the heart of envy, from the eye of evil.\n"
    "\n"
    "Mary Mother, tend thou the offspring all,\n"
    "Bride of the fair palms, guard thou my flocks,\n"
    "Kindly Columba, thou saint of many powers,\n"
    "Encompass thou the breeding cows, bestow on me herds,\n"
    "    Kindly Columba, thou saint of many powers,\n"
    "    Encompass thou the breeding cows, bestow on me\n"
    "          herds.",
    [
        "The 1900 English text is a second herding blessing asking Bride to keep the flock on ben, glen, and plain, and to save them from harm, envy, and the evil eye.",
        "A modern lesson may use this only as cited historical inspiration for an original blessing of protection for animals or travellers.",
        "Do not treat 'the eye of evil' or the ban-shee lines as a curse against a named person.",
    ],
    extra_unc=["The first words of the verse are printed in small capitals as I WILL. The hyphen in 'ban-shee' follows the 1900 page."],
))

PASSAGES.append(cg(
    "cg-i-112-loom-blessing",
    'Carmina Gadelica, vol. I, "Loom Blessing," p. 309 (item 112)',
    309,
    "Bless, O Chief of generous chiefs,\n"
    "My loom and everything a-near me,\n"
    "Bless me in my every action,\n"
    "Make Thou me safe while I live.\n"
    "From every brownie and fairy woman,\n"
    "From every evil wish and sorrow,\n"
    "Help me, O Thou helping Being,\n"
    "As long as I shall be in the land of the living.\n"
    "In name of Mary, mild of deeds,\n"
    "In name of Columba, just and potent,\n"
    "Consecrate the four posts of my loom,\n"
    "Till I begin on Monday.\n"
    "\n"
    "Her pedals, her sleay, and her shuttle,\n"
    "Her reeds, her warp, and her cogs,\n"
    "Her cloth-beam, and her thread-beam,\n"
    "Thrums and the thread of the plies.\n"
    "Every web, black, white, and fair,\n"
    "Roan, dun, checked, and red,\n"
    "Give Thy blessing everywhere,\n"
    "On every shuttle passing under the thread.\n"
    "Thus will my loom be unharmed,\n"
    "Till I shall arise on Monday;\n"
    "Beauteous Mary will give me of her love,\n"
    "And there shall be no obstruction I shall not overcome.",
    [
        "The 1900 English text is a blessing and consecration of a loom, its posts and parts, in the names of Mary and Columba, till work begins on Monday.",
        "A modern lesson may use this only as cited historical inspiration for an original blessing of a workplace or craft tool.",
        "Do not use 'Beauteous Mary will give me of her love' as a spell to compel another person's affection.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as BLESS. The ethnographic note above the verse on p. 309 is not quoted."],
))

PASSAGES.append(cg(
    "cg-i-116-prayer-for-travelling",
    'Carmina Gadelica, vol. I, "Prayer for Travelling," p. 321 (item 116)',
    321,
    "Life be in my speech,\n"
    "Sense in what I say,\n"
    "The bloom of cherries on my lips,\n"
    "Till I come back again.\n"
    "\n"
    "The love Christ Jesus gave\n"
    "Be filling every heart for me,\n"
    "The love Christ Jesus gave\n"
    "Filling me for every one.\n"
    "\n"
    "Traversing corries, traversing forests,\n"
    "Traversing valleys long and wild.\n"
    "The fair white Mary still uphold me,\n"
    "The Shepherd Jesu be my shield,\n"
    "The fair white Mary still uphold me,\n"
    "The Shepherd Jesu be my shield.",
    [
        "The 1900 English text is a traveller's prayer for life and sense in speech, for Christ's love in the heart, and for Mary and Jesu as shield through corries, forests, and valleys.",
        "A modern lesson may use this only as cited historical inspiration for an original travel blessing.",
        "Do not use the second stanza as a spell to compel another person's love or favour.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as LIFE."],
))

PASSAGES.append(cg(
    "cg-i-117-fishing-blessing",
    'Carmina Gadelica, vol. I, "Fishing Blessing," pp. 323 and 325 (item 117)',
    323,
    "The day of light has come upon us,\n"
    "Christ is born of the Virgin.\n"
    "\n"
    "In His name I sprinkle the water\n"
    "Upon every thing within my court.\n"
    "Thou King of deeds and powers above,\n"
    "Thy fishing blessing pour down on us.\n"
    "I will sit me down with an oar in my grasp,\n"
    "I will row me seven hundred and seven [strokes].\n"
    "\n"
    "I will cast down my hook,\n"
    "The first fish which I bring up,\n"
    "\n"
    "In the name of Christ, King of the elements,\n"
    "The poor shall have it as his need.\n"
    "\n"
    "And the king of fishers, the brave Peter,\n"
    "He will after it give me his blessing.\n"
    "\n"
    "Ariel, Gabriel, and John,\n"
    "Raphael benign, and Paul,\n"
    "\n"
    "Columba, tender in every distress,\n"
    "And Mary fair, the endowed of grace.\n"
    "\n"
    "Encompass ye us to the fishing-bank of ocean,\n"
    "And still ye to us the crest of the waves.\n"
    "\n"
    "Be the King of kings at the end of our course,\n"
    "Of lengthened life and of lasting happiness.\n"
    "\n"
    "Be the crown of the King from the Three on high,\n"
    "Be the cross of Christ adown to shield us,\n"
    "    The crown of the King from the Three above,\n"
    "    The cross of Christ adown to shield us.",
    [
        "The 1900 English text is a fishing blessing: sprinkling water, asking a fishing blessing, giving the first fish to the poor, and asking saints and the cross of Christ to still the waves and shield the crew.",
        "A modern lesson may use this only as cited historical inspiration for an original sea or livelihood blessing.",
        "The first fish is given to the poor; do not turn the rite toward hoarding or harm.",
    ],
    extra_unc=[
        "The English verse begins on p. 323 (first word printed in small capitals as THE) and continues on p. 325. "
        "The bracketed gloss '[strokes]' after 'seven hundred and seven' is Carmichael's, as printed. "
        "The long ethnographic note above the verse on p. 323 is not quoted."
    ],
))

PASSAGES.append(cg(
    "cg-i-119-ocean-blessing",
    'Carmina Gadelica, vol. I, "Ocean Blessing," p. 333 (item 119)',
    333,
    "God the Father all-powerful, benign,\n"
    "Jesu the Son of tears and of sorrow,\n"
    "With thy co-assistance, O! Holy Spirit.\n"
    "\n"
    "The Three-One, ever-living, ever-mighty, everlasting,\n"
    "Who brought the Children of Israel through the Red Sea,\n"
    "And Jonah to land from the belly of the great creature of the ocean,\n"
    "\n"
    "Who brought Paul and his companions in the ship,\n"
    "From the torment of the sea, from the dolour of the waves,\n"
    "From the gale that was great, from the storm that was heavy.\n"
    "\n"
    "When the storm poured on the Sea of Galilee,\n"
    "\n"
    "*    *    *    *    *    *\n"
    "\n"
    "Sain us and shield and sanctify us,\n"
    "Be Thou, King of the elements, seated at our helm,\n"
    "And lead us in peace to the end of our journey.\n"
    "With winds mild, kindly, benign, pleasant,\n"
    "Without swirl, without whirl, without eddy,\n"
    "That would do no harmful deed to us.\n"
    "\n"
    "We ask all things of Thee, O God,\n"
    "According to Thine own will and word.",
    [
        "The 1900 English text is an ocean blessing asking the Trinity to sain, shield, and sanctify the crew, to sit at the helm, and to lead the journey in peace with mild winds.",
        "A modern lesson may use this only as cited historical inspiration for an original sea or travel blessing.",
    ],
    extra_unc=[
        "The first word of the verse is printed in small capitals as GOD. After the line 'When the storm poured on the Sea of Galilee,' the 1900 page prints two rows of asterisks, marking omitted lines; the asterisks are represented here as a typographic break, and no text is invented to fill the gap."
    ],
))

PASSAGES.append(cg(
    "cg-i-120-ruler-of-the-elements",
    'Carmina Gadelica, vol. I, "Ruler of the Elements," p. 335 (item 120)',
    335,
    "The Children of Israel, God taking them,\n"
    "    Through the Red Sea obtained a path,\n"
    "They obtained the quenching of their thirst\n"
    "    From a rock that might not by man be sculptured.\n"
    "\n"
    "Who are they on the helm of my rudder,\n"
    "    Making joyance to my eastern barge?\n"
    "Peter and Paul and John the beloved,\n"
    "    Three to whom are due obeisance and laud.\n"
    "\n"
    "Who are the group near to my helm?\n"
    "    Peter and Paul and John the Baptist;\n"
    "Christ is sitting on my helm,\n"
    "    Making guidance to the wind from the south.\n"
    "\n"
    "To whom does tremble the voice of the wind?\n"
    "    To whom become tranquil strait and ocean?\n"
    "To Jesus Christ, Chief of each saint,\n"
    "    Son of Mary, Root of victory,\n"
    "    Son of Mary, Root of victory.",
    [
        "The 1900 English text is a sea prayer naming Peter, Paul, and John at the helm and Christ making guidance to the wind, so that strait and ocean become tranquil.",
        "A modern lesson may use this only as cited historical inspiration for an original sea or travel blessing.",
        "The closing epithet 'Root of victory' is a title of Christ, not an instruction to seek victory over a person.",
    ],
    extra_unc=["The first word of the verse is printed in small capitals as THE."],
))

PASSAGES.append(cg(
    "cg-i-121-sea-prayer",
    'Carmina Gadelica, vol. I, "Sea Prayer," pp. 337 and 339 (item 121)',
    337,
    "HELMSMAN    Blest be the boat.                 [Blessed]\n"
    "CREW        God the Father bless her.\n"
    "            Blest be the boat.\n"
    "            God the Son bless her.\n"
    "            Blest be the boat.\n"
    "            God the Spirit bless her.\n"
    "\n"
    "ALL         God the Father,\n"
    "            God the Son,\n"
    "            God the Spirit,\n"
    "                 Bless the boat.\n"
    "\n"
    "HELMSMAN    What can befall you\n"
    "            And God the Father with you?\n"
    "CREW        No harm can befall us.             [Not one thing]\n"
    "            What can befall you\n"
    "            And God the Son with you?\n"
    "            No harm can befall us.\n"
    "            What can befall you\n"
    "            And God the Spirit with you?\n"
    "            No harm can befall us.\n"
    "\n"
    "ALL         God the Father,\n"
    "            God the Son,\n"
    "            God the Spirit,\n"
    "               With us eternally.\n"
    "\n"
    "HELMSMAN    What can cause you anxiety\n"
    "            And the God of the elements over you?\n"
    "CREW        No anxiety can be ours.\n"
    "            What can cause you anxiety\n"
    "            And the King of the elements over you?\n"
    "            No anxiety can be ours.\n"
    "            What can cause you anxiety\n"
    "            And the Spirit of the elements over you?\n"
    "            No anxiety can be ours.\n"
    "ALL         The God of the elements,\n"
    "            The King of the elements,\n"
    "            The Spirit of the elements,\n"
    "            Close over us,\n"
    "                 Ever eternally.",
    [
        "The 1900 English text is a call-and-response sea prayer between helmsman, crew, and all, blessing the boat and answering that no harm can befall them when the Trinity is with them.",
        "A modern lesson may use this only as cited historical inspiration for an original boat, travel, or protection blessing.",
    ],
    extra_unc=[
        "The English prayer begins on p. 337 and continues on p. 339. Speaker labels are printed in small capitals as HELMSMAN, CREW, and ALL. "
        "Carmichael's glosses '[Blessed]' and '[Not one thing]' stand at the right of those lines as in the 1900 page."
    ],
))


def main():
    ids = [p["id"] for p in PASSAGES]
    if len(ids) != len(set(ids)):
        raise SystemExit(f"duplicate ids in factory: {[i for i in ids if ids.count(i) > 1]}")
    OUT.write_text(json.dumps(PASSAGES, indent=2, ensure_ascii=False) + "\n")
    print(f"wrote {len(PASSAGES)} passages to {OUT}")


if __name__ == "__main__":
    main()
