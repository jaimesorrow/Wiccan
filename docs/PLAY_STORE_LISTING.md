# Wiccan — Google Play listing and launch pack

Prepared for the first English (United States) listing. Recheck every claim
against the build uploaded to Play before submission.

## Main store listing

| Field | Final copy / value |
| --- | --- |
| App name | `Wiccan` |
| Default language | English (United States) |
| App or game | App |
| Free or paid | Free download; a Google Play subscription (free trial eligible) is required for daily content and Grimoire access |
| Category | Education |
| Support email | `hello@wiccan.app` |
| Website | `https://wiccan.app/` |
| Privacy policy | `https://wiccan.app/privacy.html` |
| Account deletion | `https://wiccan.app/delete-account.html` |

### Short description

> Explore cited witchcraft history through daily entries and guided practice.

### Full description

> Explore the history of witchcraft through guided daily entries grounded in
> ancient texts, public-domain grimoires, folklore, and documented historical
> sources, with citations and source links where available and clear
> distinctions between historical belief, modern symbolism, and personal
> reflection.
>
> Receive one selected lesson, spell, or potion entry each day, follow its
> optional guided practice, and revisit delivered entries in your Grimoire.
> Entries may include materials, reflection prompts, and safety notes where
> extra care is needed.
>
> Make the experience your own with seasonal and selectable themes. Optional
> daily reminders help you return to your practice without turning it into an
> endless feed.
>
> Wiccan is free to download. An active free trial or Wiccan Premium
> subscription is required for daily content and Grimoire access. Premium is a
> recurring subscription that also includes the home-screen widget. Google Play
> shows the localized price, billing period, and applicable terms before
> purchase. You can manage or cancel through Google Play.
>
> Wiccan is intended for adults age 18 and older. Its content is educational
> and reflective. Ritual and folklore are not medical treatment and do not
> guarantee supernatural, physical, emotional, financial, or other outcomes.

The app name, short description, and full description are within Google's
current 30 / 80 / 4,000-character limits. Do not add rankings, price claims,
testimonials, or keywords that are not supported by the submitted build.

## Store assets

Ready in the repository:

- App icon: `brand/exports/google-play/wiccan_play_icon_512.png`
- Feature graphic: `brand/exports/google-play/wiccan_feature_graphic_1024x500.png`

Still required from a current Play-distributed production build. The phone
images under `hosting/assets/screens/` are marketing-site assets and are not a
substitute for this set:

1. Today lesson — readable real content and practice steps.
2. Lesson detail — historical brief, citation and source link visible.
3. Grimoire — delivered entries, without fabricated history.
4. Paywall — monthly/yearly choice with real localized Play prices from a
   license-tester build.
5. Theme selection — show the real theme choices.
6. Home-screen widget after its Play build is verified.

Prepare 6–8 screenshots (add seasonal content only if that feature ships) and
a 30-second caption-only promo video showing the same real flow. Do not place
prices in screenshot overlays. An unlisted YouTube video is acceptable for the
promo-video URL.

### Screenshot and creative brief

Reference photography and concept mockups are visual direction only. Store
graphics may borrow the current Midnight Grimoire presentation—near-black and
parchment surfaces, antique-gold rules, botanical or celestial ornament,
Cinzel headings, Manrope body copy, restrained photography and device framing—
provided the treatment does not hide or materially alter the real app UI.
The reference staging is also safe as decorative atmosphere: aged-parchment
lunar or elemental diagrams, candlelight, amethyst, quartz, labradorite,
lavender, rosemary or other herbs, a handwritten grimoire or journal, fountain
pen, dark rustic wood, deep navy cloth, a crescent-shaped brass dish, and quiet
window-lit ritual journaling. An overhead botanical/apothecary still life may
also use a black stone mortar and pestle, dried rosemary, sage or lavender,
amber apothecary bottles, an aged brass tool, candlelight and dark wood. Keep
these objects outside the phone UI,
subordinate to the real screen, and avoid copy or composition that presents
them as an inventory, altar, required material set, verified historical
artifact, or interactive app feature.

Botanical and apothecary staging must not imply medical, health or treatment
efficacy, safe ingestion, or a shipped Cabinet/inventory feature. Pair it only
with factual historical/editorial copy and the app's existing safety posture.

Every device screen and caption must describe functionality present in the
uploaded production build. The approved screenshot set is limited to the
current Home/moon-and-Wheel view, daily lesson reader and its materials,
historical brief/citation/source, Grimoire, real-price subscription choice,
theme selection, account/onboarding where useful, and the verified widget.

Do not show or name the unshipped reference designs for **Flow Mode**, **My
Cabinet**, **Community Altar**, **Luna Grimoire**, or an expanded concept Home
as app features. The production Home's existing **Cosmic Timing** card may
appear only in an unaltered capture verified against the uploaded build; the
separate reference-design version remains prohibited. Reference-image palette,
texture, typography, decorative borders, celestial symbols and photographic
mood may inform non-functional backgrounds or framing only when the final
graphic cannot reasonably imply that an unshipped concept exists in the app.

The Luna Grimoire sign-in/Home/Lesson View triptych is likewise composition and
palette inspiration only. Do not reuse its name, screen layout, navigation,
labels or other unverified UI in current Play assets or claims.

Decorative editorial photos must remain separate from the mandatory real-app
screenshots. They may be used in a feature-graphic concept or as restrained
surrounding atmosphere, but they do not count toward the 6–8 screenshot set and
must not replace, obscure or composite over the captured production UI.

Keep overlays factual and short: cited history, one lesson each day, revisit
lessons in the Grimoire, choose a theme, and manage a Play subscription. Avoid
feature-roadmap language, outcome claims, accreditation claims, mock prices,
unpublished content, or UI composited from several future concepts.

Use the complete production set above. Do not use emulator-only states, mock
subscription prices, draft-only content, or a screen that exposes an email,
token, Firebase project detail, or other personal/operational information.

## Play Console declarations

Use the evidence-backed answers in `PLAY_CONSOLE_READINESS.md`. The key launch
choices are:

- Contains ads: No.
- In-app purchases: Yes — subscriptions.
- App access restricted: Yes — provide a reusable reviewer account and premium
  review access.
- User-generated content / social / chat: No.
- Target audience: Adults 18+ only.
- Store category: Education. Do not opt into Families or Teacher Approved;
  those programs do not apply to this adult-only app.
- Health app declaration: No health features, provided the submitted content
  continues to avoid diagnosis or treatment claims.
- Account creation and deletion: Yes; deletion is available in-app and at the
  public deletion URL above.

## Tester recruitment

The launch site recruits testers by email until the Play closed-test opt-in URL
exists. Once Play publishes that URL, replace the email CTA with the official
opt-in link or add it as the primary action.

Recruit more than the minimum so a few opt-outs do not reset the testing clock.
For a personal developer account created after November 13, 2023, plan for at
least 12 testers continuously opted in to the closed test for 14 consecutive
days before applying for production access. Confirm the requirement shown in
this Play Console account rather than assuming it applies.

### Recruitment message

> **Subject: Help test Wiccan for Android**
>
> Wiccan is preparing for its first Google Play release, and I am inviting a
> small group of Android users to join the closed test. Testers will install the
> app through Google Play, try the daily lesson and Grimoire flows, and report
> anything confusing or broken. Wiccan is intended for adults 18+.
>
> To join, visit https://wiccan.app/#testing or reply with the Google
> account you use in the Play Store. Please remain opted in for the full testing
> period and use the app on your normal Android device.

### Feedback request

Ask every tester for:

- phone manufacturer/model and Android version;
- whether installation, email sign-in, and Google sign-in worked;
- whether Today, Grimoire, Settings, notification permission, and theme changes
  behaved as expected;
- whether any text was clipped, unclear, or difficult to read;
- whether the app crashed, froze, repeated a lesson, or showed an error;
- a screenshot and approximate time for each issue, with personal data removed.

Subscription, restore, cancellation, lapse, RTDN, and deletion scenarios should
be assigned only after Play license testing, products, and backend verification
are configured. Test purchases must use Play's test environment.

## Launch sequence

1. Add the `wiccan.app` DNS records in Cloudflare, deploy `hosting/` to Firebase
   Hosting and verify the policy URLs load (see `docs/GITHUB_PAGES_DOMAIN.md`).
2. Configure the upload key and repository signing secrets; produce a signed AAB.
3. Create the Wiccan Play app with package `com.jaimesorrow.wiccan` and enable
   Play App Signing.
4. Upload the signed AAB to Internal testing first; verify install, sign-in,
   App Check, daily content, and the external policy URLs.
5. Configure subscriptions, Android Publisher access, and Play RTDN; run real
   license-tester purchase/restore/cancel tests.
6. Complete the store listing, Data Safety, content rating, target audience,
   ads, health, app access, and account-deletion declarations.
7. Publish the Closed testing track, distribute its official opt-in link, and
   run the required test period.
8. Fix findings, upload a higher `versionCode`, rerun the release checks, and
   apply for production access when the Play dashboard confirms eligibility.
9. Start production with a staged rollout and monitor crashes, ANRs, reviews,
   billing verification, RTDN, and support email.

Official references:

- https://support.google.com/googleplay/android-developer/answer/9859152
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/9845334

