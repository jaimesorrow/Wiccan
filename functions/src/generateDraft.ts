import { ContentSource } from "./contentSource";
import { ContentTier, ContentType } from "./types";
import { DraftGenerationPort } from "./draftGenerationPort";
import { ContentAdminRepositoryPort } from "./contentAdminRepositoryPort";

export interface GenerateDraftRequest {
  tier: ContentTier;
  /** Restrict to one source; omit to draw from the whole curated pool. */
  sourceId?: string;
  /** Restrict to one type; omit to draw from every type the source(s) support. */
  type?: ContentType;
  /** Number of drafts to generate in this call. Defaults to 1. */
  count?: number;
}

export interface GeneratedDraftSummary {
  contentId: string;
  sourceId: string;
  type: ContentType;
  title: string;
}

export class InvalidGenerationRequestError extends Error {}

interface SourceTypePair {
  source: ContentSource;
  type: ContentType;
}

function buildEligiblePairs(
  sources: ContentSource[],
  sourceId?: string,
  type?: ContentType,
): SourceTypePair[] {
  const candidateSources = sourceId ? sources.filter((s) => s.id === sourceId) : sources;
  if (sourceId && candidateSources.length === 0) {
    throw new InvalidGenerationRequestError(`Unknown sourceId '${sourceId}'.`);
  }

  const pairs: SourceTypePair[] = [];
  for (const source of candidateSources) {
    const eligibleTypes = type ? source.eligibleTypes.filter((t) => t === type) : source.eligibleTypes;
    if (type && sourceId && eligibleTypes.length === 0) {
      throw new InvalidGenerationRequestError(
        `Source '${source.id}' is not eligible for type '${type}'.`,
      );
    }
    for (const eligibleType of eligibleTypes) {
      pairs.push({ source, type: eligibleType });
    }
  }

  if (pairs.length === 0) {
    throw new InvalidGenerationRequestError("No eligible source/type combinations available.");
  }
  return pairs;
}

/**
 * Generates `count` drafts (default 1), round-robining across the eligible
 * (source, type) combinations so a batch call spreads across the curated
 * pool — e.g. bootstrapping toward the 365-entry launch target — rather
 * than repeating a single combination. Every draft is written with
 * status "draft" and a sourceId; nothing here ever auto-publishes.
 *
 * A generated draft whose title exactly matches an existing document
 * (draft or published) is not persisted - the LLM occasionally proposes
 * the same signature title for the same source/type pair, and silently
 * storing both would leave two indistinguishable-looking entries in the
 * pool. The returned array only ever contains drafts actually written, so
 * a shorter-than-requested result means one or more duplicates were
 * skipped; the operator can re-run for the remainder.
 */
export async function generateDrafts(
  request: GenerateDraftRequest,
  sources: ContentSource[],
  generator: DraftGenerationPort,
  repo: ContentAdminRepositoryPort,
): Promise<GeneratedDraftSummary[]> {
  const count = request.count ?? 1;
  if (count < 1) {
    throw new InvalidGenerationRequestError("count must be at least 1.");
  }

  const pairs = buildEligiblePairs(sources, request.sourceId, request.type);
  const results: GeneratedDraftSummary[] = [];

  for (let i = 0; i < count; i++) {
    const pair = pairs[i % pairs.length];
    const generated = await generator.generateDraft({
      type: pair.type,
      tradition: pair.source.tradition,
      summary: pair.source.summary,
    });

    if (await repo.titleExists(generated.title)) {
      continue;
    }

    const contentId = await repo.createDraft({
      ...generated,
      type: pair.type,
      tier: request.tier,
      tradition: pair.source.tradition,
      sourceId: pair.source.id,
    });

    results.push({ contentId, sourceId: pair.source.id, type: pair.type, title: generated.title });
  }

  return results;
}
