import { CONTENT_CATEGORIES } from "../types";
import { countSeedTypes, loadContentSeed, findSeedViolations } from "../contentSeed";
import { loadContentSources } from "../contentSource";

describe("ancientContentSeed.json", () => {
  const entries = loadContentSeed();

  it("loads and validates the real checked-in seed file", () => {
    expect(entries.length).toBe(296);
    expect(countSeedTypes(entries)).toEqual({ lesson: 200, spell: 59, potion: 37 });
  });

  it("passes the same structural gates approveDraft enforces", () => {
    expect(findSeedViolations(entries)).toEqual([]);
  });

  it("is entirely unpublished, so seeding can never go live by itself", () => {
    for (const entry of entries) {
      expect(entry.status).toBe("draft");
    }
  });

  it("carries a complete lesson structure on every entry", () => {
    for (const entry of entries) {
      expect(entry.intro.trim().length).toBeGreaterThan(0);
      expect(entry.body.trim().length).toBeGreaterThan(0);
      expect(entry.reflection.trim().endsWith("?")).toBe(true);
      expect(entry.tags.length).toBeGreaterThan(0);
      expect(CONTENT_CATEGORIES).toContain(entry.category);
      expect(entry.sourceCitation.trim().length).toBeGreaterThan(0);
      expect(entry.sourceUrl).toMatch(/^https?:\/\//);
    }
  });

  it("keeps the original 150 entries and their reader levels while adding 146 lesson drafts", () => {
    const counts = { beginner: 0, intermediate: 0, advanced: 0 };
    const original = entries.filter((entry) => !entry.id.startsWith("lesson-study-"));
    expect(original).toHaveLength(150);
    expect(countSeedTypes(original)).toEqual({ lesson: 54, spell: 59, potion: 37 });
    for (const entry of original) counts[entry.difficulty] += 1;
    expect(counts).toEqual({ beginner: 50, intermediate: 50, advanced: 50 });

    const additions = entries.filter((entry) => entry.id.startsWith("lesson-study-"));
    expect(additions).toHaveLength(146);
    for (const entry of additions) {
      expect(entry.type).toBe("lesson");
      expect(entry.body.trim().split(/\s+/).length).toBeGreaterThanOrEqual(110);
      expect(entry.materials).toBeNull();
    }
  });

  it("gives every potion a safety note", () => {
    for (const entry of entries.filter((candidate) => candidate.type === "potion")) {
      expect(entry.safetyNote).toBeTruthy();
    }
  });

  it("draws only on curated ancient sources", () => {
    const ancientIds = loadContentSources()
      .filter((source) => source.era === "ancient")
      .map((source) => source.id);
    for (const entry of entries) {
      expect(ancientIds).toContain(entry.sourceId);
    }
  });

  it("uses unique, deterministic document ids", () => {
    expect(new Set(entries.map((entry) => entry.id)).size).toBe(entries.length);
    for (const entry of entries) {
      expect(entry.id).toMatch(/^[a-z0-9-]+$/);
    }
  });

  it("rejects blank titles and reflections and duplicated bodies before import", () => {
    const first = entries[0];
    const second = { ...entries[1], title: " ", body: first.body, reflection: "Not a question" };
    const violations = findSeedViolations([first, second]).join(" ");
    expect(violations).toContain("title must not be blank");
    expect(violations).toContain("duplicate body");
    expect(violations).toContain("reflection must be a question");
  });
});
