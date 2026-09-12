import { ContentType } from "./types";
import { ContentRepositoryPort } from "./contentRepositoryPort";
import { selectTodayContent } from "./getTodayContent";
import { NotificationUserRepositoryPort } from "./notificationUserRepositoryPort";
import { FcmSenderPort } from "./fcmSenderPort";
import { getUserLocalDate, getUserLocalHour } from "./dateUtils";

/** The local hour every device should get its "your content is ready" push. */
export const TARGET_LOCAL_HOUR = 9;

export interface NotificationSendSummary {
  attempted: number;
  sent: number;
  skippedStale: number;
  failed: number;
}

function titleForType(type: ContentType): string {
  switch (type) {
    case "spell":
      return "Your spell is ready";
    case "potion":
      return "Your potion is ready";
    case "lesson":
      return "Your lesson is ready";
  }
}

/**
 * Sends the daily "your content is ready" push to every registered device
 * whose *local* time is currently in its 9am hour - a user with several
 * devices (phone, tablet, ...) gets one push per device, each addressed to
 * that device's own token, each at that device's own local 9am rather than
 * one instant shared across every time zone on earth. Intended to be
 * called from an hourly scheduled job (see sendDailyNotificationsJob):
 * each run only ever matches the slice of devices currently in their 9am
 * window, so calling it more or less often than hourly would either miss
 * or double-count devices at the boundary.
 *
 * Reuses selectTodayContent directly (the same selection logic
 * getTodayContent's callable wraps) rather than calling that callable over
 * HTTP — this function already runs with Admin SDK access, so there's no
 * reason to round-trip through the public surface. A user's daily content
 * selection itself is still made exactly once (selectTodayContent is
 * idempotent per uid+day), so multiple devices for the same user are told
 * about the same selection, not separate draws.
 *
 * One device's failure (empty content pool, transient FCM error) never
 * stops the batch; a permanently invalid token is cleared so future runs
 * skip it instead of retrying forever.
 *
 * Known limitation: this is an hourly poll, not a per-device scheduled
 * dispatch - if the hourly job's actual invocation time drifts across an
 * hour boundary (scheduler jitter, a retry), a device could in principle
 * be matched twice in one day or missed entirely. A precise fix would mean
 * tracking a per-device "already notified today" marker or moving to
 * per-device scheduled dispatch (e.g. Cloud Tasks) - not done here.
 */
export async function sendDailyNotifications(
  users: NotificationUserRepositoryPort,
  content: ContentRepositoryPort,
  fcm: FcmSenderPort,
  now: Date = new Date(),
): Promise<NotificationSendSummary> {
  const allDevices = await users.listDevicesWithToken();
  const recipients = allDevices.filter((device) => getUserLocalHour(device.timeZone, now) === TARGET_LOCAL_HOUR);
  const summary: NotificationSendSummary = {
    attempted: recipients.length,
    sent: 0,
    skippedStale: 0,
    failed: 0,
  };

  for (const device of recipients) {
    try {
      const today = getUserLocalDate(device.timeZone, now);
      const selected = await selectTodayContent(device.uid, today, content);
      const result = await fcm.send(device.token, titleForType(selected.type), selected.title);
      if (result.delivered) {
        summary.sent++;
      } else if (result.staleToken) {
        await users.clearToken(device.uid, device.token);
        summary.skippedStale++;
      } else {
        summary.failed++;
      }
    } catch {
      summary.failed++;
    }
  }

  return summary;
}
