import { getFirestore } from "firebase-admin/firestore";
import { countSeedTypes, EXPECTED_LESSON_COUNT, loadContentSeed, findSeedViolations } from "../contentSeed";
import { inspectLessonInventory } from "../contentInventory";
import { createMissingSeedContent, findSeedImportConflicts } from "../seedContentImport";
import { CONTENT_PROJECT_ID, initializeContentProject, parseContentArgs } from "./contentProject";

/**
 * Admin CLI: safely imports the checked-in ancient-source seed pool
 * (`functions/ancientContentSeed.json`) into the `content` collection.
 *
 * Run locally by a trusted operator authenticated as a service account
 * (GOOGLE_APPLICATION_CREDENTIALS pointing at a key file) - never deployed
 * as a callable, same as generate-draft/approve-draft.
 *
 * Missing documents are created as drafts. Existing documents keep their
 * status and authored fields; only similarityGroupId is synchronized and the
 * retired difficulty field is removed. The remote operation is pinned to the
 * production project and followed by a read-only lesson inventory check.
 *
 * Usage:
 *   npm run seed-content                 # validate only, writes nothing
 *   npm run seed-content -- --project wiccan-android --commit
 */
async function main(): Promise<void> {
  const args = parseContentArgs(process.argv.slice(2), true);
  const entries = loadContentSeed();
  const violations = findSeedViolations(entries);
  const counts = countSeedTypes(entries);
  if (counts.lesson !== EXPECTED_LESSON_COUNT) {
    violations.push(`Expected ${EXPECTED_LESSON_COUNT} lessons, found ${counts.lesson}; spells and potions do not count.`);
  }
  if (violations.length > 0) throw new Error(`Seed pool failed validation:\n- ${violations.join("\n- ")}`);

  const needsRights = entries.filter((entry) => entry.rightsReviewRequired);
  console.log(
    `Seed pool: ${entries.length} drafts: ${counts.lesson} lessons, ${counts.spell} spells, ${counts.potion} potions.`,
  );
  console.log(
    `Rights review required before publishing: ${needsRights.length} entries ` +
      "(quoted phrasing from a modern translation, or unconfirmed museum prose).",
  );

  console.log(`Production target: ${CONTENT_PROJECT_ID}.`);
  if (!args.commit) {
    console.log(`Validation only. Import with --project ${CONTENT_PROJECT_ID} --commit.`);
    return;
  }

  initializeContentProject(args.project);
  console.log(`Reading and importing Firebase project: ${CONTENT_PROJECT_ID}.`);
  const db = getFirestore();
  const before = await db.collection("content").get();
  const existing = before.docs.map((doc) => ({ id: doc.id, fields: doc.data() }));
  const conflicts = findSeedImportConflicts(existing, entries);
  if (conflicts.length > 0) {
    throw new Error(`Import preflight failed; no documents were written:\n- ${conflicts.join("\n- ")}`);
  }

  const result = await createMissingSeedContent(db, entries);
  console.log(
    `Created ${result.createdIds.length} drafts; preserved ${result.preservedIds.length} existing documents; ` +
      `synchronized ${result.metadataSyncedIds.length} similarity identities.`,
  );
  console.log(`Preserved edited content IDs: ${JSON.stringify(result.preservedModifiedIds)}.`);

  const snapshot = await db.collection("content").get();
  const report = inspectLessonInventory(
    snapshot.docs.map((doc) => ({ id: doc.id, fields: doc.data() })),
    entries,
  );
  console.log(JSON.stringify(report, null, 2));
  if (report.issues.length > 0) {
    throw new Error("Import completed, but remote lesson verification failed. Existing content was preserved; review the report.");
  }
  console.log(`Verified ${report.lessonCount} lessons. New drafts remain unpublished and require editorial approval.`);
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
