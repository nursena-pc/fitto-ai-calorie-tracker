package com.example.fitto.data.remote.logmeal

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal data class LogMealSegmentationResult(
    val imageId: String?,
    val candidates: List<LogMealFoodCandidate>
)

internal class LogMealResponseParser {

    fun parseSegmentation(json: String): LogMealSegmentationResult {
        val root = JSONObject(json)
        val imageId = root.findFirstString(setOf("imageId", "image_id"))
        val candidates = parseCandidates(root)
            .distinctBy { candidate -> candidate.name.lowercase(Locale.ROOT) }
            .sortedByDescending(LogMealFoodCandidate::confidence)

        return LogMealSegmentationResult(
            imageId = imageId,
            candidates = candidates
        )
    }

    fun mergeNutrition(
        candidates: List<LogMealFoodCandidate>,
        nutritionJson: String
    ): List<LogMealFoodCandidate> {
        if (candidates.isEmpty()) return emptyList()

        val nutritionBundle = parseNutritionBundle(nutritionJson)

        return candidates.mapIndexed { index, candidate ->
            val itemNutrition = candidate.foodItemPosition
                ?.let(nutritionBundle.perItem::get)
            val nutrition = itemNutrition
                ?: if (index == 0) nutritionBundle.overall else null

            if (nutrition == null || !nutrition.hasNutrition()) {
                candidate
            } else {
                candidate.copy(
                    servingGrams = nutrition.servingGrams ?: candidate.servingGrams ?: DEFAULT_SERVING_GRAMS,
                    calories = nutrition.calories,
                    protein = nutrition.protein,
                    carbs = nutrition.carbs,
                    fat = nutrition.fat,
                    fiber = nutrition.fiber
                )
            }
        }
    }

    private fun parseCandidates(root: JSONObject): List<LogMealFoodCandidate> {
        val results = mutableListOf<LogMealFoodCandidate>()
        val segmentationResults = root.optJSONArray("segmentation_results")

        if (segmentationResults != null) {
            for (index in 0 until segmentationResults.length()) {
                val item = segmentationResults.optJSONObject(index) ?: continue
                val position = item.optInt("food_item_position", index + 1)
                collectCandidates(item, position, results)
            }
        } else {
            collectCandidates(root, null, results)
        }

        return results.filter { candidate -> candidate.name.isNotBlank() }
    }

    private fun collectCandidates(
        value: Any?,
        foodItemPosition: Int?,
        results: MutableList<LogMealFoodCandidate>
    ) {
        when (value) {
            is JSONObject -> {
                value.findCandidateName()?.let { candidateName ->
                    results.add(
                        LogMealFoodCandidate(
                            name = candidateName,
                            confidence = value.findCandidateConfidence(),
                            foodItemPosition = foodItemPosition,
                            servingGrams = value.findServingGrams(),
                            calories = value.findNutrientValue(ENERGY_KEYWORDS),
                            protein = value.findNutrientValue(PROTEIN_KEYWORDS),
                            carbs = value.findNutrientValue(CARB_KEYWORDS),
                            fat = value.findNutrientValue(FAT_KEYWORDS),
                            fiber = value.findNutrientValue(FIBER_KEYWORDS)
                        )
                    )
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    collectCandidates(value.opt(key), foodItemPosition, results)
                }
            }

            is JSONArray -> {
                for (index in 0 until value.length()) {
                    collectCandidates(value.opt(index), foodItemPosition, results)
                }
            }
        }
    }

    private fun parseNutritionBundle(json: String): NutritionBundle {
        val root = JSONObject(json)
        val perItem = mutableMapOf<Int, NutritionData>()

        when (val perItemValue = root.opt("nutritional_info_per_item")) {
            is JSONObject -> {
                val keys = perItemValue.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val position = key.toIntOrNull() ?: continue
                    val itemObject = perItemValue.optJSONObject(key) ?: continue
                    val data = itemObject.extractNutritionData()
                    if (data.hasNutrition()) perItem[position] = data
                }
            }

            is JSONArray -> {
                for (index in 0 until perItemValue.length()) {
                    val itemObject = perItemValue.optJSONObject(index) ?: continue
                    val position = itemObject.optInt("food_item_position", index + 1)
                    val data = itemObject.extractNutritionData()
                    if (data.hasNutrition()) perItem[position] = data
                }
            }
        }

        return NutritionBundle(
            overall = root.extractNutritionData().takeIf { it.hasNutrition() },
            perItem = perItem
        )
    }

    private fun JSONObject.extractNutritionData(): NutritionData {
        return NutritionData(
            servingGrams = findServingGrams(),
            calories = findNutrientValue(ENERGY_KEYWORDS),
            protein = findNutrientValue(PROTEIN_KEYWORDS),
            carbs = findNutrientValue(CARB_KEYWORDS),
            fat = findNutrientValue(FAT_KEYWORDS),
            fiber = findNutrientValue(FIBER_KEYWORDS)
        )
    }

    private fun JSONObject.findServingGrams(): Double? {
        val possibleKeys = listOf(
            "serving_size",
            "servingSize",
            "quantity",
            "quantity_g",
            "quantityGrams",
            "weight",
            "weight_g",
            "grams",
            "amount"
        )

        for (key in possibleKeys) {
            optNullableDouble(key)?.let { value ->
                if (value > 0.0 && value <= MAX_SERVING_GRAMS) return value
            }
        }

        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val child = opt(key)) {
                is JSONObject -> child.findServingGrams()?.let { return it }
                is JSONArray -> {
                    for (index in 0 until child.length()) {
                        child.optJSONObject(index)?.findServingGrams()?.let { return it }
                    }
                }
            }
        }

        return null
    }

    private fun JSONObject.findNutrientValue(keywords: List<String>): Double? {
        val normalizedKeywords = keywords.map { keyword -> keyword.lowercase(Locale.ROOT) }
        findDirectNutrientValue(normalizedKeywords)?.let { return it }

        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val child = opt(key)) {
                is JSONObject -> child.findNutrientValue(keywords)?.let { return it }
                is JSONArray -> {
                    for (index in 0 until child.length()) {
                        child.optJSONObject(index)?.findNutrientValue(keywords)?.let { return it }
                    }
                }
            }
        }

        return null
    }

    private fun JSONObject.findDirectNutrientValue(
        normalizedKeywords: List<String>
    ): Double? {
        val keys = keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val normalizedKey = key.lowercase(Locale.ROOT)
            val value = opt(key)
            val keyMatches = normalizedKeywords.any { keyword -> normalizedKey.contains(keyword) }

            if (keyMatches) {
                when (value) {
                    is Number -> return value.toDouble()
                    is String -> value.replace(",", ".").toDoubleOrNull()?.let { return it }
                    is JSONObject -> {
                        value.optNullableDouble("value")?.let { amount ->
                            return convertEnergyIfNeeded(
                                label = normalizedKey,
                                unit = value.optString("unit").lowercase(Locale.ROOT),
                                value = amount
                            )
                        }
                        value.optNullableDouble("amount")?.let { return it }
                        value.optNullableDouble("quantity")?.let { return it }
                        value.optNullableDouble("indicatorAmount")?.let { return it }
                    }
                }
            }

            if (value is JSONObject) {
                val label = value.optString("name")
                    .ifBlank { value.optString("label") }
                    .ifBlank { value.optString("nutrientName") }
                    .ifBlank { value.optString("indicatorCode") }
                    .ifBlank { value.optString("code") }
                    .lowercase(Locale.ROOT)
                val nameMatches = normalizedKeywords.any { keyword -> label.contains(keyword) }

                if (nameMatches) {
                    value.optNullableDouble("value")?.let { amount ->
                        return convertEnergyIfNeeded(
                            label = label,
                            unit = value.optString("unit").lowercase(Locale.ROOT),
                            value = amount
                        )
                    }
                    value.optNullableDouble("amount")?.let { return it }
                    value.optNullableDouble("quantity")?.let { return it }
                    value.optNullableDouble("indicatorAmount")?.let { return it }
                }
            }
        }

        return null
    }

    private fun convertEnergyIfNeeded(
        label: String,
        unit: String,
        value: Double
    ): Double {
        val isEnergy = label.contains("energy") ||
                label.contains("calorie") ||
                label.contains("kcal") ||
                label.contains("enerc")

        return if (isEnergy && unit.contains("kj")) {
            value / KILOJOULES_PER_KILOCALORIE
        } else {
            value
        }
    }

    private fun JSONObject.findFirstString(keysToFind: Set<String>): String? {
        val keys = keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = opt(key)

            if (key in keysToFind) {
                val text = value?.toString()?.trim().orEmpty()
                if (text.isNotBlank() && text != "null") return text
            }

            when (value) {
                is JSONObject -> value.findFirstString(keysToFind)?.let { return it }
                is JSONArray -> {
                    for (index in 0 until value.length()) {
                        value.optJSONObject(index)?.findFirstString(keysToFind)?.let { return it }
                    }
                }
            }
        }

        return null
    }

    private fun JSONObject.findCandidateName(): String? {
        return CANDIDATE_NAME_KEYS
            .asSequence()
            .map { key -> optString(key).trim() }
            .firstOrNull { value ->
                value.isNotBlank() &&
                        !value.equals("null", ignoreCase = true) &&
                        value.length >= MIN_CANDIDATE_NAME_LENGTH
            }
    }

    private fun JSONObject.findCandidateConfidence(): Double {
        return CANDIDATE_CONFIDENCE_KEYS
            .asSequence()
            .mapNotNull { key -> optNullableDouble(key) }
            .firstOrNull()
            ?: 0.0
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

    private data class NutritionBundle(
        val overall: NutritionData?,
        val perItem: Map<Int, NutritionData>
    )

    private data class NutritionData(
        val servingGrams: Double?,
        val calories: Double?,
        val protein: Double?,
        val carbs: Double?,
        val fat: Double?,
        val fiber: Double?
    ) {
        fun hasNutrition(): Boolean {
            return (calories ?: 0.0) > 0.0 ||
                    (protein ?: 0.0) > 0.0 ||
                    (carbs ?: 0.0) > 0.0 ||
                    (fat ?: 0.0) > 0.0
        }
    }

    private companion object {
        const val DEFAULT_SERVING_GRAMS = 100.0
        const val MAX_SERVING_GRAMS = 5000.0
        const val KILOJOULES_PER_KILOCALORIE = 4.184
        const val MIN_CANDIDATE_NAME_LENGTH = 2

        val ENERGY_KEYWORDS = listOf("calorie", "calories", "energy", "kcal", "enerc")
        val PROTEIN_KEYWORDS = listOf("protein", "procnt")
        val CARB_KEYWORDS = listOf("carbohydrate", "carbs", "carb", "chocdf")
        val FAT_KEYWORDS = listOf("fat", "lipid", "fats")
        val FIBER_KEYWORDS = listOf("fiber", "fibre", "fibtg")

        val CANDIDATE_NAME_KEYS = listOf(
            "name",
            "foodName",
            "food_name",
            "dishName",
            "dish_name",
            "className",
            "class_name",
            "recognition_name"
        )

        val CANDIDATE_CONFIDENCE_KEYS = listOf(
            "prob",
            "probability",
            "confidence",
            "score",
            "recognition_score"
        )
    }
}
