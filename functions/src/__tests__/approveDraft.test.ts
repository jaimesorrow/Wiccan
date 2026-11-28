import { approveDraft as approveDraftWithApproval, DraftNotFoundError, UnsafeContentError } from "../approveDraft";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import { NewDraftContent } from "../contentAdminRepositoryPort";
import { ContentSource } from "../contentSource";
import * as contentSources from "../contentSource";
import * as sourcePassages from "../sourcePassages";

const BASE_DRAFT: NewDraftContent = {
  title: "t",
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
  illustrationUrl: "https://cdn.example.org/originals/base-draft.png",
  illustrationOriginal: true,
};

function originalArt(suffix: string): Pick<NewDraftContent, "illustrationUrl" | "illustrationOriginal"> {
  return {
    illustrationUrl: `https://cdn.example.org/originals/${suffix}.png`,
    illustrationOriginal: true,
  };
}

const ANCIENT_LESSON_SOURCE: ContentSource = {
  id: "ancient-source",
  title: "Ancient Source",
  tradition: "Ancient ritual tradition",
  era: "ancient",
  sourceKind: "ancient-grimoire",
  period: "Ancient period",
  publicDomainBasis: "Ancient historical subject matter.",
  sourceReference: "Catalogue reference",
  summary: "Curated historical summary.",
  groundedFacts: ["One verified historical fact."],
  eligibleTypes: ["lesson"],
};

const EARLY_MODERN_SOURCE: ContentSource = {
  ...ANCIENT_LESSON_SOURCE,
  id: "early-modern-source",
  era: "early-modern",
};

function approveDraft(
  contentId: string,
  repo: FakeContentAdminRepository,
  sources?: ContentSource[],
): Promise<void> {
  return approveDraftWithApproval(
    contentId,
    repo,
    { operator: "test-editor", approvalDigest: "a".repeat(64) },
    sources,
  );
}

describe("approveDraft", () => {
  it("flips a draft to published", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft(BASE_DRAFT);

    await approveDraft(contentId, repo);

    expect(await repo.getStatus(contentId)).toBe("published");
  });

  it("is idempotent for an already-published item", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft(BASE_DRAFT);
    await approveDraft(contentId, repo);

    const sources = jest.spyOn(contentSources, "loadContentSources").mockImplementation(() => {
      throw new Error("Source registry unavailable");
    });
    const passages = jest.spyOn(sourcePassages, "loadSourcePassages").mockImplementation(() => {
      throw new Error("Passage registry unavailable");
    });
    try {
      await expect(approveDraft(contentId, repo)).resolves.toBeUndefined();
      expect(await repo.getStatus(contentId)).toBe("published");
      expect(sources).not.toHaveBeenCalled();
      expect(passages).not.toHaveBeenCalled();
    } finally {
      sources.mockRestore();
      passages.mockRestore();
    }
  });

  it("throws DraftNotFoundError for a missing document", async () => {
    const repo = new FakeContentAdminRepository();

    await expect(approveDraft("does-not-exist", repo)).rejects.toThrow(DraftNotFoundError);
  });

  it("refuses to publish a draft with an empty title, intro, body, or tags", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({ ...BASE_DRAFT, title: "  ", tags: [] });

    await expect(approveDraft(contentId, repo)).rejects.toThrow(UnsafeContentError);
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("refuses to publish content without creation and modification timestamps", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft(BASE_DRAFT);
    repo.docs.get(contentId)!.updatedAt = undefined;

    await expect(approveDraft(contentId, repo)).rejects.toThrow("updatedAt is missing");
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it.each(["ritual", "RITUAL", "", "unknown"])("refuses to publish an unsupported type %p", async (type) => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({ ...BASE_DRAFT, type: type as NewDraftContent["type"] });

    await expect(approveDraft(contentId, repo)).rejects.toThrow("is unsupported");
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("refuses to publish a potion with no safety note", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type: "potion",
      tags: ["potion"],
      materials: ["a bowl of water"],
      safetyNote: null,
    });

    await expect(approveDraft(contentId, repo)).rejects.toThrow(UnsafeContentError);
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it.each(["spell", "potion"] as const)("screens a legacy %s without a source passage", async (type) => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type,
      body: "Drink the mixture.",
      lesson: "Set the bowl on the altar.",
      safetyNote: "For external use only.",
    });

    await expect(approveDraft(contentId, repo)).rejects.toThrow("may not instruct ingestion");
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("publishes a potion that does carry a safety note", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type: "potion",
      tags: ["potion"],
      materials: ["a bowl of water"],
      safetyNote: "Not for consumption.",
    });

    await approveDraft(contentId, repo);

    expect(await repo.getStatus(contentId)).toBe("published");
  });

  it("does not treat a broad ancient source record as passage verification", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type: "lesson",
      tags: ["lesson"],
      tradition: ANCIENT_LESSON_SOURCE.tradition,
      sourceId: ANCIENT_LESSON_SOURCE.id,
      reflection: "What does this source teach us about its historical setting?",
    });

    await expect(approveDraft(contentId, repo, [ANCIENT_LESSON_SOURCE])).rejects.toThrow(
      "verified source passage is missing",
    );
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("refuses to publish a lesson whose source is not ancient", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type: "lesson",
      tags: ["lesson"],
      sourceId: EARLY_MODERN_SOURCE.id,
    });

    await expect(approveDraft(contentId, repo, [EARLY_MODERN_SOURCE])).rejects.toThrow(UnsafeContentError);
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("refuses to publish a lesson with an unknown sourceId", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type: "lesson",
      tags: ["lesson"],
      sourceId: "unknown-source",
    });

    await expect(approveDraft(contentId, repo, [ANCIENT_LESSON_SOURCE])).rejects.toThrow(UnsafeContentError);
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("blocks publication until outstanding rights are cleared", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({ ...BASE_DRAFT, rightsReviewRequired: true });
    await expect(approveDraft(contentId, repo)).rejects.toThrow("rights review is outstanding");
    expect(await repo.getStatus(contentId)).toBe("draft");

    repo.docs.get(contentId)!.rightsReviewRequired = false;
    await approveDraft(contentId, repo);
    expect(await repo.getStatus(contentId)).toBe("published");
  });

  it.each([undefined, null, " ", "A statement"])("blocks a sourced lesson with an invalid structured reflection: %s", async (reflection) => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type: "lesson",
      sourceId: ANCIENT_LESSON_SOURCE.id,
      reflection,
    });
    await expect(approveDraft(contentId, repo, [ANCIENT_LESSON_SOURCE])).rejects.toThrow("structured reflection question");
    expect(await repo.getStatus(contentId)).toBe("draft");
  });
it("refuses to publish a draft without its own original image", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      illustrationUrl: undefined,
      illustrationOriginal: undefined,
    });
    await expect(approveDraft(contentId, repo)).rejects.toThrow(/illustrationUrl/);
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("refuses to publish a draft whose image is not marked original", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({ ...BASE_DRAFT, illustrationOriginal: false });
    await expect(approveDraft(contentId, repo)).rejects.toThrow(/illustrationOriginal=true/);
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("refuses to publish a draft that reuses another item's image", async () => {
    const repo = new FakeContentAdminRepository();
    const first = await repo.createDraft({ ...BASE_DRAFT, title: "First Spell", ...originalArt("shared-hero") });
    await approveDraft(first, repo);
    const second = await repo.createDraft({ ...BASE_DRAFT, title: "Second Spell", ...originalArt("shared-hero") });
    await expect(approveDraft(second, repo)).rejects.toThrow(/already used by/);
    expect(await repo.getStatus(second)).toBe("draft");
  });

  it("refuses to publish a draft whose normalized title already exists", async () => {
    const repo = new FakeContentAdminRepository();
    const first = await repo.createDraft({ ...BASE_DRAFT, title: "Threshold Salt Line", ...originalArt("salt-a") });
    await approveDraft(first, repo);
    const second = await repo.createDraft({ ...BASE_DRAFT, title: "  threshold   SALT-line! ", ...originalArt("salt-b") });
    await expect(approveDraft(second, repo)).rejects.toThrow(/duplicate content is not allowed/);
    expect(await repo.getStatus(second)).toBe("draft");
  });

  it("refuses to publish near-duplicate body text even with a new title", async () => {
    const repo = new FakeContentAdminRepository();
    const body = "1. Pour a thin line of salt across the doorway.\n2. Sweep it outward and close the door.";
    const first = await repo.createDraft({
      ...BASE_DRAFT,
      title: "Threshold Salt Line",
      intro: "A doorway closing practice.",
      body,
      ...originalArt("line-a"),
    });
    await approveDraft(first, repo);
    const second = await repo.createDraft({
      ...BASE_DRAFT,
      title: "Evening Door Charm",
      intro: "A doorway closing practice.",
      body,
      ...originalArt("line-b"),
    });
    await expect(approveDraft(second, repo)).rejects.toThrow(/duplicate content is not allowed/);
    expect(await repo.getStatus(second)).toBe("draft");
  });
});
