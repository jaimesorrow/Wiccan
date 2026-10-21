import { ContentPoolExhaustedError, DailyAssignmentUnavailableError, selectTodayContent } from "../getTodayContent";
import { SubscriptionRequiredError } from "../contentRepositoryPort";
import { FakeContentRepository, makeContent } from "../testUtils/fakeContentRepository";

const UID = "user-123";
const TODAY = "2026-09-09";

describe("selectTodayContent", () => {
  it("selects a published, in-tier candidate on the first call of the day and records it", async () => {
    const repo = new FakeContentRepository();
    repo.content = [
      makeContent({ id: "c1" }),
      makeContent({ id: "c2" }),
      makeContent({ id: "premium-1", tier: "premium" }),
      makeContent({ id: "draft-1", status: "draft" }),
    ];

    const result = await selectTodayContent(UID, TODAY, repo);

    expect(["c1", "c2", "premium-1"]).toContain(result.id);
    expect(repo.history).toEqual([
      {
        userId: UID,
        contentId: result.id,
        deliveredDate: TODAY,
        title: result.title,
        type: result.type,
        tradition: result.tradition,
        category: result.category,
      },
    ]);
    expect(repo.poolExhaustionEvents).toHaveLength(0);
  });

  it("delivers a lesson only when its sourceId is in the ancient lesson registry", async () => {
    const repo = new FakeContentRepository();
    repo.content = [
      makeContent({ id: "old-lesson", type: "lesson", sourceId: "early-modern-source" }),
      makeContent({ id: "ancient-lesson", type: "lesson", sourceId: "ancient-source" }),
    ];

    const result = await selectTodayContent(UID, TODAY, repo, new Set(["ancient-source"]));

    expect(result.id).toBe("ancient-lesson");
  });

  it("does not fall back to a non-ancient lesson when no eligible content exists", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "old-lesson", type: "lesson", sourceId: "early-modern-source" })];

    await expect(
      selectTodayContent(UID, TODAY, repo, new Set(["ancient-source"])),
    ).rejects.toThrow("No new published content available for tier 'premium'.");
  });

  it("is idempotent: a repeat call the same day returns the same content without a new history row", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "c1" }), makeContent({ id: "c2" })];

    const first = await selectTodayContent(UID, TODAY, repo);
    const second = await selectTodayContent(UID, TODAY, repo);

    expect(second.id).toBe(first.id);
    expect(repo.history).toHaveLength(1);
  });

  it("blocks the same-day history fast path after access lapses and restores the same assignment on renewal", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "assigned" })];
    await selectTodayContent(UID, TODAY, repo);
    repo.tier = "free";
    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(SubscriptionRequiredError);
    expect(repo.history).toHaveLength(1);
    repo.tier = "premium";
    expect((await selectTodayContent(UID, TODAY, repo)).id).toBe("assigned");
    expect(repo.history).toHaveLength(1);
  });

  it("rechecks access before returning an assigned document", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "assigned" })];
    const read = repo.getContentById.bind(repo);
    repo.getContentById = async (id) => {
      repo.tier = "free";
      return read(id);
    };
    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(SubscriptionRequiredError);
  });

  it("never repeats delivered content when the published pool is used up", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "c1" }), makeContent({ id: "c2" })];
    // Simulate the user having already seen every published item on prior days.
    repo.history = [
      { userId: UID, contentId: "c1", deliveredDate: "2026-09-07", title: "Title c1", type: "spell", tradition: "European folk magic", category: "MoonAndSeasonal" },
      { userId: UID, contentId: "c2", deliveredDate: "2026-09-08", title: "Title c2", type: "spell", tradition: "European folk magic", category: "MoonAndSeasonal" },
    ];

    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toThrow(
      "No new published content available for tier 'premium'.",
    );
    expect(repo.poolExhaustionEvents).toEqual([{ userId: UID, tier: "premium", today: TODAY }]);
    expect(repo.history).toHaveLength(2);
  });

  it("signals pool exhaustion with a distinct error for the callable and app", async () => {
    const repo = new FakeContentRepository();
    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(ContentPoolExhaustedError);
    expect(repo.poolExhaustionEvents).toEqual([{ userId: UID, tier: "premium", today: TODAY }]);
  });

  it("two concurrent first-of-the-day calls converge on exactly one committed assignment", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "c1" }), makeContent({ id: "c2" }), makeContent({ id: "c3" })];

    const results = await Promise.all([
      selectTodayContent(UID, TODAY, repo),
      selectTodayContent(UID, TODAY, repo),
      selectTodayContent(UID, TODAY, repo),
    ]);

    expect(new Set(results.map((r) => r.id)).size).toBe(1);
    expect(repo.history).toHaveLength(1);
    expect(repo.history[0]).toEqual({
      userId: UID,
      contentId: results[0].id,
      deliveredDate: TODAY,
      title: results[0].title,
      type: results[0].type,
      tradition: results[0].tradition,
      category: results[0].category,
    });
  });

  it("cannot deliver the same content on two concurrently selected dates", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "c1" }), makeContent({ id: "c2" })];
    const random = jest.spyOn(Math, "random").mockReturnValue(0);
    try {
      const [first, second] = await Promise.all([
        selectTodayContent(UID, TODAY, repo),
        selectTodayContent(UID, "2026-09-10", repo),
      ]);
      expect(new Set([first.id, second.id])).toEqual(new Set(["c1", "c2"]));
      expect(repo.history).toHaveLength(2);
    } finally {
      random.mockRestore();
    }
  });

  it("blocks an account without a subscription even when content was formerly marked free", async () => {
    const repo = new FakeContentRepository();
    repo.tier = "free";
    repo.content = [makeContent({ id: "formerly-free", tier: "free" })];

    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(SubscriptionRequiredError);
    expect(repo.history).toHaveLength(0);
  });

  it("uses one published pool regardless of legacy content tier", async () => {
    const repo = new FakeContentRepository();
    repo.tier = "premium";
    repo.content = [
      makeContent({ id: "free-1", tier: "free" }),
      makeContent({ id: "premium-1", tier: "premium" }),
    ];

    const result = await selectTodayContent(UID, TODAY, repo);

    expect(["free-1", "premium-1"]).toContain(result.id);
  });

  it("excludes an editorial rewording of a historically delivered lesson", async () => {
    const repo = new FakeContentRepository();
    repo.content = [
      makeContent({ id: "original", similarityGroupId: "water-and-ritual" }),
      makeContent({ id: "reworded", similarityGroupId: "water-and-ritual" }),
      makeContent({ id: "unseen", similarityGroupId: "seasonal-calendar" }),
    ];
    repo.history = [{ userId: UID, contentId: "original", deliveredDate: "2026-09-08", title: "Original", type: "spell", tradition: "European folk magic", category: "MoonAndSeasonal" }];

    expect((await selectTodayContent(UID, TODAY, repo)).id).toBe("unseen");
    expect(repo.history).toHaveLength(2);
  });

  it("does not overwrite today's historical assignment if its content was removed", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "unseen" })];
    repo.history = [{ userId: UID, contentId: "removed", deliveredDate: TODAY, title: "Removed", type: "spell", tradition: "European folk magic", category: "MoonAndSeasonal" }];

    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(DailyAssignmentUnavailableError);
    expect(repo.history).toHaveLength(1);
    expect(repo.history[0].contentId).toBe("removed");
  });

  it("a later local date receives a different unseen item", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "c1" }), makeContent({ id: "c2" })];

    const first = await selectTodayContent(UID, TODAY, repo);
    const second = await selectTodayContent(UID, "2026-09-10", repo);

    expect(second.id).not.toBe(first.id);
    expect(repo.history.map((entry) => entry.deliveredDate)).toEqual([TODAY, "2026-09-10"]);
  });
});
