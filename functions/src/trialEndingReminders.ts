import { PlayBillingPort } from "./playBillingPort";
import { PREMIUM_PRODUCT_IDS } from "./tierPolicy";
import { TrialReminderRepositoryPort } from "./trialReminderPort";
import { NotificationUserRepositoryPort } from "./notificationUserRepositoryPort";
import { FcmSenderPort } from "./fcmSenderPort";
import { SupportedLocale } from "./locale";

/** Must match the Android notification channel created in the app. */
export const BILLING_REMINDERS_CHANNEL_ID = "billing_reminders";

/** `data.type` of a trial-ending reminder; the app routes its tap to subscription management. */
export const BILLING_REMINDER_TYPE = "billing_reminder";

/** Remind once, at the first hourly run within this many hours of the trial ending. */
export const TRIAL_REMINDER_WINDOW_HOURS = 28;

export interface TrialReminderSummary {
  candidates: number;
  reminded: number;
  skipped: number;
  sent: number;
  failed: number;
}

/**
 * Plain billing copy (not the mystical voice): when the trial ends, what
 * happens next, and where to cancel. The server does not know the
 * localized price, so it points to the terms Google Play showed at signup.
 */
export function trialEndingCopy(
  expiryTimeMillis: number, timeZone: string | null, locale: SupportedLocale = "en",
): { title: string; body: string } {
  let when: string;
  try {
    when = new Intl.DateTimeFormat(locale === "es" ? "es" : locale === "pt-BR" ? "pt-BR" : "en-US", {
      month: "short", day: "numeric", hour: "numeric", minute: "2-digit",
      timeZone: timeZone ?? "UTC", timeZoneName: "short",
    }).format(new Date(expiryTimeMillis));
  } catch {
    when = new Intl.DateTimeFormat(locale === "es" ? "es" : locale === "pt-BR" ? "pt-BR" : "en-US", {
      month: "short", day: "numeric", hour: "numeric", minute: "2-digit", timeZone: "UTC", timeZoneName: "short",
    }).format(new Date(expiryTimeMillis));
  }
  if (locale === "es") return {
    title: "Tu prueba gratuita termina pronto",
    body: `Tu prueba gratuita de Wiccan termina el ${when}. Si no cancelas antes, Google Play iniciará tu suscripción y cobrará el precio mostrado al registrarte. Adminístrala en Google Play > Pagos y suscripciones > Suscripciones.`,
  };
  if (locale === "pt-BR") return {
    title: "Seu teste gratuito termina em breve",
    body: `Seu teste gratuito do Wiccan termina em ${when}. Se você não cancelar antes, o Google Play iniciará sua assinatura e cobrará o preço mostrado no cadastro. Gerencie em Google Play > Pagamentos e assinaturas > Assinaturas.`,
  };
  return {
    title: "Your free trial ends soon",
    body: `Your Wiccan free trial ends ${when}. Unless you cancel before then, Google Play will start your ` +
      "subscription and charge the price shown when you signed up. Manage it in Google Play > Payments & " +
      "subscriptions > Subscriptions.",
  };
}

/**
 * Sends one "your free trial ends soon" push per trial, to each device the
 * user is signed in on (a registered device; signing out unregisters it)
 * whose daily-practice switch is on, during the first hourly run within
 * TRIAL_REMINDER_WINDOW_HOURS of the trial's end.
 *
 * The trial reminder shares the in-app Daily practice reminder switch
 * (`notificationsEnabled`). A missing value is treated as on, the same
 * default `listDevicesWithToken` and `sendDailyNotifications` use. The
 * separate `billing_reminders` Android channel remains, so the user can
 * still mute trial reminders at the system level. It is sent as a data
 * message carrying the account's uid, so the app shows it only while that
 * same account is still signed in on the device.
 *
 * The `subscriptions/{uid}` record only nominates candidates. Google Play is
 * re-queried for each one before anything is sent, and the reminder is
 * skipped unless Play still reports an active, auto-renewing premium
 * subscription in its free-trial phase with the same end time. A user who
 * already canceled (no charge is coming) or already converted gets nothing.
 *
 * The claim is recorded before sending, so a retried or overlapping run
 * never sends a second reminder for the same trial end. A failed device send
 * is counted, not retried.
 */
export async function sendTrialEndingReminders(
  trials: TrialReminderRepositoryPort,
  billing: PlayBillingPort,
  devices: NotificationUserRepositoryPort,
  fcm: FcmSenderPort,
  now: Date = new Date(),
): Promise<TrialReminderSummary> {
  const nowMillis = now.getTime();
  const untilMillis = nowMillis + TRIAL_REMINDER_WINDOW_HOURS * 60 * 60 * 1000;
  const candidates = await trials.listTrialsEndingBetween(nowMillis, untilMillis);
  const summary: TrialReminderSummary = { candidates: candidates.length, reminded: 0, skipped: 0, sent: 0, failed: 0 };
  if (candidates.length === 0) return summary;

  const allDevices = await devices.listDevicesWithToken();

  for (const candidate of candidates) {
    try {
      const info = await billing.getSubscriptionState(candidate.purchaseToken);
      const expiry = info.expiryTimeMillis;
      const stillTrialing = info.state === "active" && info.inFreeTrial === true && info.autoRenewing &&
        info.productIds.some((id) => PREMIUM_PRODUCT_IDS.has(id)) &&
        expiry !== null && expiry > nowMillis && expiry <= untilMillis;
      if (!stillTrialing || expiry === null) {
        summary.skipped++;
        continue;
      }
      const productId = info.productIds.find((id) => PREMIUM_PRODUCT_IDS.has(id));
      if (!productId) {
        summary.skipped++;
        continue;
      }
      // Do not consume the single reminder when the account has no signed-in
      // device with the daily-practice switch on. A later hourly run can
      // still notify after they sign back in or turn the switch back on,
      // provided their trial has not ended. `notificationsEnabled !== false`
      // matches listDevicesWithToken: a missing value defaults to on.
      const recipients = allDevices.filter(
        (device) => device.uid === candidate.uid && device.notificationsEnabled !== false,
      );
      if (recipients.length === 0) {
        summary.skipped++;
        continue;
      }
      if (!(await trials.claimReminder(candidate.uid, candidate.purchaseToken, expiry))) {
        summary.skipped++;
        continue;
      }
      summary.reminded++;
      // Every signed-in device whose daily-practice switch is on (or unset).
      const ttlMillis = Math.max(1, expiry - nowMillis);
      for (const device of recipients) {
        const copy = trialEndingCopy(expiry, device.timeZone, device.locale);
        const result = await fcm.send(device.token, copy.title, copy.body, BILLING_REMINDERS_CHANNEL_ID, {
          dataOnly: true,
          data: { type: BILLING_REMINDER_TYPE, uid: candidate.uid, productId, expiryTimeMillis: String(expiry) },
          ttlMillis,
        });
        if (result.delivered) {
          summary.sent++;
        } else {
          if (result.staleToken) await devices.clearToken(device.uid, device.token);
          summary.failed++;
        }
      }
    } catch {
      summary.failed++;
    }
  }
  return summary;
}
