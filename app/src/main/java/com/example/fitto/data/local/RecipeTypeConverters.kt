package com.example.fitto.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

class RecipeTypeConverters {

    @TypeConverter
    fun stringListToJson(values: List<String>?): String {
        val array = JSONArray()
        values.orEmpty().forEach { value -> array.put(value) }
        return array.toString()
    }

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { index -> array.optString(index) }
            .filter { item -> item.isNotBlank() }
    }

    @TypeConverter
    fun stringSetToJson(values: Set<String>?): String {
        val array = JSONArray()
        values.orEmpty().forEach { value -> array.put(value) }
        return array.toString()
    }

    @TypeConverter
    fun jsonToStringSet(value: String?): Set<String> {
        if (value.isNullOrBlank()) return emptySet()
        val array = JSONArray(value)
        return List(array.length()) { index -> array.optString(index) }
            .filter { item -> item.isNotBlank() }
            .toSet()
    }
}
