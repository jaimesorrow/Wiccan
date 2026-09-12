# Incident response

How to triage the specific failure modes this codebase can actually
produce. Each entry names the real function/collection involved and the
real command to check it — not generic "check your logs" advice.

## Where to look first

- `firebase functions:log` (or Cloud Console → Cloud Functions → Logs) —
  every callable and scheduled job in `functions/src/index.ts` logs via
  `firebase-functions/logger` on failure, with `uid` and `error.message`
  as structured fields.
- Firebase Console → Crashlytics — client-side crashes (Crashlytics is
  wired in `app/build.gradle.kts` via the `firebase-crashlytics-ktx`
  dependency and Gradle plugin).
- Firebase Console → App Check → Metrics — a spike in rejected requests
  usually means either a real attack or a legitimate client (e.g. a new
  app version) failing attestation.

## "Users report they aren't receiving daily notifications"

1. Check `sendDailyNotificationsJob` (`functions/src/index.ts`) ran on
   schedule — it fires hourly and only notifies devices whose *local*
   time currently reads 9am (`getUserLocalHour`, `functions/src/dateUtils.ts`).
   A single missed hourly run only delays affected users by up to an hour,
   not a full day.
2. Check `users/{uid}/devices/{deviceId}` has a current token and
   `timeZone` for the affected user — a stale/cleared token
   (`clearToken`, fired when FCM reports permanent invalidity) means the
   device silently stopped receiving pushes until it next calls
   `registerDeviceToken`.
3. Check the job's own log line (`sendDailyNotifications complete`,
   logged with `{ attempted, sent, skippedStale, failed }`) — a high
   `failed` count with a low `attempted` count points at a systemic issue
   (FCM outage, a bad service account) rather than one user's device.

## "A user reports they were charged after deleting their account"

1. This should not happen as of the `cancelActiveSubscription` step in
   `functions/src/deleteAccount.ts` — but `cancelSubscription`
   (`googlePlayBillingRepository.ts`) swallows and only logs a failed Play
   API call (by design: never block the rest of account deletion on a
   billing hiccup). Search logs for `cancelActiveSubscription failed` with
   the affected uid.
2. If found: manually cancel the subscription via Play Console
   (Order management) or the Play Developer API's
   `purchases.subscriptions.cancel`, using the token from the last known
   `subscriptions/{uid}` state (if the record hadn't been deleted yet) or
   Play Console's own purchase history.
3. Refund via Play Console if the user was charged after their deletion
   request — Google, not this app, processes the actual charge/refund.

## "Play Billing verification is failing / entitlements look wrong"

1. `verifyPurchase` (`functions/src/verifyPurchase.ts`) and
   `recheckLapsedSubscriptionsJob` both call
   `GooglePlayBillingRepository.getSubscriptionState`
   (`purchases.subscriptionsv2.get`) — a failure here is almost always
   either (a) the Cloud Functions service account isn't linked as an
   authorized API access account in Play Console (Users and permissions →
   API access), or (b) `GOOGLE_APPLICATION_CREDENTIALS` isn't set/valid
   for the environment (see `functions/.env.example`).
2. Check `tierPolicy.ts`'s `tierForSubscription` logic if entitlement
   looks *wrong* rather than *absent* — a canceled-but-not-yet-expired
   subscription is deliberately still `premium` until `expiryTimeMillis`;
   this is correct behavior, not a bug, unless the expiry date itself is
   wrong (which points back at (1)).

## "Generated content published something unsafe or off-brand"

1. If it's still a **draft**: delete `content/{id}` directly, nothing else
   references it. `approveDraft`'s safety gate
   (`functions/src/approveDraft.ts`) already blocks publishing anything
   missing a title/intro/body/tags or a `potion` without a `safetyNote` —
   if something got through anyway, that gate is the first place to check
   for a regression.
2. If it's **published**: set its `status` back to `"draft"` in Firestore
   (or delete it) to stop future delivery; see `docs/DISASTER_RECOVERY.md`
   for why past deliveries can't be un-sent.
3. Check `generateDrafts`' duplicate-title guard
   (`functions/src/generateDraft.ts`) and `titleExists`
   (`functions/src/firestoreContentAdminRepository.ts`) if the concern is
   a batch quietly producing near-duplicate content, not a single bad item.

## "App Check is rejecting legitimate traffic" (spike in 403s from callables)

1. Every user-facing callable in `index.ts` has `{ enforceAppCheck: true }`
   — a rejection means the client's App Check token failed attestation.
2. Debug builds use `DebugAppCheckProviderInstaller`
   (`app/src/debug/kotlin/.../appcheck/DebugAppCheckModule.kt`), which
   requires the install's debug token to be manually allow-listed in
   Firebase Console (App Check → Apps → manage debug tokens) — a fresh
   debug install with an unregistered token is expected to fail until
   that's done, not a bug.
3. Release builds use `ReleaseAppCheckProviderInstaller` (Play Integrity)
   — a systemic rejection spike on release traffic usually means either a
   Play Integrity outage (check Google's status page) or the app's
   package name/SHA-256 signing cert fingerprint isn't registered
   correctly for this Firebase project's App Check configuration.

## "The scheduled subscription recheck or notification job isn't running at all"

- Both are `onSchedule` (2nd-gen) functions in `functions/src/index.ts` —
  confirm they're actually deployed (`firebase functions:list`) and that
  Cloud Scheduler shows an active job for each (Cloud Console → Cloud
  Scheduler). A function that exists in code but was never deployed after
  being added produces no error, just silence.
