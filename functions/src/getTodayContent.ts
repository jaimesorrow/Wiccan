import { Content } from "./types";
import { ContentRepositoryPort } from "./contentRepositoryPort";
import { loadContentSources } from "./contentSource";

const DEFAULT_ANCIENT_LESSON_SOURCE_IDS = new Set(
  loadContentSources()
    .filter((source) => source.era === "ancient" && source.eligibleTypes.includes("lesson"))
    .map((source) => source.id),
);

function pickRandom(items: Content[]): Content {
  return items[Math.floor(Math.random() * items.length)];
}

function isEligibleForDailyDelivery(content: Content, ancientLessonSourceIds: ReadonlySet<string>): boolean {
  if (content.type !== "lesson") return true;
  return typeof content.sourceId === "string" && ancientLessonSourceIds.has(content.sourceId);
}

/**
 * Core daily-selection logic, per the Wiccan content-engine spec:
 *
 * 1. Fast path: if this user already has a history entry for `today`,
 *    return that content (idempotent — repeated calls the same day agree).
 *    This is a plain read purely to skip the work below; it is never
 *    relied on for correctness.
 * 2. Otherwise, gather published content within the user's tier and drop
 *    any lesson that is not traceable to a curated ancient source.
 * 3. Exclude anything already delivered to the user.
 * 4. If nothing remains (pool exhausted), fall back to the full eligible
 *    published pool and log the exhaustion event for content-pool monitoring.
 * 5. Pick one candidate at random.
 * 6. Atomically commit that candidate as today's assignment via
 *    `assignDailyContent`. If a concurrent call already won today's
 *    assignment first, the candidate this call picked is discarded and
 *    the winning content is fetched and returned instead - so two
 *    truly simultaneous first-call-of-the-day requests always converge
 *    on the same single committed assignment, with exactly one history
 *    record created.
 * 7. Return the selected content.
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
  const existingContentId = await repo.findTodayDeliveredContentId(uid, today);
  if (existingContentId) {
    const existing = await repo.getContentById(existingContentId);
    if (existing) {
      return existing;
    }
    // History points at a content doc that no longer exists; fall through
    // and select fresh rather than fail the whole call.
  }

  const tier = await repo.getUserTier(uid);
  const [deliveredIds, published] = await Promise.all([
    repo.getDeliveredContentIds(uid),
    repo.getPublishedContentForTier(tier),
  ]);

  const eligiblePublished = published.filter((content) =>
    isEligibleForDailyDelivery(content, ancientLessonSourceIds),
  );
  const deliveredSet = new Set(deliveredIds);
  let candidates = eligiblePublished.filter((content) => !deliveredSet.has(content.id));

  if (candidates.length === 0) {
    await repo.logPoolExhaustion(uid, tier, today);
    candidates = eligiblePublished;
  }

  if (candidates.length === 0) {
    throw new Error(`No published content available for tier '${tier}'.`);
  }

  const candidate = pickRandom(candidates);
  const assignedId = await repo.assignDailyContent(uid, today, candidate);

  if (assignedId === candidate.id) {
    return candidate;
  }

  // A concurrent call already committed a different assignment first;
  // defer to it so both callers agree on the same single assignment.
  const winner = await repo.getContentById(assignedId);
  if (!winner) {
    throw new Error(`Assigned content '${assignedId}' could not be found.`);
  }
  return winner;
}
