package com.example.fitto.data.remote.usda

import org.json.JSONArray
import org.json.JSONObject

internal class UsdaFoodResponseParser {

    fun parse(json: String): List<UsdaFoodProduct> {
        val root = JSONObject(json)
        val foods = root.optJSONArray("foods") ?: return emptyList()

        return buildList {
            for (index in 0 until foods.length()) {
                val item = foods.optJSONObject(index) ?: continue
                item.toUsdaFoodProduct()?.let(::add)
            }
        }
    }

    private fun JSONObject.toUsdaFoodProduct(): UsdaFoodProduct? {
        val fdcId = optLong("fdcId", 0L)
        if (fdcId <= 0L) return null

        val description = optString("description").trim()
        if (description.isBlank()) return null

        val nutrients = optJSONArray("foodNutrients") ?: JSONArray()
        val calories = nutrients.findEnergyKcal()
        if (calories <= 0.0) return null

        return UsdaFoodProduct(
            fdcId = fdcId,
            description = description.lowercase().replaceFirstChar { it.titlecase() },
            dataType = optString("dataType").trim(),
            brandOwner = optString("brandOwner").takeIf { it.isNotBlank() },
            caloriesPer100g = calories,
            proteinPer100g = nutrients.findNutrientValue(
                nutrientNumbers = setOf("1003"),
                nameKeywords = listOf("protein")
            ),
            fatPer100g = nutrients.findNutrientValue(
                nutrientNumbers = setOf("1004"),
                nameKeywords = listOf("total lipid", "fat")
            ),
            carbsPer100g = nutrients.findNutrientValue(
                nutrientNumbers = setOf("1005"),
                nameKeywords = listOf("carbohydrate")
            ),
            fiberPer100g = nutrients.findNutrientValueOrNull(
                nutrientNumbers = setOf("1079"),
                nameKeywords = listOf("fiber")
            )
        )
    }

    private fun JSONArray.findEnergyKcal(): Double {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val number = item.optString("nutrientNumber")
            val name = item.optString("nutrientName").lowercase()
            val unit = item.optString("unitName").uppercase()
            val value = item.optNullableDouble("value") ?: continue

            if (number != "1008" && !name.contains("energy")) continue

            return when {
                unit.contains("KCAL") -> value
                unit.contains("KJ") -> value / KILOJOULES_PER_KILOCALORIE
                else -> value
            }
        }

        return 0.0
    }

    private fun JSONArray.findNutrientValue(
        nutrientNumbers: Set<String>,
        nameKeywords: List<String>
    ): Double {
        return findNutrientValueOrNull(nutrientNumbers, nameKeywords) ?: 0.0
    }

    private fun JSONArray.findNutrientValueOrNull(
        nutrientNumbers: Set<String>,
        nameKeywords: List<String>
    ): Double? {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val number = item.optString("nutrientNumber")
            val name = item.optString("nutrientName").lowercase()

            val matchedByNumber = number in nutrientNumbers
            val matchedByName = nameKeywords.any { keyword -> name.contains(keyword) }

            if (matchedByNumber || matchedByName) {
                return item.optNullableDouble("value")
            }
        }

        return null
    }

    private fun JSONObject.optNullableDouble(name: String): Double? {
        if (!has(name) || isNull(name)) return null

        return runCatching {
            when (val value = opt(name)) {
                is Number -> value.toDouble()
                is String -> value.replace(",", ".").toDoubleOrNull()
                else -> null
            }
        }.getOrNull()
    }

    private companion object {
        const val KILOJOULES_PER_KILOCALORIE = 4.184
    }
}
