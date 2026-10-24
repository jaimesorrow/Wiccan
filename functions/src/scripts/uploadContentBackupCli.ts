import fs from "node:fs";
import { parseArgs } from "node:util";
import { getStorage } from "firebase-admin/storage";
import { contentBackupSha256, parseContentBackup } from "../contentBackup";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

async function main(): Promise<void> {
  const { values } = parseArgs({ options: {
    project: { type: "string" }, artifact: { type: "string" }, bucket: { type: "string" },
    object: { type: "string" }, approval: { type: "string" }, manifest: { type: "string" },
  }, strict: true, allowPositionals: false });
  if (!values.artifact || !values.bucket || !values.object || !values.manifest) throw new Error("--artifact, --bucket, --object and --manifest are required.");
  if (values.project !== CONTENT_PROJECT_ID) throw new Error(`Explicit --project ${CONTENT_PROJECT_ID} is required.`);
  const bytes = fs.readFileSync(values.artifact);
  const artifact = parseContentBackup(bytes);
  if (artifact.projectId !== CONTENT_PROJECT_ID) throw new Error("Backup artifact targets another project.");
  const sha256 = contentBackupSha256(bytes);
  if (values.approval !== sha256) throw new Error("Upload approval must match the exact backup artifact SHA-256.");
  if (values.object.startsWith("/") || values.object.endsWith("/") || values.object.includes("..")) throw new Error("Invalid backup object name.");
  initializeContentProject(values.project);
  const bucket = getStorage().bucket(values.bucket);
  const [bucketMetadata] = await bucket.getMetadata();
  if (bucketMetadata.iamConfiguration?.publicAccessPrevention !== "enforced") throw new Error("Backup bucket must enforce public access prevention.");
  const file = bucket.file(values.object);
  await file.save(bytes, {
    resumable: false,
    contentType: "application/gzip",
    metadata: { metadata: { sha256, projectId: CONTENT_PROJECT_ID, collectionPath: "content" } },
    preconditionOpts: { ifGenerationMatch: 0 },
  });
  const [fileMetadata] = await file.getMetadata();
  if (!fileMetadata.generation) throw new Error("Uploaded backup did not return an immutable object generation.");
  const manifest = {
    projectId: CONTENT_PROJECT_ID,
    exportUri: `gs://${values.bucket}/${values.object}`,
    artifactSha256: sha256,
    objectGeneration: String(fileMetadata.generation),
    completedAt: artifact.completedAt,
    contentIds: artifact.documents.map((document) => document.id),
  };
  fs.writeFileSync(values.manifest, JSON.stringify(manifest, null, 2) + "\n", { mode: 0o600, flag: "wx" });
}

main().catch((error: unknown) => { console.error(error instanceof Error ? error.message : error); process.exitCode = 1; });
