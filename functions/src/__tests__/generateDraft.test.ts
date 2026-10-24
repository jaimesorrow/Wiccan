import { generateDrafts as generateDraftsCore, InvalidGeneratedDraftError, InvalidGenerationRequestError } from "../generateDraft";
import type { GenerateDraftRequest } from "../generateDraft";
import { ContentSource } from "../contentSource";
import { FakeDraftGenerationPort } from "../testUtils/fakeDraftGenerationPort";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import type { SourcePassage } from "../sourcePassages";

const SOURCE_A: ContentSource = {
  id: "source-a",
  title: "Ancient Source A",
  tradition: "Ancient ritual tradition",
  era: "ancient",
  sourceKind: "ancient-grimoire",
  period: "Ancient period",
  publicDomainBasis: "Ancient source; independently written summary.",
  sourceReference: "Catalogue A",
  summary: "Historically attested protective ritual material.",
  groundedFacts: ["A documented protective symbol was used in this tradition."],
  eligibleTypes: ["spell", "lesson"],
};

const SOURCE_B: ContentSource = {
  id: "source-b",
  title: "Ancient Source B",
  tradition: "Ancient Mediterranean magical practice",
  era: "ancient",
  sourceKind: "ancient-grimoire",
  period: "Ancient period",
  publicDomainBasis: "Ancient source; independently written summary.",
  sourceReference: "Catalogue B",
  summary: "Household potions for prosperity.",
  groundedFacts: ["The curated source record describes a household preparation."],
  eligibleTypes: ["potion"],
};

const CONTEXT_SOURCE: ContentSource = {
  ...SOURCE_A,
  id: "context-source",
  title: "Museum overview of ancient amulets",
  sourceKind: "ancient-historical-context",
};

function verifiedPassage(sourceId: string, locator = "Tablet II, lines 4–8"): SourcePassage {
  return {
    id: `passage-${sourceId}`,
    sourceId,
    textTitle: "Ancient Source",
    manuscriptId: "Museum 1",
    tradition: sourceId === SOURCE_B.id ? SOURCE_B.tradition : SOURCE_A.tradition,
    manuscriptDate: "Third century CE",
    edition: "Public-domain edition",
    editionDate: "1904",
    translator: "Test Translator",
    locator,
    url: "https://example.org/ancient-text",
    inspectedPassage: "An editor-inspected test passage.",
    attestedInstructions: ["Prepare the material.", "Complete the operation."],
    uncertainties: [],
    formula: null,
    formulaOmissionReason: "No formula is present.",
    dating: { era: "ancient", status: "verified", evidenceUrl: "https://example.org/catalogue", notes: "Catalogue date." },
    rights: { status: "verified", basis: "Public domain test edition.", reference: "https://example.org/rights", jurisdictions: ["US"] },
    scope: { status: "approved", rationale: "Documented divination method used as a modern witchcraft adaptation.", evidenceUrl: "https://example.org/scope" },
    review: { passageInspected: true, sourceFidelityApproved: true, safetyApproved: true, reviewedBy: "editor", reviewedAt: "2026-10-19T00:00:00.000Z" },
  };
}

function generateDrafts(
  request: GenerateDraftRequest,
  sources: ContentSource[],
  generator: FakeDraftGenerationPort,
  repo: FakeContentAdminRepository,
) {
  const sourceId = request.sourceId ?? sources.find((source) => source.sourceKind === "ancient-grimoire")?.id ?? "missing";
  const passage = verifiedPassage(sourceId, request.sourceCitation ?? "Tablet II, lines 4–8");
  return generateDraftsCore({ ...request, passageId: passage.id }, sources, generator, repo, [passage]);
}

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
    expect(doc?.tradition).toBe("Wiccan / Modern Witchcraft");
    expect(doc?.sourceTradition).toBe("Ancient ritual tradition");
    expect(doc?.reflection).toBe("What does this historical evidence help you notice?");
    expect(doc?.practice).toBe("Complete the numbered method once.");
    expect(doc?.sourceCitation).toBe("Tablet II, lines 4–8");
    expect(doc?.rightsNotes).toBe("Public domain test edition.");
    expect(doc?.rightsReviewRequired).toBe(true);
  });

  it("retains the editor's identity and precise citation when drafting a reworded lesson", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();
    const source = { ...SOURCE_A, sourceReference: "Catalogue A https://example.org/ancient-text" };

    const [result] = await generateDrafts(
      {
        tier: "premium",
        sourceId: source.id,
        type: "lesson",
        similarityGroupId: "protective-symbol-lesson",
        sourceCitation: "Catalogue A, tablet II, lines 4–8",
      },
      [source],
      generator,
      repo,
    );

    expect(repo.docs.get(result.contentId)).toMatchObject({
      similarityGroupId: "protective-symbol-lesson",
      sourceCitation: "Catalogue A, tablet II, lines 4–8",
      sourceUrl: "https://example.org/ancient-text",
      rightsNotes: "Public domain test edition.",
      reflection: "What does this historical evidence help you notice?",
      practice: "Complete the numbered method once.",
    });
  });

  it("rejects an empty editorial identity or citation before generating anything", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    await expect(generateDrafts(
      { tier: "free", similarityGroupId: "  " }, [SOURCE_A], generator, repo,
    )).rejects.toThrow(InvalidGenerationRequestError);
    await expect(generateDrafts(
      { tier: "free", sourceCitation: "  " }, [SOURCE_A], generator, repo,
    )).rejects.toThrow(InvalidGenerationRequestError);
    expect(generator.calls).toHaveLength(0);
    expect(repo.docs.size).toBe(0);
  });

  it("passes source provenance and grounded facts into lesson generation", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    const results = await generateDrafts(
      { tier: "free", sourceId: "source-a", type: "lesson" },
      [SOURCE_A],
      generator,
      repo,
    );

    expect(generator.calls).toHaveLength(1);
    expect(generator.calls[0]).toMatchObject({
      type: "lesson",
      tradition: SOURCE_A.tradition,
      period: SOURCE_A.period,
      sourceTitle: SOURCE_A.title,
      sourceReference: SOURCE_A.sourceReference,
      summary: SOURCE_A.summary,
      groundedFacts: SOURCE_A.groundedFacts,
    });
    expect(repo.docs.get(results[0].contentId)).toMatchObject({
      reflection: "What does this historical evidence help you notice?",
      practice: "Complete the numbered method once.",
      status: "draft",
    });
  });

  it("keeps every draft in a batch bound to the selected verified passage", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    const results = await generateDrafts({ tier: "premium", count: 3 }, [SOURCE_A, SOURCE_B], generator, repo);

    expect(results).toHaveLength(3);
    const sourceIdsUsed = new Set(results.map((r) => r.sourceId));
    // With two sources offering three total (source,type) pairs, three
    // drafts should not collapse onto a single source.
    expect(sourceIdsUsed).toEqual(new Set([SOURCE_A.id]));
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
    expect([...repo.docs.values()].every((d) => d.rightsReviewRequired === true)).toBe(true);
  });

  it("rejects ancient historical context that is not itself a grimoire", async () => {
    const generator = new FakeDraftGenerationPort();
    const repo = new FakeContentAdminRepository();

    await expect(generateDrafts(
      { tier: "free", sourceId: CONTEXT_SOURCE.id },
      [SOURCE_A, CONTEXT_SOURCE],
      generator,
      repo,
    )).rejects.toThrow(/not an approved pre-modern grimoire/);
    expect(generator.calls).toHaveLength(0);
    expect(repo.docs.size).toBe(0);
  });

  it("rejects malformed model output before writing a draft", async () => {
    const generator = new FakeDraftGenerationPort();
    generator.reflectionOverride = "This is not a question";
    const repo = new FakeContentAdminRepository();

    await expect(generateDrafts(
      { tier: "free", sourceId: SOURCE_A.id, type: "lesson" },
      [SOURCE_A],
      generator,
      repo,
    )).rejects.toThrow(InvalidGeneratedDraftError);
    expect(repo.docs.size).toBe(0);
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
      tradition: "Ancient ritual tradition",
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
      tradition: "Ancient ritual tradition",
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
