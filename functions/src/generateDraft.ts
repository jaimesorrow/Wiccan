import { citationForPassage, findPassageViolations, loadSourcePassages } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { findPracticalLessonViolations } from "./practicalLesson";
import type { NewDraftContent } from "./contentAdminRepositoryPort";
import { ContentSource, isLessonGenerationSource } from "./contentSource";
import { ContentTier, ContentType } from "./types";
import { DraftGenerationPort } from "./draftGenerationPort";
import { ContentAdminRepositoryPort } from "./contentAdminRepositoryPort";
import {
  generationConceptSignature,
  screenGeneratedCandidate,
} from "./generationOriginality";
import type { GenerationOriginalityReport } from "./generationOriginality";

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
  originality: GenerationOriginalityReport;
}

export interface RejectedGeneratedDraft {
  title: string;
  status: "rejected-duplicate";
  reasons: string[];
  closestMatch: GenerationOriginalityReport["closestMatch"];
}

export interface GeneratedDraftBatchResult {
  accepted: GeneratedDraftSummary[];
  rejected: RejectedGeneratedDraft[];
  attempts: number;
  exhausted: boolean;
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

  const grimoireSources = candidateSources.filter(isLessonGenerationSource);
  if (sourceId && grimoireSources.length === 0) {
    throw new InvalidGenerationRequestError(
      `Source '${sourceId}' is not an approved pre-modern grimoire and cannot be used for generation.`,
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
 * Every candidate is screened against the complete content inventory and
 * candidates accepted earlier in this batch. Title, slug, authored-content,
 * and concept hashes are then reserved atomically with the draft write so a
 * concurrent generator cannot commit an equivalent candidate.
 */
export async function generateDraftBatch(
  request: GenerateDraftRequest,
  sources: ContentSource[],
  generator: DraftGenerationPort,
  repo: ContentAdminRepositoryPort,
  passages: SourcePassage[] = loadSourcePassages(),
): Promise<GeneratedDraftBatchResult> {
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
    throw new InvalidGenerationRequestError("Passage requires pre-modern dating, rights, scope, fidelity and safety verification before generation.");
  }
  if (request.sourceId && request.sourceId !== passage.sourceId) throw new InvalidGenerationRequestError("Source and passage identities disagree.");
  if (request.sourceCitation && request.sourceCitation !== passage.locator) throw new InvalidGenerationRequestError("Citation must match the inspected passage locator.");
  const pairs = buildEligiblePairs(sources, passage.sourceId, request.type);
  const accepted: GeneratedDraftSummary[] = [];
  const rejected: RejectedGeneratedDraft[] = [];
  const inventory = await repo.listOriginalityInventory();
  const maximumAttempts = Math.max(count * 5, count);
  let attempts = 0;

  while (accepted.length < count && attempts < maximumAttempts) {
    const pair = pairs[attempts % pairs.length];
    attempts += 1;
    const relevantExcludedConcepts = inventory
      .filter((record) => {
        const fields = record.fields;
        return fields.sourcePassageId === passage.id || fields.lessonType === request.type || fields.type === pair.type;
      })
      .slice(-50)
      .map((record) => {
        const signature = generationConceptSignature(record.fields);
        return {
          id: record.id,
          title: typeof record.fields.title === "string" ? record.fields.title : "",
          primaryMethod: signature.primaryMethod,
          intendedPurpose: signature.intendedPurpose,
          ritualActions: signature.ritualActions,
          followUpAction: signature.followUpAction,
        };
      });
    const generated = await generator.generateDraft({
      passage,
      type: pair.type,
      tradition: pair.source.tradition,
      period: pair.source.period,
      sourceTitle: pair.source.title,
      sourceReference: pair.source.sourceReference,
      summary: pair.source.summary,
      groundedFacts: pair.source.groundedFacts,
      excludedConcepts: relevantExcludedConcepts,
    });

    const draft: NewDraftContent = {
      ...generated,
      type: pair.type,
      tier: request.tier,
      tradition: "Wiccan / Modern Witchcraft",
      sourceTradition: passage.tradition!,
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
    const originality = screenGeneratedCandidate(draft as unknown as Record<string, unknown>, inventory);
    if (!originality.accepted) {
      rejected.push({
        title: generated.title,
        status: "rejected-duplicate",
        reasons: originality.rejectionReasons,
        closestMatch: originality.closestMatch,
      });
      continue;
    }
    // Validate the exact stored shape; the repository adds the authoritative legacy status atomically.
    const violations = findPracticalLessonViolations({ ...draft, status: "draft" }, passages, false);
    if (violations.length) throw new InvalidGeneratedDraftError(violations.join("; "));
    let contentId: string;
    try {
      contentId = await repo.createDraftWithOriginalityReservations(draft, originality.reservationKeys);
    } catch (error) {
      if (!(error instanceof Error) || !error.message.startsWith("Originality reservation collision:")) throw error;
      rejected.push({
        title: generated.title,
        status: "rejected-duplicate",
        reasons: [error.message],
        closestMatch: originality.closestMatch,
      });
      continue;
    }

    inventory.push({ id: contentId, fields: { ...draft, status: "draft" } });
    accepted.push({ contentId, sourceId: pair.source.id, type: pair.type, title: generated.title, originality });
  }

  return { accepted, rejected, attempts, exhausted: accepted.length < count };
}

export async function generateDrafts(
  request: GenerateDraftRequest,
  sources: ContentSource[],
  generator: DraftGenerationPort,
  repo: ContentAdminRepositoryPort,
  passages: SourcePassage[] = loadSourcePassages(),
): Promise<GeneratedDraftSummary[]> {
  return (await generateDraftBatch(request, sources, generator, repo, passages)).accepted;
}
