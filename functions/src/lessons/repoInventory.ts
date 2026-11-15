import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { CanonicalLesson, toCanonicalLesson } from "./canonicalLesson";
import { IndexedLesson, indexLesson } from "./duplicates";

export interface InventorySource {
  name: string;
  records: number;
  /** Mapping issues per record ID (legacy records only). */
  mappingIssues: Record<string, string[]>;
}

export interface Inventory {
  lessons: IndexedLesson[];
  sources: InventorySource[];
  /** Sources that were expected but could not be read; generation must not proceed while non-empty. */
  missing: string[];
}

/** New-schema lesson files: functions/lessons/drafts/<id>.json, one canonical lesson each. */
export function canonicalDraftDir(functionsRoot: string): string {
  return join(functionsRoot, "lessons", "drafts");
}

export function readCanonicalDrafts(functionsRoot: string): CanonicalLesson[] {
  const dir = canonicalDraftDir(functionsRoot);
  if (!existsSync(dir)) return [];
  return readdirSync(dir).filter((name) => name.endsWith(".json")).sort()
    .map((name) => JSON.parse(readFileSync(join(dir, name), "utf8")) as CanonicalLesson);
}

/**
 * Every lesson checked into the repository: the legacy seed pool, every
 * rewrite-batch proposal, and new canonical drafts. Rejected and archived
 * records still count for duplicate prevention.
 */
export function loadRepoInventory(functionsRoot: string): Inventory {
  const lessons: IndexedLesson[] = [];
  const sources: InventorySource[] = [];
  const missing: string[] = [];

  const seedPath = join(functionsRoot, "ancientContentSeed.json");
  if (existsSync(seedPath)) {
    const seed = JSON.parse(readFileSync(seedPath, "utf8")) as Array<Record<string, unknown>>;
    const issues: Record<string, string[]> = {};
    seed.forEach((record) => {
      const { lesson, issues: found } = toCanonicalLesson(String(record.id), record);
      if (found.length) issues[lesson.id] = found;
      lessons.push(indexLesson(lesson, "repo:ancientContentSeed.json"));
    });
    sources.push({ name: "repo:ancientContentSeed.json", records: seed.length, mappingIssues: issues });
  } else {
    missing.push("repo:ancientContentSeed.json");
  }

  const batchDir = join(functionsRoot, "rewriteBatches");
  if (existsSync(batchDir)) {
    let count = 0;
    readdirSync(batchDir).filter((name) => name.endsWith(".json")).sort().forEach((name) => {
      const batch = JSON.parse(readFileSync(join(batchDir, name), "utf8")) as { entries?: Array<Record<string, unknown>> };
      (batch.entries ?? []).forEach((entry) => {
        const fields = entry.proposedFields as Record<string, unknown> | undefined;
        if (!fields) return;
        const { lesson } = toCanonicalLesson(`${String(entry.id)}#rewrite`, { ...fields, primaryCategoryId: entry.proposedPrimaryCategoryId });
        lessons.push(indexLesson(lesson, `repo:rewriteBatches/${name}`));
        count++;
      });
    });
    sources.push({ name: "repo:rewriteBatches", records: count, mappingIssues: {} });
  }

  const drafts = readCanonicalDrafts(functionsRoot);
  drafts.forEach((lesson) => lessons.push(indexLesson(lesson, "repo:lessons/drafts")));
  sources.push({ name: "repo:lessons/drafts", records: drafts.length, mappingIssues: {} });

  return { lessons, sources, missing };
}

/** Practical (performable, step-by-step) versus informational, for the legacy audit. */
export function classifyPracticality(lesson: CanonicalLesson): { practical: boolean; reasons: string[] } {
  const reasons: string[] = [];
  const steps = lesson.lesson.split(/\n\s*\n|\n(?=\s*\d+[.)]\s)/).map((s) => s.trim()).filter(Boolean);
  if (steps.length < 3) reasons.push(`only ${steps.length} step(s) of guidance`);
  if (lesson.tools.length === 0) reasons.push("no tools or materials");
  const words = lesson.lesson.split(/\s+/).filter(Boolean).length;
  if (words < 120) reasons.push(`guided lesson is ${words} words`);
  if (/\b(examines|this lesson (examines|explores|looks at)|historians|scholars|museum)\b/i.test(lesson.introduction)) {
    reasons.push("introduction frames it as historical study");
  }
  if (/\b(journal|reflect|write the|record the)\b/i.test(lesson.practice) && reasons.length) reasons.push("practice is reflection only");
  return { practical: reasons.length === 0, reasons };
}
