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
      expect(source.summary).toBeTruthy();
      expect(source.eligibleTypes.length).toBeGreaterThan(0);
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
