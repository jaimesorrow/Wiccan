import { CONTENT_CATEGORIES } from "../types";
import { loadContentSeed, findSeedViolations } from "../contentSeed";
import { loadContentSources } from "../contentSource";

describe("ancientContentSeed.json", () => {
  const entries = loadContentSeed();

  it("loads and validates the real checked-in seed file", () => {
    expect(entries.length).toBe(150);
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

  it("spreads evenly across the three reader levels", () => {
    const counts = { beginner: 0, intermediate: 0, advanced: 0 };
    for (const entry of entries) counts[entry.difficulty] += 1;
    expect(counts).toEqual({ beginner: 50, intermediate: 50, advanced: 50 });
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
});
