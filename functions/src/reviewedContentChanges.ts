import { createHash } from "node:crypto";
import { AUTHORED_FIELDS, findPracticalLessonViolations, stableJson } from "./practicalLesson";
import type { SourcePassage } from "./sourcePassages";

export interface ReviewedChange { id: string; expectedUpdateTime: string; fields: Record<string, unknown>; subjectChangeReason?: string }
export interface ReviewedChangeSet { projectId: string; reviewId: string; entries: ReviewedChange[] }
export interface BackupManifest { projectId: string; exportUri: string; artifactSha256: string; completedAt: string; restoreTestedAt: string; contentIds: string[] }
const PATCH_FIELDS = new Set<string>([...AUTHORED_FIELDS.filter((field) => !["type", "tier", "rightsReviewRequired", "rightsNotes", "similarityGroupId"].includes(field)), "practicalReview", "contentStatus"]);
export function changeSetDigest(changeSet: ReviewedChangeSet): string {
  return createHash("sha256").update(stableJson(changeSet)).digest("hex");
}
export function assertReviewedApply(changeSet: ReviewedChangeSet, approval: string | undefined, backup: BackupManifest | undefined): void {
  if (changeSet.projectId !== "wiccan-android" || !changeSet.reviewId?.trim() || !Array.isArray(changeSet.entries)) throw new Error("A project-specific reviewed change set is required.");
  if (new Set(changeSet.entries.map((entry) => entry.id)).size !== changeSet.entries.length) throw new Error("Duplicate document IDs in reviewed changes.");
  if (approval !== changeSetDigest(changeSet)) throw new Error("Explicit approval must match the SHA-256 digest of this exact reviewed change set.");
  if (!backup || backup.projectId !== changeSet.projectId || !/^gs:\/\//.test(backup.exportUri) || !/^[a-f0-9]{64}$/.test(backup.artifactSha256) || !Number.isFinite(Date.parse(backup.completedAt)) || !Number.isFinite(Date.parse(backup.restoreTestedAt)) || Date.parse(backup.restoreTestedAt) < Date.parse(backup.completedAt) || !Array.isArray(backup.contentIds) || changeSet.entries.some((entry) => !backup.contentIds.includes(entry.id))) {
    throw new Error("A secure backup manifest covering every ID and a completed restore drill is required.");
  }
}

export function planReviewedChange(current: Record<string, unknown> | null, updateTime: string | undefined, delivered: boolean, change: ReviewedChange, passages: SourcePassage[]) {
  if (!current) return { outcome: "conflict", reason: "document missing; IDs are never recreated" } as const;
  if (!change.id || change.id.includes("/") || !/^\d+:\d{1,9}$/.test(change.expectedUpdateTime) || !change.fields || Array.isArray(change.fields) || typeof change.fields !== "object") return { outcome: "conflict", reason: "invalid reviewed identity/version/patch" } as const;
  const keys = Object.keys(change.fields);
  if (!keys.length || keys.some((key) => !PATCH_FIELDS.has(key))) return { outcome: "conflict", reason: "patch includes unapproved or protected fields" } as const;
  if (keys.every((key) => stableJson(current[key]) === stableJson(change.fields[key]))) return { outcome: "unchanged" } as const;
  if (current.status !== "draft" || delivered) return { outcome: "conflict", reason: "published, withdrawn or previously delivered; compatibility/versioning proposal required" } as const;
  if (change.fields.contentStatus !== undefined && change.fields.contentStatus !== current.status) return { outcome: "conflict", reason: "contentStatus must preserve and mirror the authoritative status" } as const;
  if (updateTime !== change.expectedUpdateTime) return { outcome: "conflict", reason: "document changed after review" } as const;
  if (change.fields.sourceId !== undefined && change.fields.sourceId !== current.sourceId && !change.subjectChangeReason?.trim()) return { outcome: "conflict", reason: "changed source requires an explicit subject/provenance review note" } as const;
  const merged = { ...current, ...change.fields };
  const violations = findPracticalLessonViolations(merged, passages);
  if (violations.length) return { outcome: "conflict", reason: violations.join("; ") } as const;
  const patch = Object.fromEntries(keys.filter((key) => stableJson(current[key]) !== stableJson(change.fields[key])).map((key) => [key, change.fields[key]]));
  return { outcome: "ready", patch } as const;
}
