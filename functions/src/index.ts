import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onMessagePublished } from "firebase-functions/v2/pubsub";
import { auth as authV1 } from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";
import { selectTodayContent } from "./getTodayContent";
import { FirestoreContentRepository } from "./firestoreContentRepository";
import { getUserLocalDate } from "./dateUtils";
import { GooglePlayBillingRepository } from "./googlePlayBillingRepository";
import { FirestoreUserRepository } from "./firestoreUserRepository";
import { verifyPurchaseAndSyncTier, PurchaseVerificationError } from "./verifyPurchase";
import { recheckLapsedSubscriptions } from "./recheckLapsedSubscriptions";
import { FirestoreNotificationUserRepository } from "./firestoreNotificationUserRepository";
import { FirebaseFcmSender } from "./firebaseFcmSender";
import { sendDailyNotifications } from "./sendDailyNotifications";
import { getMessaging } from "firebase-admin/messaging";
import { FirestoreUserProfileRepository } from "./firestoreUserProfileRepository";
import { createUserProfileOnSignUp } from "./createUserProfile";
import { FirestoreAccountDeletionRepository } from "./firestoreAccountDeletionRepository";
import { deleteAccount } from "./deleteAccount";
import { acceptTerms as acceptTermsCore, StalePolicyVersionError } from "./acceptTerms";
import { FirestoreSubscriptionRecordRepository } from "./firestoreSubscriptionRecordRepository";
import { handleRtdnNotification, parseRtdnPurchaseToken } from "./handleRtdn";

initializeApp();

// Matches the Android app's applicationId (app/build.gradle.kts).
const ANDROID_PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME ?? "com.wiccan.app";

// The Pub/Sub topic Play Console's "Real-time developer notifications"
// setting must be pointed at (Monetization setup -> RTDN). This is an
// operational Play Console step, not something this code provisions.
const PLAY_RTDN_TOPIC = process.env.PLAY_RTDN_TOPIC ?? "play-rtdn";

/**
 * Callable: returns the caller's content for today, selecting and
 * recording it server-side if this is their first call of the day. The
 * uid always comes from the verified auth context, never from client
 * input, so a client cannot request (or game) another user's history.
 * App Check-enforced: only the real, attested Android client may call this.
 */
export const getTodayContent = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to fetch today's content.");
  }

  try {
    const repo = new FirestoreContentRepository(getFirestore());
    const timeZone = await repo.getUserTimeZone(uid);
    return await selectTodayContent(uid, getUserLocalDate(timeZone), repo);
  } catch (err) {
    logger.error("getTodayContent failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not select today's content.");
  }
});

/**
 * Callable: verifies a Play purchase token server-side and syncs
 * `users/{uid}.tier`. Like getTodayContent, uid comes only from the
 * verified auth context — a client can never verify a purchase for, or
 * report a tier on behalf of, another user. App Check-enforced.
 */
export const verifyPurchase = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to verify a purchase.");
  }

  const purchaseToken = request.data?.purchaseToken;
  const productId = request.data?.productId;
  if (typeof purchaseToken !== "string" || typeof productId !== "string") {
    throw new HttpsError("invalid-argument", "purchaseToken and productId are required.");
  }

  try {
    const billing = new GooglePlayBillingRepository(ANDROID_PACKAGE_NAME);
    const users = new FirestoreUserRepository(getFirestore());
    const subscriptions = new FirestoreSubscriptionRecordRepository(getFirestore());
    const tier = await verifyPurchaseAndSyncTier(uid, purchaseToken, productId, billing, users, subscriptions);
    return { tier };
  } catch (err) {
    if (err instanceof PurchaseVerificationError) {
      throw new HttpsError("failed-precondition", err.message);
    }
    logger.error("verifyPurchase failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not verify purchase.");
  }
});

/**
 * Daily job: re-verifies every user currently marked premium and
 * downgrades any whose subscription has lapsed since their last check —
 * subscriptions can expire without any app-side event firing. `onSchedule`
 * provisions its own Cloud Scheduler job on deploy; no separate manual
 * setup is needed beyond `firebase deploy`.
 */
export const recheckLapsedSubscriptionsJob = onSchedule("every day 03:00", async () => {
  const billing = new GooglePlayBillingRepository(ANDROID_PACKAGE_NAME);
  const users = new FirestoreUserRepository(getFirestore());
  const subscriptions = new FirestoreSubscriptionRecordRepository(getFirestore());
  const summary = await recheckLapsedSubscriptions(billing, users, subscriptions);
  logger.info("recheckLapsedSubscriptions complete", summary);
});

/**
 * Pub/Sub-triggered: handles a Google Play Real-Time Developer
 * Notification for a subscription lifecycle event (renewal,
 * cancellation, recovery, price change, etc). Keeps `users/{uid}.tier`
 * and the `subscriptions/{uid}` audit record current as events happen,
 * rather than waiting for the next daily reconciliation pass.
 *
 * Requires Play Console's Monetization setup -> "Real-time developer
 * notifications" to be pointed at this Pub/Sub topic
 * (`PLAY_RTDN_TOPIC`/"play-rtdn"), and that topic to grant the Play
 * publisher service account permission to publish to it - both
 * operational Play Console/Cloud IAM steps this code cannot perform on
 * its own. Daily reconciliation (`recheckLapsedSubscriptionsJob`) remains
 * as a backup for any notification that is missed or delayed.
 */
export const handlePlayRtdn = onMessagePublished(PLAY_RTDN_TOPIC, async (event) => {
  const messageData = event.data.message.data;
  if (!messageData) {
    return;
  }

  const purchaseToken = parseRtdnPurchaseToken(messageData);
  if (!purchaseToken) {
    // Not a subscription notification (e.g. a one-time-product or test
    // notification also delivered through this topic) - nothing to do.
    return;
  }

  try {
    const billing = new GooglePlayBillingRepository(ANDROID_PACKAGE_NAME);
    const users = new FirestoreUserRepository(getFirestore());
    const subscriptions = new FirestoreSubscriptionRecordRepository(getFirestore());
    const result = await handleRtdnNotification(purchaseToken, billing, users, subscriptions);
    logger.info("handlePlayRtdn processed", { result });
  } catch (err) {
    logger.error("handlePlayRtdn failed", { error: (err as Error).message });
  }
});

/**
 * Callable: registers the caller's current device (its FCM token and,
 * optionally, its time zone) as one entry in `users/{uid}/devices`.
 * A user may have any number of registered devices at once - phone,
 * tablet, etc. - each addressed independently, so signing in on a second
 * device never displaces the first. `users/{uid}/devices` has no client
 * write access in Firestore rules, so this is the only way either value
 * gets there — uid comes from the verified auth context, same as every
 * other callable here. App Check-enforced, per the final security state:
 * device registration is a protected profile mutation.
 */
export const registerDeviceToken = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to register a device token.");
  }

  const token = request.data?.token;
  if (typeof token !== "string" || token.length === 0) {
    throw new HttpsError("invalid-argument", "token is required.");
  }
  const timeZone = request.data?.timeZone;
  if (timeZone !== undefined && typeof timeZone !== "string") {
    throw new HttpsError("invalid-argument", "timeZone must be a string when provided.");
  }

  try {
    const users = new FirestoreNotificationUserRepository(getFirestore());
    await users.setToken(uid, token, timeZone);
  } catch (err) {
    logger.error("registerDeviceToken failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not register device token.");
  }
});

/**
 * Callable: removes the caller's *own* device registration (identified by
 * its own token), e.g. before sign-out so a signed-out device stops
 * receiving pushes meant for whoever signs in next on it - without
 * touching this user's other registered devices. App Check-enforced.
 */
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
    const users = new FirestoreNotificationUserRepository(getFirestore());
    await users.clearToken(uid, token);
  } catch (err) {
    logger.error("unregisterDeviceToken failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not unregister device token.");
  }
});

/**
 * Callable: updates the time zone of one already-registered device,
 * independent of its token, e.g. when the app detects the device's time
 * zone no longer matches what's on record. App Check-enforced.
 */
export const updateTimeZone = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to update your time zone.");
  }

  const token = request.data?.token;
  if (typeof token !== "string" || token.length === 0) {
    throw new HttpsError("invalid-argument", "token is required.");
  }
  const timeZone = request.data?.timeZone;
  if (typeof timeZone !== "string" || timeZone.length === 0) {
    throw new HttpsError("invalid-argument", "timeZone is required.");
  }

  try {
    const users = new FirestoreNotificationUserRepository(getFirestore());
    await users.setTimeZone(uid, token, timeZone);
  } catch (err) {
    logger.error("updateTimeZone failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not update time zone.");
  }
});

/**
 * Callable: records the caller's Terms of Service/Privacy Policy
 * acceptance. Rejects any version other than the server's current ones
 * (see policyVersions.ts) rather than trusting the client's claim.
 * App Check-enforced.
 */
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
    const repo = new FirestoreUserProfileRepository(getFirestore());
    await acceptTermsCore(uid, termsVersion, privacyVersion, repo);
  } catch (err) {
    if (err instanceof StalePolicyVersionError) {
      throw new HttpsError("failed-precondition", err.message);
    }
    logger.error("acceptTerms failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not record policy acceptance.");
  }
});

/**
 * Daily job: sends every user with a registered FCM token a "your content
 * is ready" push for their own local today's selection (see
 * sendDailyNotifications - each recipient's date comes from their own
 * stored time zone, never a single shared batch date). The job still
 * *fires* at one fixed UTC time for now; per-device ~9am-local send
 * scheduling is a later, larger pass (multi-device registrations +
 * per-device nextNotificationAt).
 */
export const sendDailyNotificationsJob = onSchedule("every day 09:00", async () => {
  const repo = new FirestoreContentRepository(getFirestore());
  const users = new FirestoreNotificationUserRepository(getFirestore());
  const fcm = new FirebaseFcmSender(getMessaging());
  const summary = await sendDailyNotifications(users, repo, fcm);
  logger.info("sendDailyNotifications complete", summary);
});

/**
 * 1st-gen Auth trigger: creates users/{uid} the moment a new Firebase Auth
 * account exists, so the client never creates its own profile document.
 * 1st-gen triggers coexist fine with the 2nd-gen callables/scheduled
 * functions used everywhere else in this codebase.
 */
export const onUserCreated = authV1.user().onCreate(async (user) => {
  const repo = new FirestoreUserProfileRepository(getFirestore());
  await createUserProfileOnSignUp(user.uid, repo);
});

/**
 * Callable: permanently deletes the caller's profile, delivery history, and
 * Firebase Auth account. Self-delete only — like every other callable here,
 * uid comes exclusively from the verified auth context, so a client can
 * never delete another user's account. App Check-enforced.
 */
export const deleteUserData = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to delete your account.");
  }

  try {
    const repo = new FirestoreAccountDeletionRepository(getFirestore(), getAuth());
    await deleteAccount(uid, repo);
  } catch (err) {
    logger.error("deleteUserData failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not delete account data.");
  }
});
