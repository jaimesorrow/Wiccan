import { Content } from "./types";
import { ContentRepositoryPort } from "./contentRepositoryPort";

function pickRandom(items: Content[]): Content {
  return items[Math.floor(Math.random() * items.length)];
}

/**
 * Core daily-selection logic, per the Wiccan content-engine spec:
 *
 * 1. Fast path: if this user already has a history entry for `today`,
 *    return that content (idempotent — repeated calls the same day agree).
 *    This is a plain read purely to skip the work below; it is never
 *    relied on for correctness.
 * 2. Otherwise, gather published content within the user's tier, minus
 *    anything already delivered to them.
 * 3. If nothing remains (pool exhausted), fall back to the full published
 *    pool and log the exhaustion event for content-pool monitoring.
 * 4. Pick one candidate at random.
 * 5. Atomically commit that candidate as today's assignment via
 *    `assignDailyContent`. If a concurrent call already won today's
 *    assignment first, the candidate this call picked is discarded and
 *    the winning content is fetched and returned instead - so two
 *    truly simultaneous first-call-of-the-day requests always converge
 *    on the same single committed assignment, with exactly one history
 *    record created.
 * 6. Return the selected content.
 */
export async function selectTodayContent(
  uid: string,
  today: string,
  repo: ContentRepositoryPort,
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

  const deliveredSet = new Set(deliveredIds);
  let candidates = published.filter((content) => !deliveredSet.has(content.id));

  if (candidates.length === 0) {
    await repo.logPoolExhaustion(uid, tier, today);
    candidates = published;
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
  // defer to it so both callers agree on the same content.
  const winner = await repo.getContentById(assignedId);
  if (!winner) {
    throw new Error(`Assigned content '${assignedId}' could not be found.`);
  }
  return winner;
}
