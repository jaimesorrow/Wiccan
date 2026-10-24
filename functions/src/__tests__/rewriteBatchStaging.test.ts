import { loadContentSeed } from "../contentSeed";
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
