import path from "path";
import fs from "fs";
import os from "os";
import { isLessonGenerationSource, loadContentSources } from "../contentSource";

describe("loadContentSources", () => {
  it("loads and validates the real checked-in sources.json", () => {
    const sources = loadContentSources();
    expect(sources.length).toBeGreaterThan(0);
    for (const source of sources) {
      expect(source.id).toBeTruthy();
      expect(source.era).toBeTruthy();
      expect(source.sourceKind).toBeTruthy();
      expect(source.period).toBeTruthy();
      expect(source.sourceReference).toBeTruthy();
      expect(source.summary).toBeTruthy();
      expect(source.groundedFacts.length).toBeGreaterThan(0);
      expect(source.eligibleTypes.length).toBeGreaterThan(0);
    }
  });

  it("identifies a non-empty generation pool made only of pre-modern grimoires", () => {
    const generationSources = loadContentSources().filter(isLessonGenerationSource);

    expect(generationSources.length).toBeGreaterThan(0);
    expect(generationSources.every((source) => source.sourceKind === "ancient-grimoire")).toBe(true);
  });

  it("keeps current checked-in lesson sources explicitly ancient", () => {
    const lessonSources = loadContentSources().filter((source) => source.eligibleTypes.includes("lesson"));

    expect(lessonSources.length).toBeGreaterThan(0);
    for (const source of lessonSources) {
      expect(source.era).toBe("ancient");
      expect(source.sourceReference).toBeTruthy();
      expect(source.groundedFacts.length).toBeGreaterThanOrEqual(3);
    }
  });

  it("accepts a medieval historical grimoire for passage-gated lesson generation", () => {
    const tmpFile = path.join(os.tmpdir(), `non-ancient-lesson-${Date.now()}.json`);
    fs.writeFileSync(tmpFile, JSON.stringify([{
      id: "early-modern-lesson",
      title: "Early Modern Source",
      tradition: "European folk practice",
      era: "early-modern",
      sourceKind: "historical-grimoire",
      period: "15th century CE",
      publicDomainBasis: "Historical subject matter.",
      sourceReference: "Example catalogue",
      summary: "Example summary.",
      groundedFacts: ["Example grounded fact."],
      eligibleTypes: ["lesson"],
    }]));

    try {
      const [source] = loadContentSources(tmpFile);
      expect(isLessonGenerationSource(source)).toBe(true);
    } finally {
      fs.unlinkSync(tmpFile);
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
