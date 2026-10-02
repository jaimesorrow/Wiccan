# Branch integration audit — 2026-10-02

The refreshed baseline is main `6e2e2b33f0a2813d4ff7dd6eb2bd9696d6764eaf`,
including PRs #40 and #41. All 41 PRs were examined. The 36 merged PRs have
their recorded merge commits reachable from this baseline; none is missing.
Those are #1–20, #22, #24–25, #27, #29–31 and #33–41. No PR was open at
inventory time. PRs #21, #23, #26, #28 and #32 were closed without merging;
their proposals were reviewed against the newer implementations.

## Integration decisions

The inventory contained 28 non-main remote branches: 20 already reachable
from main and eight with additional history. The eight historical heads
are preserved through an ancestry merge that leaves the modern main tree
unchanged. Their useful missing changes are ported separately:

- Hide Google sign-in when its OAuth configuration is absent or blank,
  retain email authentication and pass the Activity context to Credential
  Manager when configured. Six regression tests cover these paths.
- Cancel superseded CI runs while keeping PR and push groups separate.
- Read CI's Node version from `functions/package.json`, matching PR #40's
  Node 22 runtime rather than the stale Node 20 workflow setting.
- Carry forward PR #42's Node 22 lockfile metadata, `.nvmrc`, locked run-driver
  installs and unused plugin alias cleanup. Adapt its package/toolchain gate
  to the current namespace and isolated flavor identities, with 21 regression tests.

Current package identity, fail-closed backend configuration, billing
allow-lists/connection locking/server acknowledgement, RTDN retries and
content-pool exhaustion responses are retained. The older service-wrapper
proposal adds organization but no missing runtime behavior; its entry-point
snapshot removes current safeguards. That alternative remains available in
Git history rather than becoming the live implementation.

## Branch disposition

Every listed head must be reachable from final main before its branch is
deleted. Full head SHAs are recorded so later branch reuse cannot be mistaken
for this audited work. Cleanup rechecks each live ref and skips any moved,
protected or unmerged branch. Main is retained.

| Branch | Audited head | Preservation |
|---|---|---|
| `add-ancient-content-seed` | `c9f6c73bae33401182d72fa52289219884f00c13` | Already integrated in main. |
| `claude/build-clean` | `46f2742b9f14e2b17014398d885aa96a7c4a0728` | Formatting patch matches PR #34. |
| `claude/ci-release-signing` | `c70f915e5595132de446313b1b3c94049d71c983` | Already integrated in main. |
| `claude/final-sync` | `0f6d8c26a12323749adbf299442e2206d22daf0c` | Already integrated in main. |
| `claude/fix-main-ktlint-after-palette-merge` | `9ef16eb44ca3613604f07e331b97fae893ae24ff` | Already integrated in main. |
| `claude/modernize` | `24394c514af1396ba5fcccbea8350e8e28e292cd` | Already integrated in main. |
| `claude/run-skill` | `50814f6a09a86fa95a8573dc301d85ed32d27fd6` | Already integrated in main. |
| `claude/salvage-emulator-suite` | `494927681e590a149a684ff13589619414af9ccd` | Already integrated in main. |
| `claude/validate-main` | `a9e7b474b56135dec3cc5da6ad6152ceb77d4716` | Already integrated in main. |
| `claude/vm-setup-app-build-y2mudu` | `c65d791772de3eca83cc1fafa763353a88258008` | Already integrated in main. |
| `claude/wiccan-android-firebase-lxivy1` | `a59c1a2bb207aa05e8cd9a2db9e986f40f15373d` | Firebase and emulator work preserved by PRs #29/#35; obsolete package fallback retired. |
| `claude/wiccan-ci-release-fix-b7gdd6` | `83f1a5d534168c5bc84c72e73b86c40e647df7d6` | Secret-scan fix superseded by the stronger current preflight. |
| `codex/fix-main-build-2026-10-02` | `9a6de549467dac2beead49f4a35f3861022b0ca6` | Already integrated in main. |
| `codex/validate-main-build` | `3c22d1cf48807d2cf7a35e19d9408a3c5552ba6c` | Already integrated in main. |
| `content-format-update` | `7ded2ff94ce631b41ea5365535e25c08e6524c98` | Already integrated in main. |
| `copilot/fix-github-actions-job` | `b9f74cca377a9386221a5b24be69c0cfdc54b86a` | Firebase, Billing and Kotlin changes superseded by PRs #29/#39. |
| `copilot/fix-github-actions-job-again` | `c9f6c73bae33401182d72fa52289219884f00c13` | Already integrated in main. |
| `copilot/foundation-core-content-engine` | `88cb6f1fd923cf95fcc929420c6a625d903e331a` | Already integrated in main. |
| `copilot/make-app-public` | `12f31b6f8459d79a0cabe0b642f487e86de1bfbb` | Already integrated in main. |
| `copilot/monthly-subscription-inquiry` | `a012ac66d1e5140c32364acdb8cca56a9dbbbaed` | Already integrated in main. |
| `feature/ancient-grounded-lessons` | `c06e23386cc04eaba6a817372913570bb443b1e9` | Already integrated in main. |
| `feature/dynamic-wiccan-themes` | `8c8d2c80f0708ea90b915557f2d6fe4c898b7228` | Theme superseded by PR #24; alternative service layout preserved in history. |
| `fix/config-identity-audit` | `88f939e3f77b3ac7f23da167e1558b83bed9c1b7` | Already integrated in main. |
| `fix/unique-lessons-source-provenance` | `00408237b1f433c1e6dc0c388d23431674149be2` | Already integrated in main. |
| `release/production-integration-clean` | `c0d3b2d8ab24d42dd6942020de3d393de3c3fd91` | Auth, Firebase, billing and acknowledgement behavior preserved by PR #29. |
| `release/production-integration-v2` | `b18ee6d8f62f75f807b576247e1deeeddbd5d522` | Already integrated in main. |
| `release/production-readiness` | `57071439b8e17dbc4be5077cc7b079c9ebddace4` | Signed workflow superseded by PR #33; missing Google availability guard ported. |
| `security-architecture-remediation` | `2963bf7a8afd391b5faa5d431059957ba6fe10ae` | Runtime behavior already implemented; service layout archived; CI concurrency ported. |

The temporary integration branch is eligible for deletion after its own PR
has merged. Branch deletion removes the name; the audited commits and their
original file trees remain reachable through main's history.

## Validation

Final build results and environment limitations are recorded in
[BUILD_VALIDATION.md](BUILD_VALIDATION.md). Hosted Actions startup failures
are assessed separately from local build results; this cleanup does not
change account billing or Actions administration settings.

## Branch discovered during final refresh

PR [#42](https://github.com/jaimesorrow/Wiccan/pull/42), active draft
`codex/reconcile-firebase-packages-toolchain` at
`a40bb6eeab1331ff9f49463b93278035240e682f`, appeared after the initial inventory.
Its runtime/dependency consistency improvements are included above. Its larger
namespace/source migration is retained on the draft branch: it removes the
existing `com.wiccan.app.widget.WiccanWidgetReceiver` provider component without
compatibility handling, so installed widgets can break. It also removes
application-ID suffixes, causing dev/staging installations to replace production.
Those migration behaviors and upgrade handling require separate resolution.
Production already registers `com.jaimesorrow.wiccan` correctly; its Kotlin
namespace is independent of the Firebase application ID.

The refreshed inventory is therefore 42 PRs and 29 non-main branches before
publishing this integration. The original 28 audited heads are eligible for
cleanup after the integration merge. PR #42 remains an active draft with unique
migration work and its branch must be retained. Its complete head is not claimed
as merged by the selective ports.
