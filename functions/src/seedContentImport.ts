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
export async function createMissingSeedContent(db: Firestore, entries: SeedContent[]): Promise<SeedImportResult> {
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
          transaction.set(refs[index], {
            difficulty: FieldValue.delete(),
            similarityGroupId: entry.similarityGroupId,
          }, { merge: true });
          metadataSyncedIds.push(entry.id);
          return;
        }
        const { id, ...fields } = entry;
        transaction.create(refs[index], { ...fields, status: "draft" });
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
