import { generateDrafts, InvalidGenerationRequestError } from "../generateDraft";
import { ContentSource } from "../contentSource";
import { FakeDraftGenerationPort } from "../testUtils/fakeDraftGenerationPort";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";

const SOURCE_A: ContentSource = {
  id: "source-a",
  title: "Source A",
  tradition: "European folk magic",
  publicDomainBasis: "pre-1930",
  summary: "Herb charms for the hearth.",
  eligibleTypes: ["spell", "lesson"],
};

const SOURCE_B: ContentSource = {
  id: "source-b",
  title: "Source B",
  tradition: "Mediterranean folk magic",
  publicDomainBasis: "pre-1930",
  summary: "Household potions for prosperity.",
  eligibleTypes: ["potion"],
};

describe("generateDrafts", () => {
  it("generates a single draft with status draft and a sourceId", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    const results = await generateDrafts(
      { tier: "free", sourceId: "source-a", type: "spell" },
      [SOURCE_A, SOURCE_B],
      generator,
      repo,
    );

    expect(results).toHaveLength(1);
    const doc = repo.docs.get(results[0].contentId);
    expect(doc?.status).toBe("draft");
    expect(doc?.sourceId).toBe("source-a");
    expect(doc?.type).toBe("spell");
    expect(doc?.tier).toBe("free");
    expect(doc?.tradition).toBe("European folk magic");
  });

  it("round-robins across eligible source/type combinations for a batch", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    const results = await generateDrafts({ tier: "premium", count: 3 }, [SOURCE_A, SOURCE_B], generator, repo);

    expect(results).toHaveLength(3);
    const sourceIdsUsed = new Set(results.map((r) => r.sourceId));
    // With two sources offering three total (source,type) pairs, three
    // drafts should not collapse onto a single source.
    expect(sourceIdsUsed.size).toBeGreaterThan(1);
    expect(repo.docs.size).toBe(3);
    for (const doc of repo.docs.values()) {
      expect(doc.status).toBe("draft");
    }
  });

  it("never auto-publishes generated content", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    await generateDrafts({ tier: "free", count: 5 }, [SOURCE_A, SOURCE_B], generator, repo);

    expect([...repo.docs.values()].every((d) => d.status === "draft")).toBe(true);
  });

  it("rejects an unknown sourceId", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    await expect(
      generateDrafts({ tier: "free", sourceId: "does-not-exist" }, [SOURCE_A], generator, repo),
    ).rejects.toThrow(InvalidGenerationRequestError);
  });

  it("rejects a type the requested source isn't eligible for", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    await expect(
      generateDrafts({ tier: "free", sourceId: "source-b", type: "spell" }, [SOURCE_A, SOURCE_B], generator, repo),
    ).rejects.toThrow(InvalidGenerationRequestError);
  });

  it("does not persist a generated draft whose title already exists in the pool", async () => {
    const generator = new FakeDraftGenerationPort();
    generator.titleOverride = "Candle of Clear Intent";
    const repo = new FakeContentAdminRepository();
    await repo.createDraft({
      title: "Candle of Clear Intent",
      intro: "i",
      body: "b",
      materials: null,
      safetyNote: null,
      tags: ["spell"],
      category: "MoonAndSeasonal",
      type: "spell",
      tier: "free",
      tradition: "European folk magic",
      sourceId: "source-a",
    });

    const results = await generateDrafts(
      { tier: "free", sourceId: "source-a", type: "spell" },
      [SOURCE_A, SOURCE_B],
      generator,
      repo,
    );

    expect(results).toHaveLength(0);
    expect(repo.docs.size).toBe(1);
  });

  it("skips only the duplicate-titled attempts within a batch, keeping the rest", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();
    await repo.createDraft({
      title: "Generated spell #2",
      intro: "i",
      body: "b",
      materials: null,
      safetyNote: null,
      tags: ["spell"],
      category: "MoonAndSeasonal",
      type: "spell",
      tier: "free",
      tradition: "European folk magic",
      sourceId: "source-a",
    });

    // FakeDraftGenerationPort's auto-incrementing titles will produce
    // "Generated spell #1", "#2" (pre-existing, should be skipped), "#3".
    const results = await generateDrafts(
      { tier: "free", sourceId: "source-a", type: "spell", count: 3 },
      [SOURCE_A, SOURCE_B],
      generator,
      repo,
    );

    expect(results.map((r) => r.title)).toEqual(["Generated spell #1", "Generated spell #3"]);
  });
});
