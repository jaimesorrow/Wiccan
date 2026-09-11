import { selectTodayContent } from "../getTodayContent";
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

    expect(["c1", "c2"]).toContain(result.id);
    expect(repo.history).toEqual([
      {
        userId: UID,
        contentId: result.id,
        deliveredDate: TODAY,
        title: result.title,
        type: result.type,
        tradition: result.tradition,
      },
    ]);
    expect(repo.poolExhaustionEvents).toHaveLength(0);
  });

  it("is idempotent: a repeat call the same day returns the same content without a new history row", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "c1" }), makeContent({ id: "c2" })];

    const first = await selectTodayContent(UID, TODAY, repo);
    const second = await selectTodayContent(UID, TODAY, repo);

    expect(second.id).toBe(first.id);
    expect(repo.history).toHaveLength(1);
  });

  it("falls back to repeats and logs pool exhaustion once the published pool is used up", async () => {
    const repo = new FakeContentRepository();
    repo.content = [makeContent({ id: "c1" }), makeContent({ id: "c2" })];
    // Simulate the user having already seen every published item on prior days.
    repo.history = [
      { userId: UID, contentId: "c1", deliveredDate: "2026-09-07", title: "Title c1", type: "spell", tradition: "European folk magic" },
      { userId: UID, contentId: "c2", deliveredDate: "2026-09-08", title: "Title c2", type: "spell", tradition: "European folk magic" },
    ];

    const result = await selectTodayContent(UID, TODAY, repo);

    expect(["c1", "c2"]).toContain(result.id);
    expect(repo.poolExhaustionEvents).toEqual([{ userId: UID, tier: "free", today: TODAY }]);
    expect(repo.history).toHaveLength(3);
    expect(repo.history[2]).toEqual({
      userId: UID,
      contentId: result.id,
      deliveredDate: TODAY,
      title: result.title,
      type: result.type,
      tradition: result.tradition,
    });
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
    });
  });

  it("only offers a free user free-tier published content", async () => {
    const repo = new FakeContentRepository();
    repo.tier = "free";
    repo.content = [makeContent({ id: "premium-only", tier: "premium" })];

    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toThrow(
      "No published content available for tier 'free'.",
    );
  });

  it("lets a premium user draw from both free and premium published content", async () => {
    const repo = new FakeContentRepository();
    repo.tier = "premium";
    repo.content = [
      makeContent({ id: "free-1", tier: "free" }),
      makeContent({ id: "premium-1", tier: "premium" }),
    ];

    const result = await selectTodayContent(UID, TODAY, repo);

    expect(["free-1", "premium-1"]).toContain(result.id);
  });
});
