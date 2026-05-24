package com.example.fitto.data.remote.logmeal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class LogMealFoodRecognitionClient(
    private val apiUserToken: String
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun recognizeFood(
        imageFile: File,
        languageCode: String
    ): List<LogMealFoodCandidate> {
        if (apiUserToken.isBlank()) {
            Log.e(TAG, "LogMeal token is blank. Add LOGMEAL_API_USER_TOKEN to local.properties.")
            return emptyList()
        }

        val uploadFile = prepareImageForUpload(imageFile) ?: return emptyList()

        val segmentationBody = uploadImageForSegmentation(
            uploadFile = uploadFile,
            languageCode = languageCode
        ) ?: return emptyList()

        val segmentationRoot = JSONObject(segmentationBody)

        val imageId = segmentationRoot.findFirstString(
            keysToFind = setOf("imageId", "image_id")
        )

        Log.d(TAG, "LogMeal imageId=$imageId")

        val candidates = parseCandidates(segmentationBody)
            .distinctBy { candidate ->
                candidate.name.lowercase(Locale.ROOT)
            }
            .sortedByDescending { candidate ->
                candidate.confidence
            }

        Log.d(TAG, "LogMeal parsed candidates=${candidates.size}")

        if (imageId.isNullOrBlank()) {
            Log.e(TAG, "LogMeal imageId not found. Nutrition request skipped.")
            return candidates
        }

        val nutritionBundle = fetchNutritionForImage(imageId)

        if (nutritionBundle == null) {
            Log.e(TAG, "LogMeal nutrition is null. Returning candidates without nutrition.")
            return candidates
        }

        return mergeCandidatesWithNutrition(
            candidates = candidates,
            nutritionBundle = nutritionBundle
        )
    }

    private fun uploadImageForSegmentation(
        uploadFile: File,
        languageCode: String
    ): String? {
        val url = "https://api.logmeal.com/v2/image/segmentation/complete/v1.0"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("language", languageCode.toLogMealLanguage())
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "image",
                filename = uploadFile.name,
                body = uploadFile.asRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiUserToken")
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        Log.d(TAG, "LogMeal request started. file=${uploadFile.name} size=${uploadFile.length()}")

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                Log.d(TAG, "LogMeal response code=${response.code}")
                Log.d(TAG, "LogMeal body length=${body.length}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "LogMeal request failed. code=${response.code} body=$body")
                    return null
                }

                body.takeIf { it.isNotBlank() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "LogMeal request exception", exception)
            null
        }
    }

    private fun fetchNutritionForImage(imageId: String): NutritionBundle? {
        val url = "https://api.logmeal.com/v2/nutrition/recipe/nutritionalInfo/v1.0"
            .toHttpUrl()
            .newBuilder()
            .build()

        val requestJson = JSONObject()
            .put("imageId", imageId)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiUserToken")
            .header("Accept", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        Log.d(TAG, "LogMeal nutrition request started. imageId=$imageId")

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                Log.d(TAG, "LogMeal nutrition response code=${response.code}")
                Log.d(TAG, "LogMeal nutrition body length=${body.length}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "LogMeal nutrition failed. code=${response.code} body=$body")
                    return null
                }

                if (body.isBlank()) return null

                parseNutritionBundle(body)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "LogMeal nutrition request exception", exception)
            null
        }
    }

    private fun prepareImageForUpload(sourceFile: File): File? {
        val originalBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return null
        val scaledBitmap = originalBitmap.scaleDownIfNeeded(maxSide = 1000)

        val outputFile = File(
            sourceFile.parentFile,
            "logmeal_${sourceFile.nameWithoutExtension}.jpg"
        )

        var quality = 88

        do {
            FileOutputStream(outputFile).use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }

            quality -= 8
        } while (outputFile.length() > MAX_UPLOAD_BYTES && quality >= 48)

        if (scaledBitmap != originalBitmap) {
            originalBitmap.recycle()
            scaledBitmap.recycle()
        }

        Log.d(TAG, "Prepared image size=${outputFile.length()} quality=${quality + 8}")
        return outputFile
    }

    private fun Bitmap.scaleDownIfNeeded(maxSide: Int): Bitmap {
        val currentMaxSide = maxOf(width, height)
        if (currentMaxSide <= maxSide) return this

        val ratio = maxSide.toFloat() / currentMaxSide.toFloat()
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(
            this,
            targetWidth,
            targetHeight,
            true
        )
    }

    private fun parseCandidates(json: String): List<LogMealFoodCandidate> {
        val root = JSONObject(json)
        val results = mutableListOf<LogMealFoodCandidate>()

        val segmentationResults = root.optJSONArray("segmentation_results")

        if (segmentationResults != null) {
            for (index in 0 until segmentationResults.length()) {
                val item = segmentationResults.optJSONObject(index) ?: continue
                val position = item.optInt("food_item_position", index + 1)

                collectCandidates(
                    value = item,
                    foodItemPosition = position,
                    results = results
                )
            }
        } else {
            collectCandidates(
                value = root,
                foodItemPosition = null,
                results = results
            )
        }

        return results.filter { candidate ->
            candidate.name.isNotBlank()
        }
    }

    private fun collectCandidates(
        value: Any?,
        foodItemPosition: Int?,
        results: MutableList<LogMealFoodCandidate>
    ) {
        when (value) {
            is JSONObject -> {
                val candidateName = value.findCandidateName()

                if (candidateName != null) {
                    results.add(
                        LogMealFoodCandidate(
                            name = candidateName,
                            confidence = value.findCandidateConfidence(),
                            foodItemPosition = foodItemPosition,
                            servingGrams = value.findServingGrams(),
                            calories = value.findNutrientValue(
                                keywords = listOf("calorie", "calories", "energy", "kcal", "enerc")
                            ),
                            protein = value.findNutrientValue(
                                keywords = listOf("protein", "procnt")
                            ),
                            carbs = value.findNutrientValue(
                                keywords = listOf("carbohydrate", "carbs", "carb", "chocdf")
                            ),
                            fat = value.findNutrientValue(
                                keywords = listOf("fat", "lipid", "fats")
                            ),
                            fiber = value.findNutrientValue(
                                keywords = listOf("fiber", "fibre", "fibtg")
                            )
                        )
                    )
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()

                    collectCandidates(
                        value = value.opt(key),
                        foodItemPosition = foodItemPosition,
                        results = results
                    )
                }
            }

            is JSONArray -> {
                for (index in 0 until value.length()) {
                    collectCandidates(
                        value = value.opt(index),
                        foodItemPosition = foodItemPosition,
                        results = results
                    )
                }
            }
        }
    }

    private fun parseNutritionBundle(json: String): NutritionBundle {
        val root = JSONObject(json)
        val perItem = mutableMapOf<Int, NutritionData>()

        val perItemValue = root.opt("nutritional_info_per_item")

        when (perItemValue) {
            is JSONObject -> {
                val keys = perItemValue.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    val position = key.toIntOrNull() ?: continue
                    val itemObject = perItemValue.optJSONObject(key) ?: continue
                    val data = itemObject.extractNutritionData()

                    if (data.hasNutrition()) {
                        perItem[position] = data
                    }
                }
            }

            is JSONArray -> {
                for (index in 0 until perItemValue.length()) {
                    val itemObject = perItemValue.optJSONObject(index) ?: continue
                    val position = itemObject.optInt("food_item_position", index + 1)
                    val data = itemObject.extractNutritionData()

                    if (data.hasNutrition()) {
                        perItem[position] = data
                    }
                }
            }
        }

        val overall = root.extractNutritionData().takeIf { data ->
            data.hasNutrition()
        }

        Log.d(
            TAG,
            "Nutrition parsed. overall=${overall != null}, perItem=${perItem.size}"
        )

        return NutritionBundle(
            overall = overall,
            perItem = perItem
        )
    }

    private fun mergeCandidatesWithNutrition(
        candidates: List<LogMealFoodCandidate>,
        nutritionBundle: NutritionBundle
    ): List<LogMealFoodCandidate> {
        if (candidates.isEmpty()) return emptyList()

        return candidates.mapIndexed { index, candidate ->
            val itemNutrition = candidate.foodItemPosition
                ?.let { position -> nutritionBundle.perItem[position] }

            val nutrition = itemNutrition
                ?: if (index == 0) nutritionBundle.overall else null

            if (nutrition == null || !nutrition.hasNutrition()) {
                candidate
            } else {
                candidate.copy(
                    servingGrams = nutrition.servingGrams ?: candidate.servingGrams ?: 100.0,
                    calories = nutrition.calories,
                    protein = nutrition.protein,
                    carbs = nutrition.carbs,
                    fat = nutrition.fat,
                    fiber = nutrition.fiber
                )
            }
        }
    }

    private fun JSONObject.extractNutritionData(): NutritionData {
        return NutritionData(
            servingGrams = findServingGrams(),
            calories = findNutrientValue(
                keywords = listOf("calorie", "calories", "energy", "kcal", "enerc")
            ),
            protein = findNutrientValue(
                keywords = listOf("protein", "procnt")
            ),
            carbs = findNutrientValue(
                keywords = listOf("carbohydrate", "carbs", "carb", "chocdf")
            ),
            fat = findNutrientValue(
                keywords = listOf("fat", "lipid", "fats")
            ),
            fiber = findNutrientValue(
                keywords = listOf("fiber", "fibre", "fibtg")
            )
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
                if (value > 0.0 && value <= 5000.0) return value
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

    private fun JSONObject.findNutrientValue(
        keywords: List<String>
    ): Double? {
        val normalizedKeywords = keywords.map { keyword ->
            keyword.lowercase(Locale.ROOT)
        }

        findDirectNutrientValue(normalizedKeywords)?.let { value ->
            return value
        }

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

            val keyMatches = normalizedKeywords.any { keyword ->
                normalizedKey.contains(keyword)
            }

            if (keyMatches) {
                when (value) {
                    is Number -> return value.toDouble()

                    is String -> {
                        value.replace(",", ".").toDoubleOrNull()?.let { return it }
                    }

                    is JSONObject -> {
                        value.optNullableDouble("value")?.let { amount ->
                            val unit = value.optString("unit").lowercase(Locale.ROOT)

                            return convertEnergyIfNeeded(
                                label = normalizedKey,
                                unit = unit,
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

                val nameMatches = normalizedKeywords.any { keyword ->
                    label.contains(keyword)
                }

                if (nameMatches) {
                    value.optNullableDouble("value")?.let { amount ->
                        val unit = value.optString("unit").lowercase(Locale.ROOT)

                        return convertEnergyIfNeeded(
                            label = label,
                            unit = unit,
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
            value / 4.184
        } else {
            value
        }
    }

    private fun JSONObject.findFirstString(
        keysToFind: Set<String>
    ): String? {
        val keys = keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = opt(key)

            if (key in keysToFind) {
                val text = value?.toString()?.trim().orEmpty()

                if (text.isNotBlank() && text != "null") {
                    return text
                }
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
        val possibleKeys = listOf(
            "name",
            "foodName",
            "food_name",
            "dishName",
            "dish_name",
            "className",
            "class_name",
            "recognition_name"
        )

        return possibleKeys
            .asSequence()
            .map { key -> optString(key).trim() }
            .firstOrNull { value ->
                value.isNotBlank() &&
                        !value.equals("null", ignoreCase = true) &&
                        value.length >= 2
            }
    }

    private fun JSONObject.findCandidateConfidence(): Double {
        val possibleKeys = listOf(
            "prob",
            "probability",
            "confidence",
            "score",
            "recognition_score"
        )

        return possibleKeys
            .asSequence()
            .mapNotNull { key ->
                optNullableDouble(key)
            }
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

    private fun String.toLogMealLanguage(): String {
        return when (lowercase(Locale.ROOT)) {
            "tr", "tur" -> "tur"
            "es", "spa" -> "spa"
            "fr", "fre", "fra" -> "fre"
            "de", "ger", "deu" -> "ger"
            "it", "ita" -> "ita"
            "nl", "dut", "nld" -> "dut"
            else -> "eng"
        }
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
        const val TAG = "LogMealClient"
        const val MAX_UPLOAD_BYTES = 1_000_000L
    }
}