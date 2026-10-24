export interface ComparableAuditRecord {
  id: string;
  status: unknown;
  reviewHash: string;
  previouslyDelivered: boolean;
}

export interface ComparableAudit {
  inventoryKind: string;
  project?: string;
  totalRecords: number;
  records: ComparableAuditRecord[];
}

/** Compares live before/after audits without treating expected lesson rewrites as invariant failures. */
export function compareContentAudits(
  before: ComparableAudit,
  after: ComparableAudit,
  expectedChangedIds: ReadonlySet<string> = new Set(),
) {
  if (before.inventoryKind !== "live-firestore" || after.inventoryKind !== "live-firestore") {
    throw new Error("Before/after comparison requires two live Firestore audits.");
  }
  if (!before.project || before.project !== after.project) throw new Error("Audit projects differ.");

  const beforeById = new Map(before.records.map((record) => [record.id, record]));
  const afterById = new Map(after.records.map((record) => [record.id, record]));
  const addedIds = [...afterById.keys()].filter((id) => !beforeById.has(id)).sort();
  const removedIds = [...beforeById.keys()].filter((id) => !afterById.has(id)).sort();
  const sharedIds = [...beforeById.keys()].filter((id) => afterById.has(id));
  const changedContentIds = sharedIds.filter((id) => beforeById.get(id)!.reviewHash !== afterById.get(id)!.reviewHash).sort();
  const unexpectedChangedContentIds = changedContentIds.filter((id) => !expectedChangedIds.has(id));
  const expectedButUnchangedIds = [...expectedChangedIds].filter((id) => beforeById.has(id) && afterById.has(id) && !changedContentIds.includes(id)).sort();
  const statusChanges = sharedIds.flatMap((id) => {
    const oldStatus = beforeById.get(id)!.status;
    const newStatus = afterById.get(id)!.status;
    return oldStatus === newStatus ? [] : [{ id, before: oldStatus, after: newStatus }];
  });
  const deliveryHistoryChanges = sharedIds.flatMap((id) => {
    const oldValue = beforeById.get(id)!.previouslyDelivered;
    const newValue = afterById.get(id)!.previouslyDelivered;
    return oldValue === newValue ? [] : [{ id, before: oldValue, after: newValue }];
  });
  const invariantFailures = [
    ...(before.totalRecords === after.totalRecords ? [] : [`record count changed from ${before.totalRecords} to ${after.totalRecords}`]),
    ...(addedIds.length ? [`unexpected added IDs: ${addedIds.join(", ")}`] : []),
    ...(removedIds.length ? [`unexpected removed IDs: ${removedIds.join(", ")}`] : []),
    ...(statusChanges.length ? [`publication status changed for ${statusChanges.length} record(s)`] : []),
    ...(deliveryHistoryChanges.length ? [`delivery-history membership changed for ${deliveryHistoryChanges.length} record(s)`] : []),
    ...(unexpectedChangedContentIds.length ? [`content changed outside the reviewed set: ${unexpectedChangedContentIds.join(", ")}`] : []),
  ];

  return {
    project: before.project,
    passed: invariantFailures.length === 0,
    beforeCount: before.totalRecords,
    afterCount: after.totalRecords,
    expectedChangedIds: [...expectedChangedIds].sort(),
    changedContentIds,
    expectedButUnchangedIds,
    unexpectedChangedContentIds,
    addedIds,
    removedIds,
    statusChanges,
    deliveryHistoryChanges,
    invariantFailures,
  };
}
