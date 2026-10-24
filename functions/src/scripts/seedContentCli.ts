import fs from "node:fs";
import { parseArgs } from "node:util";
import { getFirestore } from "firebase-admin/firestore";
import { countSeedTypes, loadContentSeed } from "../contentSeed";
import { auditPracticalContent } from "../practicalContentAudit";
import { applyReviewedContentChanges } from "../seedContentImport";
import { loadSourcePassages } from "../sourcePassages";
import type { BackupManifest, ReviewedChangeSet } from "../reviewedContentChanges";
import { changeSetDigest } from "../reviewedContentChanges";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

/** Established import entry point. No legacy pool writes; migration needs a reviewed patch. */
async function main(): Promise<void> {
  const { values: args } = parseArgs({ options: {
    project: { type: "string" }, commit: { type: "boolean", default: false },
    changeset: { type: "string" }, approval: { type: "string" }, backup: { type: "string" },
    output: { type: "string" },
  }, strict: true, allowPositionals: false });
  if (args.project !== undefined && args.project !== CONTENT_PROJECT_ID) throw new Error("Wrong content project.");
  const passages = loadSourcePassages();
  if (!args.changeset) {
    if (args.commit) throw new Error("Legacy seed import is blocked. Supply a reviewed --changeset, exact --approval digest and --backup manifest.");
    const entries = loadContentSeed();
    const audit = auditPracticalContent(entries.map(({ id, ...fields }) => ({ id, fields })), passages);
    console.log(JSON.stringify({ mode: "local-audit", writes: false, liveCountVerified: false, count: entries.length, types: countSeedTypes(entries), totals: audit.totals }, null, 2));
    return;
  }
  const changeSet = JSON.parse(fs.readFileSync(args.changeset, "utf8")) as ReviewedChangeSet;
  if (changeSet.projectId !== CONTENT_PROJECT_ID) throw new Error("Change set targets another project.");
  const backup = args.backup ? JSON.parse(fs.readFileSync(args.backup, "utf8")) as BackupManifest : undefined;
  initializeContentProject(args.project);
  const header = { mode: args.commit ? "approved-apply" : "dry-run", project: args.project, changeSetDigest: changeSetDigest(changeSet) };
  const results = await applyReviewedContentChanges(getFirestore(), changeSet, passages, { commit: args.commit, approval: args.approval, backup });
  const report = { ...header, generatedAt: new Date().toISOString(), results };
  const json = JSON.stringify(report, null, 2) + "\n";
  if (args.output) fs.writeFileSync(args.output, json, { mode: 0o600, flag: "wx" });
  else process.stdout.write(json);
  if (results.some((result) => result.outcome === "conflict")) process.exitCode = 1;
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
