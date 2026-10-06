import { approveDraft, DraftNotFoundError, UnsafeContentError } from "../approveDraft";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import { NewDraftContent } from "../contentAdminRepositoryPort";
import { ContentSource } from "../contentSource";

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
};

const ANCIENT_LESSON_SOURCE: ContentSource = {
  id: "ancient-source",
  title: "Ancient Source",
  tradition: "Ancient ritual tradition",
  era: "ancient",
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

    await expect(approveDraft(contentId, repo)).resolves.toBeUndefined();
    expect(await repo.getStatus(contentId)).toBe("published");
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

  it("publishes a lesson only when its source is curated, ancient, and lesson-eligible", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      ...BASE_DRAFT,
      type: "lesson",
      tags: ["lesson"],
      tradition: ANCIENT_LESSON_SOURCE.tradition,
      sourceId: ANCIENT_LESSON_SOURCE.id,
      reflection: "What does this source teach us about its historical setting?",
    });

    await approveDraft(contentId, repo, [ANCIENT_LESSON_SOURCE]);

    expect(await repo.getStatus(contentId)).toBe("published");
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
});
