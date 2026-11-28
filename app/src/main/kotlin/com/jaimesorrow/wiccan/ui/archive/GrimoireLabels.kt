package com.jaimesorrow.wiccan.ui.archive

import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.wiccan.app.R

internal fun ContentCategory.labelResId(): Int = when (this) {
    ContentCategory.CANDLE_SPELLS -> R.string.category_candle_spells
    ContentCategory.HERBAL_RITUALS -> R.string.category_herbal_rituals
    ContentCategory.CORD_KNOT_WORK -> R.string.category_cord_knot
    ContentCategory.LUNAR_PRACTICES -> R.string.category_lunar
    ContentCategory.CHARMS_TALISMANS -> R.string.category_charms
    ContentCategory.CANDLE_MAGIC -> R.string.category_candle_magic
    ContentCategory.HERBS_BOTANICALS -> R.string.category_herbs
    ContentCategory.POTIONS_INFUSIONS -> R.string.category_potions
    ContentCategory.CRYSTALS_STONES -> R.string.category_crystals
    ContentCategory.CORD_KNOT_MAGIC -> R.string.category_cord_magic
    ContentCategory.MOON_MAGIC_TIMING -> R.string.category_moon_magic
    ContentCategory.ELEMENTAL_MAGIC -> R.string.category_elemental_magic
    ContentCategory.ALTARS_SACRED_SPACE -> R.string.category_altars
    ContentCategory.DIVINATION -> R.string.category_divination
    ContentCategory.ELEMENTAL_PRACTICES -> R.string.category_elemental_practices
    ContentCategory.PROTECTION_CLEANSING_WARDING -> R.string.category_protection_cleansing
    ContentCategory.PROTECTION -> R.string.category_protection
    ContentCategory.LOVE_AND_RELATIONSHIPS -> R.string.category_love
    ContentCategory.PROSPERITY_AND_ABUNDANCE -> R.string.category_prosperity
    ContentCategory.HEALING_AND_WELLNESS -> R.string.category_healing
    ContentCategory.DIVINATION_AND_CLARITY -> R.string.category_divination_clarity
    ContentCategory.BANISHING_AND_RELEASE -> R.string.category_banishing
    ContentCategory.HOME_AND_HEARTH -> R.string.category_home
    ContentCategory.MOON_AND_SEASONAL -> R.string.category_moon_seasonal
}

internal fun ContentType.labelResId(): Int = when (this) {
    ContentType.SPELL -> R.string.content_type_spell
    ContentType.POTION -> R.string.content_type_potion
    ContentType.LESSON, ContentType.UNKNOWN -> R.string.content_type_lesson
}
