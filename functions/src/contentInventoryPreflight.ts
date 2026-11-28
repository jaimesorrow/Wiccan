export interface ContentInventoryRecord {
  type?: unknown;
  status?: unknown;
}

export interface ContentInventorySummary {
  total: number;
  byType: Record<string, number>;
  byStatus: Record<string, number>;
  missingType: number;
  missingStatus: number;
}

const increment = (counts: Record<string, number>, value: unknown): boolean => {
  if (typeof value !== "string" || value.trim().length === 0) return false;
  const key = value.trim();
  counts[key] = (counts[key] ?? 0) + 1;
  return true;
};

/** Summarizes only inventory metadata; authored document fields never enter the report. */
export function summarizeContentInventory(records: ContentInventoryRecord[]): ContentInventorySummary {
  const byType: Record<string, number> = {};
  const byStatus: Record<string, number> = {};
  let missingType = 0;
  let missingStatus = 0;
  for (const record of records) {
    if (!increment(byType, record.type)) missingType++;
    if (!increment(byStatus, record.status)) missingStatus++;
  }
  return {
    total: records.length,
    byType: Object.fromEntries(Object.entries(byType).sort(([left], [right]) => left.localeCompare(right))),
    byStatus: Object.fromEntries(Object.entries(byStatus).sort(([left], [right]) => left.localeCompare(right))),
    missingType,
    missingStatus,
  };
}

