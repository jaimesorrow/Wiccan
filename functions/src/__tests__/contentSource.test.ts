import path from "path";
import fs from "fs";
import os from "os";
import { loadContentSources } from "../contentSource";

describe("loadContentSources", () => {
  it("loads and validates the real checked-in sources.json", () => {
    const sources = loadContentSources();
    expect(sources.length).toBeGreaterThan(0);
    for (const source of sources) {
      expect(source.id).toBeTruthy();
      expect(source.period).toBeTruthy();
      expect(source.sourceReference).toBeTruthy();
      expect(source.summary).toBeTruthy();
      expect(source.groundedFacts.length).toBeGreaterThan(0);
      expect(source.eligibleTypes.length).toBeGreaterThan(0);
    }
  });

  it("keeps lesson-eligible sources grounded in ancient historical material", () => {
    const lessonSources = loadContentSources().filter((source) => source.eligibleTypes.includes("lesson"));

    expect(lessonSources.length).toBeGreaterThan(0);
    for (const source of lessonSources) {
      expect(source.sourceReference).toBeTruthy();
      expect(source.groundedFacts.length).toBeGreaterThanOrEqual(3);
      expect(source.publicDomainBasis).toContain("ancient");
    }
  });

  it("throws on a malformed entry instead of silently dropping it", () => {
    const tmpFile = path.join(os.tmpdir(), `bad-sources-${Date.now()}.json`);
    fs.writeFileSync(tmpFile, JSON.stringify([{ id: "missing-fields" }]));

    try {
      expect(() => loadContentSources(tmpFile)).toThrow();
    } finally {
      fs.unlinkSync(tmpFile);
    }
  });
});
