import { getFirestore } from "firebase-admin/firestore";
import { inspectLessonInventory } from "../contentInventory";
import { countSeedTypes, EXPECTED_LESSON_COUNT, findSeedViolations, loadContentSeed } from "../contentSeed";
import { CONTENT_PROJECT_ID, initializeContentProject, parseContentArgs } from "./contentProject";

/** Read-only production inventory; never publishes, updates, or removes content. */
async function main(): Promise<void> {
  const args = parseContentArgs(process.argv.slice(2), false);
  const seed = loadContentSeed();
  const violations = findSeedViolations(seed);
  if (countSeedTypes(seed).lesson !== EXPECTED_LESSON_COUNT) {
    violations.push(`Local manifest must contain exactly ${EXPECTED_LESSON_COUNT} lessons.`);
  }
  if (violations.length) throw new Error(`Invalid local seed:\n- ${violations.join("\n- ")}`);
  initializeContentProject(args.project);
  console.log(`Read-only Firebase verification: ${CONTENT_PROJECT_ID}.`);
  const snapshot = await getFirestore().collection("content").get();
  const report = inspectLessonInventory(snapshot.docs.map((doc) => ({ id: doc.id, fields: doc.data() })), seed);
  console.log(JSON.stringify(report, null, 2));
  if (report.issues.length) throw new Error("Remote lesson verification failed; see the actual document counts and issues above.");
  console.log(`Verified ${report.lessonCount} actual lessons, including all ${seed.filter((entry) => entry.type === "lesson").length} manifest lessons. Drafts require editorial approval before delivery.`);
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
