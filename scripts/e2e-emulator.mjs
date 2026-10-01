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
// NOTE: every callable is declared `enforceAppCheck: true`, and the
// Functions emulator honors that - there is no App Check emulator to issue
// a token, so all 8 callables reject with UNAUTHENTICATED out of the box.
// This script detects that and explains it rather than reporting 13 bogus
// failures. See docs/FIREBASE_SETUP.md ("Local testing") for the options.

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
  return res.json();
}

async function main() {
  await seed();

  console.log("\n== auth ==");
  const { idToken: token, localId: uid } = await newUser();
  ok("emulator issues an ID token", !!token);
  console.log(`  uid=${uid}`);

  console.log("\n== auth gate ==");
  const anon = await call("getTodayContent", null);
  ok("getTodayContent rejects unauthenticated callers", anon?.error?.status === "UNAUTHENTICATED");

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
  const first = await call("getTodayContent", token);
  const firstId = first?.result?.id;
  ok("returns content for an authed user", !!first?.result, JSON.stringify(first).slice(0, 200));
  const second = await call("getTodayContent", token);
  ok("repeat call returns the same content", firstId && firstId === second?.result?.id,
    `${firstId} vs ${second?.result?.id}`);
  ok("free user is never served premium or draft content",
    firstId !== "e2e-premium-1" && firstId !== "e2e-draft-1", `got ${firstId}`);
  const hist = await db.collection("userContentHistory").doc(`${uid}_${today()}`).get();
  ok("history document is written", hist.exists);

  console.log("\n== acceptTerms ==");
  const terms = await call("acceptTerms", token, { termsVersion: 1, privacyVersion: 1 });
  ok("acceptTerms succeeds", !terms?.error, JSON.stringify(terms).slice(0, 200));
  ok("users/{uid} exists afterwards", (await db.collection("users").doc(uid).get()).exists);

  console.log("\n== device registration and preferences ==");
  const fcm = "e2e-fcm-token";
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

  console.log(`\n===== ${pass} passed, ${fail} failed =====`);
  process.exit(fail ? 1 : 0);
}

main().catch((err) => {
  console.error("\ne2e run failed:", err.message);
  console.error("Are the emulators running? firebase emulators:start --project wiccan-android");
  process.exit(1);
});
