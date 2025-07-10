package com.lifechurch.heroesinwaiting.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room converter for Map<String, Any> data types
 * Used for flexible metadata and properties storage
 */
class MapConverter {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromMap(map: Map<String, Any>?): String {
        return if (map == null) {
            "{}"
        } else {
            gson.toJson(map)
        }
    }
    
    @TypeConverter
    fun toMap(mapString: String?): Map<String, Any> {
        return if (mapString.isNullOrEmpty() || mapString == "{}") {
            emptyMap()
        } else {
            try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson(mapString, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}