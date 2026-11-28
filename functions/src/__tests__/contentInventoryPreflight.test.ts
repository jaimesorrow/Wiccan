import { summarizeContentInventory } from "../contentInventoryPreflight";

describe("content inventory preflight", () => {
  it("counts type and status without retaining authored fields", () => {
    const summary = summarizeContentInventory([
      { type: "lesson", status: "published", body: "private" } as never,
      { type: "lesson", status: "draft" },
      { type: "spell", status: "draft" },
      { type: " ", status: null },
    ]);

    expect(summary).toEqual({
      total: 4,
      byType: { lesson: 2, spell: 1 },
      byStatus: { draft: 2, published: 1 },
      missingType: 1,
      missingStatus: 1,
    });
    expect(JSON.stringify(summary)).not.toContain("private");
  });
});

