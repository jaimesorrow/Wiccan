import { createHash } from "node:crypto";
import type { ContentBackupArtifact } from "./contentBackup";
import { stableJson } from "./practicalLesson";
import type { ReviewedChangeSet } from "./reviewedContentChanges";

export interface PostMigrationAuditRecord { id: string; updateTime: string | null }
export interface PostMigrationAudit { inventoryKind: string; project?: string; records: PostMigrationAuditRecord[] }
export interface ContentRollbackEntry { id: string; expectedUpdateTime: string }
export interface ContentRollbackPlan {
  projectId: string;
  backupArtifactSha256: string;
  changeSetReviewId: string;
  entries: ContentRollbackEntry[];
}
export function buildContentRollbackPlan(
  artifact: ContentBackupArtifact,
  artifactSha256: string,
  afterAudit: PostMigrationAudit,
  changeSet: ReviewedChangeSet,
): ContentRollbackPlan {
  if (artifact.projectId !== "wiccan-android" || changeSet.projectId !== artifact.projectId || afterAudit.inventoryKind !== "live-firestore" || afterAudit.project !== artifact.projectId) {
    throw new Error("Backup, audit, and change set must target wiccan-android.");
  }
  if (!/^[a-f0-9]{64}$/.test(artifactSha256)) throw new Error("Backup artifact SHA-256 is invalid.");
  const backupIds = new Set(artifact.documents.map((document) => document.id));
  const auditById = new Map(afterAudit.records.map((record) => [record.id, record]));
  const entries = changeSet.entries.map((change) => {
    const auditRecord = auditById.get(change.id);
    if (!backupIds.has(change.id)) throw new Error(`Backup does not contain '${change.id}'.`);
    if (!auditRecord || typeof auditRecord.updateTime !== "string" || !/^\d+:\d{1,9}$/.test(auditRecord.updateTime)) throw new Error(`Post-migration update time is missing for '${change.id}'.`);
    return { id: change.id, expectedUpdateTime: auditRecord.updateTime };
  }).sort((left, right) => left.id.localeCompare(right.id));
  return { projectId: artifact.projectId, backupArtifactSha256: artifactSha256, changeSetReviewId: changeSet.reviewId, entries };
}

export function contentRollbackPlanDigest(plan: ContentRollbackPlan): string {
  return createHash("sha256").update(stableJson(plan)).digest("hex");
}
