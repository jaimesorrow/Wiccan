import fs from "fs";
import path from "path";
import { z } from "zod";
import { CONTENT_CATEGORIES, SeedContent } from "./types";
import { ContentSource, loadContentSources } from "./contentSource";

/**
 * The checked-in ancient-source content seed
 * (`functions/ancientContentSeed.json`).
 *
 * Unlike `generateDraft`, this pool is not produced by the LLM pipeline at
 * run time: it is written as a file diff for editorial review,
 * and is loaded here so the same structural gates that protect generated
 * drafts also protect seeded ones. Everything in the file is
 * `status: "draft"` by design - seeding never publishes. Publication stays
 * a deliberate per-document `approve-draft` action.
 */
const SeedContentSchema = z.object({
  id: z.string().min(1),
  type: z.enum(["spell", "potion", "lesson"]),
  tier: z.enum(["free", "premium"]),
  tradition: z.string().min(1),
  category: z.enum(CONTENT_CATEGORIES),
  title: z.string().min(1),
  intro: z.string().min(1),
  body: z.string().min(1),
  reflection: z.string().min(1),
  practice: z.string().min(1).nullable(),
  materials: z.array(z.string().min(1)).nullable(),
  safetyNote: z.string().min(1).nullable(),
  tags: z.array(z.string().min(1)).min(1),
  iconTag: z.string().min(1),
  sourceId: z.string().min(1),
  sourceCitation: z.string().min(1),
  sourceUrl: z.string().url(),
  similarityGroupId: z.string().trim().min(1).optional(),
  rightsNotes: z.string().nullable().optional(),
  /**
   * True when the entry still contains a phrase taken from a modern
   * translation, or is drawn from museum prose whose reuse terms have not
   * been confirmed. Per docs/ANCIENT_LESSON_SOURCING.md, rights must be
   * verified before such an entry is published.
   */
  rightsReviewRequired: z.boolean(),
  status: z.literal("draft"),
});

const SeedContentsSchema = z.array(SeedContentSchema).min(1);

/** Counts actual lessons, independently of the spells and potions in the pool. */
export const EXPECTED_LESSON_COUNT = 200;

export function countSeedTypes(entries: SeedContent[]): Record<SeedContent["type"], number> {
  const counts = { lesson: 0, spell: 0, potion: 0 };
  for (const entry of entries) counts[entry.type] += 1;
  return counts;
}

export const SEED_PATH_CANDIDATES = [
  // Running from src/ (ts-jest) or from lib/scripts/ (compiled CLI).
  path.join(__dirname, "..", "ancientContentSeed.json"),
  path.join(__dirname, "..", "..", "ancientContentSeed.json"),
];

function resolveSeedPath(): string {
  const found = SEED_PATH_CANDIDATES.find((candidate) => fs.existsSync(candidate));
  if (found === undefined) {
    throw new Error(`ancientContentSeed.json not found. Looked in: ${SEED_PATH_CANDIDATES.join(", ")}`);
  }
  return found;
}

/** Loads and validates the seed file. Throws loudly on a malformed entry. */
export function loadContentSeed(seedPath: string = resolveSeedPath()): SeedContent[] {
  const raw = fs.readFileSync(seedPath, "utf-8");
  const parsed: unknown = JSON.parse(raw);
  return SeedContentsSchema.parse(parsed);
}

/**
 * Structural gates the seed pool must pass before it is written to
 * Firestore. Mirrors approveDraft's checks, applied at seed time so a bad
 * row fails in review rather than sitting in the pool until someone tries
 * to publish it.
 */
export function findSeedViolations(
  entries: SeedContent[],
  sources: ContentSource[] = loadContentSources(),
): string[] {
  const violations: string[] = [];
  const seen = new Set<string>();
  const titles = new Set<string>();
  const bodies = new Set<string>();

  for (const entry of entries) {
    if (seen.has(entry.id)) violations.push(`duplicate id '${entry.id}'.`);
    seen.add(entry.id);

    const normalizedTitle = entry.title.trim().toLowerCase();
    if (!normalizedTitle) violations.push(`'${entry.id}': title must not be blank.`);
    if (titles.has(normalizedTitle)) violations.push(`duplicate title '${entry.title}'.`);
    titles.add(normalizedTitle);

    const normalizedBody = entry.body.trim().replace(/\s+/g, " ").toLowerCase();
    if (bodies.has(normalizedBody)) violations.push(`duplicate body '${entry.id}'.`);
    bodies.add(normalizedBody);
    if (!entry.intro.trim() || !normalizedBody || !entry.reflection.trim()) {
      violations.push(`'${entry.id}': introduction, body and reflection must not be blank.`);
    }
    if (!entry.reflection.trim().endsWith("?")) {
      violations.push(`'${entry.id}': reflection must be a question.`);
    }

    if (entry.type === "potion" && !entry.safetyNote?.trim()) {
      violations.push(`'${entry.id}': type 'potion' requires a safetyNote.`);
    }

    const source = sources.find((candidate) => candidate.id === entry.sourceId);
    if (!source) {
      violations.push(`'${entry.id}': sourceId '${entry.sourceId}' is not in the curated source registry.`);
      continue;
    }
    if (source.era !== "ancient") {
      violations.push(`'${entry.id}': source '${source.id}' is not marked ancient.`);
    }
    if (!source.eligibleTypes.includes(entry.type)) {
      violations.push(`'${entry.id}': source '${source.id}' is not approved for type '${entry.type}'.`);
    }
    if (source.groundedFacts.length === 0) {
      violations.push(`'${entry.id}': source '${source.id}' has no grounded historical facts.`);
    }
  }

  return violations;
}
