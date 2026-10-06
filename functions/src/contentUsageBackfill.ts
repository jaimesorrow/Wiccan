import { Firestore } from "firebase-admin/firestore";

export interface LegacyHistoryRow {
  id: string;
  fields: Record<string, unknown>;
}

export interface ContentUsageMarker {
  uid: string;
  contentId: string;
  deliveredDate: string;
  historyId: string;
}

/** Deterministic, deduplicated markers using each entry's earliest delivery date. */
export function planContentUsageBackfill(rows: LegacyHistoryRow[]): ContentUsageMarker[] {
  const markers = new Map<string, ContentUsageMarker>();
  const safeId = (value: unknown): value is string =>
    typeof value === "string" && value.length > 0 && !value.includes("/") && value !== "." && value !== "..";
  for (const row of [...rows].sort((a, b) => a.id.localeCompare(b.id))) {
    const { userId, contentId, deliveredDate } = row.fields;
    if (!safeId(row.id) || !safeId(userId) || !safeId(contentId) || typeof deliveredDate !== "string" ||
      !/^\d{4}-\d{2}-\d{2}$/.test(deliveredDate) ||
      !Number.isFinite(Date.parse(deliveredDate)) || new Date(deliveredDate).toISOString().slice(0, 10) !== deliveredDate) {
      throw new Error(`History row '${row.id}' has invalid userId, contentId, or deliveredDate; no backfill has been written.`);
    }
    const key = `${userId}/${contentId}`;
    const existing = markers.get(key);
    if (!existing || deliveredDate < existing.deliveredDate) {
      markers.set(key, { uid: userId, contentId, deliveredDate, historyId: row.id });
    }
  }
  return [...markers.values()].sort((a, b) => `${a.uid}/${a.contentId}`.localeCompare(`${b.uid}/${b.contentId}`));
}

export interface UsageBackfillSummary {
  planned: number;
  missing: number;
  preserved: number;
  skippedOrphanHistory: number;
  created: number;
}

/** Create only; recheck history/profile inside each transaction so deleted accounts stay deleted. */
export async function backfillContentUsage(
  db: Firestore,
  markers: ContentUsageMarker[],
  commit: boolean,
): Promise<UsageBackfillSummary> {
  const summary: UsageBackfillSummary = {
    planned: markers.length, missing: 0, preserved: 0, skippedOrphanHistory: 0, created: 0,
  };
  for (const marker of markers) {
    const userRef = db.collection("users").doc(marker.uid);
    const historyRef = db.collection("userContentHistory").doc(marker.historyId);
    const markerRef = db.collection("userContentUsage").doc(marker.uid).collection("content").doc(marker.contentId);
    const result = await db.runTransaction(async (transaction) => {
      const [user, history, existing] = await Promise.all([
        transaction.get(userRef), transaction.get(historyRef), transaction.get(markerRef),
      ]);
      if (!user.exists || !history.exists || history.get("userId") !== marker.uid ||
        history.get("contentId") !== marker.contentId) return "skipped";
      if (existing.exists) return "preserved";
      if (commit) transaction.create(markerRef, { deliveredDate: marker.deliveredDate });
      return "missing";
    });
    if (result === "skipped") summary.skippedOrphanHistory++;
    if (result === "preserved") summary.preserved++;
    if (result === "missing") {
      summary.missing++;
      if (commit) summary.created++;
    }
  }
  return summary;
}
