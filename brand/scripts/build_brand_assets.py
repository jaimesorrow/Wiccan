#!/usr/bin/env python3
"""Rebuild every Wiccan brand export from the two approved reference images.

Nothing is redrawn: each output is extracted pixel-for-pixel from
brand/source/ and only recoloured where the brand guidelines require it
(gold #E8BC61 on dark backgrounds, dark primary #7C6A48 on Light/Ivory,
white alpha for the Android monochrome icon).

Usage (from the repo root):
    python3 -m venv /tmp/brand-venv
    /tmp/brand-venv/bin/pip install pillow numpy scipy
    /tmp/brand-venv/bin/python brand/scripts/build_brand_assets.py
"""
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "brand" / "source"
EXPORTS = ROOT / "brand" / "exports"
RES = ROOT / "app" / "src" / "main" / "res"

GOLD = (0xE8, 0xBC, 0x61)          # stable symbol accent on dark icons
IVORY_PRIMARY = (0x7C, 0x6A, 0x48)  # Light/Ivory dark primary
MIDNIGHT_BLUE = (0x0B, 0x13, 0x2B)  # permanent Google Play / launcher background
WHITE = (255, 255, 255)

# Adaptive icon: 108dp canvas, 66dp safe zone. The emblem is itself a circle,
# so its outer diameter is kept to 62dp to sit inside the safe zone with margin.
CANVAS_DP, SAFE_DP, EMBLEM_DP = 108, 66, 62
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
LEGACY_DP = 48


def emblem_mask() -> Image.Image:
    """FIRST image: gold circle/W/flame/diamond emblem, wordmark removed."""
    a = np.asarray(Image.open(SOURCE / "01_App_Icon_Approved_Reference.png").convert("RGB")).astype(float)
    bg = np.median(a[:200, :200].reshape(-1, 3), axis=0)
    gold = np.median(a[(a[:, :, 0] > 200) & (a[:, :, 2] < 120)], axis=0)
    d = gold - bg
    alpha = np.clip(((a - bg) @ d) / (d @ d), 0, 1)   # keeps the artwork's own antialiasing
    alpha[1150:, :] = 0                                  # the WICCAN wordmark band
    alpha[alpha < 0.04] = 0
    ys, xs = np.where(alpha > 0)
    side = max(xs.max() - xs.min(), ys.max() - ys.min()) + 1
    cx, cy = (xs.min() + xs.max()) // 2, (ys.min() + ys.max()) // 2
    half = side // 2 + 2
    return Image.fromarray((alpha * 255).astype(np.uint8)).crop((cx - half, cy - half, cx + half, cy + half))


def full_logo() -> Image.Image:
    """SECOND image: thinner emblem + WICCAN wordmark, original colours, transparent background."""
    a = np.asarray(Image.open(SOURCE / "02_Full_Logo_Approved_Reference.png").convert("RGB")).astype(float)
    crop = a[80:592, 440:1100]   # emblem + wordmark only; no tagline, ornaments, leaves or swatches
    lum = 0.2126 * crop[:, :, 0] + 0.7152 * crop[:, :, 1] + 0.0722 * crop[:, :, 2]
    alpha = np.clip((lum - 70.0) / 100.0, 0, 1)
    labels, n = ndimage.label(alpha > 0.15)
    sizes = ndimage.sum(np.ones_like(alpha), labels, range(1, n + 1))
    keep = np.zeros(n + 1, bool)
    keep[1:] = sizes >= 150          # drop background ornament specks, keep strokes and letters
    alpha = np.where(ndimage.binary_dilation(keep[labels], iterations=3), alpha, 0)
    ys, xs = np.where(alpha > 0.05)
    box = (xs.min() - 6, ys.min() - 6, xs.max() + 7, ys.max() + 7)
    rgba = np.dstack([crop, alpha * 255]).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA").crop(box)


def tinted(mask: Image.Image, color) -> Image.Image:
    out = Image.new("RGBA", mask.size, color + (0,))
    out.putalpha(mask)
    return out


def on_canvas(mask: Image.Image, canvas_px: int, content_px: int, color, background=None) -> Image.Image:
    art = tinted(mask.resize((content_px, content_px), Image.LANCZOS), color)
    base = Image.new("RGBA", (canvas_px, canvas_px), (background + (255,)) if background else (0, 0, 0, 0))
    off = (canvas_px - content_px) // 2
    base.alpha_composite(art, (off, off))
    return base


def circle_masked(img: Image.Image) -> Image.Image:
    size = img.size[0]
    scale = 4
    big = Image.new("L", (size * scale, size * scale), 0)
    yy, xx = np.ogrid[: size * scale, : size * scale]
    c = (size * scale - 1) / 2
    big = Image.fromarray(((xx - c) ** 2 + (yy - c) ** 2 <= c * c).astype(np.uint8) * 255)
    m = big.resize((size, size), Image.LANCZOS)
    out = img.copy()
    out.putalpha(Image.fromarray(np.minimum(np.asarray(img.split()[3]), np.asarray(m))))
    return out


def rounded_square(img: Image.Image, margin: int, radius: int) -> Image.Image:
    from PIL import ImageDraw
    size = img.size[0]
    scale = 4
    big = Image.new("L", (size * scale, size * scale), 0)
    ImageDraw.Draw(big).rounded_rectangle(
        (margin * scale, margin * scale, (size - margin) * scale - 1, (size - margin) * scale - 1),
        radius=radius * scale, fill=255)
    out = img.copy()
    out.putalpha(big.resize((size, size), Image.LANCZOS))
    return out


def save(img: Image.Image, path: Path, mode="RGBA"):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.convert(mode).save(path, optimize=True)
    return path


def main():
    emblem = emblem_mask()
    logo = full_logo()
    logo_mask = logo.split()[3]

    # ---- Android launcher (adaptive foreground + monochrome, legacy fallbacks) ----
    for density, scale in DENSITIES.items():
        canvas = round(CANVAS_DP * scale)
        content = round(EMBLEM_DP * scale)
        mip = RES / f"mipmap-{density}"
        save(on_canvas(emblem, canvas, content, GOLD), mip / "ic_launcher_foreground.png")
        save(on_canvas(emblem, canvas, content, WHITE), mip / "ic_launcher_monochrome.png")
        # Legacy (API 24-25) icons: these are not adaptive, so they carry their
        # own conventional shapes - a rounded square with a 2dp margin, and a circle.
        legacy_px = round(LEGACY_DP * scale)
        full = on_canvas(emblem, legacy_px, round(legacy_px * 0.66), GOLD, MIDNIGHT_BLUE)
        save(rounded_square(full, margin=round(2 * scale), radius=round(8 * scale)), mip / "ic_launcher.png")
        save(circle_masked(on_canvas(emblem, legacy_px, round(legacy_px * 0.72), GOLD, MIDNIGHT_BLUE)),
             mip / "ic_launcher_round.png")

    # ---- Splash icon (API 31+ icon canvas is 288dp without an icon background) ----
    save(on_canvas(emblem, 288 * 4, 160 * 4, GOLD), RES / "drawable-xxxhdpi" / "splash_emblem.png")

    # ---- In-app brand art (tinted at runtime from the theme) ----
    save(tinted(emblem.resize((512, 512), Image.LANCZOS), WHITE), RES / "drawable-nodpi" / "wiccan_emblem.png")
    save(logo, RES / "drawable-nodpi" / "wiccan_full_logo.png")
    save(tinted(logo_mask, WHITE), RES / "drawable-nodpi" / "wiccan_full_logo_mask.png")

    # ---- Repository brand exports ----
    save(tinted(emblem.resize((1024, 1024), Image.LANCZOS), GOLD), EXPORTS / "app-icon" / "wiccan_emblem_gold_1024.png")
    save(tinted(emblem.resize((1024, 1024), Image.LANCZOS), IVORY_PRIMARY),
         EXPORTS / "app-icon" / "wiccan_emblem_ivory_primary_1024.png")
    save(tinted(emblem.resize((1024, 1024), Image.LANCZOS), WHITE), EXPORTS / "app-icon" / "wiccan_emblem_monochrome_1024.png")
    save(logo, EXPORTS / "full-logo" / "wiccan_full_logo_original_colors.png")
    save(tinted(logo_mask, IVORY_PRIMARY), EXPORTS / "full-logo" / "wiccan_full_logo_ivory_primary.png")

    # ---- Google Play ----
    play_icon = on_canvas(emblem, 512, 352, GOLD, MIDNIGHT_BLUE)   # full-bleed square; Play applies its own mask
    save(play_icon, EXPORTS / "google-play" / "wiccan_play_icon_512.png", "RGBA")

    feature = Image.new("RGBA", (1024, 500), MIDNIGHT_BLUE + (255,))
    target_h = 400
    scaled = logo.resize((round(logo.width * target_h / logo.height), target_h), Image.LANCZOS)
    feature.alpha_composite(scaled, ((1024 - scaled.width) // 2, (500 - target_h) // 2))
    save(feature, EXPORTS / "google-play" / "wiccan_feature_graphic_1024x500.png", "RGB")

    # ---- Validation previews (not shipped): launcher masks over the adaptive layers ----
    preview_dir = EXPORTS / "previews"
    fg = Image.open(RES / "mipmap-xxxhdpi" / "ic_launcher_foreground.png")
    layered = Image.new("RGBA", fg.size, MIDNIGHT_BLUE + (255,))
    layered.alpha_composite(fg)
    save(circle_masked(layered), preview_dir / "adaptive_circle_mask.png")
    mono = Image.open(RES / "mipmap-xxxhdpi" / "ic_launcher_monochrome.png")
    themed = Image.new("RGBA", mono.size, (0xDC, 0xE3, 0xF5, 255))
    themed.alpha_composite(tinted(mono.split()[3], (0x2B, 0x3A, 0x5C)))
    save(circle_masked(themed), preview_dir / "themed_icon_preview.png")

    for path in sorted(p for p in EXPORTS.rglob("*.png")):
        with Image.open(path) as im:
            print(f"{path.relative_to(ROOT)}: {im.size[0]}x{im.size[1]} {im.mode} {path.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
