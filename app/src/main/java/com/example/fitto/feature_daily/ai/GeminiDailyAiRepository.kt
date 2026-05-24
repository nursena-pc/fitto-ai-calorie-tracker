package com.example.fitto.feature_daily.ai

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
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiDailyAiRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun requestDailyAnalysis(prompt: String, appLanguage: String): AiDailyAnalysisResult =
        withContext(Dispatchers.IO) {
            val rawBody = executeGeminiRequest(prompt)
            parseDailyAnalysis(rawBody, appLanguage.normalizedResponseLanguage())
        }

    private fun executeGeminiRequest(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank()) {
            error(ERROR_MISSING_API_KEY)
        }

        val requestBody = buildRequestBody(prompt)
        val url = GEMINI_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(FITTO_AI_LOG_TAG, "Daily Gemini HTTP ${response.code}: ${rawBody.take(500)}")
                    error("gemini_http_${response.code}")
                }

                Log.d(FITTO_AI_LOG_TAG, "Daily Gemini response received successfully.")
                return rawBody
            }
        } catch (exception: IOException) {
            Log.w(FITTO_AI_LOG_TAG, "Daily Gemini network error: ${exception.message}")
            error(ERROR_NETWORK)
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

    private fun parseDailyAnalysis(
        rawBody: String,
        appLanguage: String
    ): AiDailyAnalysisResult {
        val root = runCatching { JSONObject(rawBody) }.getOrNull()
        if (root == null) {
            val text = rawBody.toSingleShortBlock(maxSentences = 6)
            return plainTextResult(text, appLanguage)
        }

        findDailyJsonObject(root)?.let { json ->
            val comments = (json.optJSONArray("mealComments") ?: json.optJSONArray("entryComments"))
                ?.toMealComments()
                .orEmpty()

            return AiDailyAnalysisResult(
                status = AiDailyStatusLevel.from(json.optString("status")),
                mealComments = comments,
                waterComment = json.optString("waterComment")
                    .toSingleShortBlock(maxSentences = 1),
                exerciseComment = json.optString("exerciseComment")
                    .toSingleShortBlock(maxSentences = 1),
                generalComment = json.optString("generalComment")
                    .ifBlank { fallbackGeneralComment(appLanguage) }
                    .toSingleShortBlock(maxSentences = 2),
                suggestion = json.optString("suggestion")
                    .ifBlank { fallbackSuggestion(appLanguage) }
                    .toSingleShortBlock(maxSentences = 1)
            )
        }

        val candidateText = root.candidateTextParts()
            .firstOrNull { it.isNotBlank() }
            ?.toSingleShortBlock(maxSentences = 6)
            .orEmpty()

        if (candidateText.isNotBlank()) {
            return plainTextResult(candidateText, appLanguage)
        }

        Log.w(FITTO_AI_LOG_TAG, "Daily Gemini response parse failed: ${rawBody.take(800)}")
        error(ERROR_INVALID_RESPONSE)
    }

    private fun JSONArray.toMealComments(): List<AiDailyMealComment> {
        val items = mutableListOf<AiDailyMealComment>()
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val mealName = item.optString("mealName")
                .ifBlank { item.optString("entryName") }
                .toSingleShortSentence(maxWords = 8)
            val comment = item.optString("comment").toSingleShortSentence(maxWords = 28)
            if (mealName.isBlank() && comment.isBlank()) continue
            items += AiDailyMealComment(
                mealName = mealName,
                comment = comment,
                level = AiDailyStatusLevel.from(item.optString("level"))
            )
        }
        return items
    }

    private fun plainTextResult(text: String, appLanguage: String): AiDailyAnalysisResult {
        return AiDailyAnalysisResult(
            status = AiDailyStatusLevel.YELLOW,
            mealComments = emptyList(),
            waterComment = "",
            exerciseComment = "",
            generalComment = text.ifBlank { fallbackGeneralComment(appLanguage) },
            suggestion = "",
            plainText = text.ifBlank { fallbackGeneralComment(appLanguage) }
        )
    }

    private fun findDailyJsonObject(value: Any?): JSONObject? {
        return when (value) {
            is JSONObject -> {
                if (DAILY_KEYS.any { value.has(it) }) {
                    value
                } else {
                    value.keys().asSequence()
                        .mapNotNull { key -> findDailyJsonObject(value.opt(key)) }
                        .firstOrNull()
                }
            }
            is JSONArray -> {
                (0 until value.length())
                    .asSequence()
                    .mapNotNull { index -> findDailyJsonObject(value.opt(index)) }
                    .firstOrNull()
            }
            is String -> parseExpectedJsonFromText(value)
            else -> null
        }
    }

    private fun parseExpectedJsonFromText(text: String): JSONObject? {
        val cleaned = text
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        jsonObjectCandidates(cleaned).forEach { candidate ->
            runCatching { JSONObject(candidate) }
                .getOrNull()
                ?.takeIf { json -> DAILY_KEYS.any { json.has(it) } }
                ?.let { return it }
        }

        return null
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

    private fun fallbackGeneralComment(appLanguage: String): String {
        return if (appLanguage == RESPONSE_LANGUAGE_EN) {
            "Your daily records were reviewed."
        } else {
            "Günlük kayıtların değerlendirildi."
        }
    }

    private fun fallbackSuggestion(appLanguage: String): String {
        return if (appLanguage == RESPONSE_LANGUAGE_EN) {
            "You can keep the rest of the day balanced with small choices."
        } else {
            "Günün kalanını küçük seçimlerle dengeli tamamlayabilirsin."
        }
    }

    private fun String.normalizedResponseLanguage(): String {
        return if (startsWith(RESPONSE_LANGUAGE_EN, ignoreCase = true)) {
            RESPONSE_LANGUAGE_EN
        } else {
            RESPONSE_LANGUAGE_TR
        }
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

    private fun String.toSingleShortBlock(maxSentences: Int): String {
        val cleaned = trim().replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return ""

        val sentences = Regex("[^.!?]+[.!?]?")
            .findAll(cleaned)
            .map { it.value.trim() }
            .filter { it.isNotBlank() }
            .take(maxSentences)
            .toList()

        return sentences.joinToString(" ").ifBlank { cleaned }
    }

    companion object {
        const val ERROR_MISSING_API_KEY = "missing_api_key"
        const val ERROR_NETWORK = "network_error"
        const val ERROR_INVALID_RESPONSE = "invalid_response"
        const val ERROR_RATE_LIMIT = "gemini_http_429"

        private const val MODEL_NAME = "gemini-3.1-flash-lite"
        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
        private const val FITTO_AI_LOG_TAG = "FittoAI"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val RESPONSE_LANGUAGE_TR = "tr"
        private const val RESPONSE_LANGUAGE_EN = "en"
        private val DAILY_KEYS = setOf("status", "mealComments", "entryComments", "waterComment", "exerciseComment", "generalComment", "suggestion")
    }
}
