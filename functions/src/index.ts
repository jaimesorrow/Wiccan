import { getAuth } from "firebase-admin/auth";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onMessagePublished } from "firebase-functions/v2/pubsub";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { auth as authV1 } from "firebase-functions/v1";
import { StalePolicyVersionError } from "./acceptTerms";
import { AnalyticsService } from "./services/analyticsService";
import { ContentService } from "./services/contentService";
import { NotificationService } from "./services/notificationService";
import { SubscriptionService } from "./services/subscriptionService";
import { UserService } from "./services/userService";
import { PurchaseVerificationError } from "./verifyPurchase";

initializeApp();

/**
 * Canonical technical Android identity. Product branding is Wiccan, but the
 * installed package/application ID remains com.nyxwell.app. Changing this
 * requires a coordinated Firebase/OAuth/App Check/Play migration.
 */
const ANDROID_PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME ?? "com.nyxwell.app";
const PLAY_RTDN_TOPIC = process.env.PLAY_RTDN_TOPIC ?? "play-rtdn";

const db = getFirestore();
const auth = getAuth();
const analytics = new AnalyticsService();
const contentService = new ContentService(db);
const subscriptionService = new SubscriptionService(db, ANDROID_PACKAGE_NAME);
const userService = new UserService(db, auth, ANDROID_PACKAGE_NAME);
const notificationService = new NotificationService(db, getMessaging());

/**
 * Returns the authenticated caller's transaction-safe daily assignment.
 * App Check is mandatory and uid is derived exclusively from auth context.
 */
export const getTodayContent = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to fetch today's content.");
  }

  try {
    return await contentService.getTodayContent(uid);
  } catch (err) {
    analytics.error("getTodayContent_failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not select today's content.");
  }
});

/**
 * Verifies a Play purchase server-side. Client billing state is never the
 * entitlement source of truth.
 */
export const verifyPurchase = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to verify a purchase.");
  }

  const purchaseToken = request.data?.purchaseToken;
  const productId = request.data?.productId;
  if (typeof purchaseToken !== "string" || purchaseToken.length === 0 || typeof productId !== "string" || productId.length === 0) {
    throw new HttpsError("invalid-argument", "purchaseToken and productId are required.");
  }

  try {
    const tier = await subscriptionService.verifyPurchase(uid, purchaseToken, productId);
    return { tier };
  } catch (err) {
    if (err instanceof PurchaseVerificationError) {
      throw new HttpsError("failed-precondition", err.message);
    }
    analytics.error("verifyPurchase_failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not verify purchase.");
  }
});

/** Daily safety-net reconciliation for missed/delayed RTDN events. */
export const recheckLapsedSubscriptionsJob = onSchedule("every day 03:00", async () => {
  const summary = await subscriptionService.recheckLapsedSubscriptions();
  analytics.info("recheckLapsedSubscriptions_complete", summary as unknown as Record<string, unknown>);
});

/**
 * Google Play RTDN handler. The RTDN event is only a signal; the service
 * re-queries Google Play and persists server-verified entitlement state.
 */
export const handlePlayRtdn = onMessagePublished(PLAY_RTDN_TOPIC, async (event) => {
  const messageData = event.data.message.data;
  if (!messageData) return;

  try {
    const result = await subscriptionService.handleRtdnMessage(messageData);
    if (result) {
      analytics.info("handlePlayRtdn_processed", { result });
    }
  } catch (err) {
    analytics.error("handlePlayRtdn_failed", { error: (err as Error).message });
  }
});

/** Protected per-device registration. */
export const registerDeviceToken = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to register a device token.");
  }

  const token = request.data?.token;
  const timeZone = request.data?.timeZone;
  if (typeof token !== "string" || token.length === 0) {
    throw new HttpsError("invalid-argument", "token is required.");
  }
  if (timeZone !== undefined && typeof timeZone !== "string") {
    throw new HttpsError("invalid-argument", "timeZone must be a string when provided.");
  }

  try {
    await userService.registerDevice(uid, token, timeZone);
  } catch (err) {
    analytics.error("registerDeviceToken_failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not register device token.");
  }
});

/** Protected removal of only the caller's current device registration. */
export const unregisterDeviceToken = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to unregister a device token.");
  }

  const token = request.data?.token;
  if (typeof token !== "string" || token.length === 0) {
    throw new HttpsError("invalid-argument", "token is required.");
  }

  try {
    await userService.unregisterDevice(uid, token);
  } catch (err) {
    analytics.error("unregisterDeviceToken_failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not unregister device token.");
  }
});

/** Protected timezone mutation for one registered device. */
export const updateTimeZone = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to update your time zone.");
  }

  const token = request.data?.token;
  const timeZone = request.data?.timeZone;
  if (typeof token !== "string" || token.length === 0 || typeof timeZone !== "string" || timeZone.length === 0) {
    throw new HttpsError("invalid-argument", "token and timeZone are required.");
  }

  try {
    await userService.updateTimeZone(uid, token, timeZone);
  } catch (err) {
    analytics.error("updateTimeZone_failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not update time zone.");
  }
});

/** Versioned Terms/Privacy acceptance; server validates accepted versions. */
export const acceptTerms = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to accept the current policies.");
  }

  const termsVersion = request.data?.termsVersion;
  const privacyVersion = request.data?.privacyVersion;
  if (typeof termsVersion !== "number" || typeof privacyVersion !== "number") {
    throw new HttpsError("invalid-argument", "termsVersion and privacyVersion are required.");
  }

  try {
    await userService.acceptTerms(uid, termsVersion, privacyVersion);
  } catch (err) {
    if (err instanceof StalePolicyVersionError) {
      throw new HttpsError("failed-precondition", err.message);
    }
    analytics.error("acceptTerms_failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not record policy acceptance.");
  }
});

/** Hourly poll; service filters to devices whose local hour is due. */
export const sendDailyNotificationsJob = onSchedule("every 1 hours", async () => {
  const summary = await notificationService.sendDueDailyNotifications();
  analytics.info("sendDailyNotifications_complete", summary as unknown as Record<string, unknown>);
});

/** Server-only profile creation on Firebase Auth account creation. */
export const onUserCreated = authV1.user().onCreate(async (user) => {
  await userService.createProfile(user.uid);
});

/** Protected self-delete; uid is never accepted from client data. */
export const deleteUserData = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to delete your account.");
  }

  try {
    await userService.deleteAccount(uid);
  } catch (err) {
    analytics.error("deleteUserData_failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not delete account data.");
  }
});
