export type FcmSendResult =
  | { delivered: true }
  | { delivered: false; staleToken: boolean; error: string };

/**
 * Port between the notification job and FCM. Kept as an interface so the
 * send loop is unit tested against a fake instead of making real pushes.
 */
/**
 * Extra delivery options. `dataOnly` sends a data message (no `notification`
 * block) at high priority, so the app itself decides whether to show it -
 * even in the background - and can refuse after sign-out or for another
 * account. `data` is copied into the message's data payload.
 */
export interface FcmSendOptions {
  dataOnly?: boolean;
  data?: Record<string, string>;
}

export interface FcmSenderPort {
  /** `channelId` defaults to the daily-practice channel when omitted. */
  send(token: string, title: string, body: string, channelId?: string, options?: FcmSendOptions): Promise<FcmSendResult>;
}
