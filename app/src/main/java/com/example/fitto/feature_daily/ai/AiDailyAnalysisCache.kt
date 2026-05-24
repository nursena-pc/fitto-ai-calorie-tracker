package com.example.fitto.feature_daily.ai

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import org.json.JSONObject

class AiDailyAnalysisCache(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = UserSessionDataGuard.scopedSharedPreferences(appContext, PREF_NAME)

    fun get(dateIso: String, triggerType: AiDailyTriggerType, dataSignature: String = ""): CachedDailyAiMessage? {
        val raw = prefs.getString(messageKey(dateIso, triggerType, dataSignature), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            CachedDailyAiMessage(
                date = json.optString("date"),
                triggerType = AiDailyTriggerType.valueOf(json.optString("triggerType")),
                aiMessage = json.optString("aiMessage"),
                createdAt = json.optLong("createdAt"),
                source = json.optString("source"),
                uid = json.optString("uid"),
                totalCalories = json.optInt("totalCalories", -1),
                mealCount = json.optInt("mealCount", -1),
                waterMl = json.optInt("waterMl", -1),
                proteinGrams = json.optDouble("proteinGrams", Double.NaN),
                exerciseSignature = json.optString("exerciseSignature"),
                nutritionPreferenceSignature = json.optString("nutritionPreferenceSignature")
            )
        }.getOrNull()
    }

    fun save(
        dateIso: String,
        triggerType: AiDailyTriggerType,
        aiMessage: String,
        totalCalories: Int,
        mealCount: Int,
        waterMl: Int,
        proteinGrams: Double,
        exerciseSignature: String,
        nutritionPreferenceSignature: String,
        dataSignature: String = "",
        createdAt: Long = System.currentTimeMillis()
    ) {
        val uid = currentUid()
        val json = JSONObject()
            .put("date", dateIso)
            .put("triggerType", triggerType.name)
            .put("aiMessage", aiMessage)
            .put("createdAt", createdAt)
            .put("source", CACHE_SOURCE)
            .put("uid", uid)
            .put("totalCalories", totalCalories)
            .put("mealCount", mealCount)
            .put("waterMl", waterMl)
            .put("proteinGrams", proteinGrams)
            .put("exerciseSignature", exerciseSignature)
            .put("nutritionPreferenceSignature", nutritionPreferenceSignature)

        prefs.edit()
            .putString(messageKey(dateIso, triggerType, dataSignature), json.toString())
            .apply()
    }

    fun clear(dateIso: String, triggerType: AiDailyTriggerType, dataSignature: String = "") {
        prefs.edit()
            .remove(messageKey(dateIso, triggerType, dataSignature))
            .apply()
    }

    fun clearAllForCurrentUser() {
        val uidPrefix = currentUidPrefix()
        prefs.edit().apply {
            prefs.all.keys
                .filter { key -> key.startsWith(uidPrefix) || key.startsWith(LEGACY_PREFIX) }
                .forEach { remove(it) }
            apply()
        }
    }

    fun hasAutoAttempted(dateIso: String, triggerType: AiDailyTriggerType, dataSignature: String = ""): Boolean {
        return prefs.getBoolean(autoAttemptKey(dateIso, triggerType, dataSignature), false)
    }

    fun markAutoAttempted(dateIso: String, triggerType: AiDailyTriggerType, dataSignature: String = "") {
        prefs.edit()
            .putBoolean(autoAttemptKey(dateIso, triggerType, dataSignature), true)
            .apply()
    }

    fun getCooldownUntil(): Long {
        return prefs.getLong(cooldownKey(), 0L)
    }

    fun setCooldownFor(milliseconds: Long) {
        prefs.edit()
            .putLong(cooldownKey(), System.currentTimeMillis() + milliseconds)
            .apply()
    }

    private fun messageKey(dateIso: String, triggerType: AiDailyTriggerType, dataSignature: String): String {
        return "${currentUidPrefix()}daily_ai_message_${dateIso}_${triggerType.name}_${dataSignature.cacheKeySuffix()}"
    }

    private fun autoAttemptKey(dateIso: String, triggerType: AiDailyTriggerType, dataSignature: String): String {
        return "${currentUidPrefix()}daily_ai_auto_attempt_${dateIso}_${triggerType.name}_${dataSignature.cacheKeySuffix()}"
    }

    private fun String.cacheKeySuffix(): String {
        return if (isBlank()) "legacy" else Integer.toHexString(hashCode())
    }

    private fun cooldownKey(): String {
        return "${currentUidPrefix()}$KEY_COOLDOWN_UNTIL"
    }

    private fun currentUidPrefix(): String {
        return "uid_${currentUid()}_"
    }

    private fun currentUid(): String {
        return UserSessionDataGuard.getActiveUid(appContext) ?: "guest"
    }

    companion object {
        const val CACHE_SOURCE = "fitto_daily_ai_v14_account_scoped"
        private const val PREF_NAME = "fitto_daily_ai_cache"
        private const val KEY_COOLDOWN_UNTIL = "daily_ai_cooldown_until"
        private const val LEGACY_PREFIX = "daily_ai_"
    }
}

data class CachedDailyAiMessage(
    val date: String,
    val triggerType: AiDailyTriggerType,
    val aiMessage: String,
    val createdAt: Long,
    val source: String,
    val uid: String,
    val totalCalories: Int,
    val mealCount: Int,
    val waterMl: Int,
    val proteinGrams: Double,
    val exerciseSignature: String,
    val nutritionPreferenceSignature: String
)
