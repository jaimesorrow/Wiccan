import fs from "node:fs";
import path from "node:path";
import { curriculumConcept, findLexicalDuplicateConcerns } from "../contentSimilarity";
import { findPracticalLessonViolations } from "../practicalLesson";

interface EditorialEntry {
  id: string;
  proposedFields: Record<string, unknown>;
}

const entries: EditorialEntry[] = [];
for (let batchNumber = 1; batchNumber <= 12; batchNumber += 1) {
  const filename = `batch-${String(batchNumber).padStart(2, "0")}.editorial.json`;
  const packet = JSON.parse(fs.readFileSync(path.join(process.cwd(), "rewriteBatches", filename), "utf8")) as {
    writes: boolean;
    firebaseReady: boolean;
    entries: EditorialEntry[];
  };
  if (packet.writes || packet.firebaseReady) throw new Error(`${filename} unexpectedly enables Firebase writes.`);
  entries.push(...packet.entries);
}

if (entries.length !== 296 || new Set(entries.map((entry) => entry.id)).size !== 296) {
  throw new Error(`Expected 296 distinct rewrite IDs; found ${entries.length} rows and ${new Set(entries.map((entry) => entry.id)).size} IDs.`);
}

const invalid = entries.flatMap((entry) => {
  const fields = entry.proposedFields;
  const words = String(fields.lesson).trim().split(/\s+/).length;
  const violations = findPracticalLessonViolations(fields, [], false);
  const expectedBlocker = violations.length === 1 && violations[0] === "verified source passage is missing from the editor-owned register";
  return words >= 250 && words <= 450 && expectedBlocker ? [] : [{ id: entry.id, words, violations }];
});
const duplicateConcerns = findLexicalDuplicateConcerns(entries.map((entry) =>
  curriculumConcept(entry.id, entry.proposedFields),
));

const result = {
  records: entries.length,
  distinctIds: new Set(entries.map((entry) => entry.id)).size,
  invalidRecords: invalid,
  lexicalDuplicateConcerns: duplicateConcerns,
  writesEnabled: false,
};
console.log(JSON.stringify(result, null, 2));
if (invalid.length || duplicateConcerns.length) process.exitCode = 1;
