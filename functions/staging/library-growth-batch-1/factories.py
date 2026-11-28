#!/usr/bin/env python3
import json
from copy import deepcopy
from pathlib import Path

OUT = Path(__file__).with_name("new_passages.json")
REVIEWED_AT = "2026-11-27T16:45:00-09:00"
REVIEWED_BY = (
    "Beta (AI inspection against the printed page images of the cited pre-1931 edition, "
    "staged for Jaime Sorrow's editorial review), 2026-11-27"
)

LL_RIGHTS = {
    "status": "verified",
    "basis": "Griffith & Thompson, The Demotic Magical Papyrus of London and Leiden, vol. I, published Oxford 1904. First published 1904 (before 1931). US: published before 1931, so public domain. UK and EU: both translators are dead more than 70 years (F. Ll. Griffith d. 14 March 1934; Sir Herbert Thompson d. 26 May 1944), so life+70 expired at the end of 2014 and the 25-year typographical-arrangement right has long expired; public domain since 1 January 2015. The Internet Archive Toronto copy is marked NOT_IN_COPYRIGHT; the Wellcome copy's 'In Copyright' statement is a conservative library default inconsistent with these dates. Death dates checked 2026-11-21 against Cambridge University Library ArchiveSearch (Thompson) and Nature obituary, 14 April 1934 (Griffith).",
    "reference": "https://archive.org/details/demoticmagicalpa01grifuoft",
    "jurisdictions": ["US", "UK", "EU"],
}
LL_DATING = {
    "era": "ancient",
    "status": "verified",
    "evidenceUrl": "https://www.britishmuseum.org/collection/object/Y_EA10070-2",
    "notes": "British Museum record EA 10070,2 (P Leiden/London): Roman Period, production date 3rd century (200-225 AD), findspot Thebes. Edition used: Griffith & Thompson, Oxford 1904 (first published 1904, before 1931).",
}
TH_RIGHTS = {
    "status": "verified",
    "basis": "R. Campbell Thompson, Semitic Magic, its Origins and Development, published London 1908. First published 1908 (before 1931). US: published before 1931, so public domain. UK and EU: Thompson died 4 May 1941, so life+70 expired at the end of 2011; public domain. The English text quoted is Thompson 1908, not a post-1930 translation. Internet Archive item semiticmagicitso0000thom.",
    "reference": "https://archive.org/details/semiticmagicitso0000thom",
    "jurisdictions": ["US", "UK", "EU"],
}
TH_DATING = {
    "era": "ancient",
    "status": "verified",
    "evidenceUrl": "https://oracc.museum.upenn.edu/cmawro/corpus",
    "notes": "Maqlu is a first-millennium BCE Mesopotamian anti-witchcraft incantation-and-ritual series preserved on cuneiform tablets, including Kouyunjik tablets in the British Museum (Thompson 1908 cites K 249). The English wording quoted here is Thompson's 1908 translation, first published 1908 (before 1931), not a later edition.",
}

def ll_url(page):
    return f"https://archive.org/details/demoticmagicalpa01grifuoft/page/n{page + 11}/mode/1up"

def th_url(n):
    return f"https://archive.org/details/semiticmagicitso0000thom/page/n{n}/mode/1up"

def review():
    return {
        "passageInspected": True,
        "sourceFidelityApproved": True,
        "safetyApproved": True,
        "reviewedBy": REVIEWED_BY,
        "reviewedAt": REVIEWED_AT,
    }

def ll(id, locator, page, text, instructions, scope, formula=None, formulaOmissionReason=None, extra_unc=None):
    rec = {
        "id": id,
        "sourceId": "london-leiden-demotic-magical-papyrus",
        "textTitle": "The Demotic Magical Papyrus of London and Leiden",
        "manuscriptId": "British Museum EA 10070 (P. London) and Leiden RMO P. Leiden I 383 (P. Leiden)",
        "tradition": "Graeco-Roman Egyptian (Demotic), 3rd century AD",
        "manuscriptDate": "3rd century CE (200-225 AD, British Museum record)",
        "compositionDate": "Roman-period Egypt; compiled by the 3rd century CE",
        "edition": "F. Ll. Griffith and Herbert Thompson, The Demotic Magical Papyrus of London and Leiden, vol. I, Oxford: Clarendon Press",
        "editionDate": "1904",
        "translator": "F. Ll. Griffith and Herbert Thompson",
        "locator": locator,
        "url": ll_url(page),
        "inspectedPassage": text,
        "attestedInstructions": instructions,
        "uncertainties": [
            "Checked by Beta against the printed page image(s) of the 1904 Oxford edition (Internet Archive item demoticmagicalpa01grifuoft) on 2026-11-27. Line numbers in parentheses are the editors' own. Editorial brackets, '(bis)', '(sic)', and question marks in parentheses are the 1904 editors'. Pending Jaime Sorrow's human editorial review."
        ] + (extra_unc or []),
        "dating": deepcopy(LL_DATING),
        "rights": deepcopy(LL_RIGHTS),
        "scope": {"status": "approved", "rationale": scope, "evidenceUrl": ll_url(page)},
        "review": review(),
    }
    if formula:
        rec["formula"] = formula
    else:
        rec["formula"] = None
        rec["formulaOmissionReason"] = formulaOmissionReason or "The quoted passage contains instructions only; no spoken formula."
    return rec

def th(id, locator, ia_n, text, instructions, scope, formula=None, formulaOmissionReason=None, extra_unc=None):
    rec = {
        "id": id,
        "sourceId": "mesopotamian-maqlu",
        "textTitle": "Semitic Magic, its Origins and Development (English quotations from the Maqlu series)",
        "manuscriptId": "Assyrian Maqlu series; Kouyunjik (Nineveh) tablets in the British Museum (Thompson 1908 cites K 249 and related K-numbers)",
        "tradition": "Ancient Mesopotamian ritual practice",
        "manuscriptDate": "First-millennium BCE manuscript tradition (Assyrian library tablets from Nineveh)",
        "compositionDate": "First-millennium BCE Mesopotamia",
        "edition": "R. Campbell Thompson, Semitic Magic, its Origins and Development, London: Luzac & Co.",
        "editionDate": "1908",
        "translator": "R. Campbell Thompson",
        "locator": locator,
        "url": th_url(ia_n),
        "inspectedPassage": text,
        "attestedInstructions": instructions,
        "uncertainties": [
            "Checked by Beta against the printed page image(s) of Thompson, Semitic Magic (London: Luzac, 1908), Internet Archive item semiticmagicitso0000thom, on 2026-11-27. The English is Thompson's 1908 wording, not a later edition. Unicode characters follow the printed diacritics. Pending Jaime Sorrow's human editorial review."
        ] + (extra_unc or []),
        "dating": deepcopy(TH_DATING),
        "rights": deepcopy(TH_RIGHTS),
        "scope": {"status": "approved", "rationale": scope, "evidenceUrl": th_url(ia_n)},
        "review": review(),
    }
    if formula:
        rec["formula"] = formula
    else:
        rec["formula"] = None
        rec["formulaOmissionReason"] = formulaOmissionReason or "The quoted passage contains instructions only; no spoken formula."
    return rec

SCOPE_LL = (
    "Approved use: as cited historical inspiration for a clearly labelled original modern working, not a reconstruction, "
    "following Jaime Sorrow's rule that practical lessons may stand when correctly cited from historical magical texts. "
    "Anything beyond what this passage attests is modern adaptation. Generated content must not instruct the use of a child as a ritual medium, "
    "must not turn historical recipes into unsafe, coercive, medical, or harmful instructions, and must not present unattested steps as if the papyrus prescribed them."
)
SCOPE_TH = (
    "Approved use: as cited historical inspiration for a clearly labelled original modern working about undoing harmful magic or seeking purification, not a reconstruction. "
    "Maqlu describes ancient anti-witchcraft ritual; generated content must not convert these incantations into instructions to harm people, property, or accused witches, "
    "and must not present unattested steps as if the tablets prescribed them."
)

P = []
