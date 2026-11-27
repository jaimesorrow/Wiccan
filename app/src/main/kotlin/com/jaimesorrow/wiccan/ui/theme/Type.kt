package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleDownloadableFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.wiccan.app.R

/**
 * Google Play Services' downloadable-fonts provider. Certificates come
 * verbatim from Google's own Jetchat Compose sample
 * (`res/values/font_certs.xml`) - the standard, published cert set for
 * this provider, not something generated for this app.
 */
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/**
 * Cinzel, bundled from Google Fonts under the OFL (see assets/fonts/OFL-Cinzel.txt).
 * Letterspaced classical serif for the wordmark, screen titles, and short ritual headings.
 * Never used for long-form body copy. Bundled, rather than downloaded at runtime, so the
 * face is present offline and in screenshot tests.
 */
internal val WiccanDisplayFont = FontFamily(
    Font(R.font.cinzel_regular, weight = FontWeight.Normal),
    Font(R.font.cinzel_medium, weight = FontWeight.Medium),
    Font(R.font.cinzel_semibold, weight = FontWeight.SemiBold),
)

/**
 * Cormorant Garamond, OFL (assets/fonts/OFL-CormorantGaramond.txt). Editorial serif for
 * lesson prose and card titles. Italic is the grimoire-page voice; upright is for short headings.
 */
internal val WiccanEditorialFont = FontFamily(
    Font(R.font.cormorant_garamond_regular, weight = FontWeight.Normal),
    Font(R.font.cormorant_garamond_medium, weight = FontWeight.Medium),
    Font(R.font.cormorant_garamond_semibold, weight = FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
)

/**
 * Caveat, OFL (assets/fonts/OFL-Caveat.txt). Handwritten labels and marginal notes only.
 * Too irregular for body, billing, legal, safety, or verification copy.
 */
internal val WiccanHandwritingFont = FontFamily(
    Font(R.font.caveat_regular, weight = FontWeight.Normal),
    Font(R.font.caveat_medium, weight = FontWeight.Medium),
)

/**
 * IM Fell DW Pica, OFL (assets/fonts/OFL-IMFellDWPica.txt). Calligraphic grimoire voice for
 * numbered lesson steps on the tattered parchment panel only.
 */
internal val WiccanGrimoireStepsFont = FontFamily(
    Font(R.font.im_fell_dw_pica_regular, weight = FontWeight.Normal),
)

/**
 * Manrope: a highly readable modern geometric sans used for everything a
 * person actually reads at length or interacts with - body copy, settings,
 * navigation, forms, and buttons. Downloaded fonts load asynchronously and
 * fall back to the system sans in the meantime (or permanently, offline
 * with no Google Play Services) - never a blocking or crashing path.
 */
private val WiccanBodyFont = FontFamily(
    GoogleDownloadableFont(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    GoogleDownloadableFont(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    GoogleDownloadableFont(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    GoogleDownloadableFont(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
)

/**
 * The full Material3 type scale, shared by every [WiccanPalette] - only
 * color changes between palettes, never type. Display/headline roles carry
 * the editorial Cinzel voice; title/body/label roles use Manrope so
 * settings, forms, and long-form content stay maximally readable at every
 * Android font-scale setting (sizes are all `.sp`, which follows the
 * system font-scale preference automatically).
 */
internal val WiccanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = WiccanDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.5.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = WiccanDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.4.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = WiccanDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.3.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = WiccanDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.2.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = WiccanDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.2.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = WiccanDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.1.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = WiccanBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp,
    ),
)
