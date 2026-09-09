import assert from "node:assert/strict";
import test from "node:test";
import { Content, ContentStore, LoggerLike, UserContentHistory, getTodayContentForUser } from "./index";

class InMemoryContentStore implements ContentStore {
  constructor(
    private readonly content: Content[],
    private readonly history: UserContentHistory[] = [],
    private readonly tier: "free" | "premium" = "free",
  ) {}

  async getTodayContent(userId: string, deliveredDate: string): Promise<Content | null> {
    const entry = this.history.find(
      (item) => item.userId === userId && item.deliveredDate === deliveredDate,
    );
    return this.content.find((item) => item.id === entry?.contentId) ?? null;
  }

  async getUserTier(): Promise<"free" | "premium"> {
    return this.tier;
  }

  async listPublishedContent(): Promise<Content[]> {
    return this.content.filter((item) => item.status === "published");
  }

  async listDeliveredContentIds(userId: string): Promise<string[]> {
    return this.history.filter((item) => item.userId === userId).map((item) => item.contentId);
  }

  async writeHistory(entry: UserContentHistory): Promise<void> {
    this.history.push(entry);
  }

  getHistory(): UserContentHistory[] {
    return this.history;
  }
}

class TestLogger implements LoggerLike {
  warnings: Array<{ message: string; metadata?: Record<string, unknown> }> = [];

  warn(message: string, metadata?: Record<string, unknown>): void {
    this.warnings.push({ message, metadata });
  }
}

const spell: Content = {
  id: "spell-1",
  type: "spell",
  tier: "free",
  tradition: "European folk magic",
  title: "Morning Ember",
  intro: "A grounding candle spell.",
  body: "Light the candle and focus on your breath.",
  materials: ["candle"],
  safetyNote: "Never leave candles unattended.",
  tags: ["grounding"],
  status: "published",
};

test("returns and records content on the first call of the day", async () => {
  const store = new InMemoryContentStore([spell]);

  const result = await getTodayContentForUser({
    uid: "user-1",
    store,
    today: "2026-09-09",
    random: () => 0,
  });

  assert.equal(result.id, spell.id);
  assert.deepEqual(store.getHistory(), [
    {
      userId: "user-1",
      contentId: spell.id,
      deliveredDate: "2026-09-09",
    },
  ]);
});

test("returns the same content on repeat calls for the same day", async () => {
  const history = [
    {
      userId: "user-1",
      contentId: spell.id,
      deliveredDate: "2026-09-09",
    },
  ];
  const store = new InMemoryContentStore([spell], history);

  const result = await getTodayContentForUser({
    uid: "user-1",
    store,
    today: "2026-09-09",
    random: () => {
      throw new Error("random should not be used when content already exists");
    },
  });

  assert.equal(result.id, spell.id);
  assert.equal(store.getHistory().length, 1);
});

test("falls back to the full eligible published pool when all content was previously delivered", async () => {
  const logger = new TestLogger();
  const history = [
    {
      userId: "user-1",
      contentId: spell.id,
      deliveredDate: "2026-09-08",
    },
  ];
  const store = new InMemoryContentStore([spell], history);

  const result = await getTodayContentForUser({
    uid: "user-1",
    store,
    today: "2026-09-09",
    random: () => 0,
    loggerInstance: logger,
  });

  assert.equal(result.id, spell.id);
  assert.equal(store.getHistory().length, 2);
  assert.equal(logger.warnings.length, 1);
  assert.match(logger.warnings[0].message, /pool exhausted/i);
});
