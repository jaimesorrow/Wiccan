import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { auth as authV1 } from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";
import { selectTodayContent } from "./getTodayContent";
import { FirestoreContentRepository } from "./firestoreContentRepository";
import { todayIsoDate } from "./dateUtils";
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

initializeApp();

// Matches the Android app's applicationId (app/build.gradle.kts).
const ANDROID_PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME ?? "com.wiccan.app";

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
    return await selectTodayContent(uid, todayIsoDate(), repo);
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
    const tier = await verifyPurchaseAndSyncTier(uid, purchaseToken, productId, billing, users);
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
  const summary = await recheckLapsedSubscriptions(billing, users);
  logger.info("recheckLapsedSubscriptions complete", summary);
});

/**
 * Callable: stores the caller's current FCM token on their own user doc.
 * `users/{uid}` has no client write access in Firestore rules, so this is
 * the only way the token gets there — uid comes from the verified auth
 * context, same as every other callable here.
 */
export const registerFcmToken = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to register a device token.");
  }

  const token = request.data?.token;
  if (typeof token !== "string" || token.length === 0) {
    throw new HttpsError("invalid-argument", "token is required.");
  }

  try {
    const users = new FirestoreNotificationUserRepository(getFirestore());
    await users.setToken(uid, token);
  } catch (err) {
    logger.error("registerFcmToken failed", { uid, error: (err as Error).message });
    throw new HttpsError("internal", "Could not register device token.");
  }
});

/**
 * Daily job: sends every user with a registered FCM token a "your content
 * is ready" push for today's selection. A single fixed UTC time for now —
 * per-user timezone-aware scheduling is a later pass once the user
 * profile actually stores a timezone.
 */
export const sendDailyNotificationsJob = onSchedule("every day 09:00", async () => {
  const repo = new FirestoreContentRepository(getFirestore());
  const users = new FirestoreNotificationUserRepository(getFirestore());
  const fcm = new FirebaseFcmSender(getMessaging());
  const summary = await sendDailyNotifications(todayIsoDate(), users, repo, fcm);
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
