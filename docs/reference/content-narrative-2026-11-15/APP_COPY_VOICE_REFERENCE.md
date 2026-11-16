# App copy voice reference

Source: merged pull request [#153](https://github.com/jaimesorrow/Wiccan/pull/153), reconciled after the lesson rewrite merge in pull request #154.

This is the approved interface-copy layer that complements the practical lesson narrative. The implemented voice is warm, moonlit, concise, and clear. It must never make a consequential action ambiguous.

## Voice boundary

- Mystical language is appropriate for welcome text, Today, Grimoire, palette descriptions, reminders, empty states, and non-critical errors.
- Buttons remain short and specific.
- Billing, trial, renewal, cancellation, restore-purchase, legal, privacy, analytics, content-safety, password rules, email verification, and account-deletion wording remain plain.
- Authentication and reset messages do not confirm whether an account exists.
- Safety headings and potion disclosure text remain plain.
- The user-facing archive is **Grimoire**. Bottom navigation remains **Today**, **Grimoire**, and **Settings**.

## Current anchor copy

| Surface | Approved wording |
| --- | --- |
| Sign-in headline | The moonlit path begins here. |
| Daily line | The moon has turned. Your next practice awaits. |
| Main CTA | Begin your path. |
| Paywall headline | Walk the moonlit path |
| Today labels | Today's lesson / Today's spell / Today's potion |
| Saved state | Kept in your Grimoire |
| Empty Grimoire | Your Grimoire is still quiet. Open Today to receive your first lesson. |
| Invalid credentials | The gate stayed shut. Check your email and password, then try again. |
| Purchase verification error | We couldn't verify that purchase with Google Play. Try again, or contact support. |
| Notification body | The moon has turned. `{title}` |
| Verification email subject | The moon is waiting — verify your Wiccan email |

## Completed scope

- Loading and accessibility descriptions
- Sign in, account creation, password recovery, email verification, and name step
- Policy acceptance and mandatory-subscription paywall
- Today, shared lesson detail, Grimoire timeline/categories/entry, and bottom navigation
- Settings, account and support, reminders, all six palettes, and home-screen widget
- Daily notification channel and content
- Branded verification email
- Specific invalid-credential and Google Play restore/verification errors
- Migration of hardcoded Compose strings into `strings.xml`

The repository does not contain custom Firebase password-reset or change-email email templates. Those continue to use Firebase Authentication's built-in sender; only the surrounding in-app copy is controlled here.

## Verification recorded by the implementation

- Android formatting, unit tests, Android-test compilation, release lint, and production debug compilation passed.
- Functions tests, lint, and build passed.
- Instrumented tests compiled but were not run on an emulator for this copy change.

When changing this copy, preserve the plain-language boundary above and update tests that assert exact strings.
