#!/usr/bin/env python3
import json
from copy import deepcopy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "sourcePassages.json"
NEW = Path(__file__).with_name("new_passages.json")

REQUIRED = ["id","sourceId","textTitle","manuscriptId","tradition","manuscriptDate","edition","editionDate","translator","locator","inspectedPassage"]

def web_url(value):
    try:
        from urllib.parse import urlparse
        u = urlparse(value)
        return isinstance(value, str) and value.strip() and u.scheme in {"http", "https"} and bool(u.netloc)
    except Exception:
        return False

def find_violations(p):
    issues = []
    for key in REQUIRED:
        if not (isinstance(p.get(key), str) and p[key].strip()):
            issues.append(f"passage.{key} is required")
    if not web_url(p.get("url")):
        issues.append("passage.url must locate the inspected passage")
    d = p.get("dating") or {}
    if d.get("era") not in {"ancient","medieval","early-modern"} or d.get("status") != "verified" or not web_url(d.get("evidenceUrl")) or not (isinstance(d.get("notes"), str) and d["notes"].strip()):
        issues.append("dating")
    r = p.get("rights") or {}
    if r.get("status") != "verified" or not (isinstance(r.get("basis"), str) and r["basis"].strip()) or not web_url(r.get("reference")) or not r.get("jurisdictions"):
        issues.append("rights")
    s = p.get("scope") or {}
    if s.get("status") != "approved" or not (isinstance(s.get("rationale"), str) and s["rationale"].strip()) or not web_url(s.get("evidenceUrl")):
        issues.append("scope")
    inst = p.get("attestedInstructions")
    if not isinstance(inst, list) or not inst or not all(isinstance(i, str) and i.strip() for i in inst):
        issues.append("attestedInstructions")
    if not ((isinstance(p.get("formula"), str) and p["formula"].strip()) or (isinstance(p.get("formulaOmissionReason"), str) and p["formulaOmissionReason"].strip())):
        issues.append("formula")
    rev = p.get("review") or {}
    if not rev.get("passageInspected") or not rev.get("sourceFidelityApproved") or not rev.get("safetyApproved"):
        issues.append("review flags")
    if not (isinstance(rev.get("reviewedBy"), str) and rev["reviewedBy"].strip()) or not rev.get("reviewedAt"):
        issues.append("review meta")
    # first-published-before-1931 check
    try:
        year = int(str(p.get("editionDate","")).strip()[:4])
        if year >= 1931:
            issues.append(f"editionDate {year} is not before 1931")
    except ValueError:
        issues.append("editionDate not a year")
    return issues

def main():
    existing = json.loads(SRC.read_text())
    new = json.loads(NEW.read_text())
    ids = [p["id"] for p in existing]
    for p in existing + new:
        v = find_violations(p)
        if v:
            raise SystemExit(f"VIOLATIONS {p.get('id')}: {v}")
    seen = set(ids)
    for p in new:
        if p["id"] in seen:
            raise SystemExit(f"duplicate id {p['id']}")
        seen.add(p["id"])
        if int(str(p["editionDate"])[:4]) >= 1931:
            raise SystemExit(f"{p['id']} editionDate not before 1931")
    merged = existing + new
    SRC.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n")
    print(f"existing {len(existing)}; new {len(new)}; total {len(merged)}")

if __name__ == "__main__":
    main()
