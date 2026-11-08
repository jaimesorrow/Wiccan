# Wiccan brand assets

Source of truth: the owner-approved brand kit in `source/` (see
`Wiccan_Brand_Guidelines.pdf`, dated 2026-10-28). Nothing here is redrawn:
`scripts/build_brand_assets.py` extracts both identities pixel-for-pixel from
the reference PNGs and only recolours where the guidelines require it.

| Identity | Source | Used for |
| --- | --- | --- |
| App icon / seal (first image): gold circle, W, candle flame, diamond — **no lettering** | `source/01_App_Icon_Approved_Reference.png` (wordmark band removed) | Launcher icon, themed icon, splash, Google Play icon, compact in-app emblem (`WiccanEmblem`) |
| Full logo (second image): thinner emblem + WICCAN wordmark | `source/02_Full_Logo_Approved_Reference.png` (emblem + wordmark only; tagline, ornaments, leaves and swatches excluded) | Sign-in, onboarding, paywall, loading screen (`WiccanFullLogo`), feature graphic |

Colour rules: dark icons use gold `#E8BC61`; Light/Ivory uses its dark
primary `#7C6A48`; the launcher and Google Play icon use the permanent
Midnight Blue `#0B132B` background. On Light/Ivory the full logo is drawn in
the dark primary so the cream wordmark stays legible; on dark palettes it
keeps its original colours.

## Rebuild

```bash
python3 -m venv /tmp/brand-venv
/tmp/brand-venv/bin/pip install pillow numpy scipy
/tmp/brand-venv/bin/python brand/scripts/build_brand_assets.py
```

This regenerates `app/src/main/res/mipmap-*/ic_launcher*.png`,
`drawable-xxxhdpi/splash_emblem.png`, `drawable-nodpi/wiccan_*.png` and
everything under `exports/`. `BrandAssetsTest` (app unit tests) checks sizes,
the 66dp safe zone, the monochrome layer, Play specs and contrast.

## Exports

- `exports/google-play/wiccan_play_icon_512.png` — 512×512, 32-bit RGBA, Midnight Blue, full-bleed (Play applies its own mask).
- `exports/google-play/wiccan_feature_graphic_1024x500.png` — 1024×500, 24-bit (no alpha).
- `exports/app-icon/` — emblem at 1024 px in gold, Light/Ivory primary and monochrome (transparent).
- `exports/full-logo/` — full logo in original colours and in the Light/Ivory primary (transparent).
- `exports/previews/` — validation renders only (launcher circle mask, themed icon, six palettes); not for shipping.

## Known limits — owner review needed

- **Full-logo resolution:** the only source is a 1536×1152 composite, so the
  extracted full logo is 637×485 px. That is sharp for in-app use and the
  feature graphic, but too small for large print or high-resolution
  marketing. Provide the original vector/high-resolution full logo to unlock
  larger exports.
- The full logo was separated from a textured background by brightness; small
  background ornaments were removed. Please confirm the extraction in
  `exports/full-logo/` matches the approved design.
- **Trademark:** brand approval is not trademark clearance. The WICCAN name,
  seal and logo need review by qualified counsel before launch.
- The Play listing has **not** been changed; upload these files only after owner approval.
