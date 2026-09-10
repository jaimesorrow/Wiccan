import { ContentTier } from "./types";
import { PlaySubscriptionState } from "./playBillingPort";

/** Grace period still grants access while Google retries a failed renewal. */
const ACTIVE_STATES: ReadonlySet<PlaySubscriptionState> = new Set(["active", "in_grace_period"]);

export function tierForSubscriptionState(state: PlaySubscriptionState): ContentTier {
  return ACTIVE_STATES.has(state) ? "premium" : "free";
}
