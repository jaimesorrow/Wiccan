import { FcmSenderPort, FcmSendOptions, FcmSendResult } from "../fcmSenderPort";

export class FakeFcmSenderPort implements FcmSenderPort {
  sentTo: { token: string; title: string; body: string; channelId?: string; options?: FcmSendOptions }[] = [];
  private readonly results = new Map<string, FcmSendResult>();

  respondWith(token: string, result: FcmSendResult): void {
    this.results.set(token, result);
  }

  async send(
    token: string, title: string, body: string, channelId?: string, options?: FcmSendOptions,
  ): Promise<FcmSendResult> {
    const entry: { token: string; title: string; body: string; channelId?: string; options?: FcmSendOptions } =
      { token, title, body };
    if (channelId !== undefined) entry.channelId = channelId;
    if (options !== undefined) entry.options = options;
    this.sentTo.push(entry);
    return this.results.get(token) ?? { delivered: true };
  }
}
