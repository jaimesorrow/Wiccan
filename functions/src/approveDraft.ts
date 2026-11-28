import { findPracticalLessonViolations, findPracticalSafetyViolations, practicalContentHash } from "./practicalLesson";
import { loadSourcePassages, nonblank } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { ContentAdminRepositoryPort, StoredContent } from "./contentAdminRepositoryPort";
import type { PublicationRequest } from "./contentAdminRepositoryPort";
import { ContentSource, isLessonGenerationSource, loadContentSources } from "./contentSource";
import { CONTENT_TYPES, isContentType } from "./types";
import { originalIllustrationViolations } from "./originalIllustration";
import { screenGeneratedCandidate } from "./generationOriginality";
import type { GenerationInventoryRecord } from "./generationOriginality";

export class DraftNotFoundError extends Error {}
export class UnsafeContentError extends Error {}

/**
 * Structural safety/completeness checks a draft must pass before it can
 * reach a real user - independent of (and a backstop for) whatever the
 * generation prompt asked the LLM to do. A prompt instruction is a
 * request, not a guarantee; this is the enforcement point.
 */
function findSafetyViolations(content: StoredContent, sources: ContentSource[]): string[] {
  const violations: string[] = [];
  // The app can only render the supported types; anything else (e.g. "ritual",
  // which is a lessonType) must never go live as `type`.
  if (!isContentType(content.type)) {
    violations.push(`type '${String(content.type)}' is unsupported; use one of ${CONTENT_TYPES.join(", ")}.`);
  }

  if (!nonblank(content.title)) violations.push("title is empty.");
  if (!nonblank(content.intro)) violations.push("intro is empty.");
  if (!nonblank(content.body)) violations.push("body is empty.");
  if (!Array.isArray(content.tags) || content.tags.length === 0) violations.push("tags is empty.");
  if (content.createdAt == null) violations.push("createdAt is missing.");
  if (content.updatedAt == null) violations.push("updatedAt is missing.");
  if (content.rightsReviewRequired === true) {
    violations.push("rights review is outstanding; clear rights or rewrite the content before publishing.");
  }

  // A potion mixes materials for the user to prepare or use directly, so
  // it always needs an explicit safety disclaimer - never left to the
  // LLM's discretion at the point content actually goes live.
  if (content.type === "potion" && (!content.safetyNote || content.safetyNote.trim().length === 0)) {
    violations.push("type 'potion' requires a non-empty safetyNote.");
  }

  // Product requirement: every newly published Lesson of the Day must be
  // grounded in a curated pre-modern grimoire, not generic modern occult prose
  // presented as historical evidence. This is enforced again at publish time so
  // a manually created or tampered draft cannot bypass the source registry.
  if (content.type === "lesson") {
    if (!content.reflection?.trim().endsWith("?")) {
      violations.push("lesson requires a non-empty structured reflection question.");
    }
    const source = sources.find((candidate) => candidate.id === content.sourceId);
    if (!source) {
      violations.push(`lesson sourceId '${content.sourceId}' is not present in the curated source registry.`);
    } else {
      if (!isLessonGenerationSource(source)) {
        violations.push(`lesson source '${source.id}' is not an approved pre-modern grimoire.`);
      }
      if (!source.eligibleTypes.includes("lesson")) {
        violations.push(`source '${source.id}' is not approved for lesson generation.`);
      }
      if (source.groundedFacts.length === 0) {
        violations.push(`lesson source '${source.id}' has no grounded historical facts.`);
      }
    }
  }

  return violations;
}

export interface PublicationInventory {
  /** Exclude this draft when scanning the inventory for duplicates or reused images. */
  contentId?: string;
  records: GenerationInventoryRecord[];
}

function findDuplicatePublicationViolations(
  content: StoredContent,
  inventory?: PublicationInventory,
): string[] {
  if (!inventory) return [];
  const others = inventory.records.filter((record) => record.id !== inventory.contentId);
  const originality = screenGeneratedCandidate(content as unknown as Record<string, unknown>, others);
  if (originality.accepted) return [];
  const match = originality.closestMatch
    ? ` closest match '${originality.closestMatch.comparedId}'`
    : "";
  const reasons = originality.rejectionReasons.join(", ") || "duplicate of existing lesson, spell or potion";
  return [`duplicate content is not allowed${match} (${reasons}).`];
}

/** Returns every publication blocker without changing Firestore. */
export function validateDraftForPublication(
  content: StoredContent,
  sources?: ContentSource[],
  passages?: SourcePassage[],
  inventory?: PublicationInventory,
): string[] {
  const practicalViolations = content.type === "lesson" || content.sourcePassageId
    ? findPracticalLessonViolations(content, passages ?? loadSourcePassages())
    : findPracticalSafetyViolations(content);
  const applicableSources = content.type === "lesson" ? sources ?? loadContentSources() : [];
  return [
    ...findSafetyViolations(content, applicableSources),
    ...practicalViolations,
    ...originalIllustrationViolations(content, inventory?.records ?? [], inventory?.contentId),
    ...findDuplicatePublicationViolations(content, inventory),
  ];
}

export interface DraftApprovalPlanEntry {
  contentId: string;
  expectedUpdateTime: string;
  contentHash: string;
}

export async function planDraftApproval(
  contentId: string,
  repo: ContentAdminRepositoryPort,
  sources?: ContentSource[],
  passages?: SourcePassage[],
): Promise<DraftApprovalPlanEntry> {
  const versioned = await repo.getVersionedContent(contentId);
  if (versioned === null) throw new DraftNotFoundError(`No content document '${contentId}' found.`);
  const content = versioned.content;
  if (content.status !== "draft") throw new UnsafeContentError(`Content '${contentId}' is not a draft.`);
  const inventory: PublicationInventory = { contentId, records: await repo.listOriginalityInventory() };
  const violations = validateDraftForPublication(content, sources, passages, inventory);
  if (violations.length > 0) {
    throw new UnsafeContentError(`Content '${contentId}' failed the pre-publish safety check: ${violations.join(" ")}`);
  }
  return { contentId, expectedUpdateTime: versioned.updateTime, contentHash: practicalContentHash(content) };
}

/**
 * draft -> published. Idempotent: approving an already-published item is
 * a no-op rather than an error, so a retried call is safe. Refuses to
 * publish a draft that fails the safety/completeness and source-grounding
 * gates.
 */
export async function approveDraft(
  contentId: string,
  repo: ContentAdminRepositoryPort,
  approval: Pick<PublicationRequest, "operator" | "approvalDigest">,
  sources?: ContentSource[],
  passages?: SourcePassage[],
): Promise<void> {
  if (!approval.operator.trim() || !/^[a-f0-9]{64}$/.test(approval.approvalDigest)) {
    throw new Error("Explicit publication operator and approval digest are required.");
  }
  const content = await repo.getContent(contentId);
  if (content === null) {
    throw new DraftNotFoundError(`No content document '${contentId}' found.`);
  }
  if (content.status === "published") {
    return;
  }

  const inventory: PublicationInventory = { contentId, records: await repo.listOriginalityInventory() };
  const violations = validateDraftForPublication(content, sources, passages, inventory);
  if (violations.length > 0) {
    throw new UnsafeContentError(
      `Content '${contentId}' failed the pre-publish safety check: ${violations.join(" ")}`,
    );
  }

  const entry = await planDraftApproval(contentId, repo, sources, passages);
  const request: PublicationRequest = {
    contentId,
    expectedUpdateTime: entry.expectedUpdateTime,
    expectedContentHash: entry.contentHash,
    ...approval,
  };
  await repo.publishBatch([request]);
}
