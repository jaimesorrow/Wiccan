# Firebase backend setup (wiccan-android)

What it takes to run the Cloud Functions backend locally, and what is still
owed on the real `wiccan-android` project before the app can rely on it.

Steps marked **CONSOLE** need a browser signed in as the project owner —
they cannot be done from this repo. **REPO** steps are already done and are
recorded here so the state is auditable.

## Local testing with the emulator suite

The whole backend runs locally with no Blaze plan, no credentials, and no
risk to production data. Configured under `emulators` in `firebase.json`.

```bash
# terminal 1
cd functions && npm install && npm run build
cd .. && ANDROID_PACKAGE_NAME=com.jaimesorrow.wiccan \
  firebase emulators:start --project wiccan-android

# terminal 2
node scripts/e2e-emulator.mjs
```

Emulator UI: <http://127.0.0.1:4000>. Ports: auth 9099, functions 5001,
firestore 8080, pubsub 8085. All twelve functions initialize.

Rebuild (`npm run build`) after editing anything under `functions/src` — the
emulator serves compiled output from `functions/lib`.

### Why ANDROID_PACKAGE_NAME is needed to start the emulator

`createBillingRepository()` in `functions/src/index.ts` throws when
`ANDROID_PACKAGE_NAME` is unset, deliberately failing closed rather than
falling back to the production package in another environment
(`scripts/release-preflight.sh` enforces the absence of such a fallback).

`deleteUserData` cancels the caller's Play subscription before erasing
their data, so it hits that path. Start the emulator without the variable
and account deletion fails with `INTERNAL` while everything else passes —
that is correct fail-closed behaviour, not a bug. The functions run inside
the emulator process, so the variable must be exported when the emulator
is **started**; setting it in the test script has no effect on them.

### App Check blocks the callables by default

All 8 callables are `enforceAppCheck: true`, the Functions emulator honors
that, and there is no App Check emulator to mint a token — so every
callable returns `UNAUTHENTICATED` even with a valid ID token. The emulator
log shows the split precisely:

```
{"verifications":{"app":"MISSING","auth":"VALID"}}
```

`scripts/e2e-emulator.mjs` detects this case and explains it rather than
reporting a wall of bogus failures. Two ways forward, neither adopted:

- Scope enforcement to non-emulator runs —
  `enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== "true"`. The
  Functions emulator sets `FUNCTIONS_EMULATOR=true` and deployed functions
  never do, so production enforcement would be unchanged. It is still a
  deliberate weakening of a hardened control in one environment, so it
  needs an explicit decision rather than a quiet default.
- Drive the flows from a real debug build with an App Check debug token
  registered (§4 below), which tests the real client path but cannot run in
  CI.

With enforcement isolated locally, the suite passes **18/18** against
`main`, covering: authenticated and unauthenticated `getTodayContent`;
daily-assignment stability across repeat calls (same content twice); tier
gating (a free user is never served premium or draft content); the
`userContentHistory` write; `acceptTerms` and the `users/{uid}` document;
`registerDeviceToken` and the `users/{uid}/devices` subcollection; the
`devices` collection-group query; `verifyPurchase` argument validation; the
`dailyContentEnabled` kill switch; and the `deleteUserData` cascade.

That covers three things `docs/STATUS.md` lists as never exercised against
a real Firestore: the collection-group device query, the daily-assignment
transaction, and the deletion cascade.

## 1. Billing plan

**CONSOLE.** Cloud Functions will not deploy on the free Spark plan.
Upgrade `wiccan-android` to **Blaze**, and set a budget alert at the same
time — `sendDailyNotificationsJob` (hourly) and
`recheckLapsedSubscriptionsJob` (daily) run whether or not anyone uses the
app.

## 2. Firestore

**CONSOLE.** Firestore Database → Create database. The region is permanent
and Cloud Functions default to `us-central1`, so `nam5`/`us-central` avoids
a cross-region hop on every call.

**REPO.** Rules and indexes are written and ready:

```bash
firebase deploy --only firestore:rules,firestore:indexes --project production
```

## 3. Authentication

**CONSOLE.** Enable **Google** and **Email/Password** under Authentication
→ Sign-in method. Enabling Google requires selecting a project support
email; without it the save does not complete.

The app no longer fails to build when this is off — `AuthRepositoryImpl`
resolves `default_web_client_id` by name at runtime and hides the Google
button when absent. But until the provider is enabled, `oauth_client` in
`app/src/prod/google-services.json` stays `[]` and Google sign-in is
simply unavailable. Verify with:

```bash
./gradlew :app:processProdDebugGoogleServices
grep default_web_client_id \
  app/build/generated/res/processProdDebugGoogleServices/values/values.xml
```

A match means a web OAuth client exists. Re-download
`google-services.json` after enabling the provider or adding a SHA-1.

`onUserCreated` is a v1 Auth trigger (`authV1.user().onCreate`) and
deploys as a 1st-gen function; no extra console setup.

## 4. App Check

**CONSOLE.** Register the Android app with **Play Integrity**. Until then a
real (non-emulator) build gets `UNAUTHENTICATED` on all 8 callables.

For debug builds the app installs the debug provider
(`app/src/debug/.../DebugAppCheckModule.kt`). Run the debug build once,
copy the token it logs, and add it under App Check → Manage debug tokens.
This is per-install and must be repeated per device. A debug token bypasses
attestation — treat it as a secret and never commit it.

## 5. Cloud Functions secrets

**LOCAL.** Create `functions/.env.wiccan-android` (gitignored;
`functions/.env.example` is the template):

- `ANTHROPIC_API_KEY` — required by `generateDraft` / `npm run generate-draft`.
- `ANDROID_PACKAGE_NAME` — **required**, no default. `com.jaimesorrow.wiccan`
  for production; dev/staging must use their own IDs.
- `GOOGLE_APPLICATION_CREDENTIALS` — service account JSON with Google Play
  Developer API access. Required by `verifyPurchase`,
  `recheckLapsedSubscriptionsJob`, `handlePlayRtdn`, and `deleteUserData`'s
  subscription cancellation.

## 6. Play RTDN (Pub/Sub)

**CONSOLE.** `handlePlayRtdn` subscribes to the topic named by
`PLAY_RTDN_TOPIC` (default `play-rtdn`).

1. Create a Pub/Sub topic `play-rtdn` in the `wiccan-android` GCP project.
2. Play Console → Monetization setup → Real-time developer notifications →
   point it at `projects/wiccan-android/topics/play-rtdn`.
3. Grant Google Play's service account publish rights on the topic.

## 7. Deploy

```bash
cd functions && npm install && npm run build && npm test
cd .. && firebase deploy --project production
```

Name `production` explicitly. `.firebaserc`'s `default` and `staging`
aliases are still unresolved placeholders, so a bare `firebase deploy`
fails rather than silently shipping to production.

## Still outstanding after all of the above

- `PolicyUrls` (`app/src/main/kotlin/com/wiccan/app/core/config/PolicyUrls.kt`)
  points at unhosted `wiccan.app` URLs. Play rejects listings with
  placeholder legal URLs, and the Data Safety form needs a publicly
  reachable account-deletion page.
- `handlePlayRtdn` and `verifyPurchase`'s field shapes have never run
  against a real Play Console — they need a real purchase token, which no
  emulator can mint.
- `dev` and `staging` flavors still have no Firebase project of their own.
