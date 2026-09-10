import { Messaging } from "firebase-admin/messaging";
import { FcmSenderPort, FcmSendResult } from "./fcmSenderPort";

/** FCM error codes that mean the token is permanently dead, not a transient failure. */
const STALE_TOKEN_ERROR_CODES: ReadonlySet<string> = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

export class FirebaseFcmSender implements FcmSenderPort {
  constructor(private readonly messaging: Messaging) {}

  async send(token: string, title: string, body: string): Promise<FcmSendResult> {
    try {
      await this.messaging.send({
        token,
        notification: { title, body },
      });
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
