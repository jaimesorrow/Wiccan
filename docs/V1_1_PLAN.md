# Wiccan v1.1 — first post-launch update

Status: plan, written 2026-11-21 against `main` at `e648602`. Nothing here ships before 1.0 (versionCode 2) is live on Play.
Owner decisions so far: yearly plan **$39.99** (USD 39,990,000 micros), a **standalone** plan alongside the $5.99 monthly plan (t101u).
Code for items 1 and 2 is on branch `feat/v1.1-monetization` (draft PR, do not merge before 1.0 ships).

Voice rule throughout: the app's own copy uses the mystical voice; legal, billing, price, trial, renewal, cancellation, safety, delete-account and verification text stays plain.

Ownership: Betelgeuse = app UI, Beta = tests, Decca = deploys (hosting/functions/rules/indexes) and the Play upload, Reviewbot = PR reviews and Play Console reads/writes, Jaime = merchant account, prices and anything legal.

Policy versions today: `PolicyVersions.kt` and `functions/src/policyVersions.ts` are **4/4** on `main`; PR #190 moves them to **5/5**. Anything below that changes Terms or Privacy text means **v6** after #190 merges. Batch every v1.1 legal change into **one** v6 bump so users re-accept once.

---

## 1. Yearly plan

`wiccan_premium_yearly`, US $39.99, standalone.

### What exists today
- `app/.../billing/WiccanBillingClient.kt` already declares `PREMIUM_YEARLY_PRODUCT_ID = "wiccan_premium_yearly"` in `PREMIUM_PRODUCT_IDS`, but `ProductDetails.subscriptionOffer()` returned `null` for any product other than monthly. It accepted only `P1M`, infinite recurrence, and USD exactly `5_990_000` micros, plus an optional free trial phase of `P7D`/`P1W`.
- `SubscriptionRepositoryImpl` loaded and launched only `PREMIUM_MONTHLY_PRODUCT_ID`. Restore/verify already passes either product ID (`verifyPurchase` filters `PREMIUM_PRODUCT_IDS`).
- Paywall (`ui/paywall/PaywallScreen.kt`, `PaywallViewModel.kt`, `PaywallContract.kt`) showed one monthly price.
- Server: `functions/src/tierPolicy.ts` `PREMIUM_PRODUCT_IDS` already contains `wiccan_premium_yearly`; `verifyPurchase.ts`, `handleRtdn.ts`, `recheckLapsedSubscriptions.ts`, `googlePlayBillingRepository.ts` and `firestoreAccountDeletionRepository.ts` all work off that set. There is already a Jest test "accepts the yearly premium product".
- Terms (`LegalDocuments.kt` "Subscriptions and billing", `hosting/terms.html:56-57`) say "an automatically renewing **monthly** subscription with a US base price of $5.99" and "cancel before its end to avoid the first **monthly** charge".
- `hosting/index.html:114` FAQ says "Google Play bills monthly"; `docs/PLAY_STORE_LISTING.md` full description is period-neutral.

### App changes (done in the draft PR)
- `SubscriptionPlan { MONTHLY, YEARLY }`; `SubscriptionOffer` gains `plan`, `priceAmountMicros`, `priceCurrencyCode` (defaults keep old call sites valid).
- `subscriptionOffer()` validates per product: monthly = `P1M` + USD 5,990,000 (unchanged); yearly = `P1Y` + USD 39,990,000 (`PREMIUM_YEARLY_USD_PRICE_MICROS`). Any other USD price fails closed; non-USD prices are Play's localized prices and are shown as Play formats them.
- `SubscriptionRepository.getYearlyOffer()` returns `null` if Play has no valid yearly product, so a missing yearly plan never breaks the monthly checkout. `launchPurchaseFlow(activity, plan = MONTHLY)` uses the offer loaded for that plan and account.
- `billing/PlanValue.kt`: per-month equivalent ($39.99 → "$3.33", rounded down) and saving vs 12× monthly (44%, rounded down; only when both prices are in the same currency).
- Paywall becomes a conversion screen (see 1a).

### 1a. Paywall copy and layout (Jaime: "really sell the app")
- Top (mystical): logo, "Walk the moonlit path", body line, then **"Within the grimoire"**:
  - The Grimoire: every spell, potion, and lesson you gather, kept in one book
  - A guided rite each dawn, with clear steps from candle to close
  - Rites for the moon's phases and the eight sabbats of the turning year — **hidden behind `SEASONAL_RITES_LIVE = false` until item 3's content is live**, so the screen never promises something the app lacks
  - Drawn from historical grimoires and magical papyri, with sources cited where available (matches the "cited where available" wording from PR #190)
- **Choose your plan** (plain): Yearly card first, preselected, badge "Best value", "$39.99 / year", "Works out to $3.33 / month, billed once a year", "Save 44% compared with paying monthly", "7-day free trial" (only if Play says this account is eligible). Monthly card second: "$5.99 / month", "Billed every month", trial badge if eligible.
- Bottom (plain, fixed): the selected plan's terms ("7 days free" / "Then $39.99 per year. Auto-renews until canceled."), CTA "Start My 7-Day Free Trial" or "Continue to Subscription", Restore Purchases, and the full disclosure ("No charge during the 7-day trial. Google Play charges $39.99 yearly after the trial unless canceled before it ends. Cancel anytime through Google Play."), Terms and Privacy links.
- No countdowns, no fake urgency, no struck-through "was" prices, no pre-checked add-ons. All numbers come from Play at runtime.
- If the yearly product isn't available, the screen falls back to the exact monthly-only flow of 1.0.

### Functions / rules
- No functional change needed: yearly is already in `PREMIUM_PRODUCT_IDS`. The draft PR adds tests for a yearly trial purchase, yearly RTDN conversion, and yearly grace period → on hold.
- Firestore rules: none.

### Play Console (after the merchant account exists; Jaime or Reviewbot)
- Create subscription `wiccan_premium_yearly`, base plan `yearly` (auto-renewing, **P1Y**), US price **$39.99**, then let Play generate local prices for all countries (review a few, e.g. GBP/EUR/INR/BRL).
- Offer: free trial **7 days**, eligibility "**New customer acquisition — never had any subscription**" (app-wide, not per-product), so a user can't take a monthly trial and then a yearly trial. Set the same eligibility on the monthly trial.
- Grace period and account hold: see item 4.
- Benefits text in Play (shown in the subscription center): plain, e.g. "Daily guided practice", "Full Grimoire", "Seasonal rites" (only once item 3 ships).
- Decision for Jaime: Google also allows a single product with two base plans (monthly + yearly), which makes switching plans easier through Play's own UI. The code and Data safety already assume **two products**, and Jaime chose a standalone plan, so the plan keeps two products. Switching monthly↔yearly for an existing subscriber is **not** in v1.1. A subscriber never sees the paywall; they'd cancel and resubscribe. Add `SubscriptionUpdateParams` (replacement mode) in v1.2 if people ask.

### Policy / Terms text — forces a version bump (Terms → 6)
Replace the "Subscriptions and billing" opening in `LegalDocuments.kt` **and** `hosting/terms.html` (parity test `LegalDocumentsTest`):
> "Wiccan requires a Google Play subscription after sign-in and onboarding. There is no free content plan. Wiccan Premium is offered as a monthly plan (US base price $5.99) and a yearly plan (US base price $39.99). Eligible new subscribers receive a 7-day free trial, followed by the automatically renewing plan they chose. The purchase screen and Google Play show your actual eligibility, localized price, billing period, and terms before you confirm."
> "If a trial is offered, cancel before its end to avoid the first charge for your plan. ..."
- `hosting/index.html` FAQ: "bills monthly" → "bills monthly or yearly, depending on the plan you choose".
- Privacy: no change for item 1 (same data, same product type).
- The draft PR deliberately does **not** edit legal text yet, to avoid conflicting with #190's v5. After #190 merges, rebase and make this change in the same v6 bump as item 2's privacy line.

### Tests
- Unit (in PR): `SubscriptionOfferTest` (yearly accepted at $39.99, wrong USD price rejected, yearly never accepts monthly terms), `PlanValueTest` ($3.33, 44%, currency mismatch, JPY), `SubscriptionRepositoryImplTest` (missing yearly hides plan, yearly launch uses yearly product, yearly restore verifies as yearly), `PaywallViewModelTest` (monthly-only fallback, yearly preselected, user choice survives refresh, per-plan trial eligibility).
- Beta: Compose UI test for the plan cards (selection, TalkBack state "Selected", RadioButton role), plus a screenshot test in each of the six palettes.
- License-tester purchases on internal testing: yearly with trial, yearly without trial (second account that already had a trial), monthly unchanged, restore on a second device, cancel during trial, refund.

### Risks
- Showing "44%" or "$3.33" in a currency where Play's localized prices break the ratio: computed from Play's actual micros, same-currency only, rounded down.
- Trial abuse across products: solved by app-wide "never had any subscription" eligibility in Play.
- Play rejecting the paywall for unclear terms: price, period, trial length, auto-renew and cancellation path are all on the screen next to the CTA.

---

## 2. A trial that counts

### What exists today
- **Daily reminder**: `functions/src/sendDailyNotifications.ts` (hourly `sendDailyNotificationsJob`, sends at each device's local 9am, copy "Your spell awaits / The moon has turned. {title}"), device opt-out in `users/{uid}/devices/*.notificationsEnabled`, Android channel `daily_practice` (`notification/WiccanNotificationChannels.kt`), foreground display gated in `ForegroundPushHandler`.
- **Day-1 lesson**: `functions/src/getTodayContent.ts` `selectTodayContent` picks a **random** eligible published item (`pickRandom`). Day 1 of a trial can be any spell, potion or lesson.
- **Trial end**: no reminder. The server only records `subscriptions/{uid}` (`productId`, `purchaseToken`, `subscriptionState`, `expiryTimeMillis`, `autoRenewing`) and mirrors `users/{uid}.subscriptionExpiresAtMillis`. It did not know whether a purchase was in its free trial.

### Changes — trial-ending reminder (done in the draft PR)
- `PlaySubscriptionInfo.inFreeTrial` from subscriptionsv2 `lineItems[].offerPhase.freeTrial` (premium line items only), saved to `subscriptions/{uid}.inFreeTrial` by verify, RTDN and the daily recheck.
- New hourly `sendTrialEndingRemindersJob` (`functions/src/trialEndingReminders.ts`, port `trialReminderPort.ts`, Firestore adapter `firestoreTrialReminderRepository.ts`):
  - candidates: `inFreeTrial == true` and trial ending within **28 hours**;
  - re-queries Google Play first and only sends if Play still reports active + auto-renewing + free-trial phase with the same end time. A user who already canceled gets nothing, because no charge is coming;
  - claims `trialReminderSentForExpiry` in a transaction **before** sending, so a retried or overlapping run never double-sends;
  - sends to each of the user's devices with notifications on, on a new Android channel **`billing_reminders`** ("Trial and billing reminders"), so users can mute the daily push and keep this one, or the other way round;
  - honors the `systemConfig/global.notificationsEnabled` kill switch.
- Copy (plain): **"Your free trial ends soon"** / "Your Wiccan free trial ends Jan 7, 8:00 PM AKST. Unless you cancel before then, Google Play will start your subscription and charge the price shown when you signed up. Manage it in Google Play > Payments & subscriptions > Subscriptions." The time is in the device's own time zone.
- App: channel created in `WiccanApplication`; `ForegroundNotificationPoster` uses the channel the server named and a separate notification ID (1002), so the reminder never replaces the daily push.
- New composite index `subscriptions (inFreeTrial ASC, expiryTimeMillis ASC)` in `firestore.indexes.json`. Decca deploys indexes before functions.

### Changes — strong day-1 lessons and the daily reminder (later PR, not in the draft)
- Add optional `starterRank` (1–7) to published content (`functions/src/types.ts`, `CONTENT_SCHEMA_V2.md`). In `selectTodayContent`, for a user with fewer than 7 deliveries, prefer the lowest unseen `starterRank` item before falling back to random. Curate 7 of the strongest, fully cited, low-risk lessons (no potions on day 1, given the potion safety disclosure) as the first week.
- After the first purchase succeeds, prompt for the notification permission (Android 13+) with mystical framing ("Let the grimoire call to you each dawn") and a plain line that it can be turned off in Settings. Today the permission is asked during onboarding; Betelgeuse owns where the prompt sits.
- Optional: a one-time in-app card on trial day 1 ("Your first rite awaits") linking Today → Grimoire → Settings.

### Play Console
- None for the reminder. Google Play itself sends some subscription emails. Don't rely on them for this.

### Policy text — Privacy change (rides the same v6 bump as item 1)
- `LegalDocuments.kt:255` and `hosting/privacy.html:43`: "The app requests only the notification permission, for the optional daily reminder." → "…for the optional daily reminder and, during a free trial, a reminder before the trial ends." Purposes paragraph: already lists "deliver requested notifications"; add "remind you before a free trial ends". No new data is collected (FCM token and time zone already declared). The Data safety form is unchanged.
- Decision for Jaime: the reminder currently goes **only to devices with notifications on**, because it reuses the daily opt-in. Sending it even to users who turned the daily reminder off would be friendlier on billing, but it needs a separate Settings toggle and clearer privacy text.

### Tests
- In PR: `trialEndingReminders.test.ts` (8 cases: sends once per trial per device, in the device's time zone; no repeat; skips canceled trials; skips stale converted records; window; opt-out and dead token; Play failure isolation; UTC fallback), `googlePlayBillingRepository.test.ts` (free-trial phase only from a premium line item), `verifyPurchase.test.ts` (trial recorded), `handleRtdn.test.ts` (yearly conversion clears trial; grace/hold), `ForegroundNotificationPosterTest` (billing channel, separate ID).
- Beta: Firestore emulator test for the new index query and the claim transaction; e2e on internal testing with a license tester (Play test trials are shortened, so check the real timing on the sandbox schedule).

### Risks
- Play's sandbox shortens trial and renewal periods (minutes, not days). The 28-hour window means test reminders fire almost immediately. Verify on a license tester and expect that.
- An `offerPhase` field shape not verified against a live purchase (same caveat as the existing subscriptionsv2 code). Confirm on the first sandbox trial.
- The hourly job reads all device docs once per run when there are candidates. Fine at launch scale; switch to a per-user device query if users grow past ~50k.

---

## 3. Retention

### What exists today
- `ui/theme/SeasonalThemeResolver.kt` maps dates to palettes on the eight-sabbat Wheel of the Year (Yule Dec 21 … Samhain). It is purely visual and documented as making no historical claim.
- Content category `MoonAndSeasonal` exists in `functions/src/types.ts`. There is no moon-phase calculation and no date-aware selection. `selectTodayContent` ignores the calendar.
- Live content: about 104 published items; 58 lesson drafts; target 300+. Generator `generateContentDraftsJob` writes at most 2 review-required drafts/day; publication goes through `approve-draft`.

### Changes
- Content: add optional `seasonalWindow` to content, `{ kind: "sabbat", id: "samhain" }` or `{ kind: "moon", phase: "full" | "new" | "waxing" | "waning" }`.
- Functions: a pure `moonPhase(date)` (a standard synodic-month approximation is accurate to within about a day, which is enough for a daily app; unit-tested against published NASA phase dates) and `sabbatFor(date)` (fixed dates, matching `SeasonalThemeResolver`; equinox/solstice dates by table). In `selectTodayContent`, on a sabbat day or a new/full moon, prefer an unseen item whose `seasonalWindow` matches; otherwise the normal pick. It stays idempotent per uid+day.
- App: a small "Tonight: Full Moon" / "Samhain" header chip on Today (Betelgeuse). The text must stay non-historical-claim, per the resolver's own doc comment, unless the lesson cites a source.
- Content production: 8 sabbat rites and 4 moon-phase rites minimum, each from a cited historical source and in the practical step-by-step format. They count toward the 300.
- Then flip `SEASONAL_RITES_LIVE = true` on the paywall.

### Play Console
- Optional: LiveOps / Promotional content cards for sabbats (Samhain, Yule) once there's an audience.

### Policy text
- None. No version bump.

### Tests
- `moonPhase` against a fixed table of 2026–2027 phases; `sabbatFor` boundaries; selection prefers seasonal items only on matching days and never repeats a delivered item.

### Risks
- Claiming historical observance of modern sabbats: keep sabbat names as the calendar framing and cite sources only for the practice itself.

---

## 4. Win-back

Grace period, account hold and lapsed-subscriber offers.

### What exists today
- `tierPolicy.ts` `tierForSubscription`: `active`/`in_grace_period` → premium (if expiry is in the future), `canceled` → premium until expiry, `on_hold`/`paused`/`pending`/`expired` → free. Correct for Play's model: grace keeps access, hold removes it.
- `handleRtdn.ts` re-queries Play for every notification type, so it doesn't branch on `notificationType`. `recheckLapsedSubscriptions.ts` runs daily at 03:00 as a backup. The RTDN topic `projects/wiccan-android/topics/play-rtdn` exists with the Play publisher IAM grant (done 2026-11-21). The Play Console RTDN setting is still pending Reviewbot.
- **Gap**: an `on_hold` user who fixes payment comes back through `SUBSCRIPTION_RECOVERED` RTDN → re-query → premium. Good. But `recheckLapsedSubscriptions` only lists **premium** users, so if the RTDN is lost, a recovered user stays free until they open the app and restore. That is a real gap.
- No in-app message for grace or hold ("fix your payment"). Play shows its own in-app messaging only if the app calls `showInAppMessages`.

### Changes
- App: call `BillingClient.showInAppMessages(activity, InAppMessageParams(TRANSACTIONAL))` on app start for signed-in users. Play then shows its own "payment declined, fix it" sheet during grace or hold. `WiccanBillingClient` + `MainActivity` (Betelgeuse).
- App: when `users/{uid}` tier is free **and** `subscriptions/{uid}.subscriptionState == on_hold`, show a plain "Your payment didn't go through. Update it in Google Play to restore access." on the paywall with a button to `https://play.google.com/store/account/subscriptions?sku=…&package=com.jaimesorrow.wiccan`. This needs a server-mirrored `subscriptionState` on the readable `users/{uid}` profile (rules change: client read-only field) or a callable.
- Functions: extend the daily recheck to also re-verify users whose last recorded state is `on_hold`/`in_grace_period`/`paused` (new `listUsersInRecoverableState`), so a lost `SUBSCRIPTION_RECOVERED` RTDN can't strand a paying user.
- Win-back offers: in Play, create a **"Win-back"** offer per plan (eligibility "Upgrade / Developer determined" isn't needed; use **"Lapsed subscribers"**, for example 50% off the first month of monthly or $29.99 for the first yearly). Play shows these in the Play Store subscription center and through Play's own win-back surfaces. In-app: when `getOffer()` sees an offer with tag `winback`, the paywall shows it. That needs `subscriptionOffer()` to accept exactly one discounted-intro-phase shape tagged `winback`, still failing closed on anything else.

### Play Console (Jaime or Reviewbot)
- Grace period: **7 days** for monthly, **14 days** for yearly (Play's recommended maximums vary by period; pick within what Play offers).
- Account hold: on (Play default **30 days** after grace).
- Pause: **off** for v1.1, since `paused` = free today and there's no UI for it.
- RTDN: topic `projects/wiccan-android/topics/play-rtdn`, send test notification (Reviewbot, pending its desktop).
- Win-back offers as above, tag `winback`.

### Policy text
- Terms already say "Refund eligibility is governed by … Google Play" and "renews at the then-current price". Discounted win-back offers are shown by Play with their own terms, so no change is required. If Jaime wants Terms to mention promotional offers, add one sentence in the v6 bump: "From time to time Google Play may show promotional prices; the price and duration shown before you confirm apply."

### Tests
- `recheckLapsedSubscriptions` recovers an `on_hold` user to premium; `tierPolicy` grace/hold already covered (draft PR adds a yearly RTDN grace→hold case); `subscriptionOffer` accepts the tagged win-back shape and rejects other discounts; Beta: emulator test that a held user sees the fix-payment message.

### Risks
- Mirroring `subscriptionState` to a client-readable field leaks nothing personal, but update `firestore.rules` and its tests so the client can't write it.

---

## 5. Store listing

### What exists today
- `docs/PLAY_STORE_LISTING.md`: category **Lifestyle**, short description "Daily lessons, spells, and folklore for a thoughtful spiritual practice.", a period-neutral full description, icon and feature graphic in `brand/exports/google-play/`, screenshots "still required from a current Play-distributed build". `docs/PLAY_CONSOLE_READINESS.md:59` recommended Lifestyle, or Books & Reference "only if the listing is framed primarily as historical education".

### Category question — Jaime wants **Education** (t101u)
- What changes: the category is a store-listing setting (Store settings → App category). It can be changed at any time and goes through normal update review. Content rating, Data safety, target audience and ads declarations do **not** change because of it.
- Families / Teacher Approved: do **not** apply. Both are for apps targeting children, and Wiccan's target audience is **18+ only**. Keep "Appeals to children: No" and do not opt into the Families program. Google may look harder at an Education listing that also seems child-appealing, so the 18+ statement stays prominent in the full description and the icon/screenshots stay adult.
- The IARC questionnaire was answered as "All Other App Types". Re-running it as "Reference, News, or Educational" is optional, not required by the category change. Reviewbot can decide after re-reading the questionnaire, since it could shift regional ratings.
- Honesty requirement: Education must be earned by the listing and the app, so the text **leads with the history**. Every lesson already carries a historical brief, citation and source link. The listing's first lines should say that, and the screenshots should show a lesson's source/citation section. Avoid any claim of accredited/curricular education.
- Update `docs/PLAY_STORE_LISTING.md` and `docs/PLAY_CONSOLE_READINESS.md:59` to Education with this reasoning (done by whoever lands the listing PR. The draft PR leaves the docs alone to avoid churn before launch).

### Listing copy (draft, lead with history; mystical but no outcome claims)
- Short (≤80): "Learn real historical magic: cited grimoire lessons, spells and rites, daily."
- Full description opening: "Wiccan teaches the history of practical magic from the sources themselves: Egyptian magical papyri, the London-Leiden papyrus, the Metternich Stela, medieval and early-modern grimoires. Each day brings one step-by-step lesson, spell or potion with a short historical brief and its citation…" Then Grimoire, seasonal rites (once item 3 ships), themes, then the plain subscription paragraph (monthly or yearly, trial, cancel in Play), then the 18+ and "not medical, no guaranteed outcomes" lines that are already there.
- Keywords (natural use only, no stuffing): history of magic, grimoire, spells, witchcraft, folk magic, magical papyri, moon rituals, sabbats, herbal lore, practical magic.

### Screenshots and video
- 6 phone screenshots from the **production** build on a real device (8 if seasonal ships): (1) Today lesson with steps, (2) the lesson's historical brief + citation, (3) Grimoire, (4) paywall plan choice with real Play prices from a license tester, (5) themes, (6) widget. Text overlays in Cinzel, mystical voice, no prices in overlays.
- Short promo video (30 s, YouTube, unlisted is fine): screen recording of the same flow, captions only, no voice claims. Feature graphic already exists.
- Custom store listing for 18+ English markets later; store listing experiments (A/B test) on the short description and first screenshot once installs exceed a few hundred/week.

### Policy text
- None in the app. The listing must keep matching the app (no "free" without the subscription caveat, no rankings or testimonials).

### Tests
- A `release-preflight.sh` check that `docs/PLAY_STORE_LISTING.md` fields stay within 30/80/4000 characters.

### Risks
- Education + witchcraft content may draw a closer review; the history-first framing and citations are the defence.

---

## 6. Translations

Spanish and Portuguese first.

### What exists today
- English only. 261 strings in `app/src/main/res/values/strings.xml`, no `values-*` folders, no `localeFilters`/`generateLocaleConfig` in `app/build.gradle.kts`.
- **Hard-coded English outside resources**: `ui/legal/LegalDocuments.kt` (all Terms/Privacy/Content-safety text as Kotlin strings, parity-tested against `hosting/*.html` by `LegalDocumentsTest`), `WiccanThemeModeUi.kt` display names/descriptions (check), and some error mapping in `core/error/AppErrorMapper.kt` (check).
- Server copy: notification text in `sendDailyNotifications.ts` and `trialEndingReminders.ts` is English; verification email `verificationEmail.ts` is English.
- Content: all Firestore `content` docs are English; citations point to English translations.

### Approach
1. **UI first, content later.** Ship v1.1 with the UI, paywall, notifications and Play listing in **es (Latin America + Spain via `es`)** and **pt-BR**; keep lessons in English with a plain note on Today ("Lessons are currently in English"). Translating ~300 cited lessons badly would damage the history-first promise, and Firestore content needs its own review pipeline.
2. String extraction: move every user-visible literal into `strings.xml` (Android Lint `HardcodedText` + a ktlint/grep check in `release-preflight.sh`), then add `values-es/strings.xml` and `values-pt-rBR/strings.xml`. Enable `androidResources { generateLocaleConfig = true }` for Android 13 per-app language, plus a language row in Settings.
3. Mystical voice in translation: a human-reviewed translation of the app copy (glossary: Grimoire = "Grimorio"/"Grimório", sabbat, rite…), not raw machine output. Billing/legal stays plain.
4. Legal text: Terms/Privacy in es/pt are **translations for convenience** with English controlling. Move `LegalDocuments` to resources (or per-locale Kotlin objects) and extend the parity test to `hosting/es/*.html` and `hosting/pt-br/*.html`. Adding translated legal pages is not a content change to the English policy, so **no version bump** for translation alone (users accept the same version in their language). Any wording change made during translation goes through the normal bump.
5. Server copy: send the device locale with `registerDevice` (new optional `locale` field on `users/{uid}/devices/*`), and pick notification/email copy by locale. Privacy text: add "language preference" to service activity (in the v6 bump if it lands together, otherwise v7).
6. Content later (v1.2+): add `translations.{es,pt-BR}.{title,intro,body,steps,reflection,safetyNote}` to content docs, translated from the English lesson (never re-generated from the source), reviewed like any publish, with the citation kept to the original English/original-language edition; `getTodayContent` returns the user's language when present, English otherwise.
7. Play: add es-419, es-ES, pt-BR store listings (translated description, localized screenshots). Prices are already localized by Play.

### Tests
- Lint: no hard-coded text; a unit test that every key in `values/` exists in `values-es/` and `values-pt-rBR/` with the same format args; Compose screenshot tests in es/pt for overflow (Spanish/Portuguese run 20–30% longer — the plan cards and CTA need to wrap).

### Risks
- Legal translation liability: English controls, and the translated pages say so.
- Partial-language experience (UI es, lessons en) could feel broken: say it plainly on Today and in the listing ("lessons in English").

---

## Release checklist for v1.1
1. 1.0 (versionCode 2) live; merchant account linked; monthly product live.
2. #190 merged (v5). Rebase `feat/v1.1-monetization`; add Terms + Privacy text (items 1, 2) and bump to **v6** in both `PolicyVersions.kt` and `policyVersions.ts`, with `hosting/` parity.
3. Reviewbot creates `wiccan_premium_yearly` ($39.99, P1Y, 7-day trial, "never had any subscription") and sets grace/hold.
4. Decca deploys Firestore indexes → functions (`sendTrialEndingRemindersJob`) → hosting (v6 pages) together, then uploads versionCode 3 to internal testing.
5. License-tester pass (Beta): yearly/monthly × trial/no-trial, trial reminder, restore, cancel, grace/hold.
6. Mark ready, Reviewbot review, merge, closed → production.

## Decisions needed from Jaime
- ✅ Yearly price: $39.99, standalone plan (t101u).
- ✅ Category: Education (t101u). Confirm he's fine leaving IARC as is unless Reviewbot recommends re-running it.
- Trial reminder only to devices with notifications on (current), or a separate always-on billing toggle?
- Win-back discount levels (suggested: 50% off first month of monthly; $29.99 first year).
- Grace period length (suggested 7 days monthly / 14 days yearly).
- Translations: UI-first in es + pt-BR with lessons in English for now (recommended), and budget for a human review of the translated copy.
