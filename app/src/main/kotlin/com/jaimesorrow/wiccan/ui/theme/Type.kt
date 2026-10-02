package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.jaimesorrow.wiccan.R

/**
 * Google Play Services' downloadable-fonts provider. Certificates come
 * verbatim from Google's own Jetchat Compose sample
 * (`res/values-v23/font_certs.xml`) - the standard, published cert set for
 * this provider, not something generated for this app.
 */
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/**
 * Cinzel: an elegant, letterspaced classical serif used sparingly for the
 * Wiccan wordmark, major section headings, and ritual/lesson titles - the
 * "editorial" voice of the app. Never used for long-form body copy.
 */
private val WiccanDisplayFont = FontFamily(
    Font(googleFont = GoogleFont("Cinzel"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Cinzel"), fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Cinzel"), fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
)

/**
 * Manrope: a highly readable modern geometric sans used for everything a
 * person actually reads at length or interacts with - body copy, settings,
 * navigation, forms, and buttons. Downloaded fonts load asynchronously and
 * fall back to the system sans in the meantime (or permanently, offline
 * with no Google Play Services) - never a blocking or crashing path.
 */
private val WiccanBodyFont = FontFamily(
    Font(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Manrope"), fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
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
