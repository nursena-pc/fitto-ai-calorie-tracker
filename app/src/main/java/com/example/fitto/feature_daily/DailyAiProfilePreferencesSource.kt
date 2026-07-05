package com.example.fitto.feature_daily

import android.content.Context
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore

internal fun interface DailyAiProfilePreferencesSource {
    fun load(): DailyAiProfilePreferences
}

internal class ProfileStoreDailyAiProfilePreferencesSource(
    context: Context
) : DailyAiProfilePreferencesSource {

    private val appContext = context.applicationContext

    override fun load(): DailyAiProfilePreferences {
        return DailyAiProfilePreferences(
            dietPreferences = ProfilePreferenceStore.nutritionDietPreferencesText(appContext),
            allergies = ProfilePreferenceStore.getNutritionAllergies(appContext),
            dislikedFoods = ProfilePreferenceStore.getNutritionDislikedFoods(appContext),
            mealRoutine = ProfilePreferenceStore.nutritionMealRoutineText(appContext)
        )
    }
}
