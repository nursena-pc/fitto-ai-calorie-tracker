package com.example.fitto.feature_dietplans.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard

class UserNutritionTargetStore(context: Context) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun getDailyTargetCalories(): Int {
        return prefs.getInt(KEY_DAILY_TARGET_CALORIES, DEFAULT_DAILY_TARGET_CALORIES)
            .coerceIn(MIN_DAILY_TARGET_CALORIES, MAX_DAILY_TARGET_CALORIES)
    }

    fun saveDailyTargetCalories(calories: Int) {
        prefs.edit()
            .putInt(
                KEY_DAILY_TARGET_CALORIES,
                calories.coerceIn(MIN_DAILY_TARGET_CALORIES, MAX_DAILY_TARGET_CALORIES)
            )
            .apply()
    }

    companion object {
        private const val PREF_NAME = "fitto_user_nutrition_targets"
        private const val KEY_DAILY_TARGET_CALORIES = "daily_target_calories"

        const val DEFAULT_DAILY_TARGET_CALORIES = 1800
        const val MIN_DAILY_TARGET_CALORIES = 1200
        const val MAX_DAILY_TARGET_CALORIES = 3200
    }
}
