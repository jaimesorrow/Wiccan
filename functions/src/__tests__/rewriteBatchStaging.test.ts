import fs from "node:fs";
import path from "node:path";
import { curriculumConcept, findLexicalDuplicateConcerns } from "../contentSimilarity";
import { loadContentSeed } from "../contentSeed";
import { findPracticalLessonViolations } from "../practicalLesson";
import { createRewriteStagingBatches } from "../rewriteBatchStaging";

describe("local rewrite batch staging", () => {
  it("partitions all checked-in drafts into write-disabled batches of at most 25", () => {
    const seed = loadContentSeed();
    const original = JSON.stringify(seed);
    const batches = createRewriteStagingBatches(seed);
    expect(batches).toHaveLength(12);
    expect(batches.slice(0, -1).every((batch) => batch.entries.length === 25)).toBe(true);
    expect(batches.at(-1)?.entries).toHaveLength(21);
    expect(batches.flatMap((batch) => batch.entries.map((entry) => entry.id))).toEqual(seed.map((entry) => entry.id));
    expect(batches.every((batch) => batch.writes === false && batch.entries.every((entry) =>
      entry.preservedStatus === "draft" && entry.liveBindingRequired && entry.expectedUpdateTime === null && entry.proposedFields === null,
    ))).toBe(true);
    expect(batches.every((batch) => /^[a-f0-9]{64}$/.test(batch.batchSha256))).toBe(true);
    expect(JSON.stringify(seed)).toBe(original);
    expect(createRewriteStagingBatches(seed).map((batch) => batch.batchSha256))
      .toEqual(batches.map((batch) => batch.batchSha256));
    expect(batches.every((batch) => batch.entries.every((entry) => entry.validationBlockers.length === 2))).toBe(true);
    batches[0].entries[0].originalFields.tags.push("staging-only-mutation");
    expect(seed[0].tags).not.toContain("staging-only-mutation");
  });

  it("rejects unsafe batch sizes, duplicate IDs, invalid IDs and non-drafts", () => {
    const first = loadContentSeed()[0];
    expect(() => createRewriteStagingBatches([first], 26)).toThrow("between 1 and 25");
    expect(() => createRewriteStagingBatches([first, first])).toThrow("Duplicate");
    expect(() => createRewriteStagingBatches([{ ...first, id: "nested/id" }])).toThrow("valid root document ID");
    expect(() => createRewriteStagingBatches([{ ...first, status: "published" } as typeof first])).toThrow("not a draft");
  });
});

describe.each([1, 2])("batch %i editorial artifact", (batchNumber) => {
  const packetPath = path.join(__dirname, "..", "..", "rewriteBatches", `batch-${String(batchNumber).padStart(2, "0")}.editorial.json`);
  const packet = JSON.parse(fs.readFileSync(packetPath, "utf8")) as {
    batchSize: number;
    writes: boolean;
    firebaseReady: boolean;
    entries: Array<{ id: string; disposition: string; proposedFields: Record<string, unknown> | null }>;
  };

  it("dispositions exactly 25 preserved seed IDs without enabling writes", () => {
    const seed = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "..", "ancientContentSeed.json"), "utf8")) as Array<{ id: string }>;
    expect(packet.batchSize).toBe(25);
    const offset = (batchNumber - 1) * 25;
    expect(packet.entries.map((entry) => entry.id)).toEqual(seed.slice(offset, offset + 25).map((entry) => entry.id));
    expect(new Set(packet.entries.map((entry) => entry.id)).size).toBe(25);
    expect(packet.writes).toBe(false);
    expect(packet.firebaseReady).toBe(false);
  });

  it("keeps completed proposals as drafts with 250–450-word numbered lessons", () => {
    const completed = packet.entries.filter((entry) => entry.proposedFields !== null);
    expect(completed).toHaveLength(25);
    for (const entry of completed) {
      const fields = entry.proposedFields!;
      const lesson = String(fields.lesson);
      const words = lesson.trim().split(/\s+/).length;
      expect(fields.status).toBe("draft");
      expect(fields.contentStatus).toBe("draft");
      expect(fields.tradition).toBe("Wiccan / Modern Witchcraft");
      expect(fields.body).toBe(fields.lesson);
      expect(fields.intro).toBe(fields.introduction);
      expect(fields.materials).toEqual(fields.tools);
      expect(words).toBeGreaterThanOrEqual(250);
      expect(words).toBeLessThanOrEqual(450);
      const steps = lesson.match(/^\d+\. /gm)?.map((step) => Number.parseInt(step, 10)) ?? [];
      expect(steps.length).toBeGreaterThanOrEqual(7);
      expect(steps).toEqual(Array.from({ length: steps.length }, (_, index) => index + 1));
      expect(fields.rightsReviewRequired).toBe(true);
      expect(findPracticalLessonViolations(fields, [], false)).toEqual([
        "verified source passage is missing from the editor-owned register",
      ]);
    }
    expect(findLexicalDuplicateConcerns(completed.map((entry) =>
      curriculumConcept(entry.id, entry.proposedFields!),
    ))).toEqual([]);
  });
});
