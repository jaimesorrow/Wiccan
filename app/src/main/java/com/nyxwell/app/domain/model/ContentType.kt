package com.nyxwell.app.domain.model

enum class ContentType {
    SPELL,
    POTION,
    LESSON,
    ;

    companion object {
        fun fromWireValue(value: String): ContentType = when (value.lowercase()) {
            "spell" -> SPELL
            "potion" -> POTION
            "lesson" -> LESSON
            else -> throw IllegalArgumentException("Unsupported content type: $value")
        }
    }

    fun toWireValue(): String = name.lowercase()
}
