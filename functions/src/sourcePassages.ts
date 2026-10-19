import fs from "node:fs";
import path from "node:path";

/** Editor-owned evidence. Never populated or approved by the model. */
export interface SourcePassage {
  id: string;
  sourceId: string;
  textTitle?: string;
  manuscriptId?: string;
  tradition?: string;
  manuscriptDate?: string;
  compositionDate?: string;
  edition?: string;
  editionDate?: string;
  translator?: string;
  locator?: string;
  url?: string;
  inspectedPassage?: string;
  attestedInstructions?: string[];
  uncertainties?: string[];
  formula?: string | null;
  formulaOmissionReason?: string;
  timing?: string;
  dating?: { era: string; status: string; evidenceUrl: string; notes: string };
  rights?: { status: string; basis: string; reference: string; jurisdictions: string[] };
  scope?: { status: string; rationale: string; evidenceUrl: string };
  review?: {
    passageInspected: boolean;
    sourceFidelityApproved: boolean;
    safetyApproved: boolean;
    reviewedBy: string;
    reviewedAt: string;
  };
}

export const nonblank = (value: unknown): value is string => typeof value === "string" && value.trim().length > 0;
const webUrl = (value: unknown): boolean => {
  try { return nonblank(value) && ["https:", "http:"].includes(new URL(value).protocol); } catch { return false; }
};

/** Structural evidence checks support, but cannot replace, inspection by an editor. */
export function findPassageViolations(passage: SourcePassage): string[] {
  const issues: string[] = [];
  for (const key of ["id", "sourceId", "textTitle", "manuscriptId", "tradition", "manuscriptDate", "edition", "editionDate", "translator", "locator", "inspectedPassage"] as const) {
    if (!nonblank(passage[key])) issues.push(`passage.${key} is required`);
  }
  if (!webUrl(passage.url)) issues.push("passage.url must locate the inspected passage");
  if (passage.dating?.era !== "ancient" || passage.dating.status !== "verified" || !webUrl(passage.dating.evidenceUrl) || !nonblank(passage.dating.notes)) {
    issues.push("ancient manuscript dating needs verified catalogue/scholarly evidence");
  }
  if (passage.rights?.status !== "verified" || !nonblank(passage.rights.basis) || !webUrl(passage.rights.reference) || !passage.rights.jurisdictions?.length || !passage.rights.jurisdictions.every(nonblank)) {
    issues.push("commercial-use rights for the exact edition/translation need verification by jurisdiction");
  }
  if (passage.scope?.status !== "approved" || !nonblank(passage.scope.rationale) || !webUrl(passage.scope.evidenceUrl)) {
    issues.push("Wiccan/witchcraft scope needs a reasoned review and supporting reference");
  }
  if (!Array.isArray(passage.attestedInstructions) || !passage.attestedInstructions.length || !passage.attestedInstructions.every(nonblank)) {
    issues.push("source-attested instructions are required before authoring");
  }
  if (!nonblank(passage.formula) && !nonblank(passage.formulaOmissionReason)) issues.push("record why a formula is absent");
  if (!passage.review?.passageInspected || !passage.review.sourceFidelityApproved || !passage.review.safetyApproved || !nonblank(passage.review.reviewedBy) || !nonblank(passage.review.reviewedAt) || !Number.isFinite(Date.parse(passage.review.reviewedAt))) {
    issues.push("passage inspection, fidelity and suitability require a dated editorial review");
  }
  return issues;
}

export function loadSourcePassages(registerPath = path.join(__dirname, "..", "sourcePassages.json")): SourcePassage[] {
  const raw: unknown = JSON.parse(fs.readFileSync(registerPath, "utf8"));
  if (!Array.isArray(raw)) throw new Error("Source passage register must be an array.");
  const ids = new Set<string>();
  for (const entry of raw) {
    const record = entry && typeof entry === "object" ? entry as Record<string, unknown> : null;
    if (!record || !nonblank(record.id) || !nonblank(record.sourceId) || ids.has(record.id)) {
      throw new Error("Source passage register has a missing or duplicate identity.");
    }
    ids.add(record.id);
  }
  return raw as SourcePassage[];
}

export function citationForPassage(passage: SourcePassage): Record<string, string> {
  return Object.fromEntries(["textTitle", "manuscriptId", "tradition", "manuscriptDate", "compositionDate", "edition", "editionDate", "translator", "locator", "url"].map((key) => [key, passage[key as keyof SourcePassage] ?? ""])) as Record<string, string>;
}
