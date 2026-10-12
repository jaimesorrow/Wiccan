import { ContentSource } from "./contentSource";
import { ContentTier, ContentType, isContentCategory } from "./types";
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
  /** Stable editorial lesson identity, reused for revisions and near-duplicates. */
  similarityGroupId?: string;
  /** Optional precise passage citation; defaults to the curated source reference. */
  sourceCitation?: string;
}

export interface GeneratedDraftSummary {
  contentId: string;
  sourceId: string;
  type: ContentType;
  title: string;
}

export class InvalidGenerationRequestError extends Error {}
export class InvalidGeneratedDraftError extends Error {}

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

  const grimoireSources = candidateSources.filter(
    (source) => source.era === "ancient" && source.sourceKind === "ancient-grimoire",
  );
  if (sourceId && grimoireSources.length === 0) {
    throw new InvalidGenerationRequestError(
      `Source '${sourceId}' is not an approved ancient grimoire and cannot be used for generation.`,
    );
  }

  const pairs: SourceTypePair[] = [];
  for (const source of grimoireSources) {
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

function validateGeneratedDraft(
  generated: Awaited<ReturnType<DraftGenerationPort["generateDraft"]>>,
  type: ContentType,
): void {
  const violations: string[] = [];
  if (generated.title.trim().length === 0) violations.push("title is empty");
  if (generated.intro.trim().length === 0) violations.push("intro is empty");
  if (generated.body.trim().length === 0) violations.push("body is empty");
  if (!generated.reflection?.trim().endsWith("?")) {
    violations.push("reflection must be a non-empty question");
  }
  if (generated.practice !== null && generated.practice !== undefined && generated.practice.trim().length === 0) {
    violations.push("practice must be non-empty or null");
  }
  if (generated.materials !== null && generated.materials.some((material) => material.trim().length === 0)) {
    violations.push("materials cannot contain blank entries");
  }
  if (generated.tags.length === 0 || generated.tags.some((tag) => tag.trim().length === 0)) {
    violations.push("tags must contain at least one non-empty tag");
  }
  if (!isContentCategory(generated.category)) violations.push("category is invalid");
  if (type === "potion" && !generated.safetyNote?.trim()) {
    violations.push("potion requires a non-empty safetyNote");
  }

  if (violations.length > 0) {
    throw new InvalidGeneratedDraftError(`Generated ${type} failed validation: ${violations.join("; ")}.`);
  }
}

/**
 * Generates `count` drafts (default 1), round-robining across the eligible
 * (source, type) combinations so a batch call spreads across the curated
 * pool — e.g. bootstrapping toward the 365-entry launch target — rather
 * than repeating a single combination. Every draft is written with
 * status "draft" and a sourceId; nothing here ever auto-publishes.
 *
 * Historical lesson generation is source-grounded: the LLM receives the
 * source title, period, editorial reference, and a curated set of factual
 * claims. This prevents "ancient teaching" from becoming invented lineage
 * or generic modern occult material presented as history.
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
  if (request.similarityGroupId !== undefined && request.similarityGroupId.trim().length === 0) {
    throw new InvalidGenerationRequestError("similarityGroupId must be non-empty when provided.");
  }
  if (request.sourceCitation !== undefined && request.sourceCitation.trim().length === 0) {
    throw new InvalidGenerationRequestError("sourceCitation must be non-empty when provided.");
  }

  const pairs = buildEligiblePairs(sources, request.sourceId, request.type);
  const results: GeneratedDraftSummary[] = [];

  for (let i = 0; i < count; i++) {
    const pair = pairs[i % pairs.length];
    const generated = await generator.generateDraft({
      type: pair.type,
      tradition: pair.source.tradition,
      period: pair.source.period,
      sourceTitle: pair.source.title,
      sourceReference: pair.source.sourceReference,
      summary: pair.source.summary,
      groundedFacts: pair.source.groundedFacts,
    });
    validateGeneratedDraft(generated, pair.type);

    if (await repo.titleExists(generated.title)) {
      continue;
    }

    const sourceUrl = pair.source.sourceReference.match(/https?:\/\/\S+/)?.[0];
    const contentId = await repo.createDraft({
      ...generated,
      type: pair.type,
      tier: request.tier,
      tradition: pair.source.tradition,
      sourceId: pair.source.id,
      sourceCitation: request.sourceCitation ?? pair.source.sourceReference,
      rightsNotes: pair.source.publicDomainBasis,
      // AI output always requires explicit editorial/source-rights review.
      // approveDraft refuses publication while this remains true.
      rightsReviewRequired: true,
      ...(sourceUrl === undefined ? {} : { sourceUrl }),
      similarityGroupId: request.similarityGroupId?.trim() ??
        `generated:${pair.source.id}:${generated.title.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-")}`,
    });

    results.push({ contentId, sourceId: pair.source.id, type: pair.type, title: generated.title });
  }

  return results;
}
