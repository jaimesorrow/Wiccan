# Play Console readiness package

Everything here is grounded in what the code actually does — checked
against the manifest, Firestore/Functions data model, and billing
implementation.

## Canonical Play identity

- Product name: **Wiccan**
- Play/Firebase Android package: **`com.nyxwell.app`**
- Play Billing Library: **9.1.0**
- Subscription product ID: **`wiccan_premium_monthly`**

Do not create the Play listing under `com.wiccan.app`; that is the Kotlin
source namespace, not the installed application ID.

## Data Safety section

Data types this app actually collects or transmits, per the real code:

| Data type | Collected? | Where / why |
|---|---|---|
| Email address | Yes | Firebase Auth (Google sign-in or email/password) — account identity only |
| User IDs | Yes | Firebase Auth uid, used as the Firestore account identity |
| Purchase history | Yes | Google Play Billing subscription state, verified server-side and stored in `subscriptions/{uid}` for entitlement/audit |
| Device or other IDs | Yes | FCM registration token, stored per-device in `users/{uid}/devices` for push delivery |
| App activity (content viewed) | Yes | `userContentHistory` — which spell/potion/lesson was delivered to which user on which day |
| Precise or approximate location | No | Not requested or read; an IANA timezone string may be stored for notification scheduling |
| Contacts, photos, videos, audio, financial info beyond purchase, health/fitness | No | Never requested or read |

Notes for the Data Safety form:
- Data is encrypted in transit through Firebase/Google APIs.
- Account deletion: the in-app flow invokes `deleteUserData`; the backend removes profile/history/devices/subscription records and the Auth account, and attempts to cancel an active Play subscription before erasing its local audit record.
- Google also requires a public external deletion resource; `PolicyUrls.ACCOUNT_DELETION_URL` is still a placeholder and must be replaced before submission.

## Content rating questionnaire

Wiccan contains spells, potions, folklore-inspired lessons, and modern
Wiccan practice framed as spirituality/lifestyle content. The generation
and approval pipeline prohibits medical treatment claims, unsafe ingestion,
hazardous instructions, and guaranteed supernatural outcomes. There is no
user-generated content, social feed, chat, or advertising.

The owner must answer the current Play/IARC questionnaire truthfully based
on the final shipped content and target audience.

## Store listing requirements

- [ ] App icon (512×512 PNG) and feature graphic (1024×500).
- [ ] At least 2 phone screenshots; use final production UI.
- [ ] Short description (≤80 chars) and full description.
- [ ] Replace `PolicyUrls.PRIVACY_POLICY` placeholder with a real public Privacy Policy URL.
- [ ] Replace `PolicyUrls.TERMS_OF_SERVICE` placeholder with a real public Terms URL.
- [ ] Replace `PolicyUrls.ACCOUNT_DELETION_URL` with a public deletion page usable without installing the app.
- [ ] Replace `PolicyUrls.SUPPORT_EMAIL` with a real monitored support address.
- [ ] Complete category, target audience, content rating, Data Safety, health, financial-features, and app-access declarations as applicable in the current Console.

## Firebase / signing identity

Before any Play-distributed test:

- [ ] Register Android app `com.nyxwell.app` in the correct Firebase project.
- [ ] Add debug/release/upload SHA fingerprints as appropriate.
- [ ] After enabling Play App Signing, add the Play App Signing SHA-1/SHA-256 fingerprints to Firebase/OAuth/App Check configuration.
- [ ] Download the matching production `google-services.json`.
- [ ] Configure Play Integrity App Check for the production app.

## Subscriptions / Monetization setup

- [ ] Create `wiccan_premium_monthly` with the intended monthly auto-renewing base plan.
- [ ] Confirm localized price/frequency disclosure in the Paywall from live ProductDetails.
- [ ] Link the Google Play Developer API service account for package `com.nyxwell.app`.
- [ ] Set `ANDROID_PACKAGE_NAME=com.nyxwell.app` in the deployed Functions environment.
- [ ] Point Play RTDN at `PLAY_RTDN_TOPIC` (`play-rtdn` by default) and grant Play permission to publish.
- [ ] Use license testers/internal testing to verify purchase, acknowledgement, restore, cancellation-through-expiry, grace/hold/expiry/revoke behavior.
- [ ] Verify RTDN end-to-end: Play → Pub/Sub → `handlePlayRtdn` → server entitlement update.

## Automated preflight

Run:

```bash
bash scripts/play-preflight.sh
```

For the final external release gate:

```bash
STRICT_EXTERNAL=1 bash scripts/play-preflight.sh
```

The strict mode intentionally fails while Firebase/legal/support placeholders
remain.

## Cross-reference

See `docs/RELEASE_CHECKLIST.md` for signing, build, device-test, migration,
backup, and release-bundle verification.
