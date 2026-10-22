import { findPracticalLessonViolations } from "./practicalLesson";
import { loadSourcePassages } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { ContentAdminRepositoryPort, StoredContent } from "./contentAdminRepositoryPort";
import { ContentSource, loadContentSources } from "./contentSource";

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

  if (content.title.trim().length === 0) violations.push("title is empty.");
  if (content.intro.trim().length === 0) violations.push("intro is empty.");
  if (content.body.trim().length === 0) violations.push("body is empty.");
  if (content.tags.length === 0) violations.push("tags is empty.");
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
  // grounded in a curated ancient source, not generic modern occult prose
  // presented as ancient history. This is enforced again at publish time so
  // a manually created or tampered draft cannot bypass the source registry.
  if (content.type === "lesson") {
    if (!content.reflection?.trim().endsWith("?")) {
      violations.push("lesson requires a non-empty structured reflection question.");
    }
    const source = sources.find((candidate) => candidate.id === content.sourceId);
    if (!source) {
      violations.push(`lesson sourceId '${content.sourceId}' is not present in the curated source registry.`);
    } else {
      if (source.era !== "ancient") {
        violations.push(`lesson source '${source.id}' is not marked ancient.`);
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

/**
 * draft -> published. Idempotent: approving an already-published item is
 * a no-op rather than an error, so a retried call is safe. Refuses to
 * publish a draft that fails the safety/completeness and source-grounding
 * gates.
 */
export async function approveDraft(
  contentId: string,
  repo: ContentAdminRepositoryPort,
  sources: ContentSource[] = loadContentSources(),
  passages: SourcePassage[] = loadSourcePassages(),
): Promise<void> {
  const content = await repo.getContent(contentId);
  if (content === null) {
    throw new DraftNotFoundError(`No content document '${contentId}' found.`);
  }
  if (content.status === "published") {
    return;
  }

  const practicalViolations = content.type === "lesson" || content.sourcePassageId
    ? findPracticalLessonViolations(content, passages)
    : [];
  const violations = [...findSafetyViolations(content, sources), ...practicalViolations];
  if (violations.length > 0) {
    throw new UnsafeContentError(
      `Content '${contentId}' failed the pre-publish safety check: ${violations.join(" ")}`,
    );
  }

  await repo.publish(contentId);
}

