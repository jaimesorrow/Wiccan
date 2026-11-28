import { findPassageViolations, loadSourcePassages } from "../sourcePassages";

describe("sourcePassages register", () => {
  const passages = loadSourcePassages();

  it("loads a non-empty register of unique ids", () => {
    expect(passages.length).toBeGreaterThan(0);
    const ids = passages.map((passage) => passage.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it("accepts every checked-in passage and requires editionDate before 1931", () => {
    for (const passage of passages) {
      expect(findPassageViolations(passage)).toEqual([]);
      const year = Number.parseInt(String(passage.editionDate ?? "").trim().slice(0, 4), 10);
      expect(year).toBeGreaterThan(0);
      expect(year).toBeLessThan(1931);
    }
  });
});
