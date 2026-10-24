import fs from "node:fs";
import { parseArgs } from "node:util";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import type { DocumentData } from "firebase-admin/firestore";
import { contentBackupSha256, decodeFirestoreValue, encodeFirestoreValue, parseContentBackup } from "../contentBackup";
import type { BackupManifest } from "../reviewedContentChanges";
import { stableJson } from "../practicalLesson";
import { CONTENT_PROJECT_ID } from "./contentProject";

async function main(): Promise<void> {
  const { values } = parseArgs({ options: {
    artifact: { type: "string" }, manifest: { type: "string" }, output: { type: "string" },
  }, strict: true, allowPositionals: false });
  if (!values.artifact || !values.manifest || !values.output) throw new Error("--artifact, --manifest and --output are required.");
  if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Restore drills are permitted only with FIRESTORE_EMULATOR_HOST set.");
  const bytes = fs.readFileSync(values.artifact);
  const artifact = parseContentBackup(bytes);
  const manifest = JSON.parse(fs.readFileSync(values.manifest, "utf8")) as BackupManifest;
  const artifactIds = artifact.documents.map((document) => document.id).sort();
  if (artifact.projectId !== CONTENT_PROJECT_ID || manifest.projectId !== CONTENT_PROJECT_ID || manifest.artifactSha256 !== contentBackupSha256(bytes) ||
      !/^gs:\/\//.test(manifest.exportUri) || !manifest.objectGeneration ||
      JSON.stringify([...manifest.contentIds].sort()) !== JSON.stringify(artifactIds)) {
    throw new Error("Backup artifact and upload manifest do not agree.");
  }
  const restoreProject = `demo-wiccan-restore-${Date.now()}`;
  const db = getFirestore(initializeApp({ projectId: restoreProject }, `restore-${Date.now()}`));
  for (let index = 0; index < artifact.documents.length; index += 400) {
    const batch = db.batch();
    for (const document of artifact.documents.slice(index, index + 400)) {
      batch.create(db.collection("content").doc(document.id), decodeFirestoreValue(document.data, db) as DocumentData);
    }
    await batch.commit();
  }
  const restored = await db.collection("content").get();
  if (restored.size !== artifact.documents.length) throw new Error("Restore drill document count differs from the backup.");
  const expected = new Map(artifact.documents.map((document) => [document.id, stableJson(document.data)]));
  for (const document of restored.docs) {
    if (expected.get(document.id) !== stableJson(encodeFirestoreValue(document.data()))) throw new Error(`Restore drill mismatch for '${document.id}'.`);
  }
  const testedManifest: BackupManifest = { ...manifest, restoreTestedAt: new Date().toISOString() };
  fs.writeFileSync(values.output, JSON.stringify(testedManifest, null, 2) + "\n", { mode: 0o600, flag: "wx" });
}

main().catch((error: unknown) => { console.error(error instanceof Error ? error.message : error); process.exitCode = 1; });
