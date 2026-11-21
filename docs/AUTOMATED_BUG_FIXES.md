# Anthropic bug fixes and approved Play delivery

Two workflows keep code generation separate from release authority.

## Bug-hunt workflow

`Anthropic bug hunt` runs each Monday and can also be started manually. Add
`ANTHROPIC_API_KEY` as a repository Actions secret. The workflow asks Claude
to verify one Android bug, add a regression test, make the smallest fix, run
the repository checks, and open a pull request. It cannot merge or publish.

Review every generated pull request like a human-authored change. CI success
does not replace device, Firebase, billing, or product review.

## Google Play workflow

Create a GitHub Environment named `google-play` and configure yourself (or the
release owners) as required reviewers. Store these as environment secrets so
they are unavailable until approval:

- `PLAY_SERVICE_ACCOUNT_JSON`
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Give the Play service account only the app-level release permissions it needs.
Run `Publish approved build to Google Play` from the `main` branch, choose the
track, and approve the protected environment. The default is an internal-track
draft. The job rebuilds and validates the exact approved commit, verifies the
bundle signature, and only then uploads it.

Before every upload, increment `versionCode` and set the intended
`versionName` in `app/build.gradle.kts`; Google Play rejects reused version
codes. Production rollout still requires the real-device and Play Billing
checks in `docs/RELEASE_CHECKLIST.md`.
