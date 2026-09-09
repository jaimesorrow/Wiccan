package com.nyxwell.app.data.local

import androidx.room.TypeConverter

/** Room can't persist List<String> natively; store as a delimited string. */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(LIST_SEPARATOR)

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.let { if (it.isEmpty()) emptyList() else it.split(LIST_SEPARATOR) }

    private companion object {
        // Unit separator control char (); won't appear in authored tag/material text.
        const val LIST_SEPARATOR = "\u001F"
    }
}
