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
| Category | Lifestyle |
| Support email | `hello@wiccan.app` |
| Website | `https://wiccan.app/` |
| Privacy policy | `https://wiccan.app/privacy.html` |
| Account deletion | `https://wiccan.app/delete-account.html` |

### Short description

> Daily lessons, spells, and folklore for a thoughtful spiritual practice.

### Full description

> Build a thoughtful daily practice with Wiccan. Explore original lessons,
> spells, potions, and ritual studies inspired by historical folklore and source
> material, with careful distinctions between historical belief, modern
> symbolism, and personal reflection.
>
> Receive one selected entry each day, then revisit delivered entries in your
> Grimoire. Lessons may include historical context, materials, an optional
> practice, reflection prompts, and safety notes where extra care is needed.
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

Still required from a current Play-distributed build:

1. Today screen — one daily entry, with readable real content.
2. Grimoire — delivered entries, without fabricated history.
3. Lesson detail — source/reflection/safety sections visible.
4. Theme selection — show the real theme choices.
5. Optional: home-screen widget after its Play build is verified.

Use at least two current phone screenshots. Do not use emulator-only states,
mock subscription prices, draft-only content, or a screen that exposes an email,
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

