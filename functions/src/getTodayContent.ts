import { Content } from "./types";
import { ContentRepositoryPort } from "./contentRepositoryPort";

function pickRandom(items: Content[]): Content {
  return items[Math.floor(Math.random() * items.length)];
}

/**
 * Core daily-selection logic, per the Nyxwell content-engine spec:
 *
 * 1. If this user already has a history entry for `today`, return that
 *    content (idempotent — repeated calls the same day agree).
 * 2. Otherwise, gather published content within the user's tier, minus
 *    anything already delivered to them.
 * 3. If nothing remains (pool exhausted), fall back to the full published
 *    pool and log the exhaustion event for content-pool monitoring.
 * 4. Pick one candidate at random.
 * 5. Record the delivery.
 * 6. Return the selected content.
 *
 * Note: steps 1 and 4-5 are not wrapped in a Firestore transaction, so two
 * truly simultaneous first-call-of-the-day requests from the same user
 * could each pick and record a delivery. That race is an accepted,
 * documented limitation of this foundation pass rather than something
 * this function guards against.
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

  const selected = pickRandom(candidates);
  await repo.recordDelivery(uid, selected.id, today);
  return selected;
}
