import { Content } from "./types";
import { ContentAlreadyDeliveredError, ContentNoLongerEligibleError, ContentRepositoryPort, SubscriptionRequiredError } from "./contentRepositoryPort";
import { loadContentSources } from "./contentSource";
import { contentSimilarityKey, isEligibleForDailyDelivery } from "./contentEligibility";

const DEFAULT_ANCIENT_LESSON_SOURCE_IDS = new Set(
  loadContentSources()
    .filter((source) =>
      source.era === "ancient" && source.sourceKind === "ancient-grimoire" && source.eligibleTypes.includes("lesson"),
    )
    .map((source) => source.id),
);

export class ContentPoolExhaustedError extends Error {
  constructor(tier: string) {
    super(`No new published content available for tier '${tier}'.`);
    this.name = "ContentPoolExhaustedError";
  }
}

function pickRandom(items: Content[]): Content {
  return items[Math.floor(Math.random() * items.length)];
}

/** Preserve the committed assignment if its underlying document was removed. */
export class DailyAssignmentUnavailableError extends Error {
  constructor(contentId: string) {
    super(`Assigned content '${contentId}' could not be found.`);
    this.name = "DailyAssignmentUnavailableError";
  }
}

async function readAssignedContent(uid: string, contentId: string, repo: ContentRepositoryPort): Promise<Content> {
  const content = await repo.getContentById(contentId);
  if (await repo.getUserTier(uid) !== "premium") throw new SubscriptionRequiredError();
  if (!content) throw new DailyAssignmentUnavailableError(contentId);
  return content;
}

/**
 * Core daily-selection logic, per the Wiccan content-engine spec:
 *
 * 1. Verify active, unexpired subscription access before any history read.
 *    Fast path: if this user already has a history entry for `today`,
 *    return that content (idempotent — repeated calls the same day agree).
 *    This is a plain read purely to skip the work below; it is never
 *    relied on for correctness.
 * 2. Otherwise, gather the single published subscriber content pool and drop
 *    any lesson that is not traceable to a curated ancient source.
 * 3. Exclude anything already delivered to the user.
 * 4. If nothing remains, log pool exhaustion and fail closed. Never repeat
 *    a lesson already delivered to this user.
 * 5. Pick a candidate at random and atomically reserve both its content
 *    ID and today's assignment. If another date reserved that ID first,
 *    retry with a different candidate. If another call won today, return
 *    that same-day winner. Never deliver the same ID on two days.
 * 6. Return the selected content.
 *
 * An already-committed history row is intentionally honored even if the
 * referenced lesson predates the ancient-source policy; changing it during
 * the same day would violate the per-user-per-day idempotency guarantee.
 */
export async function selectTodayContent(
  uid: string,
  today: string,
  repo: ContentRepositoryPort,
  ancientLessonSourceIds: ReadonlySet<string> = DEFAULT_ANCIENT_LESSON_SOURCE_IDS,
): Promise<Content> {
  const tier = await repo.getUserTier(uid);
  if (tier !== "premium") throw new SubscriptionRequiredError();
  const existingContentId = await repo.findTodayDeliveredContentId(uid, today);
  if (existingContentId) {
    return readAssignedContent(uid, existingContentId, repo);
  }

  const [deliveredIds, published] = await Promise.all([
    repo.getDeliveredContentIds(uid),
    repo.getPublishedContentForTier(tier),
  ]);

  const eligiblePublished = published.filter((content) =>
    isEligibleForDailyDelivery(content, ancientLessonSourceIds),
  );
  const deliveredSet = new Set(deliveredIds);
  const deliveredGroups = new Set(published.filter((content) => deliveredSet.has(content.id))
    .map(contentSimilarityKey).filter((key): key is string => key !== null));
  let candidates = eligiblePublished.filter((content) => {
    const group = contentSimilarityKey(content);
    return !deliveredSet.has(content.id) && (!group || !deliveredGroups.has(group));
  });

  while (candidates.length > 0) {
    const candidate = pickRandom(candidates);
    let assignedId: string;
    try {
      assignedId = await repo.assignDailyContent(uid, today, candidate, ancientLessonSourceIds);
    } catch (err) {
      if (!(err instanceof ContentAlreadyDeliveredError) && !(err instanceof ContentNoLongerEligibleError)) throw err;
      candidates = candidates.filter((content) => content.id !== candidate.id);
      continue;
    }

    // Read the authoritative document again: publication, entitlement,
    // title, and editorial grouping may have changed since pool lookup.
    return readAssignedContent(uid, assignedId, repo);
  }

  // A concurrent caller may have committed today while we ran out of
  // candidates. Honor that assignment rather than report false exhaustion.
  const committedId = await repo.findTodayDeliveredContentId(uid, today);
  if (committedId) return readAssignedContent(uid, committedId, repo);
  await repo.logPoolExhaustion(uid, tier, today);
  throw new ContentPoolExhaustedError(tier);
}
