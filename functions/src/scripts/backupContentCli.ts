import fs from "node:fs";
import { parseArgs } from "node:util";
import { getFirestore } from "firebase-admin/firestore";
import { captureContentBackup, contentBackupSha256, serializeContentBackup } from "../contentBackup";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

async function main(): Promise<void> {
  const { values } = parseArgs({ options: {
    project: { type: "string" }, output: { type: "string" }, report: { type: "string" },
  }, strict: true, allowPositionals: false });
  if (!values.output || !values.report) throw new Error("--output and --report paths are required.");
  if (values.project !== CONTENT_PROJECT_ID) throw new Error(`Explicit --project ${CONTENT_PROJECT_ID} is required.`);
  initializeContentProject(values.project);
  const artifact = await captureContentBackup(getFirestore(), CONTENT_PROJECT_ID);
  const bytes = serializeContentBackup(artifact);
  const report = {
    mode: "local-secure-capture", writesToFirestore: false, projectId: artifact.projectId,
    completedAt: artifact.completedAt, artifactSha256: contentBackupSha256(bytes),
    documentCount: artifact.documents.length, contentIds: artifact.documents.map((document) => document.id),
  };
  fs.writeFileSync(values.output, bytes, { mode: 0o600, flag: "wx" });
  fs.writeFileSync(values.report, JSON.stringify(report, null, 2) + "\n", { mode: 0o600, flag: "wx" });
}

main().catch((error: unknown) => { console.error(error instanceof Error ? error.message : error); process.exitCode = 1; });
