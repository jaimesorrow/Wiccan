package com.wiccan.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw color tokens for each [WiccanPalette]. Values are inspired by the
 * approved concept board but are not copied verbatim from it - several
 * were lightened, darkened, or reassigned to a different role so every
 * palette clears comfortable contrast on its own background rather than
 * matching the mockup's exact hex codes (see [WiccanColorSchemes] for how
 * these become a full Material3 `ColorScheme`).
 *
 * Every palette keeps the prior near-black theme's convention of an
 * in-palette "error"/safety tone (a muted terracotta/rose, never a
 * jarring Material red) so a safety callout never clashes with the rest
 * of the mood.
 */
internal object WiccanPaletteColors {

    object NearBlack {
        val Background = Color(0xFF0A0810)
        val Surface = Color(0xFF14101C)
        val SurfaceVariant = Color(0xFF1E1826)
        val Primary = Color(0xFF9B82C9)
        val OnPrimary = Color(0xFF1B1428)
        val PrimaryContainer = Color(0xFF3A2D5A)
        val OnPrimaryContainer = Color(0xFFE8E1D4)
        val Secondary = Color(0xFF6B9C82)
        val OnSecondary = Color(0xFF0F1D16)
        val SecondaryContainer = Color(0xFF2E4A3B)
        val OnSecondaryContainer = Color(0xFFE8E1D4)
        val Tertiary = Color(0xFFC49A66)
        val OnTertiary = Color(0xFF241A0C)
        val TertiaryContainer = Color(0xFF5A431F)
        val OnTertiaryContainer = Color(0xFFF0E4D0)
        val OnBackground = Color(0xFFE8E1D4)
        val OnSurfaceVariant = Color(0xFFA89EB0)
        val Outline = Color(0xFF3A3346)
        val Error = Color(0xFFCE8172)
        val OnError = Color(0xFF2A0F0A)
        val ErrorContainer = Color(0xFF5C2E24)
        val OnErrorContainer = Color(0xFFF2DCD4)
    }

    object MidnightBlue {
        val Background = Color(0xFF0B132B)
        val Surface = Color(0xFF111C3A)
        val SurfaceVariant = Color(0xFF1C2A4A)
        val Primary = Color(0xFF6FA8DC)
        val OnPrimary = Color(0xFF07213A)
        val PrimaryContainer = Color(0xFF1C3D5E)
        val OnPrimaryContainer = Color(0xFFDCEBFA)
        val Secondary = Color(0xFFA8C6E8)
        val OnSecondary = Color(0xFF0E2438)
        val SecondaryContainer = Color(0xFF2A4560)
        val OnSecondaryContainer = Color(0xFFE4EEFA)
        val Tertiary = Color(0xFFC9B6E8)
        val OnTertiary = Color(0xFF241A38)
        val TertiaryContainer = Color(0xFF453B63)
        val OnTertiaryContainer = Color(0xFFEDE6F7)
        val OnBackground = Color(0xFFE8F1FF)
        val OnSurfaceVariant = Color(0xFF9FB0CC)
        val Outline = Color(0xFF2E3F5C)
        val Error = Color(0xFFD08B7E)
        val OnError = Color(0xFF2C0F0A)
        val ErrorContainer = Color(0xFF5E3128)
        val OnErrorContainer = Color(0xFFF4DED6)
    }

    object RoyalPlum {
        val Background = Color(0xFF1B0E24)
        val Surface = Color(0xFF241530)
        val SurfaceVariant = Color(0xFF32203F)
        val Primary = Color(0xFFB48CE0)
        val OnPrimary = Color(0xFF23103A)
        val PrimaryContainer = Color(0xFF4B2D6B)
        val OnPrimaryContainer = Color(0xFFEBE0F7)
        val Secondary = Color(0xFFDCA9F0)
        val OnSecondary = Color(0xFF34103F)
        val SecondaryContainer = Color(0xFF5A2E68)
        val OnSecondaryContainer = Color(0xFFF4E4FA)
        val Tertiary = Color(0xFFE0B8E8)
        val OnTertiary = Color(0xFF391E42)
        val TertiaryContainer = Color(0xFF5E3A67)
        val OnTertiaryContainer = Color(0xFFF7E9F9)
        val OnBackground = Color(0xFFEFE4F5)
        val OnSurfaceVariant = Color(0xFFB79FC4)
        val Outline = Color(0xFF44314F)
        val Error = Color(0xFFDE8E9A)
        val OnError = Color(0xFF370E14)
        val ErrorContainer = Color(0xFF6B2E37)
        val OnErrorContainer = Color(0xFFF7DCE0)
    }

    object DeepForest {
        val Background = Color(0xFF0A1710)
        val Surface = Color(0xFF102019)
        val SurfaceVariant = Color(0xFF17301F)
        val Primary = Color(0xFF6FBE8C)
        val OnPrimary = Color(0xFF08281A)
        val PrimaryContainer = Color(0xFF1E4A31)
        val OnPrimaryContainer = Color(0xFFDDF2E4)
        val Secondary = Color(0xFF9FD8B2)
        val OnSecondary = Color(0xFF0C2E1C)
        val SecondaryContainer = Color(0xFF255239)
        val OnSecondaryContainer = Color(0xFFE4F5EA)
        val Tertiary = Color(0xFFCBE0A8)
        val OnTertiary = Color(0xFF23300F)
        val TertiaryContainer = Color(0xFF44562A)
        val OnTertiaryContainer = Color(0xFFEEF5DC)
        val OnBackground = Color(0xFFE3EEE2)
        val OnSurfaceVariant = Color(0xFFA0BCA8)
        val Outline = Color(0xFF2C4534)
        val Error = Color(0xFFCF9268)
        val OnError = Color(0xFF301707)
        val ErrorContainer = Color(0xFF5C3A20)
        val OnErrorContainer = Color(0xFFF5E1CE)
    }

    object WarmEarth {
        val Background = Color(0xFF1C140E)
        val Surface = Color(0xFF261C13)
        val SurfaceVariant = Color(0xFF33261A)
        val Primary = Color(0xFFC9995C)
        val OnPrimary = Color(0xFF2A1B08)
        val PrimaryContainer = Color(0xFF5A4023)
        val OnPrimaryContainer = Color(0xFFF5E3C8)
        val Secondary = Color(0xFFB48A6A)
        val OnSecondary = Color(0xFF29190C)
        val SecondaryContainer = Color(0xFF52392A)
        val OnSecondaryContainer = Color(0xFFF0DFCE)
        val Tertiary = Color(0xFFEBD9B8)
        val OnTertiary = Color(0xFF352A16)
        val TertiaryContainer = Color(0xFF5C4C2C)
        val OnTertiaryContainer = Color(0xFFF7EEDA)
        val OnBackground = Color(0xFFEFE2D0)
        val OnSurfaceVariant = Color(0xFFBBA48A)
        val Outline = Color(0xFF4A3A28)
        val Error = Color(0xFFCE8B72)
        val OnError = Color(0xFF2E1408)
        val ErrorContainer = Color(0xFF5C3423)
        val OnErrorContainer = Color(0xFFF5DDD0)
    }

    object LightIvory {
        val Background = Color(0xFFFAF7F1)
        val Surface = Color(0xFFF4EEE3)
        val SurfaceVariant = Color(0xFFEAE1D2)
        val Primary = Color(0xFF7C6A48)
        val OnPrimary = Color(0xFFFFFFFF)
        val PrimaryContainer = Color(0xFFE4D6B8)
        val OnPrimaryContainer = Color(0xFF2E2410)
        val Secondary = Color(0xFF6E7C5E)
        val OnSecondary = Color(0xFFFFFFFF)
        val SecondaryContainer = Color(0xFFDCE4CC)
        val OnSecondaryContainer = Color(0xFF222B16)
        val Tertiary = Color(0xFFA9822E)
        val OnTertiary = Color(0xFFFFFFFF)
        val TertiaryContainer = Color(0xFFF0DFAE)
        val OnTertiaryContainer = Color(0xFF362A06)
        val OnBackground = Color(0xFF2D2A26)
        val OnSurfaceVariant = Color(0xFF5C554A)
        val Outline = Color(0xFFC9BFA8)
        val Error = Color(0xFFB0554A)
        val OnError = Color(0xFFFFFFFF)
        val ErrorContainer = Color(0xFFF5DAD2)
        val OnErrorContainer = Color(0xFF3A140C)
    }
}
