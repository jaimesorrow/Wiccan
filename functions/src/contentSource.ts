import fs from "fs";
import path from "path";
import { z } from "zod";
import { ContentType } from "./types";

/**
 * A hand-curated, manually vetted public-domain source. Adding one is a
 * deliberate edit to sources.json — never auto-populated from a crawl.
 */
export interface ContentSource {
  id: string;
  title: string;
  tradition: string;
  publicDomainBasis: string;
  /** Jaime's own short, vetted summary — never a verbatim excerpt. */
  summary: string;
  eligibleTypes: ContentType[];
}

const ContentSourceSchema = z.object({
  id: z.string().min(1),
  title: z.string().min(1),
  tradition: z.string().min(1),
  publicDomainBasis: z.string().min(1),
  summary: z.string().min(1),
  eligibleTypes: z.array(z.enum(["spell", "potion", "lesson"])).min(1),
});

const ContentSourcesSchema = z.array(ContentSourceSchema);

/**
 * Loads and validates sources.json. Throws on a malformed entry rather
 * than silently dropping it — a curation mistake should fail loudly, not
 * quietly shrink the eligible pool.
 */
export function loadContentSources(
  sourcesPath: string = path.join(__dirname, "..", "sources.json"),
): ContentSource[] {
  const raw = fs.readFileSync(sourcesPath, "utf-8");
  const parsed: unknown = JSON.parse(raw);
  return ContentSourcesSchema.parse(parsed);
}
