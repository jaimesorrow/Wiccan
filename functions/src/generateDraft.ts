import { citationForPassage, findPassageViolations, loadSourcePassages } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { findPracticalLessonViolations } from "./practicalLesson";
import type { NewDraftContent } from "./contentAdminRepositoryPort";
import { ContentSource } from "./contentSource";
import { ContentTier, ContentType } from "./types";
import { DraftGenerationPort } from "./draftGenerationPort";
import { ContentAdminRepositoryPort } from "./contentAdminRepositoryPort";

export interface GenerateDraftRequest {
  tier: ContentTier;
  /** Restrict to one source; omit to draw from the whole curated pool. */
  sourceId?: string;
  /** Required approved passage identity, selected before any model call. */
  passageId?: string;
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
  passages: SourcePassage[] = loadSourcePassages(),
): Promise<GeneratedDraftSummary[]> {
  const count = request.count ?? 1;
  if (!Number.isInteger(count) || count < 1 || count > 100) {
    throw new InvalidGenerationRequestError("count must be an integer between 1 and 100.");
  }
  if (request.similarityGroupId !== undefined && request.similarityGroupId.trim().length === 0) {
    throw new InvalidGenerationRequestError("similarityGroupId must be non-empty when provided.");
  }
  if (request.sourceCitation !== undefined && request.sourceCitation.trim().length === 0) {
    throw new InvalidGenerationRequestError("sourceCitation must be non-empty when provided.");
  }

  if (!request.passageId) throw new InvalidGenerationRequestError("Select a verified passageId before generation.");
  const passage = passages.find((candidate) => candidate.id === request.passageId);
  if (!passage || findPassageViolations(passage).length) {
    throw new InvalidGenerationRequestError("Passage requires ancient dating, rights, scope, fidelity and safety verification before generation.");
  }
  if (request.sourceId && request.sourceId !== passage.sourceId) throw new InvalidGenerationRequestError("Source and passage identities disagree.");
  if (request.sourceCitation && request.sourceCitation !== passage.locator) throw new InvalidGenerationRequestError("Citation must match the inspected passage locator.");
  const pairs = buildEligiblePairs(sources, passage.sourceId, request.type);
  const results: GeneratedDraftSummary[] = [];

  for (let i = 0; i < count; i++) {
    const pair = pairs[i % pairs.length];
    const generated = await generator.generateDraft({
      passage,
      type: pair.type,
      tradition: pair.source.tradition,
      period: pair.source.period,
      sourceTitle: pair.source.title,
      sourceReference: pair.source.sourceReference,
      summary: pair.source.summary,
      groundedFacts: pair.source.groundedFacts,
    });

    if (await repo.titleExists(generated.title)) {
      continue;
    }

    const draft: NewDraftContent = {
      ...generated,
      type: pair.type,
      tier: request.tier,
      tradition: passage.tradition!,
      sourceId: passage.sourceId,
      sourcePassageId: passage.id,
      sourceCitation: passage.locator,
      sourceUrl: passage.url,
      citations: [citationForPassage(passage)],
      rightsNotes: passage.rights!.basis,
      rightsReviewRequired: true,
      introduction: generated.intro,
      lesson: generated.body,
      contentStatus: "draft",
      slug: generated.title.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""),
      tools: generated.tools ?? generated.materials ?? [],
      similarityGroupId: request.similarityGroupId?.trim() ?? `generated:${passage.id}:${generated.title.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-")}`,
    };
    // The model cannot certify its own output, even if an adapter returns extra keys.
    delete draft.practicalReview;
    // Validate the exact stored shape; the repository adds the authoritative legacy status atomically.
    const violations = findPracticalLessonViolations({ ...draft, status: "draft" }, passages, false);
    if (violations.length) throw new InvalidGeneratedDraftError(violations.join("; "));
    const contentId = await repo.createDraft(draft);

    results.push({ contentId, sourceId: pair.source.id, type: pair.type, title: generated.title });
  }

  return results;
}
