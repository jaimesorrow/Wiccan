package com.nyxwell.app.domain.model

enum class ContentTier {
    FREE,
    PREMIUM,
    ;

    companion object {
        fun fromWireValue(value: String): ContentTier = when (value.lowercase()) {
            "free" -> FREE
            "premium" -> PREMIUM
            else -> throw IllegalArgumentException("Unsupported content tier: $value")
        }
    }

    fun toWireValue(): String = name.lowercase()
}
