import { loadHistoricalSourceRegistry } from "../historicalSourceRegistry";

describe("historical source registry", () => {
  it("contains each of the ten requested candidates exactly once", () => {
    const records = loadHistoricalSourceRegistry();
    expect(records).toHaveLength(10);
    expect(new Set(records.map((record) => record.id))).toHaveProperty("size", 10);
  });

  it("does not approve a candidate without both passage and rights approval", () => {
    for (const record of loadHistoricalSourceRegistry()) {
      if (record.status === "approved") {
        expect(record.rightsStatus).toBe("verified");
        expect(record.passageStatus).toBe("approved");
      }
    }
  });

  it("keeps the production passage gate closed while passage review is pending", () => {
    expect(loadHistoricalSourceRegistry().every((record) => record.status !== "approved")).toBe(true);
  });
});
