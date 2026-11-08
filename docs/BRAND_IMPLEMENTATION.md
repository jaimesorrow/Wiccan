# Wiccan — approved brand implementation

Source of truth: **Wiccan_Brand_Guidelines.pdf (2026-10-28)** and the two approved image references supplied by the owner.

## Asset identity
- FIRST image: bold circular **W / candle flame / diamond** seal; symbol-only on the Android launcher, Android splash, and Google Play listing. Do not use its lettering in the app icon.
- SECOND image: **separate full-logo design** with its own thin emblem and fixed WICCAN wordmark. Never substitute the first image for the second or redraw its wordmark.
- Permanent Google Play listing background is **Midnight Blue (#0B132B)**. Approved dark-icon accent **#E8BC61**.
- Android adaptive icons: 108dp layers, central 66dp protected zone, OS monochrome icon, no baked circular/rounded mask.

## Implemented here
- The original crescent launcher vector is replaced with a compact vector trace of the FIRST image. Foreground symbol geometry is inside the central 66dp safe area.
- Separate OS monochrome vector and adaptive icon declaration.
- Matching splash foreground via existing resource mapping.
- Branded sign-in and onboarding seal, including dark-primary variant against Light/Ivory.
- Existing six-palette theme values and Cinzel/Manrope typography remain unchanged.

## Play Console — owner approval required
Source-image exports were prepared separately in the Wiccan brand implementation package:
- `play/google_play_icon_512.png` — 512×512 RGBA PNG, <1MB.
- `play/google_play_feature_graphic_1024x500.png` — 1024×500 PNG derived from SECOND approved poster.
- `reference/01_approved_app_seal.png` and `reference/02_approved_full_logo.png` — unchanged extracted references.

These binary exports are **not yet checked into this GitHub branch**. Add the exact approved source image(s) and reviewed Play exports to permanent repo assets before considering the Play listing ready. Do not redraw the full-logo wordmark or replace it with arbitrary typography.

## Not in scope
No Firebase, Firestore, subscriptions, lesson content, credentials, pricing, or security-rule changes. No Play Console publication.

## Release verification
- Review the trace alongside the approved first image to confirm the contours and safe area.
- Confirm Android packaging, launcher mask behavior, OS themed icons and splash on an emulator/device.
- Check high contrast and legibility for all six palettes, including enlarged font and small screens.
- Confirm both distinct approved identities and conduct trademark clearance before public launch.
