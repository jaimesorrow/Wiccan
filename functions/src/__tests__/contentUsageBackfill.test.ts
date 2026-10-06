import { planContentUsageBackfill } from "../contentUsageBackfill";

describe("legacy Archive delivery marker backfill", () => {
  it("deduplicates by uid/content with the earliest date and stable order", () => {
    const rows = [
      { id: "h3", fields: { userId: "b", contentId: "c", deliveredDate: "2026-09-30" } },
      { id: "h2", fields: { userId: "a", contentId: "c", deliveredDate: "2026-09-29" } },
      { id: "h1", fields: { userId: "a", contentId: "c", deliveredDate: "2026-09-28" } },
    ];
    expect(planContentUsageBackfill(rows)).toEqual([
      { uid: "a", contentId: "c", deliveredDate: "2026-09-28", historyId: "h1" },
      { uid: "b", contentId: "c", deliveredDate: "2026-09-30", historyId: "h3" },
    ]);
    expect(planContentUsageBackfill([...rows].reverse())).toEqual(planContentUsageBackfill(rows));
  });

  it.each([
    { userId: "", contentId: "c", deliveredDate: "2026-09-28" },
    { userId: "a/b", contentId: "c", deliveredDate: "2026-09-28" },
    { userId: "a", contentId: "..", deliveredDate: "2026-09-28" },
    { userId: "a", contentId: "c", deliveredDate: "2026-02-30" },
    { userId: "a", contentId: "c", deliveredDate: "yesterday" },
  ])("rejects invalid history before issuing any writes: %p", (fields) => {
    expect(() => planContentUsageBackfill([{ id: "history", fields }])).toThrow("no backfill has been written");
  });
});
