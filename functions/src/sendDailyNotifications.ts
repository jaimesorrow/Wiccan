import { ContentType } from "./types";
import { ContentRepositoryPort } from "./contentRepositoryPort";
import { selectTodayContent } from "./getTodayContent";
import { NotificationUserRepositoryPort } from "./notificationUserRepositoryPort";
import { FcmSenderPort } from "./fcmSenderPort";

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
 * Sends the daily "your content is ready" push to every user with a
 * registered FCM token. Reuses selectTodayContent directly (the same
 * selection logic getTodayContent's callable wraps) rather than calling
 * that callable over HTTP — this function already runs with Admin SDK
 * access, so there's no reason to round-trip through the public surface.
 *
 * One user's failure (empty content pool, transient FCM error) never
 * stops the batch; a permanently invalid token is cleared so future runs
 * skip it instead of retrying forever.
 */
export async function sendDailyNotifications(
  today: string,
  users: NotificationUserRepositoryPort,
  content: ContentRepositoryPort,
  fcm: FcmSenderPort,
): Promise<NotificationSendSummary> {
  const recipients = await users.listUsersWithToken();
  const summary: NotificationSendSummary = {
    attempted: recipients.length,
    sent: 0,
    skippedStale: 0,
    failed: 0,
  };

  for (const user of recipients) {
    try {
      const selected = await selectTodayContent(user.uid, today, content);
      const result = await fcm.send(user.fcmToken, titleForType(selected.type), selected.title);
      if (result.delivered) {
        summary.sent++;
      } else if (result.staleToken) {
        await users.clearToken(user.uid);
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
