import { compareContentAudits } from "../contentAuditComparison";
import type { ComparableAudit } from "../contentAuditComparison";

function audit(records: ComparableAudit["records"]): ComparableAudit {
  return { inventoryKind: "live-firestore", project: "wiccan-android", totalRecords: records.length, records };
}

describe("content audit comparison", () => {
  it("accepts only reviewed content hash changes while preserving IDs, statuses, and delivery history", () => {
    const before = audit([
      { id: "draft-1", status: "draft", reviewHash: "before", previouslyDelivered: false },
      { id: "published-1", status: "published", reviewHash: "same", previouslyDelivered: true },
    ]);
    const after = audit([
      { id: "draft-1", status: "draft", reviewHash: "after", previouslyDelivered: false },
      { id: "published-1", status: "published", reviewHash: "same", previouslyDelivered: true },
    ]);

    expect(compareContentAudits(before, after, new Set(["draft-1"]))).toMatchObject({
      passed: true,
      changedContentIds: ["draft-1"],
      invariantFailures: [],
    });
  });

  it("rejects status, ID, delivery-history, and unreviewed content changes", () => {
    const before = audit([
      { id: "draft-1", status: "draft", reviewHash: "a", previouslyDelivered: false },
      { id: "removed", status: "draft", reviewHash: "b", previouslyDelivered: false },
    ]);
    const after = audit([
      { id: "draft-1", status: "published", reviewHash: "changed", previouslyDelivered: true },
      { id: "added", status: "draft", reviewHash: "c", previouslyDelivered: false },
    ]);

    const report = compareContentAudits(before, after);
    expect(report.passed).toBe(false);
    expect(report.addedIds).toEqual(["added"]);
    expect(report.removedIds).toEqual(["removed"]);
    expect(report.unexpectedChangedContentIds).toEqual(["draft-1"]);
    expect(report.statusChanges).toHaveLength(1);
    expect(report.deliveryHistoryChanges).toHaveLength(1);
  });
});
