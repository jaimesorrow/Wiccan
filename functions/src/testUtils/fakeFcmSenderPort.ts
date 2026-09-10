import { FcmSenderPort, FcmSendResult } from "../fcmSenderPort";

export class FakeFcmSenderPort implements FcmSenderPort {
  sentTo: { token: string; title: string; body: string }[] = [];
  private readonly results = new Map<string, FcmSendResult>();

  respondWith(token: string, result: FcmSendResult): void {
    this.results.set(token, result);
  }

  async send(token: string, title: string, body: string): Promise<FcmSendResult> {
    this.sentTo.push({ token, title, body });
    return this.results.get(token) ?? { delivered: true };
  }
}
