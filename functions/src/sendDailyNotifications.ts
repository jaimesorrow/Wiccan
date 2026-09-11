import { ContentType } from "./types";
import { ContentRepositoryPort } from "./contentRepositoryPort";
import { selectTodayContent } from "./getTodayContent";
import { NotificationUserRepositoryPort } from "./notificationUserRepositoryPort";
import { FcmSenderPort } from "./fcmSenderPort";
import { getUserLocalDate } from "./dateUtils";

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
 * across every user - a user with several devices (phone, tablet, ...)
 * gets one push per device, each addressed to that device's own token.
 * Reuses selectTodayContent directly (the same selection logic
 * getTodayContent's callable wraps) rather than calling that callable over
 * HTTP — this function already runs with Admin SDK access, so there's no
 * reason to round-trip through the public surface. A user's daily content
 * selection itself is still made exactly once (selectTodayContent is
 * idempotent per uid+day), so multiple devices for the same user are told
 * about the same selection, not separate draws.
 *
 * Each device's "today" is computed from its own stored time zone via the
 * same getUserLocalDate used by the getTodayContent callable - never a
 * single shared batch date - so a notification always names the same
 * content that device's next app open will show, regardless of how its
 * local date compares to the server's, another user's, or even another of
 * the same user's own devices.
 *
 * One device's failure (empty content pool, transient FCM error) never
 * stops the batch; a permanently invalid token is cleared so future runs
 * skip it instead of retrying forever.
 */
export async function sendDailyNotifications(
  users: NotificationUserRepositoryPort,
  content: ContentRepositoryPort,
  fcm: FcmSenderPort,
): Promise<NotificationSendSummary> {
  const recipients = await users.listDevicesWithToken();
  const summary: NotificationSendSummary = {
    attempted: recipients.length,
    sent: 0,
    skippedStale: 0,
    failed: 0,
  };

  for (const device of recipients) {
    try {
      const today = getUserLocalDate(device.timeZone);
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
