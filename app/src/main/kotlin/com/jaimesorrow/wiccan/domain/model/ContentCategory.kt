package com.jaimesorrow.wiccan.domain.model

/** The Library grouping for a piece of content, chosen at generation time. */
enum class ContentCategory {
    PROTECTION,
    LOVE_AND_RELATIONSHIPS,
    PROSPERITY_AND_ABUNDANCE,
    HEALING_AND_WELLNESS,
    DIVINATION_AND_CLARITY,
    BANISHING_AND_RELEASE,
    HOME_AND_HEARTH,
    MOON_AND_SEASONAL,
    ;

    companion object {
        /** Parses the PascalCase wire value the Cloud Functions `ContentCategory` union sends. */
        fun fromWireValue(value: String): ContentCategory = when (value) {
            "Protection" -> PROTECTION
            "LoveAndRelationships" -> LOVE_AND_RELATIONSHIPS
            "ProsperityAndAbundance" -> PROSPERITY_AND_ABUNDANCE
            "HealingAndWellness" -> HEALING_AND_WELLNESS
            "DivinationAndClarity" -> DIVINATION_AND_CLARITY
            "BanishingAndRelease" -> BANISHING_AND_RELEASE
            "HomeAndHearth" -> HOME_AND_HEARTH
            "MoonAndSeasonal" -> MOON_AND_SEASONAL
            else -> error("Unknown content category '$value'")
        }
    }
}
