import { approveDraft, DraftNotFoundError, UnsafeContentError } from "../approveDraft";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import { NewDraftContent } from "../contentAdminRepositoryPort";

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
});
