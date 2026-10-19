package com.jaimesorrow.wiccan.domain.model

/** The Grimoire grouping for a piece of content, chosen at generation time. */
enum class ContentCategory {
    CANDLE_MAGIC,
    HERBS_BOTANICALS,
    POTIONS_INFUSIONS,
    CRYSTALS_STONES,
    CORD_KNOT_MAGIC,
    MOON_MAGIC_TIMING,
    ELEMENTAL_MAGIC,
    ALTARS_SACRED_SPACE,
    DIVINATION,
    // Read compatibility for delivered Grimoire entries written before the practical taxonomy.
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
            "candle-magic" -> CANDLE_MAGIC
            "herbs-botanicals" -> HERBS_BOTANICALS
            "potions-infusions" -> POTIONS_INFUSIONS
            "crystals-stones" -> CRYSTALS_STONES
            "cord-knot-magic" -> CORD_KNOT_MAGIC
            "moon-phases-timing" -> MOON_MAGIC_TIMING
            "elemental-magic" -> ELEMENTAL_MAGIC
            "altars-sacred-space" -> ALTARS_SACRED_SPACE
            "divination" -> DIVINATION
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

        fun fromStoredValue(value: String): ContentCategory =
            entries.firstOrNull { it.name == value } ?: fromWireValue(value)
    }
}
