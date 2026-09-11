import { sendDailyNotifications } from "../sendDailyNotifications";
import { FakeContentRepository, makeContent } from "../testUtils/fakeContentRepository";
import { FakeNotificationUserRepository } from "../testUtils/fakeNotificationUserRepository";
import { FakeFcmSenderPort } from "../testUtils/fakeFcmSenderPort";

describe("sendDailyNotifications", () => {
  it("sends a push to every user with a token, titled by content type", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1", type: "spell", title: "Candle of Clear Intent" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "token-1");
    users.setToken("user-2", "token-2");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(users, content, fcm);

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

    const summary = await sendDailyNotifications(users, content, fcm);

    expect(summary).toEqual({ attempted: 1, sent: 0, skippedStale: 1, failed: 0 });
    expect(users.cleared).toEqual([{ uid: "user-1", token: "dead-token" }]);
  });

  it("counts a transient send failure without clearing the token", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "flaky-token");
    const fcm = new FakeFcmSenderPort();
    fcm.respondWith("flaky-token", { delivered: false, staleToken: false, error: "quota exceeded" });

    const summary = await sendDailyNotifications(users, content, fcm);

    expect(summary).toEqual({ attempted: 1, sent: 0, skippedStale: 0, failed: 1 });
    expect(users.cleared).toEqual([]);
  });

  it("sends one push per registered device when a user has more than one", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1", type: "spell", title: "Candle of Clear Intent" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "phone-token");
    users.setToken("user-1", "tablet-token");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(users, content, fcm);

    expect(summary).toEqual({ attempted: 2, sent: 2, skippedStale: 0, failed: 0 });
    expect(fcm.sentTo.map((s) => s.token).sort()).toEqual(["phone-token", "tablet-token"]);
    // Both devices are told about the same selection - one draw per user per day, not per device.
    expect(content.history).toHaveLength(1);
  });

  it("clearing a stale device leaves the same user's other devices untouched", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "dead-token");
    users.setToken("user-1", "healthy-token");
    const fcm = new FakeFcmSenderPort();
    fcm.respondWith("dead-token", { delivered: false, staleToken: true, error: "not registered" });

    const summary = await sendDailyNotifications(users, content, fcm);

    expect(summary).toEqual({ attempted: 2, sent: 1, skippedStale: 1, failed: 0 });
    expect(users.cleared).toEqual([{ uid: "user-1", token: "dead-token" }]);
    expect(await users.listDevicesWithToken()).toEqual([{ uid: "user-1", token: "healthy-token", timeZone: null }]);
  });

  it("keeps processing remaining users after one user's selection throws", async () => {
    const content = new FakeContentRepository();
    content.content = []; // empty pool -> selectTodayContent throws for every user
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "token-1");
    users.setToken("user-2", "token-2");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(users, content, fcm);

    expect(summary).toEqual({ attempted: 2, sent: 0, skippedStale: 0, failed: 2 });
    expect(fcm.sentTo).toHaveLength(0);
  });

  it("computes each recipient's delivery date from their own time zone, not a shared batch date", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1" })];
    const users = new FakeNotificationUserRepository();
    // A moment that is still "yesterday" in UTC-11 but already "tomorrow"
    // in UTC+13 - if both users shared one batch date, at least one of
    // them would get a delivery recorded under the wrong local day.
    const fixedNow = new Date("2026-09-10T23:30:00.000Z");
    jest.useFakeTimers().setSystemTime(fixedNow);
    try {
      users.setToken("user-west", "token-west", "Pacific/Midway"); // UTC-11
      users.setToken("user-east", "token-east", "Pacific/Auckland"); // UTC+13 (NZST)
      const fcm = new FakeFcmSenderPort();

      await sendDailyNotifications(users, content, fcm);

      const westEntry = content.history.find((h) => h.userId === "user-west");
      const eastEntry = content.history.find((h) => h.userId === "user-east");
      expect(westEntry?.deliveredDate).toBe("2026-09-10");
      expect(eastEntry?.deliveredDate).toBe("2026-09-11");
    } finally {
      jest.useRealTimers();
    }
  });
});
