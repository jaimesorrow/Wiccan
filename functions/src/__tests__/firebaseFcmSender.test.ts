import { Messaging } from "firebase-admin/messaging";
import { FirebaseFcmSender } from "../firebaseFcmSender";

function fakeMessaging() {
  const sent: unknown[] = [];
  const messaging = { send: jest.fn(async (message: unknown) => { sent.push(message); return "id"; }) };
  return { sent, messaging: messaging as unknown as Messaging };
}

describe("FirebaseFcmSender", () => {
  it("sends the daily push as a notification message on the daily channel", async () => {
    const { sent, messaging } = fakeMessaging();
    await new FirebaseFcmSender(messaging).send("token", "Title", "Body");
    expect(sent).toEqual([{
      token: "token",
      notification: { title: "Title", body: "Body" },
      android: { notification: { channelId: "daily_practice" } },
    }]);
  });

  it("sends a data-only billing reminder at high priority so the app decides whether to show it", async () => {
    const { sent, messaging } = fakeMessaging();
    await new FirebaseFcmSender(messaging).send("token", "Your free trial ends soon", "Body", "billing_reminders", {
      dataOnly: true,
      data: { type: "billing_reminder", uid: "user-1" },
    });
    expect(sent).toEqual([{
      token: "token",
      data: {
        type: "billing_reminder",
        uid: "user-1",
        title: "Your free trial ends soon",
        body: "Body",
        channelId: "billing_reminders",
      },
      android: { priority: "high" },
    }]);
    expect(sent[0]).not.toHaveProperty("notification");
  });

  it("reports a dead token as stale", async () => {
    const messaging = {
      send: jest.fn(async () => {
        throw Object.assign(new Error("gone"), { code: "messaging/registration-token-not-registered" });
      }),
    } as unknown as Messaging;
    expect(await new FirebaseFcmSender(messaging).send("token", "t", "b"))
      .toEqual({ delivered: false, staleToken: true, error: "gone" });
  });
});
