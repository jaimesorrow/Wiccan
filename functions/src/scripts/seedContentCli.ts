import { getFirestore } from "firebase-admin/firestore";
import { countSeedTypes, EXPECTED_LESSON_COUNT, loadContentSeed, findSeedViolations } from "../contentSeed";
import { inspectLessonInventory } from "../contentInventory";
import { createMissingSeedContent, findSeedImportConflicts } from "../seedContentImport";
import { CONTENT_PROJECT_ID, initializeContentProject, parseContentArgs } from "./contentProject";

/** Validate locally by default; explicitly pinned, create-only production import. */
async function main(): Promise<void> {
  const args = parseContentArgs(process.argv.slice(2), true);
  const entries = loadContentSeed();
  const violations = findSeedViolations(entries);
  const counts = countSeedTypes(entries);
  if (counts.lesson !== EXPECTED_LESSON_COUNT) {
    violations.push(`Expected ${EXPECTED_LESSON_COUNT} lessons, found ${counts.lesson}; spells and potions do not count.`);
  }
  if (violations.length > 0) throw new Error(`Seed pool failed validation:\n- ${violations.join("\n- ")}`);

  console.log(`Local seed: ${entries.length} drafts: ${counts.lesson} lessons, ${counts.spell} spells, ${counts.potion} potions.`);
  console.log(`Outstanding rights review: ${entries.filter((entry) => entry.rightsReviewRequired).length} entries.`);
  console.log(`Production target: ${CONTENT_PROJECT_ID}.`);
  if (!args.commit) {
    console.log(`Local validation only; Firebase has not been accessed. Import: --project ${CONTENT_PROJECT_ID} --commit`);
    return;
  }

  initializeContentProject(args.project);
  console.log(`Reading and importing Firebase project: ${CONTENT_PROJECT_ID}.`);
  const db = getFirestore();
  const before = await db.collection("content").get();
  const existing = before.docs.map((doc) => ({ id: doc.id, fields: doc.data() }));
  console.log(`Preflight: ${existing.length} existing content documents; ${existing.filter((doc) => doc.fields.type === "lesson").length} actual lessons.`);
  const conflicts = findSeedImportConflicts(existing, entries);
  if (conflicts.length) throw new Error(`Import preflight failed; no documents were written:\n- ${conflicts.join("\n- ")}`);
  const result = await createMissingSeedContent(db, entries);
  console.log(`Created ${result.createdIds.length} drafts; preserved ${result.preservedIds.length} existing documents in full.`);
  console.log(`Preserved edited content IDs: ${JSON.stringify(result.preservedModifiedIds)}.`);
  const snapshot = await db.collection("content").get();
  const report = inspectLessonInventory(snapshot.docs.map((doc) => ({ id: doc.id, fields: doc.data() })), entries);
  console.log(JSON.stringify(report, null, 2));
  if (report.issues.length) {
    throw new Error("Import completed, but remote lesson verification failed. Existing content was preserved; review the report.");
  }
  console.log(`Verified ${report.lessonCount} lessons, including all ${EXPECTED_LESSON_COUNT} manifest lessons. New drafts remain unpublished and require editorial approval.`);
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
