import { findPracticalLessonViolations } from "./practicalLesson";
import { loadSourcePassages } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { assertReviewedApply, planReviewedChange } from "./reviewedContentChanges";
import type { BackupManifest, ReviewedChangeSet } from "./reviewedContentChanges";
import { FieldValue, Firestore } from "firebase-admin/firestore";
import { SeedContent } from "./types";
import { InventoryDocument } from "./contentInventory";

const TRANSACTION_LIMIT = 400;

export interface SeedImportResult {
  createdIds: string[];
  preservedIds: string[];
  preservedModifiedIds: string[];
  metadataSyncedIds: string[];
}

/** Read-only preflight; a different document ID is never used to create a duplicate twin. */
export function findSeedImportConflicts(documents: InventoryDocument[], entries: SeedContent[]): string[] {
  const existingIds = new Map(documents.map((document) => [document.id, document]));
  const conflicts: string[] = [];
  const normalize = (value: unknown): string => typeof value === "string" ? value.trim().replace(/\s+/g, " ").toLowerCase() : "";
  for (const entry of entries) {
    const existing = existingIds.get(entry.id);
    if (existing) {
      if (existing.fields.type !== entry.type || existing.fields.sourceId !== entry.sourceId) {
        conflicts.push(`ID '${entry.id}' already belongs to another type or source.`);
      }
      continue;
    }
    const title = normalize(entry.title);
    const body = normalize(entry.body);
    const twin = documents.find((document) => normalize(document.fields.title) === title || normalize(document.fields.body) === body);
    if (twin) conflicts.push(`Missing seed ID '${entry.id}' duplicates the title or body of existing ID '${twin.id}'.`);
  }
  return conflicts;
}

/**
 * Create missing documents while preserving existing draft/published content.
 * Existing documents receive only the current editorial similarity identity
 * and removal of the retired difficulty field; their status and authored
 * fields are left untouched. Transaction retries protect concurrent edits.
 */
export async function createMissingSeedContent(db: Firestore, entries: SeedContent[], passages: SourcePassage[] = loadSourcePassages()): Promise<SeedImportResult> {
  const result: SeedImportResult = {
    createdIds: [],
    preservedIds: [],
    preservedModifiedIds: [],
    metadataSyncedIds: [],
  };
  for (let i = 0; i < entries.length; i += TRANSACTION_LIMIT) {
    const chunk = entries.slice(i, i + TRANSACTION_LIMIT);
    const refs = chunk.map((entry) => db.collection("content").doc(entry.id));
    const completed = await db.runTransaction(async (transaction) => {
      const snapshots = await transaction.getAll(...refs);
      const createdIds: string[] = [];
      const preservedIds: string[] = [];
      const preservedModifiedIds: string[] = [];
      const metadataSyncedIds: string[] = [];
      chunk.forEach((entry, index) => {
        const existing = snapshots[index];
        if (existing.exists) {
          if (existing.get("type") !== entry.type || existing.get("sourceId") !== entry.sourceId) {
            throw new Error(`Seed ID '${entry.id}' collides with an existing document of another type or source. Nothing in this transaction was written.`);
          }
          preservedIds.push(entry.id);
          if (["title", "intro", "body", "reflection"].some((field) => existing.get(field) !== entry[field as keyof SeedContent])) {
            preservedModifiedIds.push(entry.id);
          }
          // Preserve editorial identity, rights decisions and all other existing metadata.
          return;
        }
        const violations = findPracticalLessonViolations(entry, passages);
        if (violations.length) throw new Error(`Seed '${entry.id}' needs passage/editorial review before import: ${violations.join("; ")}`);
        const { id, ...fields } = entry;
        transaction.create(refs[index], {
          ...fields,
          status: "draft",
          contentStatus: "draft",
          createdAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        });
        createdIds.push(id);
      });
      return { createdIds, preservedIds, preservedModifiedIds, metadataSyncedIds };
    });
    result.createdIds.push(...completed.createdIds);
    result.preservedIds.push(...completed.preservedIds);
    result.preservedModifiedIds.push(...completed.preservedModifiedIds);
    result.metadataSyncedIds.push(...completed.metadataSyncedIds);
  }
  return result;
}


/** Extends the established importer; defaults to read-only and preserves protected metadata. */
export async function applyReviewedContentChanges(
  db: Firestore,
  changeSet: ReviewedChangeSet,
  passages: SourcePassage[],
  options: { commit?: boolean; approval?: string; backup?: BackupManifest } = {},
): Promise<{ id: string; outcome: string; reason?: string }[]> {
  if (changeSet.projectId !== "wiccan-android" || !changeSet.reviewId?.trim() || !Array.isArray(changeSet.entries)) throw new Error("Invalid target/review change set.");
  if (options.commit) assertReviewedApply(changeSet, options.approval, options.backup);
  if (new Set(changeSet.entries.map((entry) => entry.id)).size !== changeSet.entries.length) throw new Error("Duplicate IDs in changes.");
  if (changeSet.entries.length > TRANSACTION_LIMIT) throw new Error(`Reviewed change set exceeds the ${TRANSACTION_LIMIT}-document atomic limit; split it and approve each digest separately.`);
  if (changeSet.entries.some((change) => !change.id || change.id.includes("/"))) throw new Error("Invalid document ID.");
  return db.runTransaction(async (transaction) => {
    const refs = changeSet.entries.map((change) => db.collection("content").doc(change.id));
    const snapshots = await transaction.getAll(...refs);
    const deliveredIds = new Set<string>();
    for (let index = 0; index < changeSet.entries.length; index += 30) {
      const ids = changeSet.entries.slice(index, index + 30).map((entry) => entry.id);
      const history = await transaction.get(db.collection("userContentHistory").where("contentId", "in", ids).select("contentId"));
      history.docs.forEach((document) => {
        const contentId = document.get("contentId");
        if (typeof contentId === "string") deliveredIds.add(contentId);
      });
    }
    const plans = changeSet.entries.map((change, index) => {
      const snapshot = snapshots[index];
      const version = snapshot.updateTime ? `${snapshot.updateTime.seconds}:${snapshot.updateTime.nanoseconds}` : undefined;
      return planReviewedChange(snapshot.exists ? snapshot.data()! : null, version, deliveredIds.has(change.id), change, passages);
    });
    if (plans.some((plan) => plan.outcome === "conflict")) {
      return plans.map((plan, index) => plan.outcome === "ready"
        ? { id: changeSet.entries[index].id, outcome: "conflict", reason: "atomic change set contains another conflict; no documents were written" }
        : { id: changeSet.entries[index].id, ...plan });
    }
    if (options.commit) {
      plans.forEach((plan, index) => {
        if (plan.outcome === "ready") transaction.update(refs[index], { ...plan.patch, updatedAt: FieldValue.serverTimestamp() });
      });
    }
    return plans.map((plan, index) => ({
      id: changeSet.entries[index].id,
      outcome: plan.outcome === "ready" ? (options.commit ? "updated-draft" : "would-update-draft") : plan.outcome,
    }));
  });
}
