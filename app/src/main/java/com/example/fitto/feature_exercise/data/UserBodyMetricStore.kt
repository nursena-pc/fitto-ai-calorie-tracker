package com.example.fitto.feature_exercise.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard

class UserBodyMetricStore(
    context: Context
) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun getWeightKg(): Double {
        return prefs.getFloat(KEY_WEIGHT_KG, DEFAULT_WEIGHT_KG.toFloat())
            .toDouble()
            .takeIf { it in MIN_WEIGHT_KG..MAX_WEIGHT_KG }
            ?: DEFAULT_WEIGHT_KG
    }

    fun saveWeightKg(weightKg: Double) {
        if (weightKg !in MIN_WEIGHT_KG..MAX_WEIGHT_KG) return

        prefs.edit()
            .putFloat(KEY_WEIGHT_KG, weightKg.toFloat())
            .apply()
    }

    fun hasSavedWeight(): Boolean {
        return prefs.contains(KEY_WEIGHT_KG)
    }

    companion object {
        private const val PREF_NAME = "fitto_user_body_metrics"
        private const val KEY_WEIGHT_KG = "weight_kg"

        const val DEFAULT_WEIGHT_KG = 70.0
        const val MIN_WEIGHT_KG = 25.0
        const val MAX_WEIGHT_KG = 300.0
    }
}