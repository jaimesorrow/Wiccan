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

# terminal 3: build/install the Android emulator client
./gradlew :app:installProdDebug -PuseFirebaseEmulators=true
adb shell pm clear com.jaimesorrow.wiccan
```

Emulator UI: <http://127.0.0.1:4000>. Ports: auth 9099, functions 5001,
firestore 8080, pubsub 8085. The emulator logs the mystical verification
message's one-time action link because local emulators intentionally do not
deliver real email.

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

### App Check and Android emulator routing

Firebase does not provide an App Check emulator. Callable options therefore
disable App Check only when the Functions emulator itself supplies
`FUNCTIONS_EMULATOR=true`; deployed functions never set that variable and
continue to enforce App Check. `scripts/release-preflight.sh` locks in this
fail-closed production condition.

The Android app routes Auth, Firestore, and Functions to `10.0.2.2` only for
an explicitly opted-in debug build:

```bash
./gradlew :app:installProdDebug -PuseFirebaseEmulators=true
adb shell pm clear com.jaimesorrow.wiccan
```

Before launching after **every change between emulator routing and normal
Firebase**, clear app data with the command above. Both modes install as
`com.jaimesorrow.wiccan`, so replacing the APK preserves Firebase Auth state
from the previous backend. Clearing data signs you out and removes local
settings and cached content; use a test device, then sign in against the
selected backend. The same reset is required when switching back:

```bash
./gradlew :app:installProdDebug
adb shell pm clear com.jaimesorrow.wiccan
```

Release builds hardcode emulator routing off. Emulator-mode debug builds
also skip App Check installation, FCM token registration, Analytics, and
Crashlytics collection so a local test does not mix emulated user data with
production-only Firebase services. Without the Gradle property, an ordinary
debug build keeps its normal Firebase behavior.

The end-to-end suite covers authenticated and unauthenticated `getTodayContent`;
daily-assignment stability across repeat calls (same content twice); subscription
gating (inactive accounts receive no content, even through existing same-day history);
expiry and renewal with preserved history; rejection of client-forged access; the
`userContentHistory` write; `acceptTerms` and the `users/{uid}` document;
`registerDeviceToken` and the `users/{uid}/devices` subcollection; the
`devices` collection-group query; `verifyPurchase` argument validation; the
`dailyContentEnabled` kill switch; and the `deleteUserData` cascade. Its
current pass count is recorded by each verified run rather than hardcoded in
this guide.

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

Existing installations may have delivery histories created before
`userContentUsage` markers were introduced. Before deploying the restricted
content-read rules, backfill those markers with the authenticated admin tool
described in [PRODUCTION_SETUP.md](PRODUCTION_SETUP.md). Without that migration,
legacy Archive entries cannot be read by their owners. Content collection listing
is denied to clients; only server-delivered content IDs are readable.

```bash
firebase deploy --only firestore:rules,firestore:indexes --project production
```

The ancient-grimoire content agent writes through the Admin SDK, not the
client rules. Every generated document is stored in `content` with
`status = draft`, `rightsReviewRequired = true`, `generatedBy`,
`generationSourceKind`, and a server-generated `generatedAt` timestamp.
The Firestore emulator suite verifies that storage contract. No extra index is
needed for draft creation or the agent's case-insensitive duplicate-title scan.
Deploying rules and indexes does not generate, import, or publish content.

## 3. Authentication

**CONSOLE — verified 2026-10-06 UTC.** **Google** and **Email/Password**
are enabled in `wiccan-android`. For another project, enable them under
Authentication → Sign-in method; Google also requires a project support email.

**REPO.** `app/src/prod/google-services.json` includes the public web OAuth
client ID read from Firebase's Google provider configuration as a
`client_type: 3` entry. The existing project, Android app and API key fields
are retained; this change is not a complete replacement SDK download.

`AuthRepositoryImpl` resolves `default_web_client_id` by name at runtime
and hides the Google button when that resource is absent. The Google
Services plugin generates it from the web OAuth entry. Verify resource
generation with:

```bash
./gradlew :app:processProdDebugGoogleServices
grep default_web_client_id \
  app/build/generated/res/processProdDebugGoogleServices/values/values.xml
```

A match confirms that the build contains the web client ID; it does not
verify a successful sign-in. Check that the installed build's SHA-1 matches
a registered Firebase Android certificate, including the Play App Signing
certificate for builds distributed by Play, then test Google sign-in on a
device. SHA-1 and SHA-256 certificates are present in Firebase, but their
relationship to the distributed build has not been verified. Re-download
`google-services.json` when changing provider or certificate registrations.

`onUserCreated` is a v1 Auth trigger (`authV1.user().onCreate`) and
deploys as a 1st-gen function; no extra console setup.

### Branded verification email

**REPO.** Email account creation and resend now call `sendVerificationEmail`
instead of Firebase Auth's stock sender. The callable derives the recipient
from the authenticated user, generates a single-use Firebase action link, rate
limits requests in `emailVerificationRequests/{uid}`, and sends the
Wiccan-branded moon-phase HTML/text message. Clients cannot select a recipient,
read the rate-limit collection, or turn the function into an open relay.

**CONSOLE / CLI.** Production delivery needs an SMTP provider with a verified
sender domain:

```bash
# Store the full SMTP connection URL in Secret Manager. Do not put it in .env.
firebase functions:secrets:set SMTP_URL --project production

# In functions/.env.wiccan-android (gitignored):
SMTP_FROM=Wiccan <noreply@your-verified-domain.example>

firebase deploy --only functions:sendVerificationEmail --project production
```

Publish SPF and DKIM records supplied by the SMTP provider, and add a DMARC
record before production traffic. In Firebase Console → Authentication →
Settings → Authorized domains, keep the Firebase action-handler domain enabled.
Password-reset and change-email messages still use Firebase's built-in sender;
customize those separately under Authentication → Templates so their visible
name, reply-to address and wording match Wiccan.

No Google Play Console product, OAuth consent-screen, API-key, or Android
package change is required for the email template. Google Cloud only needs the
Functions runtime's Secret Manager access, which Firebase grants when the
secret is bound during deployment.

## 4. App Check

**CONSOLE.** Register the Android app with **Play Integrity**. Until then a
real (non-emulator) build gets `UNAUTHENTICATED` on the protected callables.

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
- `SMTP_FROM` — visible From header for the branded verification message. The
  corresponding `SMTP_URL` is stored with `firebase functions:secrets:set`, not
  in this file.
- For deployed Functions, grant the **runtime service account** access to this
  app through Play Console and enable the Android Publisher API. The repository
  uses Application Default Credentials, so a deployed function receives its
  runtime identity automatically. A local service-account file path in `.env`
  does not upload that key or configure a deployed runtime.
- For local admin scripts, connect an authorized Google Cloud identity or set
  `GOOGLE_APPLICATION_CREDENTIALS` to a service-account file outside this repo.
  Firestore seeding needs Firestore access; local Play API calls additionally
  need Play Console access. Never commit the key or print its contents.

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

- `PolicyUrls` (`app/src/main/kotlin/com/jaimesorrow/wiccan/core/config/PolicyUrls.kt`)
  points at unhosted `wiccan.app` URLs. Play rejects listings with
  placeholder legal URLs, and the Data Safety form needs a publicly
  reachable account-deletion page.
- `handlePlayRtdn` and `verifyPurchase`'s field shapes have never run
  against a real Play Console — they need a real purchase token, which no
  emulator can mint.
- `dev` and `staging` flavors still have no Firebase project of their own.
