package com.nyxwell.app.domain.model

enum class ContentStatus {
    DRAFT,
    PUBLISHED,
    ;

    companion object {
        fun fromWireValue(value: String): ContentStatus = when (value.lowercase()) {
            "draft" -> DRAFT
            "published" -> PUBLISHED
            else -> throw IllegalArgumentException("Unsupported content status: $value")
        }
    }

    fun toWireValue(): String = name.lowercase()
}
