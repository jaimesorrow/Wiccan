import { getFirestore } from "firebase-admin/firestore";
import { backfillContentUsage, planContentUsageBackfill } from "../contentUsageBackfill";
import { CONTENT_PROJECT_ID, initializeContentProject, parseContentArgs } from "./contentProject";

async function main(): Promise<void> {
  const args = parseContentArgs(process.argv.slice(2), true);
  initializeContentProject(args.project);
  console.log(`Firebase project: ${CONTENT_PROJECT_ID}. Mode: ${args.commit ? "create missing markers" : "dry run"}.`);
  const db = getFirestore();
  const history = await db.collection("userContentHistory").get();
  const markers = planContentUsageBackfill(history.docs.map((doc) => ({ id: doc.id, fields: doc.data() })));
  const summary = await backfillContentUsage(db, markers, args.commit);
  console.log(JSON.stringify(summary, null, 2));
  if (!args.commit && summary.missing) {
    console.log("Missing delivery markers must be backfilled with --commit before deploying the restricted content rules.");
  }
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
