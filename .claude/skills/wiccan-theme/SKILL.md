---
name: wiccan-theme
description: Guardrails for any change to Android UI/Compose code in this app — the six-palette design-token system, the MVI Contract/ViewModel/Screen split, and the Cinzel/Manrope typography roles. Use before adding or editing a screen, component, or color under app/src/main/kotlin/com/jaimesorrow/wiccan/ui/, or before touching the theme/widget packages. Not for Cloud Functions or non-UI backend work.
---

# Wiccan visual identity & screen conventions

The app renders in one of six coordinated `WiccanPalette` values (Near
Black, Midnight Blue, Royal Plum, Deep Forest, Warm Earth, Light/Ivory)
selected by `WiccanThemeMode` — automatic Wheel-of-the-Year
(`SeasonalThemeResolver`), Follow System, or a locked palette. Every
palette shares identical typography, spacing, component geometry,
navigation, and iconography — **only color changes between them.**
Treat that invariant as load-bearing: a change that makes one palette
look structurally different from the others is a bug, not a feature.

## Never hardcode a color

Screens read `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`
— never `Color(0x...)` inline in a screen or component. A genuinely new
semantic color needs an entry in **all six** palettes:

1. `ui/theme/WiccanPaletteColors.kt` — the six `object` blocks (raw hex
   tokens, one block per palette).
2. `ui/theme/WiccanColorSchemes.kt` — maps each palette's tokens onto the
   Material3 `ColorScheme` role you're adding.

Adding a color to one palette and not the others will compile but breaks
the shared-identity guarantee — check all six before considering it done.

## Typography

`ui/theme/Type.kt`: Cinzel (elegant display serif) is reserved for the
Wiccan wordmark, major section headings, and ritual/lesson titles —
never long-form body text. Manrope covers everything else (body copy,
settings, navigation, forms, buttons). Both load via Android's
downloadable Google Fonts provider and fall back to the system font
while loading/offline — don't bundle font binaries or add a different
font mechanism.

## Screen structure (MVI)

Each feature under `ui/<feature>/` is:

- `<Feature>Contract.kt` — `UiState` data class, sealed `UiEvent`, sealed `UiEffect`.
- `<Feature>ViewModel` — `StateFlow<UiState>` + a `Flow<UiEffect>` (a
  `Channel`, for one-shot things like snackbars) + a single
  `onEvent(event)` dispatcher. Don't add a second public entry point for
  triggering state changes.
- `<Feature>Screen.kt` — a public `Screen(viewModel: X = hiltViewModel())`
  wrapper, delegating to an `internal fun <Feature>ScreenContent(state, effects, onEvent, modifier)`
  that takes no ViewModel/Hilt dependency (see `TodayScreen.kt`/
  `TodayScreenContent`). This split is what makes a screen previewable —
  new screens that want `@Preview` coverage need it too.

## Visual validation

There is usually no Android emulator/device available to render the app.
Validate a visual change with `@Preview` composables — see
`ui/theme/WiccanThemePreviews.kt` for the existing pattern (wrap content
in `WiccanTheme(themeMode = <a specific palette>)`). Add previews for at
least two visually distinct palettes (e.g. a dark one and Light/Ivory)
for any new screen or significantly changed component, and say plainly
that this is preview-based validation, not a real screenshot, when
reporting the change.

## The Glance widget is a separate composition

`widget/WiccanWidget.kt` cannot read `MaterialTheme.colorScheme` — it
resolves the active palette directly via
`resolveWiccanPalette(themeMode, isSystemDark).toColorScheme()`, reached
through the `WiccanWidgetEntryPoint` Hilt `EntryPoint` (Glance doesn't
construct widgets through Hilt the way Activities/ViewModels are). Any
widget UI change needs the same "no hardcoded colors" discipline as the
main app.
