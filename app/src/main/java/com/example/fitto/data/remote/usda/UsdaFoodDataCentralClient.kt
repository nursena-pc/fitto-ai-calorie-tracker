package com.example.fitto.data.remote.usda

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UsdaFoodDataCentralClient(
    private val apiKey: String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun searchFoods(
        query: String,
        pageSize: Int = 8,
        pageNumber: Int = 1
    ): List<UsdaFoodProduct> {
        val safeQuery = query.trim()
        if (safeQuery.length < 3) {
            Log.d(TAG, "Query too short: $safeQuery")
            return emptyList()
        }

        val url = "https://api.nal.usda.gov/fdc/v1/foods/search"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("api_key", apiKey.ifBlank { "DEMO_KEY" })
            .build()

        val requestJson = JSONObject()
            .put("query", safeQuery)
            .put("pageSize", pageSize.coerceIn(1, 20))
            .put("pageNumber", pageNumber.coerceAtLeast(1))
            .put(
                "dataType",
                JSONArray()
                    .put("Foundation")
                    .put("SR Legacy")
                    .put("Survey (FNDDS)")
                    .put("Branded")
            )

        Log.d(TAG, "USDA request query=$safeQuery")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "USDA response code=${response.code}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "USDA request failed. code=${response.code}")
                    return emptyList()
                }

                val body = response.body?.string().orEmpty()
                Log.d(TAG, "USDA body length=${body.length}")

                if (body.isBlank()) return emptyList()

                val products = parseProducts(body)
                Log.d(TAG, "USDA parsed products=${products.size}")

                products
            }
        } catch (e: Exception) {
            Log.e(TAG, "USDA request exception", e)
            emptyList()
        }
    }

    private fun parseProducts(json: String): List<UsdaFoodProduct> {
        val root = JSONObject(json)
        val foods = root.optJSONArray("foods") ?: return emptyList()

        return buildList {
            for (index in 0 until foods.length()) {
                val item = foods.optJSONObject(index) ?: continue
                val product = item.toUsdaFoodProduct() ?: continue
                add(product)
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
        if (calories <= 0.0) {
            Log.d(TAG, "USDA skipped no calories: $description")
            return null
        }

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

            val isEnergy = number == "1008" || name.contains("energy")
            if (!isEnergy) continue

            return when {
                unit.contains("KCAL") -> value
                unit.contains("KJ") -> value / 4.184
                else -> value
            }
        }

        return 0.0
    }

    private fun JSONArray.findNutrientValue(
        nutrientNumbers: Set<String>,
        nameKeywords: List<String>
    ): Double {
        return findNutrientValueOrNull(
            nutrientNumbers = nutrientNumbers,
            nameKeywords = nameKeywords
        ) ?: 0.0
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
            val matchedByName = nameKeywords.any { keyword ->
                name.contains(keyword)
            }

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
        const val TAG = "UsdaFoodClient"
    }
}