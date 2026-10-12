# Google Play release compliance — Wiccan

Last audited: **2026-10-12** against the repository and Google Play's official
developer-policy/help pages. Recheck the linked policies immediately before
submission because Play requirements and Console questions change.

This is an engineering and release audit, not legal advice. The repository now
contains Wiccan-specific native Terms, Privacy, and Content & Safety screens,
but a qualified attorney/privacy reviewer and the legal app owner must approve
the final public documents.

## Release decision

**Not ready for public Play submission yet.** The code-level placement and
disclosures are implemented, but the following owner/Console work cannot be
truthfully completed from the repository:

- replace `wiccan.app` placeholder legal/deletion URLs and
  `support@wiccan.app` with real, public, monitored destinations;
- identify the legal developer/operator and privacy contact in the hosted policy;
- confirm the recommended **18+** target-audience selection and regional terms;
- complete Data Safety, content-rating, app-access, ads, and health declarations;
- provide a reusable reviewer account plus free review access to premium content;
- configure the subscription products, Play Developer API, RTDN, app signing,
  store listing, screenshots, and production Firebase/App Check settings;
- have a qualified reviewer approve the Terms, Privacy, rights, retention, and
  regional consumer/subscription language.

Do not mark these complete until the Play Console and public URLs can be opened
and verified from a signed-out browser/device.

## Curated Play Console answers

These answers reflect the current code. Re-audit if functionality or SDKs change.

| Console topic | Wiccan answer | Evidence / release note |
|---|---|---|
| Contains ads | **No** | No ads SDK or ad surface is present. |
| In-app purchases | **Yes — subscriptions** | Google Play Billing; monthly UI today, monthly/yearly product IDs accepted by the backend. |
| App access restricted | **Yes** | Sign-in and policy acceptance gate the app; premium content is paywalled. Supply reviewer instructions and credentials. |
| User-generated content | **No** | Users cannot post, message, upload, or share content. |
| Social/chat features | **No** | No user-to-user interaction. |
| User-facing generative AI | **No** | The app cannot generate content. An operator-only offline tool creates ancient-grimoire-grounded drafts for human review. This is an inference from the current Play definition; recheck if generation is ever exposed in-app. |
| News app | **No** | Historical/spiritual daily content, not news. |
| Government app | **No** | No government affiliation or service. |
| Health app declaration | **Recommended: No health features** | Wiccan is educational/spiritual reflection and disclaims diagnosis/treatment. Confirm that the store listing and every published lesson avoid health/medical claims; otherwise reassess. |
| Target audience | **Recommended: adults 18+ only** | Conservative fit for occult themes, potions/materials, and safety responsibility. The owner must confirm this selection; do not market to children. |
| Store category | **Recommended: Lifestyle** | Best fit for spiritual reflection and daily practice; owner may choose Books & Reference only if the listing is framed primarily as historical education. |
| Precise/approximate location | **No** | No location permission or location API. An IANA time-zone string is stored for notification timing and is not used to derive precise location. |
| Account creation | **Yes** | Email/password or Google sign-in. |
| Account deletion | **Yes** | In-app Settings flow exists; external deletion-request page is still a release blocker. |

### Content rating answers

Answer the live IARC wording honestly. Wiccan should disclose supernatural,
occult, witchcraft, spell, potion, and ritual themes even though the content is
educational/spiritual. Also disclose any references to fire, plants, substances,
or historical violence if the live published pool contains them. Current code
has no gambling, sexual content, profanity feature, user interaction, or
user-generated content. The rating authority, not this repository, assigns the
final rating.

### App-access review instructions

In Play Console, select that some/all functionality is restricted and provide:

1. a reusable email/password reviewer account that does not require personal
   information, one-time codes, location, or an expiring link;
2. steps: sign in → review/accept native policies → Today → Grimoire → Settings;
3. a way to review Premium without payment, such as a pre-entitled reviewer
   account or Google-approved license-testing configuration;
4. a note that the policies are reachable before sign-in, at the blocking
   acceptance gate, in Settings, and from the subscription screen;
5. English instructions and any special Firebase/App Check test-track steps.

Never place production administrator credentials in the review instructions.

## Data Safety declaration

Google requires the declaration to include data handled by third-party SDKs,
including pseudonymous data. Transfers to a qualifying service provider may not
count as “sharing” under Play's definition, but the data is still “collected.”
The owner must verify contracts and the exact current form before submitting.

Recommended rows for the current build:

| Play data type | Collected | Shared | Required / optional | Purposes |
|---|---:|---:|---|---|
| Email address | Yes | No* | Required for email auth; Google account choice is optional | Account management, app functionality, security |
| User IDs | Yes | No* | Required | Account management, app functionality, fraud prevention/security |
| Purchase history | Yes | No* | Optional (only subscribers/restores) | App functionality, account management, fraud prevention/security |
| App interactions | Yes | No* | Required by current analytics setup | Analytics, app functionality |
| Crash logs | Yes | No* | Required by current Crashlytics setup | Analytics, app functionality |
| Diagnostics | Yes | No* | Required by current Crashlytics/App Check setup | Analytics, fraud prevention/security |
| Device or other IDs | Yes | No* | FCM/App Check identifiers are handled by current SDKs | App functionality, security, developer communications |
| Precise/approximate location | No | No | Not collected | — |
| Contacts, messages, photos/videos, audio, files | No | No | Not collected | — |
| Health/fitness data | No | No | Not collected | — |
| Payment card/bank details | No | No | Handled by Google Play, not Wiccan | — |

`*` “No sharing” assumes Google/Firebase processing qualifies for Play's service-
provider exception and no undisclosed export is configured. Verify this rather
than copying the answer blindly.

Additional form answers:

- **Encrypted in transit:** Yes, for the app/Firebase/Play paths in this repo.
- **Users can request deletion:** Yes, after the public web request page is live.
- **Independent security review:** Do not claim one unless a qualifying review
  has actually been completed and remains valid.
- **Data sale:** No.
- **Behavioral advertising:** No.

Explicit Analytics events are `sign_up_completed`, `daily_content_viewed`,
`paywall_viewed`, `subscription_started`, `subscription_lapsed`, and
`account_deleted`; Firebase SDK automatic events/identifiers must also be
included in the final declaration.

## Privacy and account deletion

Google Play requires a public, non-geofenced, non-PDF privacy-policy URL in the
store listing and in the app. It must identify the developer/contact, describe
collection/use/sharing, security, retention, and deletion. The account-deletion
requirement needs both:

- **in app:** Settings → Delete Account; and
- **outside app:** a working public page where a user can request deletion
  without reinstalling Wiccan.

The current deletion implementation first cancels every known active Google Play
auto-renewal. If Play cancellation fails, deletion stops so the user can retry;
it does not silently remove the local account while billing continues. Once
confirmed, it deletes delivery history, usage reservations, devices, subscription
records, profile, and Firebase Auth account. Limited pseudonymous purchase-token
and account hashes may remain for anti-fraud/anti-replay purposes. The hosted
policy must state the purpose and retention basis for those hashes.

## Terms, user responsibilities, and screen placement

Native policy version 2 includes Wiccan-specific terms and disclosures:

- historical, educational, and spiritual-reflection purpose;
- no guaranteed supernatural outcomes;
- no medical, mental-health, legal, financial, or emergency advice;
- no ingestion, inhalation, topical use, burning, or mixing of unknown or
  hazardous materials; fire, ventilation, allergy, poison-control, wildlife,
  property, consent, and local-law responsibilities;
- AI-assisted drafts are limited to vetted ancient-grimoire records and require
  human source, duplication, safety, and rights review before publication;
- recurring Play billing, localized price/period, cancellation, continued access
  through the paid period, refunds, restoration, and account-deletion effects;
- privacy categories, processors, no sale/no ads, security, retention, deletion,
  and pseudonymous anti-fraud retention.

Placement is intentionally layered:

| Location | Purpose |
|---|---|
| Sign-in / create account | Terms and Privacy are viewable before account creation; copy explains that acceptance follows. |
| Post-auth policy gate | Blocks the app until the current Terms are accepted and Privacy/Safety are acknowledged. |
| Paywall | Terms and Privacy sit beside the localized recurring price and renewal/cancellation disclosure. |
| Settings | Permanent access to Terms, Privacy, Content & Safety, support, subscription management, and account deletion. |

Android and Functions policy constants are both version `2`; any future material
change must bump the applicable versions together.

## Subscription compliance

The paywall must keep all of these visible before purchase:

- localized Google Play price and **per month** billing period;
- automatically renews until canceled;
- Play charges the payment method on confirmation;
- cancellation through Google Play and normal access through the paid period;
- Terms and Privacy links;
- clear premium benefits and a working Restore Purchases action;
- free daily content remains usable without subscribing.

The current UI purchases only `wiccan_premium_monthly`. The backend also
allow-lists `wiccan_premium_yearly`, but do not advertise annual pricing until a
real annual offer is exposed and its localized price/period are shown in-app.
Repository planning prices are not authoritative; Play Console localized prices
are authoritative.

## Permissions and SDK audit

The main manifest requests only:

- `INTERNET` — Firebase, authentication, content, billing verification;
- `POST_NOTIFICATIONS` — runtime permission for optional daily notifications.

No location, contacts, camera, microphone, storage/media, health, SMS, phone, or
accessibility permission is requested. Re-run this audit whenever a dependency,
manifest, SDK, or data flow changes.

SDKs that affect the Data Safety form: Firebase Auth, Firestore, Functions,
Messaging, Analytics, Crashlytics, App Check/Play Integrity, Google Credential
Manager/Google ID, and Google Play Billing.

## Store listing and technical release checklist

- [ ] 512×512 PNG app icon and 1024×500 feature graphic (current launcher art
  is not proof of store readiness).
- [ ] At least two current phone screenshots and accurate short/full descriptions.
- [ ] “Contains ads” set to No and “In-app purchases” displayed.
- [ ] Production signing/App Signing configured; version code incremented.
- [ ] Current target API policy rechecked. Repo currently uses `targetSdk = 36`.
- [ ] Production Firebase, App Check/Play Integrity, Auth certificates, Functions,
  Firestore rules/indexes, content, billing, RTDN, and alerts verified.
- [ ] Closed/internal-track real-device pass: Google/email auth, policy gate,
  Today, Grimoire, notifications, monthly purchase, restore, cancel, lapse,
  account deletion, and external deletion request.
- [ ] Store listing claims compared against every published content item.
- [ ] Rights/source review complete for published ancient-grimoire content.

## Official Google sources

- [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Data Safety form guidance](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Subscriptions policy](https://support.google.com/googleplay/android-developer/answer/9900533)
- [Prepare an app for review / App content](https://support.google.com/googleplay/android-developer/answer/9859455)
- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [AI-generated content policy](https://support.google.com/googleplay/android-developer/answer/13985936)
- [Health apps declaration](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Google Play Developer Program Policies](https://play.google.com/about/developer-content-policy/)

Also follow [PRODUCTION_SETUP.md](PRODUCTION_SETUP.md),
[RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md), and
[FIREBASE_SETUP.md](FIREBASE_SETUP.md).
