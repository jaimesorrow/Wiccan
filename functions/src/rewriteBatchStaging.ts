import { createHash } from "node:crypto";
import type { SeedContent } from "./types";
import { stableJson } from "./practicalLesson";

export const DEFAULT_REWRITE_BATCH_SIZE = 25;

export interface RewriteStagingEntry {
  id: string;
  preservedStatus: "draft";
  liveBindingRequired: true;
  expectedUpdateTime: null;
  reviewState: "awaiting_rewrite";
  validationBlockers: readonly [
    "live Firestore update time is not bound",
    "verified source passage and independent editorial review are not bound",
  ];
  sourceSnapshotSha256: string;
  originalFields: Omit<SeedContent, "id">;
  proposedFields: null;
}

export interface RewriteStagingBatch {
  schemaVersion: 1;
  projectId: "wiccan-android";
  sourceKind: "checked-in-seed";
  writes: false;
  batchNumber: number;
  totalBatches: number;
  batchSize: number;
  entries: RewriteStagingEntry[];
  batchSha256: string;
}

function sha256(value: unknown): string {
  return createHash("sha256").update(stableJson(value)).digest("hex");
}

/**
 * Prepare review templates from the checked-in seed without contacting Firebase.
 * These are deliberately not ReviewedChangeSets: every entry still needs a live
 * update-time binding, a complete proposed rewrite, source review, and editorial
 * approval before the existing migration command can consume it.
 */
export function createRewriteStagingBatches(
  seed: SeedContent[],
  batchSize = DEFAULT_REWRITE_BATCH_SIZE,
): RewriteStagingBatch[] {
  if (!Number.isSafeInteger(batchSize) || batchSize < 1 || batchSize > DEFAULT_REWRITE_BATCH_SIZE) {
    throw new Error(`Rewrite batch size must be between 1 and ${DEFAULT_REWRITE_BATCH_SIZE}.`);
  }
  if (!seed.length) throw new Error("At least one checked-in draft is required.");
  const ids = new Set<string>();
  const entries = seed.map((record): RewriteStagingEntry => {
    if (!record.id.trim() || record.id.includes("/")) throw new Error("Every rewrite candidate needs a valid root document ID.");
    if (ids.has(record.id)) throw new Error(`Duplicate rewrite candidate ID '${record.id}'.`);
    ids.add(record.id);
    if (record.status !== "draft") throw new Error(`Rewrite candidate '${record.id}' is not a draft.`);
    const { id, ...recordFields } = record;
    const originalFields = structuredClone(recordFields);
    return {
      id,
      preservedStatus: "draft",
      liveBindingRequired: true,
      expectedUpdateTime: null,
      reviewState: "awaiting_rewrite",
      validationBlockers: [
        "live Firestore update time is not bound",
        "verified source passage and independent editorial review are not bound",
      ],
      sourceSnapshotSha256: sha256(originalFields),
      originalFields,
      proposedFields: null,
    };
  });
  const totalBatches = Math.ceil(entries.length / batchSize);
  return Array.from({ length: totalBatches }, (_, index) => {
    const batchEntries = entries.slice(index * batchSize, (index + 1) * batchSize);
    const unsigned = {
      schemaVersion: 1 as const,
      projectId: "wiccan-android" as const,
      sourceKind: "checked-in-seed" as const,
      writes: false as const,
      batchNumber: index + 1,
      totalBatches,
      batchSize: batchEntries.length,
      entries: batchEntries,
    };
    return { ...unsigned, batchSha256: sha256(unsigned) };
  });
}
