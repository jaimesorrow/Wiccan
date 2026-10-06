import { getFirestore } from "firebase-admin/firestore";
import { parseArgs } from "node:util";
import { FirestoreContentAdminRepository } from "../firestoreContentAdminRepository";
import { approveDraft } from "../approveDraft";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

/**
 * Admin CLI: flips one content document from "draft" to "published". Run
 * locally by a trusted operator authenticated as a service account
 * (GOOGLE_APPLICATION_CREDENTIALS pointing at a key file) — never deployed
 * as a public Cloud Function, per Session 17's lockdown of content
 * generation/approval.
 *
 * Usage: npm run approve-draft -- --project wiccan-android --contentId <id>
 */
async function main(): Promise<void> {
  const { values } = parseArgs({
    args: process.argv.slice(2),
    options: { project: { type: "string" }, contentId: { type: "string" } },
    strict: true,
    allowPositionals: false,
  });
  const contentId = values.contentId;
  if (!contentId?.trim() || contentId.includes("/")) {
    throw new Error(`Usage: --project ${CONTENT_PROJECT_ID} --contentId <document-id>`);
  }

  initializeContentProject(values.project);
  // Printed first and unconditionally: this publishes into a real
  // project's content pool, so the operator gets one last look at which
  // environment they're about to touch before anything happens.
  console.log(`Operating against Firebase project: ${CONTENT_PROJECT_ID}`);

  const repo = new FirestoreContentAdminRepository(getFirestore());
  await approveDraft(contentId, repo);
  console.log(`Approved '${contentId}'.`);
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
