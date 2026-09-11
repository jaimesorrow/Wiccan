import { ContentAdminRepositoryPort, StoredContent } from "./contentAdminRepositoryPort";

export class DraftNotFoundError extends Error {}
export class UnsafeContentError extends Error {}

/**
 * Structural safety/completeness checks a draft must pass before it can
 * reach a real user - independent of (and a backstop for) whatever the
 * generation prompt asked the LLM to do. A prompt instruction is a
 * request, not a guarantee; this is the enforcement point.
 */
function findSafetyViolations(content: StoredContent): string[] {
  const violations: string[] = [];

  if (content.title.trim().length === 0) violations.push("title is empty.");
  if (content.intro.trim().length === 0) violations.push("intro is empty.");
  if (content.body.trim().length === 0) violations.push("body is empty.");
  if (content.tags.length === 0) violations.push("tags is empty.");

  // A potion mixes materials for the user to prepare or use directly, so
  // it always needs an explicit safety disclaimer - never left to the
  // LLM's discretion at the point content actually goes live.
  if (content.type === "potion" && (!content.safetyNote || content.safetyNote.trim().length === 0)) {
    violations.push("type 'potion' requires a non-empty safetyNote.");
  }

  return violations;
}

/**
 * draft -> published. Idempotent: approving an already-published item is
 * a no-op rather than an error, so a retried call is safe. Refuses to
 * publish a draft that fails the safety/completeness gate.
 */
export async function approveDraft(
  contentId: string,
  repo: ContentAdminRepositoryPort,
): Promise<void> {
  const content = await repo.getContent(contentId);
  if (content === null) {
    throw new DraftNotFoundError(`No content document '${contentId}' found.`);
  }
  if (content.status === "published") {
    return;
  }

  const violations = findSafetyViolations(content);
  if (violations.length > 0) {
    throw new UnsafeContentError(
      `Content '${contentId}' failed the pre-publish safety check: ${violations.join(" ")}`,
    );
  }

  await repo.publish(contentId);
}
