import { approveDraft, DraftNotFoundError } from "../approveDraft";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";

describe("approveDraft", () => {
  it("flips a draft to published", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      title: "t",
      intro: "i",
      body: "b",
      materials: null,
      safetyNote: null,
      tags: [],
      type: "spell",
      tier: "free",
      tradition: "European folk magic",
      sourceId: "source-a",
    });

    await approveDraft(contentId, repo);

    expect(await repo.getStatus(contentId)).toBe("published");
  });

  it("is idempotent for an already-published item", async () => {
    const repo = new FakeContentAdminRepository();
    const contentId = await repo.createDraft({
      title: "t",
      intro: "i",
      body: "b",
      materials: null,
      safetyNote: null,
      tags: [],
      type: "spell",
      tier: "free",
      tradition: "European folk magic",
      sourceId: "source-a",
    });
    await approveDraft(contentId, repo);

    await expect(approveDraft(contentId, repo)).resolves.toBeUndefined();
    expect(await repo.getStatus(contentId)).toBe("published");
  });

  it("throws DraftNotFoundError for a missing document", async () => {
    const repo = new FakeContentAdminRepository();

    await expect(approveDraft("does-not-exist", repo)).rejects.toThrow(DraftNotFoundError);
  });
});
