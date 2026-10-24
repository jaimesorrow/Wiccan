import fs from "fs";
import path from "path";
import { z } from "zod";
import { ContentType } from "./types";

export type HistoricalEra = "ancient" | "medieval" | "early-modern" | "modern";
export type ContentSourceKind = "ancient-grimoire" | "historical-grimoire" | "ancient-historical-context";

/**
 * A hand-curated, manually vetted historical source.
 *
 * Sources are never auto-populated from a crawl. For every generated content
 * type, groundedFacts is the factual boundary: the generator may explain and
 * contextualize those facts, but must not invent additional ancient claims,
 * quotations, lineages, dates, or ritual details.
 *
 * Product requirement: Lesson of the Day is sourced from ancient material.
 * The schema therefore rejects any lesson-eligible source whose era is not
 * explicitly "ancient". Changing that policy later must be a deliberate
 * code change, not an accidental sources.json edit.
 */
export interface ContentSource {
  id: string;
  title: string;
  tradition: string;
  era: HistoricalEra;
  /** Only pre-modern grimoire sources are eligible for AI content generation. */
  sourceKind: ContentSourceKind;
  period: string;
  publicDomainBasis: string;
  /** Human-readable provenance for editorial review (work, corpus, museum/catalogue, or scholarly database). */
  sourceReference: string;
  /** Jaime's own short, vetted summary — never a verbatim excerpt. */
  summary: string;
  /** Independently written, source-grounded facts the generator is allowed to use for historical lesson claims. */
  groundedFacts: string[];
  eligibleTypes: ContentType[];
}

const ContentSourceSchema = z.object({
  id: z.string().min(1),
  title: z.string().min(1),
  tradition: z.string().min(1),
  era: z.enum(["ancient", "medieval", "early-modern", "modern"]),
  sourceKind: z.enum(["ancient-grimoire", "historical-grimoire", "ancient-historical-context"]),
  period: z.string().min(1),
  publicDomainBasis: z.string().min(1),
  sourceReference: z.string().min(1),
  summary: z.string().min(1),
  groundedFacts: z.array(z.string().min(1)).min(1),
  eligibleTypes: z.array(z.enum(["spell", "potion", "lesson"])).min(1),
}).superRefine((source, ctx) => {
  if (source.sourceKind === "ancient-grimoire" && source.era !== "ancient") {
    ctx.addIssue({
      code: "custom",
      path: ["sourceKind"],
      message: "Sources classified as ancient grimoires must have era 'ancient'.",
    });
  }
  if (source.sourceKind === "historical-grimoire" && !["medieval", "early-modern"].includes(source.era)) {
    ctx.addIssue({
      code: "custom",
      path: ["era"],
      message: "Historical grimoires must be medieval or early-modern.",
    });
  }
});

const ContentSourcesSchema = z.array(ContentSourceSchema);

/**
 * Loads and validates sources.json. Throws on a malformed entry instead
 * of silently dropping it — a curation mistake should fail loudly, not
 * quietly shrink the eligible pool.
 */
export function loadContentSources(
  sourcesPath: string = path.join(__dirname, "..", "sources.json"),
): ContentSource[] {
  const raw = fs.readFileSync(sourcesPath, "utf-8");
  const parsed: unknown = JSON.parse(raw);
  return ContentSourcesSchema.parse(parsed);
}

/** Candidate summaries never qualify a source by themselves; exact passage review remains mandatory. */
export function isLessonGenerationSource(source: ContentSource): boolean {
  return (source.sourceKind === "ancient-grimoire" || source.sourceKind === "historical-grimoire") && source.era !== "modern";
}
