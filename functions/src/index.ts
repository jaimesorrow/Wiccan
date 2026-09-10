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
import { ContentTier, ContentType } from "./types";
import { loadContentSources } from "./contentSource";
import { ClaudeDraftGenerator } from "./claudeDraftGenerator";
import { FirestoreContentAdminRepository } from "./firestoreContentAdminRepository";
import { generateDrafts, InvalidGenerationRequestError } from "./generateDraft";
import { approveDraft as approveDraftAndSync, DraftNotFoundError } from "./approveDraft";
import { FirestoreNotificationUserRepository } from "./firestoreNotificationUserRepository";
import { FirebaseFcmSender } from "./firebaseFcmSender";
import { sendDailyNotifications } from "./sendDailyNotifications";
import { getMessaging } from "firebase-admin/messaging";

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

const VALID_TIERS: ReadonlySet<string> = new Set(["free", "premium"]);
const VALID_TYPES: ReadonlySet<string> = new Set(["spell", "potion", "lesson"]);

/**
 * Callable: generates `count` (default 1) content drafts from the curated
 * sources.json pool via Claude, round-robining across eligible source/type
 * combinations. Every draft is written with status "draft" and a
 * sourceId — nothing here ever auto-publishes. Requires sign-in for now;
 * Session 17 locks this down to admin-only and removes it from the
 * public callable surface entirely.
 */
export const generateDraft = onCall(async (request) => {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to generate content.");
  }

  const { tier, sourceId, type, count } = request.data ?? {};
  if (typeof tier !== "string" || !VALID_TIERS.has(tier)) {
    throw new HttpsError("invalid-argument", "tier must be 'free' or 'premium'.");
  }
  if (sourceId !== undefined && typeof sourceId !== "string") {
    throw new HttpsError("invalid-argument", "sourceId must be a string when provided.");
  }
  if (type !== undefined && (typeof type !== "string" || !VALID_TYPES.has(type))) {
    throw new HttpsError("invalid-argument", "type must be 'spell', 'potion', or 'lesson' when provided.");
  }
  if (count !== undefined && (typeof count !== "number" || !Number.isInteger(count) || count < 1)) {
    throw new HttpsError("invalid-argument", "count must be a positive integer when provided.");
  }

  try {
    const sources = loadContentSources();
    const generator = new ClaudeDraftGenerator();
    const repo = new FirestoreContentAdminRepository(getFirestore());
    const results = await generateDrafts(
      { tier: tier as ContentTier, sourceId, type: type as ContentType | undefined, count },
      sources,
      generator,
      repo,
    );
    return { drafts: results };
  } catch (err) {
    if (err instanceof InvalidGenerationRequestError) {
      throw new HttpsError("invalid-argument", err.message);
    }
    logger.error("generateDraft failed", { error: (err as Error).message });
    throw new HttpsError("internal", "Could not generate a draft.");
  }
});

/**
 * Callable: flips one content document from "draft" to "published". This
 * is the entire review step for now — Jaime reviews drafts directly in
 * the Firestore console and calls this per item he approves.
 */
export const approveDraft = onCall(async (request) => {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Sign-in is required to approve a draft.");
  }

  const contentId = request.data?.contentId;
  if (typeof contentId !== "string" || contentId.length === 0) {
    throw new HttpsError("invalid-argument", "contentId is required.");
  }

  try {
    const repo = new FirestoreContentAdminRepository(getFirestore());
    await approveDraftAndSync(contentId, repo);
    return { contentId, status: "published" };
  } catch (err) {
    if (err instanceof DraftNotFoundError) {
      throw new HttpsError("not-found", err.message);
    }
    logger.error("approveDraft failed", { contentId, error: (err as Error).message });
    throw new HttpsError("internal", "Could not approve draft.");
  }
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
