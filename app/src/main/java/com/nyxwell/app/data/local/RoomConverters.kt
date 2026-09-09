package com.nyxwell.app.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

class RoomConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { index -> array.getString(index) }
    }
}
