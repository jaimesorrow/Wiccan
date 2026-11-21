import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { ContentPoolExhaustedError, selectTodayContent } from "../getTodayContent";
import { isEligibleForDailyDelivery } from "../contentEligibility";
import { contentFromStoredDocument } from "../firestoreContentRepository";
import { AUTHORED_FIELDS, practicalContentHash } from "../practicalLesson";
import { FakeContentRepository } from "../testUtils/fakeContentRepository";
import type { Content } from "../types";

const UID = "integrity-user";
const TODAY = "2026-11-21";
const REVIEWED_FIELDS_TO_MUTATE = ["slug", "introduction", "lesson", "title"] as const;

type Fixture = Record<string, unknown> & { id: string; type: string; sourceId?: string };

function loadFixture(name: string): Fixture {
  return JSON.parse(readFileSync(resolve(__dirname, "fixtures", name), "utf8")) as Fixture;
}

const fixtures: { name: string; file: string }[] = [
  { name: "lesson london-leiden-056", file: "published-lesson-london-leiden-056.json" },
  { name: "spell maqlu-088", file: "published-spell-maqlu-088.json" },
  { name: "potion maqlu-143", file: "published-potion-maqlu-143.json" },
];

function storedFields(fixture: Fixture): Record<string, unknown> {
  return Object.fromEntries(Object.entries(fixture).filter(([key]) => key !== "id"));
}

function mapped(fixture: Fixture): Content {
  return contentFromStoredDocument(fixture.id, storedFields(fixture));
}

function ancientSources(fixture: Fixture): Set<string> {
  return typeof fixture.sourceId === "string" ? new Set([fixture.sourceId]) : new Set();
}

describe("published content integrity", () => {
  it.each(fixtures)("$name: the publisher and the delivery mapper hash the same reviewed fields", ({ file }) => {
    const fixture = loadFixture(file);
    const rawHash = practicalContentHash(storedFields(fixture));
    const mappedHash = practicalContentHash(mapped(fixture));
    expect(mappedHash).toBe(rawHash);
    const storedHash = (fixture.practicalReview as { contentHash?: string } | undefined)?.contentHash;
    if (typeof storedHash === "string") {
      expect(rawHash).toBe(storedHash);
      expect(mappedHash).toBe(storedHash);
    }
  });

  it.each(fixtures)("$name: is served by getTodayContent", async ({ file }) => {
    const fixture = loadFixture(file);
    const content = mapped(fixture);
    expect(isEligibleForDailyDelivery(content, ancientSources(fixture))).toBe(true);
    const repo = new FakeContentRepository();
    repo.content = [content];
    const selected = await selectTodayContent(UID, TODAY, repo, ancientSources(fixture));
    expect(selected.id).toBe(fixture.id);
  });

  it.each(fixtures)("$name: a mutated reviewed field changes the hash and is rejected for lessons", async ({ file }) => {
    const fixture = loadFixture(file);
    const originalHash = practicalContentHash(storedFields(fixture));
    for (const field of REVIEWED_FIELDS_TO_MUTATE) {
      if (typeof fixture[field] !== "string") continue;
      const mutatedFields = { ...storedFields(fixture), [field]: `${fixture[field]} tampered` };
      const mutatedHash = practicalContentHash(mutatedFields);
      expect(mutatedHash).not.toBe(originalHash);
      expect(practicalContentHash(contentFromStoredDocument(fixture.id, mutatedFields))).toBe(mutatedHash);

      const mutatedContent = contentFromStoredDocument(fixture.id, mutatedFields);
      if (fixture.type === "lesson") {
        expect(isEligibleForDailyDelivery(mutatedContent, ancientSources(fixture))).toBe(false);
        const repo = new FakeContentRepository();
        repo.content = [mutatedContent];
        await expect(selectTodayContent(UID, TODAY, repo, ancientSources(fixture)))
          .rejects.toBeInstanceOf(ContentPoolExhaustedError);
      }
    }
  });

  it("dropping slug, introduction or lesson (the old mapper) fails the stored review checksum", () => {
    const fixture = loadFixture("published-lesson-london-leiden-056.json");
    const storedHash = (fixture.practicalReview as { contentHash: string }).contentHash;
    expect(practicalContentHash(storedFields(fixture))).toBe(storedHash);

    const dropped = { ...storedFields(fixture) };
    delete dropped.slug;
    delete dropped.introduction;
    delete dropped.lesson;
    expect(practicalContentHash(dropped)).not.toBe(storedHash);
    expect(AUTHORED_FIELDS).toEqual(expect.arrayContaining(["slug", "introduction", "lesson"]));
  });

  it("a lesson with a missing review checksum is still ineligible", () => {
    const fixture = loadFixture("published-lesson-london-leiden-056.json");
    const withoutHash = contentFromStoredDocument(fixture.id, {
      ...storedFields(fixture),
      practicalReview: { ...(fixture.practicalReview as object), contentHash: undefined },
    });
    expect(isEligibleForDailyDelivery(withoutHash, ancientSources(fixture))).toBe(false);
    const withoutReview = contentFromStoredDocument(fixture.id, {
      ...storedFields(fixture),
      practicalReview: undefined,
    });
    expect(isEligibleForDailyDelivery(withoutReview, ancientSources(fixture))).toBe(false);
  });
});
