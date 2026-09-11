import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { FirestoreContentAdminRepository } from "../firestoreContentAdminRepository";
import { approveDraft } from "../approveDraft";

/**
 * Admin CLI: flips one content document from "draft" to "published". Run
 * locally by a trusted operator authenticated as a service account
 * (GOOGLE_APPLICATION_CREDENTIALS pointing at a key file) — never deployed
 * as a public Cloud Function, per Session 17's lockdown of content
 * generation/approval.
 *
 * Usage: npm run approve-draft -- --contentId <id>
 */
async function main(): Promise<void> {
  const [flag, contentId] = process.argv.slice(2);
  if (flag !== "--contentId" || !contentId) {
    throw new Error("Usage: --contentId <id>");
  }

  initializeApp();
  const repo = new FirestoreContentAdminRepository(getFirestore());
  await approveDraft(contentId, repo);
  console.log(`Approved '${contentId}'.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
