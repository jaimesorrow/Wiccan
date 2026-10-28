#!/usr/bin/env node
// End-to-end exercise of the Cloud Functions backend against the local
// Firebase emulator suite. Covers the paths unit tests can't reach: real
// Firestore reads/writes, the daily-assignment transaction, the
// users/{uid}/devices collection-group query, the kill switch, and the
// account-deletion cascade.
//
// Usage (two terminals, from the repo root):
//   1. cd functions && npm install && npm run build
//   2. ANDROID_PACKAGE_NAME=com.jaimesorrow.wiccan \
//        firebase emulators:start --project wiccan-android
//   3. node scripts/e2e-emulator.mjs
//
// ANDROID_PACKAGE_NAME is required in step 2 for the account-deletion
// check: deleteUserData cancels the Play subscription first, and the
// functions fail closed without it (see the NOTE below).
//
// Seeds and deletes its own data in the emulator only. It never touches a
// real project: the Firestore/Auth hosts below are hardcoded to localhost,
// and firebase-admin refuses to reach production when they are set.
//
// App Check remains enforced in every deployed environment. The Functions
// emulator sets FUNCTIONS_EMULATOR=true, which is the one explicit local
// exception because Firebase does not provide an App Check emulator.

import { createRequire } from "node:module";

process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";
process.env.FIREBASE_AUTH_EMULATOR_HOST ??= "127.0.0.1:9099";
process.env.GCLOUD_PROJECT ??= "wiccan-android";

// NOTE: deleteUserData cancels the caller's Play subscription first, and
// createBillingRepository() in functions/src/index.ts throws when
// ANDROID_PACKAGE_NAME is unset -- deliberately failing closed rather than
// falling back to the production package in another environment. The
// functions run inside the emulator process, not this one, so that variable
// has to be exported when the emulator is STARTED (see usage above);
// setting it here would have no effect on them.

const PROJECT = process.env.GCLOUD_PROJECT;
const FN = `http://127.0.0.1:5001/${PROJECT}/us-central1`;
const AUTH = `http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1`;

const require = createRequire(new URL("../functions/package.json", import.meta.url));
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");

initializeApp({ projectId: PROJECT });
const db = getFirestore();

let pass = 0;
let fail = 0;
const ok = (name, cond, detail = "") => {
  if (cond) {
    pass++;
    console.log(`  PASS  ${name}`);
  } else {
    fail++;
    console.log(`  FAIL  ${name}${detail ? ` -- ${detail}` : ""}`);
  }
};

async function call(name, token, data = {}) {
  const res = await fetch(`${FN}/${name}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ data }),
  });
  return res.json();
}

const today = () => new Date().toISOString().slice(0, 10);

async function seed() {
  console.log("\n== seed ==");
  await db.collection("systemConfig").doc("global").set({
    dailyContentEnabled: true,
    notificationsEnabled: true,
  });
  const base = {
    type: "ritual",
    tradition: "Eclectic",
    category: "MoonAndSeasonal",
    intro: "intro",
    body: "body",
    tags: ["e2e"],
  };
  for (let i = 1; i <= 3; i++) {
    await db.collection("content").doc(`e2e-free-${i}`)
      .set({ ...base, tier: "free", status: "published", title: `Free lesson ${i}` });
  }
  await db.collection("content").doc("e2e-premium-1")
    .set({ ...base, tier: "premium", status: "published", title: "Premium lesson" });
  await db.collection("content").doc("e2e-draft-1")
    .set({ ...base, tier: "free", status: "draft", title: "Draft lesson" });
  console.log("  3 free + 1 premium + 1 draft published, systemConfig.global set");
}

async function newUser() {
  const res = await fetch(`${AUTH}/accounts:signUp?key=emulator`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: `e2e-${Date.now()}@test.local`,
      password: "password123",
      returnSecureToken: true,
    }),
  });
  const created = await res.json();
  const unverifiedToken = created.idToken;
  const verificationEmail = await call("sendVerificationEmail", unverifiedToken);
  // Verify the dedicated test user and refresh its ID token before requesting content.
  await getAuth().updateUser(created.localId, { emailVerified: true });
  const login = await fetch(AUTH + "/accounts:signInWithPassword?key=emulator", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: created.email, password: "password123", returnSecureToken: true }),
  });
  return { ...(await login.json()), unverifiedToken, verificationEmail };
}

async function main() {
  await seed();

  console.log("\n== auth ==");
  const { idToken: token, localId: uid, unverifiedToken, verificationEmail } = await newUser();
  ok("emulator issues an ID token", !!token);
  ok("branded verification callable creates a Firebase action link",
    verificationEmail?.result?.sent === true &&
    verificationEmail?.result?.devVerificationLink?.includes("oobCode="),
    JSON.stringify(verificationEmail).slice(0, 200));
  console.log(`  uid=${uid}`);

  console.log("\n== auth gate ==");
  const anon = await call("getTodayContent", null);
  ok("getTodayContent rejects unauthenticated callers", anon?.error?.status === "UNAUTHENTICATED");

  const unverified = await call("getTodayContent", unverifiedToken);
  ok("getTodayContent rejects unverified email accounts", unverified?.error?.status === "PERMISSION_DENIED");
  const unverifiedProfile = await call("completeOnboardingProfile", unverifiedToken, {
    firstName: "Unverified",
    lastName: "Account",
  });
  ok("profile completion rejects unverified email accounts",
    unverifiedProfile?.error?.status === "PERMISSION_DENIED");

  const probe = await call("getTodayContent", token);
  if (probe?.error?.status === "UNAUTHENTICATED") {
    console.log("\n  App Check is blocking every callable.");
    console.log("  The ID token above is valid -- the emulator log shows");
    console.log('  {"verifications":{"app":"MISSING","auth":"VALID"}} -- but each');
    console.log("  callable is declared enforceAppCheck: true and there is no App");
    console.log("  Check emulator to mint a token. See docs/FIREBASE_SETUP.md.");
    process.exit(2);
  }

  console.log("\n== getTodayContent ==");
  ok("signed-in account without a subscription is denied", probe?.error?.status === "PERMISSION_DENIED",
    JSON.stringify(probe).slice(0, 200));
  const forged = await call("getTodayContent", token, { tier: "premium", subscriptionExpiresAtMillis: Date.now() + 3_600_000 });
  ok("client entitlement fields cannot bypass subscription verification", forged?.error?.status === "PERMISSION_DENIED");
  // Emulator-only fixture, not a production entitlement or a Play sandbox purchase.
  await db.collection("users").doc(uid).set({
    tier: "premium", subscriptionExpiresAtMillis: Date.now() + 3_600_000,
  }, { merge: true });
  const invalidProfile = await call("completeOnboardingProfile", token, {
    firstName: "Line\nBreak",
    lastName: "Account",
  });
  ok("profile completion rejects control characters", invalidProfile?.error?.status === "INVALID_ARGUMENT");
  const profile = await call("completeOnboardingProfile", token, {
    firstName: "  Rowan  ",
    lastName: "Wilde",
  });
  ok("verified account can complete its profile", !profile?.error, JSON.stringify(profile).slice(0, 200));
  const profileDocument = await db.collection("users").doc(uid).get();
  ok("profile completion trims and stores the names",
    profileDocument.get("firstName") === "Rowan" && profileDocument.get("lastName") === "Wilde");
  for (const timezone of [null, 7, "", " UTC", "UTC ", "Not/A_Real_Zone", "x".repeat(256)]) {
    const invalid = await call("getTodayContent", token, { timezone });
    ok(`getTodayContent rejects invalid timezone ${JSON.stringify(timezone).slice(0, 60)}`,
      invalid?.error?.status === "INVALID_ARGUMENT");
  }
  const first = await call("getTodayContent", token);
  const firstId = first?.result?.id;
  ok("returns content for a verified subscriber", !!first?.result, JSON.stringify(first).slice(0, 200));
  const second = await call("getTodayContent", token);
  ok("repeat call returns the same content", firstId && firstId === second?.result?.id,
    `${firstId} vs ${second?.result?.id}`);
  for (const timezone of ["UTC", "America/Anchorage", "GMT+05:30", "UTC-03:30"]) {
    const zoned = await call("getTodayContent", token, { timezone });
    ok(`getTodayContent accepts timezone ${timezone}`, !!zoned?.result,
      JSON.stringify(zoned).slice(0, 200));
  }
  ok("subscription pool never serves draft content", firstId && firstId !== "e2e-draft-1", `got ${firstId}`);
  const hist = await db.collection("userContentHistory").doc(`${uid}_${today()}`).get();
  ok("history document is written", hist.exists);
  await db.collection("users").doc(uid).update({ subscriptionExpiresAtMillis: Date.now() - 1 });
  const expired = await call("getTodayContent", token);
  ok("expiry blocks the same-day history fast path", expired?.error?.status === "PERMISSION_DENIED");
  ok("expiry preserves delivery history", (await hist.ref.get()).exists);
  await db.collection("users").doc(uid).update({ subscriptionExpiresAtMillis: Date.now() + 3_600_000 });
  const renewed = await call("getTodayContent", token);
  ok("renewal restores the same committed assignment", renewed?.result?.id === firstId);

  console.log("\n== acceptTerms ==");
  const terms = await call("acceptTerms", token, { termsVersion: 3, privacyVersion: 3 });
  ok("acceptTerms succeeds", !terms?.error, JSON.stringify(terms).slice(0, 200));
  ok("users/{uid} exists afterwards", (await db.collection("users").doc(uid).get()).exists);

  console.log("\n== device registration and preferences ==");
  const fcm = "e2e-fcm-token";
  const oversized = await call("registerDeviceToken", token, { token: "x".repeat(4097), timeZone: "UTC" });
  ok("registerDeviceToken rejects oversized tokens", oversized?.error?.status === "INVALID_ARGUMENT");
  const invalidZone = await call("registerDeviceToken", token, { token: fcm, timeZone: "Not/A_Real_Zone" });
  ok("registerDeviceToken rejects invalid time zones", invalidZone?.error?.status === "INVALID_ARGUMENT");
  const reg = await call("registerDeviceToken", token, { token: fcm, timeZone: "America/New_York" });
  ok("registerDeviceToken succeeds", !reg?.error, JSON.stringify(reg).slice(0, 200));
  const devices = await db.collection("users").doc(uid).collection("devices").get();
  ok("device lands in users/{uid}/devices", devices.size === 1, `size=${devices.size}`);
  const tz = await call("updateTimeZone", token, { token: fcm, timeZone: "Europe/London" });
  ok("updateTimeZone succeeds", !tz?.error, JSON.stringify(tz).slice(0, 200));
  const notif = await call("setNotificationsEnabled", token, { token: fcm, enabled: false });
  ok("setNotificationsEnabled succeeds", !notif?.error, JSON.stringify(notif).slice(0, 200));

  console.log("\n== collection-group query ==");
  ok("collectionGroup('devices') resolves", (await db.collectionGroup("devices").get()).size >= 1);

  console.log("\n== verifyPurchase argument validation ==");
  const bad = await call("verifyPurchase", token, {});
  ok("rejects a call with no purchaseToken/productId", bad?.error?.status === "INVALID_ARGUMENT");

  console.log("\n== kill switch ==");
  await db.collection("systemConfig").doc("global")
    .set({ dailyContentEnabled: false, notificationsEnabled: true });
  const killed = await call("getTodayContent", token);
  ok("dailyContentEnabled=false blocks getTodayContent", killed?.error?.status === "UNAVAILABLE",
    JSON.stringify(killed).slice(0, 200));
  await db.collection("systemConfig").doc("global")
    .set({ dailyContentEnabled: true, notificationsEnabled: true });

  console.log("\n== account deletion ==");
  const del = await call("deleteUserData", token);
  ok("deleteUserData succeeds", !del?.error, JSON.stringify(del).slice(0, 200));
  ok("user document is removed", !(await db.collection("users").doc(uid).get()).exists);
  ok("devices subcollection is removed",
    (await db.collection("users").doc(uid).collection("devices").get()).size === 0);
  ok("verification email throttle is removed",
    !(await db.collection("emailVerificationRequests").doc(uid).get()).exists);

  console.log(`\n===== ${pass} passed, ${fail} failed =====`);
  process.exit(fail ? 1 : 0);
}

main().catch((err) => {
  console.error("\ne2e run failed:", err.message);
  console.error("Are the emulators running? firebase emulators:start --project wiccan-android");
  process.exit(1);
});

