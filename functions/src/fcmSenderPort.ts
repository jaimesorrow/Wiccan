export type FcmSendResult =
  | { delivered: true }
  | { delivered: false; staleToken: boolean; error: string };

/**
 * Port between the notification job and FCM. Kept as an interface so the
 * send loop is unit tested against a fake instead of making real pushes.
 */
export interface FcmSenderPort {
  send(token: string, title: string, body: string): Promise<FcmSendResult>;
}
