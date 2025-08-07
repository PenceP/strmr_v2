package org.jellyfin.androidtv.data.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return Json.decodeFromString(value)
    }
}