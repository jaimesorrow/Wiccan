import fs from "fs";
import { CONTENT_CATEGORIES } from "../types";
import { loadContentSeed, findSeedViolations, SEED_PATH_CANDIDATES } from "../contentSeed";
import { loadContentSources } from "../contentSource";

describe("ancientContentSeed.json", () => {
  const entries = loadContentSeed();

  it("loads and validates the real checked-in seed file", () => {
    expect(entries.length).toBe(296);
  });

  it("passes the same structural gates approveDraft enforces", () => {
    expect(findSeedViolations(entries)).toEqual([]);
  });

  it("is entirely unpublished, so seeding can never go live by itself", () => {
    for (const entry of entries) {
      expect(entry.title.trim().length).toBeGreaterThan(0);
      expect(entry.tradition.trim().length).toBeGreaterThan(0);
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
      expect(typeof entry.rightsReviewRequired).toBe("boolean");
      expect(entry).toHaveProperty("practice");
      expect(entry).toHaveProperty("safetyNote");
    }
  });

  it("contains no serialized difficulty labels while preserving free/premium access", () => {
    const seedPath = SEED_PATH_CANDIDATES.find((candidate) => fs.existsSync(candidate));
    if (!seedPath) throw new Error("Missing checked-in seed file.");
    const rawEntries: Record<string, unknown>[] = JSON.parse(fs.readFileSync(seedPath, "utf-8"));
    for (const entry of rawEntries) expect(entry).not.toHaveProperty("difficulty");
    expect(entries.filter((entry) => entry.tier === "free")).toHaveLength(100);
    expect(entries.filter((entry) => entry.tier === "premium")).toHaveLength(196);
  });

  it("uses shared editorial identities for identifiable rewordings, independent of type", () => {
    const chargingWater = entries.find((entry) => entry.id === "egypt-heka-001-charging-water-by-contact");
    const healingCippi = entries.find((entry) => entry.id === "egypt-heka-004-healing-statues-and-cippi");
    const torch = entries.find((entry) => entry.id === "maqlu-115-i-am-raising-the-torch");
    const torchBundle = entries.find((entry) => entry.id === "maqlu-145-the-aromatic-torch-bundle");
    expect(chargingWater?.similarityGroupId).toBe(healingCippi?.similarityGroupId);
    expect(torch?.similarityGroupId).toBe(torchBundle?.similarityGroupId);
    expect(torch?.type).not.toBe(torchBundle?.type);
    for (const entry of entries) expect(entry.similarityGroupId).toMatch(/^[a-z0-9-]+$/);
  });

  it("does not group different lessons solely because they share a broad source citation", () => {
    const wedjat = entries.find((entry) => entry.id === "egypt-heka-023-wedjat-eye-and-heart-scarab");
    const amuletPower = entries.find((entry) => entry.id === "egypt-heka-025-where-an-amulet-s-power-came-from");
    expect(wedjat?.sourceCitation).toBe(amuletPower?.sourceCitation);
    expect(wedjat?.similarityGroupId).not.toBe(amuletPower?.similarityGroupId);
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
