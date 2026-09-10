import { sendDailyNotifications } from "../sendDailyNotifications";
import { FakeContentRepository, makeContent } from "../testUtils/fakeContentRepository";
import { FakeNotificationUserRepository } from "../testUtils/fakeNotificationUserRepository";
import { FakeFcmSenderPort } from "../testUtils/fakeFcmSenderPort";

const TODAY = "2026-09-10";

describe("sendDailyNotifications", () => {
  it("sends a push to every user with a token, titled by content type", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1", type: "spell", title: "Candle of Clear Intent" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "token-1");
    users.setToken("user-2", "token-2");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(TODAY, users, content, fcm);

    expect(summary).toEqual({ attempted: 2, sent: 2, skippedStale: 0, failed: 0 });
    expect(fcm.sentTo).toHaveLength(2);
    expect(fcm.sentTo[0]).toEqual({
      token: "token-1",
      title: "Your spell is ready",
      body: "Candle of Clear Intent",
    });
  });

  it("clears a permanently stale token and counts it as skipped, not failed", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "dead-token");
    const fcm = new FakeFcmSenderPort();
    fcm.respondWith("dead-token", { delivered: false, staleToken: true, error: "not registered" });

    const summary = await sendDailyNotifications(TODAY, users, content, fcm);

    expect(summary).toEqual({ attempted: 1, sent: 0, skippedStale: 1, failed: 0 });
    expect(users.clearedTokens).toEqual(["user-1"]);
  });

  it("counts a transient send failure without clearing the token", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "flaky-token");
    const fcm = new FakeFcmSenderPort();
    fcm.respondWith("flaky-token", { delivered: false, staleToken: false, error: "quota exceeded" });

    const summary = await sendDailyNotifications(TODAY, users, content, fcm);

    expect(summary).toEqual({ attempted: 1, sent: 0, skippedStale: 0, failed: 1 });
    expect(users.clearedTokens).toEqual([]);
  });

  it("keeps processing remaining users after one user's selection throws", async () => {
    const content = new FakeContentRepository();
    content.content = []; // empty pool -> selectTodayContent throws for every user
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "token-1");
    users.setToken("user-2", "token-2");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(TODAY, users, content, fcm);

    expect(summary).toEqual({ attempted: 2, sent: 0, skippedStale: 0, failed: 2 });
    expect(fcm.sentTo).toHaveLength(0);
  });
});
