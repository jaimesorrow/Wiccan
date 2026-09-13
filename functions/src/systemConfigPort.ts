export interface SystemConfig {
  /** Emergency stop for daily content selection - getTodayContent and the notification job both respect it. */
  dailyContentEnabled: boolean;
  /** Emergency stop for the daily notification job specifically, independent of content selection itself. */
  notificationsEnabled: boolean;
}

/**
 * Port to the single systemConfig/global doc - ops-only runtime flags an
 * operator can flip in an emergency (e.g. a bad content generation batch,
 * an FCM incident) without a redeploy. Both flags default to enabled
 * (fail-open) when the doc or field doesn't exist, so a fresh environment
 * with no config doc behaves exactly like normal operation - this is an
 * opt-in "turn something off," never an opt-in allowlist.
 */
export interface SystemConfigPort {
  getConfig(): Promise<SystemConfig>;
}
