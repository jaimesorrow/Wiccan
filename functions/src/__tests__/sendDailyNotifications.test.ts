import { sendDailyNotifications } from "../sendDailyNotifications";
import { FakeContentRepository, makeContent } from "../testUtils/fakeContentRepository";
import { FakeNotificationUserRepository } from "../testUtils/fakeNotificationUserRepository";
import { FakeFcmSenderPort } from "../testUtils/fakeFcmSenderPort";

// UTC devices (the default when no time zone is on file) read local hour 9
// at this instant - used by every test that isn't specifically exercising
// the per-time-zone filtering itself.
const NINE_AM_UTC = new Date("2026-01-01T09:00:00.000Z");

describe("sendDailyNotifications", () => {
  it("sends a push to every user with a token, titled by content type", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1", type: "spell", title: "Candle of Clear Intent" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "token-1");
    users.setToken("user-2", "token-2");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

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

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

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

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

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

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

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

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

    expect(summary).toEqual({ attempted: 2, sent: 1, skippedStale: 1, failed: 0 });
    expect(users.cleared).toEqual([{ uid: "user-1", token: "dead-token" }]);
    expect(await users.listDevicesWithToken()).toEqual([{ uid: "user-1", token: "healthy-token", timeZone: null }]);
  });

  it("only sends to devices whose local time currently reads 9am, skipping the rest", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1" })];
    const users = new FakeNotificationUserRepository();
    // At 2026-01-01T09:00:00Z: UTC reads 9am (included), America/Los_Angeles
    // (UTC-8 in January) reads 1am (excluded).
    users.setToken("user-utc", "utc-token", "Etc/UTC");
    users.setToken("user-la", "la-token", "America/Los_Angeles");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

    expect(summary).toEqual({ attempted: 1, sent: 1, skippedStale: 0, failed: 0 });
    expect(fcm.sentTo.map((s) => s.token)).toEqual(["utc-token"]);
  });

  it("computes each recipient's delivery date from their own time zone, not a shared batch date", async () => {
    const content = new FakeContentRepository();
    content.content = [makeContent({ id: "c1" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-west", "token-west", "Pacific/Midway"); // UTC-11
    users.setToken("user-east", "token-east", "Pacific/Auckland"); // UTC+12 in September (pre-DST)
    const fcm = new FakeFcmSenderPort();

    // Two separate 9am-local moments, one per zone, each far enough from
    // the other's own 9am that only the intended device is picked up per
    // call - the same "9am-local, not a shared instant" behavior this
    // whole feature exists for.
    await sendDailyNotifications(users, content, fcm, new Date("2026-09-10T20:00:00.000Z")); // 9am Pacific/Midway
    await sendDailyNotifications(users, content, fcm, new Date("2026-09-10T21:00:00.000Z")); // 9am Pacific/Auckland

    const westEntry = content.history.find((h) => h.userId === "user-west");
    const eastEntry = content.history.find((h) => h.userId === "user-east");
    expect(westEntry?.deliveredDate).toBe("2026-09-10");
    expect(eastEntry?.deliveredDate).toBe("2026-09-11");
  });

  it("keeps processing remaining devices after one device's selection throws", async () => {
    const content = new FakeContentRepository();
    content.content = []; // empty pool -> selectTodayContent throws for every user
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "token-1");
    users.setToken("user-2", "token-2");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

    expect(summary).toEqual({ attempted: 2, sent: 0, skippedStale: 0, failed: 2 });
    expect(fcm.sentTo).toHaveLength(0);
  });

  it("only offers a free user free-tier published content", async () => {
    const content = new FakeContentRepository();
    content.tier = "free";
    content.content = [makeContent({ id: "premium-only", tier: "premium" })];
    const users = new FakeNotificationUserRepository();
    users.setToken("user-1", "token-1");
    const fcm = new FakeFcmSenderPort();

    const summary = await sendDailyNotifications(users, content, fcm, NINE_AM_UTC);

    expect(summary).toEqual({ attempted: 1, sent: 0, skippedStale: 0, failed: 1 });
  });
});
