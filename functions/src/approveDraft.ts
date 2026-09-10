import { ContentAdminRepositoryPort } from "./contentAdminRepositoryPort";

export class DraftNotFoundError extends Error {}

/**
 * draft -> published. Idempotent: approving an already-published item is
 * a no-op rather than an error, so a retried call is safe.
 */
export async function approveDraft(
  contentId: string,
  repo: ContentAdminRepositoryPort,
): Promise<void> {
  const status = await repo.getStatus(contentId);
  if (status === null) {
    throw new DraftNotFoundError(`No content document '${contentId}' found.`);
  }
  if (status === "published") {
    return;
  }
  await repo.publish(contentId);
}
