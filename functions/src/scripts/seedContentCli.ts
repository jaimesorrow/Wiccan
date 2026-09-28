import { getApp, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { loadContentSeed, findSeedViolations } from "../contentSeed";

const CONTENT_COLLECTION = "content";
const BATCH_LIMIT = 400;

/**
 * Admin CLI: upserts the checked-in ancient-source seed pool
 * (`functions/ancientContentSeed.json`) into the `content` collection.
 *
 * Run locally by a trusted operator authenticated as a service account
 * (GOOGLE_APPLICATION_CREDENTIALS pointing at a key file) - never deployed
 * as a callable, same as generate-draft/approve-draft.
 *
 * Every document is written with status "draft": seeding fills the pool,
 * it never publishes. An existing document's status is left untouched, so
 * re-running after some entries were approved cannot silently unpublish
 * them. Document IDs are deterministic (derived from source and title), so
 * a re-run updates the same 150 documents instead of creating duplicates.
 *
 * Usage:
 *   npm run seed-content                 # validate only, writes nothing
 *   npm run seed-content -- --commit     # validate, then upsert
 */
async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const commit = args.includes("--commit");

  const entries = loadContentSeed();
  const violations = findSeedViolations(entries);
  if (violations.length > 0) {
    throw new Error(`Seed pool failed validation:\n- ${violations.join("\n- ")}`);
  }

  const needsRights = entries.filter((entry) => entry.rightsReviewRequired);
  console.log(`Seed pool: ${entries.length} entries, all status "draft".`);
  console.log(
    `Rights review required before publishing: ${needsRights.length} entries ` +
      "(quoted phrasing from a modern translation, or unconfirmed museum prose).",
  );

  if (!commit) {
    console.log("Validation only. Re-run with --commit to write to Firestore.");
    return;
  }

  initializeApp();
  // Printed before any write: this fills a real project's content pool, so
  // the operator gets one last look at which environment they're touching.
  console.log(`Operating against Firebase project: ${getApp().options.projectId ?? "(unknown)"}`);

  const db = getFirestore();
  let written = 0;
  for (let i = 0; i < entries.length; i += BATCH_LIMIT) {
    const batch = db.batch();
    for (const entry of entries.slice(i, i + BATCH_LIMIT)) {
      const { id, ...fields } = entry;
      const ref = db.collection(CONTENT_COLLECTION).doc(id);
      const existing = await ref.get();
      // Never regress an already-approved document back to draft.
      const payload = existing.exists
        ? { ...fields, status: (existing.get("status") as string | undefined) ?? "draft" }
        : fields;
      batch.set(ref, payload, { merge: true });
      written += 1;
    }
    await batch.commit();
  }

  console.log(`Upserted ${written} content documents. Nothing was published.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
