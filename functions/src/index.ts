import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import { selectTodayContent } from "./getTodayContent";
import { FirestoreContentRepository } from "./firestoreContentRepository";
import { todayIsoDate } from "./dateUtils";
import { GooglePlayBillingRepository } from "./googlePlayBillingRepository";
import { FirestoreUserRepository } from "./firestoreUserRepository";
import { verifyPurchaseAndSyncTier, PurchaseVerificationError } from "./verifyPurchase";
import { recheckLapsedSubscriptions } from "./recheckLapsedSubscriptions";

initializeApp();

// Matches the Android app's applicationId (app/build.gradle.kts).
const ANDROID_PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME ?? "com.nyxwell.app";

/**
 * Callable: returns the caller's content for today, selecting and
 * recording it server-side if this is their first call of the day. The
 * uid always comes from the verified auth context, never from client
 * input, so a client cannot request (or game) another user's history.
 */
export const getTodayContent = onCall(async (request) => {
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
 * report a tier on behalf of, another user.
 */
export const verifyPurchase = onCall(async (request) => {
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
