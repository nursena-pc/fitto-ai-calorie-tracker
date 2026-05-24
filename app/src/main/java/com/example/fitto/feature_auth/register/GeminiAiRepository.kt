package com.example.fitto.feature_auth.register

import android.util.Log
import com.example.fitto.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import java.util.concurrent.TimeUnit

class GeminiAiRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun requestTargetSuggestion(
        prompt: String,
        appLanguage: String
    ): AiTargetSuggestion = withContext(Dispatchers.IO) {
        val rawBody = executeGeminiRequest(prompt)
        parseSuggestion(rawBody, appLanguage.normalizedResponseLanguage())
    }

    suspend fun requestPlanSuggestion(
        prompt: String,
        appLanguage: String
    ): AiPlanSuggestion = withContext(Dispatchers.IO) {
        val rawBody = executeGeminiRequest(prompt)
        parsePlanSuggestion(rawBody, appLanguage.normalizedResponseLanguage())
    }

    private fun executeGeminiRequest(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank()) {
            error("missing_api_key")
        }

        val requestBody = buildRequestBody(prompt)
        val url = GEMINI_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(FITTO_AI_LOG_TAG, "Gemini HTTP ${response.code}: ${rawBody.take(500)}")
                error("gemini_http_${response.code}")
            }

            Log.d(FITTO_AI_LOG_TAG, "Gemini response received successfully.")
            return rawBody
        }
    }

    private fun buildRequestBody(prompt: String) = JSONObject()
        .put(
            "contents",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
            )
        )
        .put(
            "generationConfig",
            JSONObject()
                .put("temperature", 0.2)
                .put("responseMimeType", "application/json")
        )
        .toString()
        .toRequestBody(JSON_MEDIA_TYPE)

    private fun parseSuggestion(
        rawBody: String,
        appLanguage: String
    ): AiTargetSuggestion {
        val json = extractModelJson(
            rawBody = rawBody,
            requiredKeys = setOf("suggestedTargetWeightKg", "title", "explanation", "warning")
        )
        val targetWeight = json.targetWeightKg("suggestedTargetWeightKg")

        if (targetWeight !in MIN_TARGET_WEIGHT_KG..MAX_TARGET_WEIGHT_KG) {
            error("Gemini returned an invalid target weight.")
        }

        return AiTargetSuggestion(
            suggestedTargetWeightKg = targetWeight,
            suggestedTargetRangeMinKg = json.optionalWeightKg("suggestedTargetRangeMinKg"),
            suggestedTargetRangeMaxKg = json.optionalWeightKg("suggestedTargetRangeMaxKg"),
            title = json.optString("title")
                .ifBlank { fallbackTitle(appLanguage) }
                .toSingleShortSentence(maxWords = 8),
            explanation = json.optString("explanation")
                .ifBlank { fallbackExplanation(appLanguage) }
                .toSingleShortSentence(maxWords = 24),
            timelineComment = json.optString("timelineComment")
                .toSingleShortSentence(maxWords = 20),
            motivationComment = json.optString("motivationComment")
                .toSingleShortSentence(maxWords = 20),
            riskLevel = AiTargetRiskLevel.fromRaw(json.optString("riskLevel")),
            reasons = json.optJSONArray("reasons").toStringList(maxItems = 3, maxWords = 16),
            alternativeTargetsKg = json.optJSONArray("alternativeTargetsKg").toIntList(maxItems = 5),
            warning = json.optString("warning")
                .ifBlank { fallbackWarning(appLanguage) }
                .toSingleShortSentence(maxWords = 12)
        )
    }

    private fun parsePlanSuggestion(
        rawBody: String,
        appLanguage: String
    ): AiPlanSuggestion {
        val json = extractModelJson(
            rawBody = rawBody,
            requiredKeys = setOf("planStatus", "recommendedDailyCalories", "recommendedWeeklyChangeKg", "summary", "warning")
        )

        return AiPlanSuggestion(
            planStatus = AiPlanStatus.fromRaw(json.optString("planStatus")),
            recommendedDailyCalories = json.targetWeightKg("recommendedDailyCalories"),
            recommendedWeeklyChangeKg = json.optDouble("recommendedWeeklyChangeKg", 0.0),
            targetComment = json.optString("targetComment").toSingleShortSentence(maxWords = 24),
            timelineComment = json.optString("timelineComment").toSingleShortSentence(maxWords = 24),
            calorieComment = json.optString("calorieComment").toSingleShortSentence(maxWords = 24),
            motivationComment = json.optString("motivationComment").toSingleShortSentence(maxWords = 24),
            summary = json.optString("summary")
                .ifBlank { fallbackPlanSummary(appLanguage) }
                .toSingleShortSentence(maxWords = 26),
            warning = json.optString("warning")
                .ifBlank { fallbackWarning(appLanguage) }
                .toSingleShortSentence(maxWords = 12)
        )
    }

    private fun fallbackTitle(appLanguage: String): String {
        return if (appLanguage == RESPONSE_LANGUAGE_EN) {
            "Fitto AI suggestion"
        } else {
            "Fitto AI önerisi"
        }
    }

    private fun fallbackExplanation(appLanguage: String): String {
        return if (appLanguage == RESPONSE_LANGUAGE_EN) {
            "This can be a sustainable starting target."
        } else {
            "Bu hedef sürdürülebilir bir başlangıç hedefi olabilir."
        }
    }

    private fun fallbackPlanSummary(appLanguage: String): String {
        return if (appLanguage == RESPONSE_LANGUAGE_EN) {
            "This plan can offer a balanced starting point for your goal."
        } else {
            "Bu plan hedefin için dengeli bir başlangıç sunabilir."
        }
    }

    private fun fallbackWarning(appLanguage: String): String {
        return if (appLanguage == RESPONSE_LANGUAGE_EN) {
            "This is not medical advice."
        } else {
            "Bu öneri tıbbi tavsiye değildir."
        }
    }

    private fun String.normalizedResponseLanguage(): String {
        return if (startsWith(RESPONSE_LANGUAGE_EN, ignoreCase = true)) {
            RESPONSE_LANGUAGE_EN
        } else {
            RESPONSE_LANGUAGE_TR
        }
    }

    private fun JSONObject.targetWeightKg(key: String): Int {
        val rawValue = opt(key) ?: return 0
        return when (rawValue) {
            is Number -> rawValue.toDouble().roundToInt()
            is String -> rawValue
                .replace(',', '.')
                .let { NUMBER_REGEX.find(it)?.value }
                ?.toDoubleOrNull()
                ?.roundToInt()
                ?: 0
            else -> 0
        }
    }

    private fun JSONObject.optionalWeightKg(key: String): Int? {
        return targetWeightKg(key).takeIf { it > 0 }
    }

    private fun JSONArray?.toStringList(maxItems: Int, maxWords: Int): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
            .take(maxItems)
            .map { it.toSingleShortSentence(maxWords) }
    }

    private fun JSONArray?.toIntList(maxItems: Int): List<Int> {
        if (this == null) return emptyList()
        return (0 until length())
            .mapNotNull { index -> opt(index) }
            .mapNotNull { value ->
                when (value) {
                    is Number -> value.toDouble().roundToInt()
                    is String -> NUMBER_REGEX.find(value)?.value?.toDoubleOrNull()?.roundToInt()
                    else -> null
                }
            }
            .filter { it in MIN_TARGET_WEIGHT_KG..MAX_TARGET_WEIGHT_KG }
            .take(maxItems)
    }

    private fun String.toSingleShortSentence(maxWords: Int): String {
        val cleaned = trim().replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return ""

        val firstSentenceEnd = listOf('.', '!', '?')
            .map { cleaned.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()

        val firstSentence = if (firstSentenceEnd != null) {
            cleaned.substring(0, firstSentenceEnd + 1).trim()
        } else {
            cleaned
        }

        val words = firstSentence.split(" ").filter { it.isNotBlank() }
        if (words.size <= maxWords) return firstSentence

        return words.take(maxWords).joinToString(" ").trimEnd(',', ';', ':', '.') + "."
    }

    private fun extractModelJson(
        rawBody: String,
        requiredKeys: Set<String>
    ): JSONObject {
        val root = JSONObject(rawBody)

        findJsonObject(root, requiredKeys)?.let { return it }

        val candidateTexts = root.candidateTextParts()
        candidateTexts.forEach { text ->
            parseExpectedJsonFromText(text, requiredKeys)?.let { return it }
        }

        root.collectStringValues().forEach { text ->
            parseExpectedJsonFromText(text, requiredKeys)?.let { return it }
        }

        val finishReason = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optString("finishReason")
            .orEmpty()

        Log.w(
            FITTO_AI_LOG_TAG,
            "Gemini response JSON parse failed. finishReason=$finishReason " +
                "texts=${candidateTexts.map { it.take(160) }} raw=${rawBody.take(800)}"
        )
        error("Gemini response did not contain valid JSON.")
    }

    private fun findJsonObject(
        value: Any?,
        requiredKeys: Set<String>
    ): JSONObject? {
        return when (value) {
            is JSONObject -> {
                if (requiredKeys.all { value.has(it) }) {
                    value
                } else {
                    value.keys().asSequence()
                        .mapNotNull { key -> findJsonObject(value.opt(key), requiredKeys) }
                        .firstOrNull()
                }
            }
            is JSONArray -> {
                (0 until value.length())
                    .asSequence()
                    .mapNotNull { index -> findJsonObject(value.opt(index), requiredKeys) }
                    .firstOrNull()
            }
            else -> null
        }
    }

    private fun JSONObject.candidateTextParts(): List<String> {
        val texts = mutableListOf<String>()
        val candidates = optJSONArray("candidates") ?: return texts

        for (candidateIndex in 0 until candidates.length()) {
            val parts = candidates.optJSONObject(candidateIndex)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: continue

            for (partIndex in 0 until parts.length()) {
                val text = parts.optJSONObject(partIndex)
                    ?.optString("text")
                    .orEmpty()
                if (text.isNotBlank()) texts += text
            }
        }

        return texts
    }

    private fun JSONObject.collectStringValues(): List<String> {
        val strings = mutableListOf<String>()

        fun collect(value: Any?) {
            when (value) {
                is JSONObject -> value.keys().forEach { key -> collect(value.opt(key)) }
                is JSONArray -> for (index in 0 until value.length()) collect(value.opt(index))
                is String -> if (value.isNotBlank()) strings += value
            }
        }

        collect(this)
        return strings.distinct()
    }

    private fun parseExpectedJsonFromText(
        text: String,
        requiredKeys: Set<String>
    ): JSONObject? {
        val cleaned = text
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        jsonObjectCandidates(cleaned).forEach { candidate ->
            runCatching { JSONObject(candidate) }
                .getOrNull()
                ?.takeIf { json -> requiredKeys.all { json.has(it) } }
                ?.let { return it }
        }

        return null
    }

    private fun jsonObjectCandidates(text: String): List<String> {
        val candidates = mutableListOf<String>()
        var depth = 0
        var startIndex = -1
        var inString = false
        var isEscaped = false

        text.forEachIndexed { index, char ->
            if (isEscaped) {
                isEscaped = false
                return@forEachIndexed
            }

            if (char == '\\' && inString) {
                isEscaped = true
                return@forEachIndexed
            }

            if (char == '"') {
                inString = !inString
                return@forEachIndexed
            }

            if (inString) return@forEachIndexed

            when (char) {
                '{' -> {
                    if (depth == 0) startIndex = index
                    depth++
                }
                '}' -> {
                    if (depth > 0) depth--
                    if (depth == 0 && startIndex >= 0) {
                        candidates += text.substring(startIndex, index + 1)
                        startIndex = -1
                    }
                }
            }
        }

        return candidates
    }

    private companion object {
        const val MODEL_NAME = "gemini-3.1-flash-lite"
        const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
        const val MIN_TARGET_WEIGHT_KG = 25
        const val MAX_TARGET_WEIGHT_KG = 300
        const val FITTO_AI_LOG_TAG = "FittoAI"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val RESPONSE_LANGUAGE_TR = "tr"
        const val RESPONSE_LANGUAGE_EN = "en"
        val NUMBER_REGEX = Regex("\\d+(?:\\.\\d+)?")
    }
}
