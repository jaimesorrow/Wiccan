import {
  sendTrialEndingReminders, trialEndingCopy, BILLING_REMINDERS_CHANNEL_ID, BILLING_REMINDER_TYPE,
} from "../trialEndingReminders";
import { FakeTrialReminderRepository } from "../testUtils/fakeTrialReminderRepository";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { FakeNotificationUserRepository } from "../testUtils/fakeNotificationUserRepository";
import { FakeFcmSenderPort } from "../testUtils/fakeFcmSenderPort";

const NOW = new Date("2027-01-07T09:00:00.000Z");
const HOUR = 60 * 60 * 1000;
const TRIAL_END = NOW.getTime() + 20 * HOUR;

function setup(options: { expiry?: number; autoRenewing?: boolean; inFreeTrial?: boolean; state?: "active" | "canceled" } = {}) {
  const expiry = options.expiry ?? TRIAL_END;
  const trials = new FakeTrialReminderRepository();
  trials.trials = [{ uid: "user-1", purchaseToken: "token-1", expiryTimeMillis: expiry }];
  const billing = new FakePlayBillingPort();
  billing.respondWith("token-1", options.state ?? "active", ["wiccan_premium_yearly"], {
    expiryTimeMillis: expiry, autoRenewing: options.autoRenewing ?? true, inFreeTrial: options.inFreeTrial ?? true,
  });
  const devices = new FakeNotificationUserRepository();
  devices.setToken("user-1", "phone", "America/Anchorage");
  devices.setToken("user-1", "tablet", null);
  devices.setToken("user-2", "other-user", null);
  const fcm = new FakeFcmSenderPort();
  return { trials, billing, devices, fcm };
}

describe("sendTrialEndingReminders", () => {
  it("sends one plain billing reminder to each of the trialing user's devices on the billing channel", async () => {
    const { trials, billing, devices, fcm } = setup();
    const summary = await sendTrialEndingReminders(trials, billing, devices, fcm, NOW);
    expect(summary).toEqual({ candidates: 1, reminded: 1, skipped: 0, sent: 2, failed: 0 });
    expect(fcm.sentTo.map((sent) => sent.token)).toEqual(["phone", "tablet"]);
    expect(fcm.sentTo.every((sent) => sent.channelId === BILLING_REMINDERS_CHANNEL_ID)).toBe(true);
    expect(fcm.sentTo[0].title).toBe("Your free trial ends soon");
    expect(fcm.sentTo[0].body).toContain("Jan 7, 8:00 PM AKST");
    expect(fcm.sentTo[1].body).toContain("Jan 8, 5:00 AM UTC");
  });

  it("never sends a second reminder for the same trial on a later or overlapping run", async () => {
    const { trials, billing, devices, fcm } = setup();
    await sendTrialEndingReminders(trials, billing, devices, fcm, NOW);
    const again = await sendTrialEndingReminders(trials, billing, devices, fcm, new Date(NOW.getTime() + HOUR));
    expect(again.reminded).toBe(0);
    expect(fcm.sentTo).toHaveLength(2);
  });

  it("skips a trial the user already canceled, since no charge is coming", async () => {
    const { trials, billing, devices, fcm } = setup({ autoRenewing: false });
    expect((await sendTrialEndingReminders(trials, billing, devices, fcm, NOW)).skipped).toBe(1);
    expect(fcm.sentTo).toHaveLength(0);
    expect(trials.claimed.size).toBe(0);
  });

  it("re-checks Google Play and skips a stale record that already converted to paid", async () => {
    const { trials, billing, devices, fcm } = setup({ inFreeTrial: false });
    expect((await sendTrialEndingReminders(trials, billing, devices, fcm, NOW)).skipped).toBe(1);
    expect(fcm.sentTo).toHaveLength(0);
  });

  it("does not remind while the trial end is still more than a day away", async () => {
    const { trials, billing, devices, fcm } = setup({ expiry: NOW.getTime() + 3 * 24 * HOUR });
    expect((await sendTrialEndingReminders(trials, billing, devices, fcm, NOW)).candidates).toBe(0);
    expect(fcm.sentTo).toHaveLength(0);
  });

  it("does not depend on the daily-practice switch: a device with the daily push off still gets it", async () => {
    const { trials, billing, devices, fcm } = setup();
    await devices.setNotificationsEnabled("user-1", "phone", false);
    await devices.setNotificationsEnabled("user-1", "tablet", false);
    const summary = await sendTrialEndingReminders(trials, billing, devices, fcm, NOW);
    expect(summary).toMatchObject({ reminded: 1, sent: 2, failed: 0 });
    expect(fcm.sentTo.map((sent) => sent.token)).toEqual(["phone", "tablet"]);
  });

  it("sends a data message tagged with the account, so the app can refuse it after sign-out", async () => {
    const { trials, billing, devices, fcm } = setup();
    await sendTrialEndingReminders(trials, billing, devices, fcm, NOW);
    for (const sent of fcm.sentTo) {
      expect(sent.options).toEqual({
        dataOnly: true,
        data: {
          type: BILLING_REMINDER_TYPE,
          uid: "user-1",
          productId: "wiccan_premium_yearly",
          expiryTimeMillis: String(TRIAL_END),
        },
        ttlMillis: TRIAL_END - NOW.getTime(),
      });
    }
  });

  it("only reaches devices the user is still signed in on, never another account's device", async () => {
    const { trials, billing, devices, fcm } = setup();
    // Signing out unregisters the device (unregisterDeviceToken clears it).
    await devices.clearToken("user-1", "tablet");
    await sendTrialEndingReminders(trials, billing, devices, fcm, NOW);
    expect(fcm.sentTo.map((sent) => sent.token)).toEqual(["phone"]);
  });

  it("clears a dead token", async () => {
    const { trials, billing, devices, fcm } = setup();
    fcm.respondWith("phone", { delivered: false, staleToken: true, error: "gone" });
    const summary = await sendTrialEndingReminders(trials, billing, devices, fcm, NOW);
    expect(summary).toMatchObject({ reminded: 1, sent: 1, failed: 1 });
    expect(fcm.sentTo.map((sent) => sent.token)).toEqual(["phone", "tablet"]);
    expect(devices.cleared).toEqual([{ uid: "user-1", token: "phone" }]);
  });

  it("counts a Play lookup failure without stopping the batch", async () => {
    const { trials, billing, devices, fcm } = setup();
    trials.trials.unshift({ uid: "user-2", purchaseToken: "unknown-token", expiryTimeMillis: TRIAL_END });
    const summary = await sendTrialEndingReminders(trials, billing, devices, fcm, NOW);
    expect(summary).toMatchObject({ candidates: 2, reminded: 1, failed: 1, sent: 2 });
  });

  it("keeps the copy plain and falls back to UTC for an unknown time zone", () => {
    const copy = trialEndingCopy(TRIAL_END, "Not/AZone");
    expect(copy.body).toContain("UTC");
    expect(copy.body).toContain("Unless you cancel before then");
    expect(copy.body).toContain("Google Play > Payments & subscriptions > Subscriptions");
  });
});
