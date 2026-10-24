import fs from "node:fs";
import { parseArgs } from "node:util";
import { getFirestore } from "firebase-admin/firestore";
import type { DocumentData } from "firebase-admin/firestore";
import { contentBackupSha256, decodeFirestoreValue, parseContentBackup } from "../contentBackup";
import { buildContentRollbackPlan, contentRollbackPlanDigest } from "../contentRollback";
import type { PostMigrationAudit } from "../contentRollback";
import type { BackupManifest, ReviewedChangeSet } from "../reviewedContentChanges";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

async function main(): Promise<void> {
  const { values } = parseArgs({ options: {
    project: { type: "string" }, artifact: { type: "string" }, manifest: { type: "string" },
    changeset: { type: "string" }, afterAudit: { type: "string" }, output: { type: "string" },
    approval: { type: "string" }, commit: { type: "boolean", default: false },
  }, strict: true, allowPositionals: false });
  if (!values.artifact || !values.manifest || !values.changeset || !values.afterAudit || !values.output) {
    throw new Error("--artifact, --manifest, --changeset, --afterAudit and --output are required.");
  }
  if (values.project !== CONTENT_PROJECT_ID) throw new Error(`Explicit --project ${CONTENT_PROJECT_ID} is required.`);
  const bytes = fs.readFileSync(values.artifact);
  const artifact = parseContentBackup(bytes);
  const sha256 = contentBackupSha256(bytes);
  const manifest = JSON.parse(fs.readFileSync(values.manifest, "utf8")) as BackupManifest;
  const changeSet = JSON.parse(fs.readFileSync(values.changeset, "utf8")) as ReviewedChangeSet;
  const afterAudit = JSON.parse(fs.readFileSync(values.afterAudit, "utf8")) as PostMigrationAudit;
  const plan = buildContentRollbackPlan(artifact, sha256, afterAudit, changeSet);
  const digest = contentRollbackPlanDigest(plan);
  const artifactIds = artifact.documents.map((document) => document.id).sort();
  if (manifest.projectId !== CONTENT_PROJECT_ID || manifest.artifactSha256 !== sha256 || !manifest.objectGeneration ||
      !manifest.restoreTestedAt || !Number.isFinite(Date.parse(manifest.restoreTestedAt)) ||
      JSON.stringify([...manifest.contentIds].sort()) !== JSON.stringify(artifactIds)) {
    throw new Error("Rollback requires the matching restore-tested backup manifest.");
  }
  if (values.commit && values.approval !== digest) throw new Error("Rollback approval must match the exact rollback-plan SHA-256.");
  if (plan.entries.length > 400) throw new Error("Rollback exceeds the single-transaction safety limit; split and separately approve reviewed change sets.");
  initializeContentProject(values.project);
  const db = getFirestore();
  const backupById = new Map(artifact.documents.map((document) => [document.id, document]));
  const results = await db.runTransaction(async (transaction) => {
    const refs = plan.entries.map((entry) => db.collection("content").doc(entry.id));
    const snapshots = await transaction.getAll(...refs);
    const conflicts = snapshots.flatMap((current, index) => {
      const entry = plan.entries[index];
      const updateTime = current.updateTime ? `${current.updateTime.seconds}:${current.updateTime.nanoseconds}` : undefined;
      if (!current.exists) return [{ id: entry.id, outcome: "conflict", reason: "document missing; rollback never recreates IDs" }];
      if (updateTime !== entry.expectedUpdateTime) return [{ id: entry.id, outcome: "conflict", reason: "document changed after the post-migration audit" }];
      return [];
    });
    if (conflicts.length) return conflicts;
    if (values.commit) {
      refs.forEach((ref, index) => {
        const backup = backupById.get(plan.entries[index].id)!;
        transaction.set(ref, decodeFirestoreValue(backup.data, db) as DocumentData);
      });
    }
    return plan.entries.map((entry) => ({ id: entry.id, outcome: values.commit ? "restored" : "would-restore" }));
  });
  const report = { mode: values.commit ? "approved-rollback" : "rollback-dry-run", projectId: CONTENT_PROJECT_ID, rollbackPlanDigest: digest, results };
  fs.writeFileSync(values.output, JSON.stringify(report, null, 2) + "\n", { mode: 0o600, flag: "wx" });
  if (results.some((result) => result.outcome === "conflict")) process.exitCode = 1;
}

main().catch((error: unknown) => { console.error(error instanceof Error ? error.message : error); process.exitCode = 1; });
