import { existingTitleList, generateDraftBatch, MAX_ATTEMPTS_PER_DRAFT } from "../generateDraft";
import { ContentSource } from "../contentSource";
import type { SourcePassage } from "../sourcePassages";
import type { DraftGenerationRequest, GeneratedDraftFields } from "../draftGenerationPort";
import { FakeDraftGenerationPort } from "../testUtils/fakeDraftGenerationPort";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import type { StoredContent } from "../contentAdminRepositoryPort";

const SOURCE: ContentSource = {
  id: "source-a",
  title: "Ancient Source",
  tradition: "Ancient ritual tradition",
  era: "ancient",
  sourceKind: "ancient-grimoire",
  period: "Ancient period",
  publicDomainBasis: "Ancient source; independently written summary.",
  sourceReference: "Catalogue A",
  summary: "Historically attested protective ritual material.",
  groundedFacts: ["A documented protective symbol was used in this tradition."],
  eligibleTypes: ["spell"],
};

const PASSAGE: SourcePassage = {
  id: "passage-source-a",
  sourceId: "source-a",
  textTitle: "Ancient Source",
  manuscriptId: "Museum 1",
  tradition: "Ancient ritual tradition",
  manuscriptDate: "Third century CE",
  edition: "Public-domain edition",
  editionDate: "1904",
  translator: "Test Translator",
  locator: "Tablet II, lines 4–8",
  url: "https://example.org/ancient-text",
  inspectedPassage: "An editor-inspected test passage.",
  attestedInstructions: ["Prepare the material.", "Complete the operation."],
  uncertainties: [],
  formula: null,
  formulaOmissionReason: "No formula is present.",
  dating: { era: "ancient", status: "verified", evidenceUrl: "https://example.org/catalogue", notes: "Catalogue date." },
  rights: { status: "verified", basis: "Public domain test edition.", reference: "https://example.org/rights", jurisdictions: ["US"] },
  scope: { status: "approved", rationale: "Documented method used as a modern witchcraft adaptation.", evidenceUrl: "https://example.org/scope" },
  review: { passageInspected: true, sourceFidelityApproved: true, safetyApproved: true, reviewedBy: "editor", reviewedAt: "2026-10-19T00:00:00.000Z" },
};

const PASSAGE_B: SourcePassage = {
  ...PASSAGE,
  id: "passage-source-b",
  locator: "Tablet III, lines 1–6",
  inspectedPassage: "A second editor-inspected test passage.",
};

const PASSAGE_C: SourcePassage = {
  ...PASSAGE,
  id: "passage-source-c",
  locator: "Tablet IV, lines 2–9",
  inspectedPassage: "A third editor-inspected test passage.",
};

const ALL_PASSAGES = [PASSAGE, PASSAGE_B, PASSAGE_C];
const requestB = { tier: "free" as const, sourceId: "source-a", passageId: "passage-source-b" };
const requestC = { tier: "free" as const, sourceId: "source-a", passageId: "passage-source-c" };

/** Genuinely distinct drafts: different title, summary, steps and follow-up. */
const FRESH: Record<string, Partial<GeneratedDraftFields>> = {
  hearth: {
    title: "Hearth Ash Mark",
    summary: "Mark the hearthstone with cold ash to close the evening.",
    body: "1. Rub a pinch of cold ash on the hearthstone.\n2. Brush the stone clean.",
    practice: "Describe how the swept stone looked afterward.",
  },
  lamp: {
    title: "Lamp Of Welcome",
    summary: "Trim and light a gate lamp as a greeting for guests.",
    body: "1. Trim a lamp wick.\n2. Light it at the gate and snuff it at night.",
    practice: "Note who passed the lamp during the evening.",
  },
  quiet: {
    title: "Spoon Of Quiet",
    summary: "Stir a bowl of still water clockwise to settle the mind.",
    body: "1. Fill a bowl with still water.\n2. Stir it three times with a wooden spoon and pour it out.",
    practice: "Record how your breathing changed while stirring.",
  },
};

const request = { tier: "free" as const, sourceId: "source-a", passageId: "passage-source-a" };

/** Replays a scripted list of title/body overrides on top of the fake model's valid draft shape. */
class ScriptedGenerator extends FakeDraftGenerationPort {
  constructor(private readonly script: Array<Partial<GeneratedDraftFields>>) { super(); }
  override async generateDraft(req: DraftGenerationRequest): Promise<GeneratedDraftFields> {
    const index = this.calls.length;
    const base = await super.generateDraft(req);
    return { ...base, ...(this.script[Math.min(index, this.script.length - 1)] ?? {}) };
  }
}

async function seededRepo(): Promise<{ repo: FakeContentAdminRepository; published: StoredContent; draft: StoredContent }> {
  // Existing library: one published doc and one draft, each bound to its own passage.
  const repo = new FakeContentAdminRepository();
  const firstSeed = new ScriptedGenerator([
    { title: "Threshold Salt Line", body: "1. Pour a thin line of salt across the doorway.\n2. Sweep it outward and close the door." },
  ]);
  const secondSeed = new ScriptedGenerator([
    { title: "Window Water Blessing", body: "1. Set a glass of water on the sill at dawn.\n2. Pour it onto the garden at dusk." },
  ]);
  const firstMade = await generateDraftBatch({ ...request, count: 1 }, [SOURCE], firstSeed, repo, ALL_PASSAGES);
  const secondMade = await generateDraftBatch({ ...requestB, count: 1 }, [SOURCE], secondSeed, repo, ALL_PASSAGES);
  expect(firstMade.accepted).toHaveLength(1);
  expect(secondMade.accepted).toHaveLength(1);
  const first = repo.docs.get(firstMade.accepted[0].contentId)!;
  const second = repo.docs.get(secondMade.accepted[0].contentId)!;
  first.status = "published"; // existing content counts whether it is published or draft
  return { repo, published: first, draft: second };
}

describe("generator duplicate protection", () => {
  it("rejects a title that matches a published doc after normalisation, then regenerates", async () => {
    const { repo } = await seededRepo();
    const generator = new ScriptedGenerator([
      { title: "  THRESHOLD  salt-line! " },
      FRESH.hearth,
    ]);
    const result = await generateDraftBatch({ ...requestC, count: 1 }, [SOURCE], generator, repo, ALL_PASSAGES);
    expect(result.rejected).toHaveLength(1);
    expect(result.rejected[0].status).toBe("rejected-duplicate");
    expect(result.accepted.map((entry) => entry.title)).toEqual(["Hearth Ash Mark"]);
    expect(result.attempts).toBe(2);
  });

  it("rejects a title that matches an existing draft", async () => {
    const { repo } = await seededRepo();
    const generator = new ScriptedGenerator([{ title: "window water blessing" }]);
    const result = await generateDraftBatch({ ...requestC, count: 1 }, [SOURCE], generator, repo, ALL_PASSAGES);
    expect(result.accepted).toHaveLength(0);
    expect(result.rejected.length).toBeGreaterThan(0);
  });

  it("rejects the same practice under a new title (normalised body hash / actions)", async () => {
    const { repo, published } = await seededRepo();
    const generator = new ScriptedGenerator([{ title: "Doorway Salt Charm", body: `${published.lesson ?? published.body}` }]);
    const result = await generateDraftBatch({ ...requestC, count: 1 }, [SOURCE], generator, repo, ALL_PASSAGES);
    expect(result.accepted).toHaveLength(0);
    expect(result.rejected[0].reasons.join(" ")).toMatch(/threshold|content|instruction|concept|ritual/i);
  });

  it("rejects the same body that differs only in case, spacing and punctuation", async () => {
    const { repo, published } = await seededRepo();
    const body = String(published.lesson ?? published.body).toUpperCase().replace(/\./g, " .  ");
    const generator = new ScriptedGenerator([{ title: "A Wholly New Name", body }]);
    const result = await generateDraftBatch({ ...requestC, count: 1 }, [SOURCE], generator, repo, ALL_PASSAGES);
    expect(result.accepted).toHaveLength(0);
  });

  it("gives up after the attempt allowance instead of looping, and writes nothing", async () => {
    const { repo } = await seededRepo();
    const before = repo.docs.size;
    const generator = new ScriptedGenerator([{ title: "Threshold Salt Line" }]);
    const result = await generateDraftBatch({ ...requestC, count: 2 }, [SOURCE], generator, repo, ALL_PASSAGES);
    expect(result.exhausted).toBe(true);
    expect(result.attempts).toBe(2 * MAX_ATTEMPTS_PER_DRAFT);
    expect(generator.calls).toHaveLength(2 * MAX_ATTEMPTS_PER_DRAFT);
    expect(repo.docs.size).toBe(before);
  });

  it("does not let two drafts in one batch collide with each other", async () => {
    const repo = new FakeContentAdminRepository();
    const source = { ...SOURCE, eligibleTypes: ["spell", "lesson"] as const };
    const generator = new ScriptedGenerator([
      FRESH.quiet,
      { ...FRESH.quiet, title: "spoon of quiet" },
    ]);
    const result = await generateDraftBatch({ ...request, count: 2 }, [source], generator, repo, ALL_PASSAGES);
    expect(result.accepted.map((entry) => entry.title)).toEqual(["Spoon Of Quiet"]);
    expect(result.rejected.length).toBeGreaterThan(0);
    expect(result.rejected[0].status).toBe("rejected-duplicate");
  });

  it("rejects a second item from a passage that already has one, even when the practice and type are new", async () => {
    const { repo } = await seededRepo();
    const source = { ...SOURCE, eligibleTypes: ["spell", "lesson"] as const };
    const generator = new ScriptedGenerator([FRESH.hearth]);
    const result = await generateDraftBatch(
      { ...request, type: "lesson", count: 1 },
      [source],
      generator,
      repo,
      ALL_PASSAGES,
    );
    expect(result.accepted).toHaveLength(0);
    expect(result.rejected[0].reasons.join(" ")).toMatch(/source-passage-id/);
  });

  it("passes every existing title (published and draft) to the model as an avoid-list", async () => {
    const { repo } = await seededRepo();
    const generator = new ScriptedGenerator([FRESH.hearth]);
    await generateDraftBatch({ ...requestC, count: 1 }, [SOURCE], generator, repo, ALL_PASSAGES);
    expect(generator.calls[0].existingTitles).toEqual(expect.arrayContaining(["Threshold Salt Line", "Window Water Blessing"]));
  });

  it("adds a rejected candidate's title to the avoid-list for the regeneration", async () => {
    const { repo } = await seededRepo();
    const generator = new ScriptedGenerator([
      { title: "Window Water Blessing" },
      FRESH.hearth,
    ]);
    await generateDraftBatch({ ...requestC, count: 1 }, [SOURCE], generator, repo, ALL_PASSAGES);
    expect(generator.calls).toHaveLength(2);
    expect(generator.calls[1].existingTitles).toContain("Window Water Blessing");
  });
});

describe("existingTitleList", () => {
  it("dedupes case-insensitively, trims, and skips non-string titles", () => {
    const list = existingTitleList(
      [
        { id: "1", fields: { title: " Moon Bath " } },
        { id: "2", fields: { title: "moon bath" } },
        { id: "3", fields: { title: 42 } },
        { id: "4", fields: {} },
      ],
      ["Salt Line", "SALT LINE", ""],
    );
    expect(list).toEqual(["Moon Bath", "Salt Line"]);
  });
});
