import { Messaging } from "firebase-admin/messaging";
import { FcmSenderPort, FcmSendOptions, FcmSendResult } from "./fcmSenderPort";
import { DAILY_PRACTICE_CHANNEL_ID } from "./sendDailyNotifications";

/** FCM error codes that mean the token is permanently dead, not a transient failure. */
const STALE_TOKEN_ERROR_CODES: ReadonlySet<string> = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

export class FirebaseFcmSender implements FcmSenderPort {
  constructor(private readonly messaging: Messaging) {}

  async send(
    token: string, title: string, body: string, channelId?: string, options?: FcmSendOptions,
  ): Promise<FcmSendResult> {
    const channel = channelId ?? DAILY_PRACTICE_CHANNEL_ID;
    try {
      if (options?.dataOnly) {
        // The app builds the notification itself (WiccanMessagingService), so
        // it can check who is signed in first. High priority so Doze does not
        // hold back a reminder that ends in a visible notification.
        await this.messaging.send({
          token,
          data: { ...(options.data ?? {}), title, body, channelId: channel },
          android: { priority: "high" },
        });
      } else {
        await this.messaging.send({
          token,
          notification: { title, body },
          android: { notification: { channelId: channel } },
          ...(options?.data ? { data: options.data } : {}),
        });
      }
      return { delivered: true };
    } catch (err) {
      const code = (err as { code?: string }).code ?? "unknown";
      return {
        delivered: false,
        staleToken: STALE_TOKEN_ERROR_CODES.has(code),
        error: (err as Error).message,
      };
    }
  }
}
