package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyTriggerType

data class DailyAiSnapshot(
    val dateIso: String,
    val userName: String,
    val goalType: String,
    val dailyCalorieTarget: Int,
    val totalCalories: Int,
    val proteinGrams: Double,
    val proteinTargetGrams: Int,
    val carbGrams: Double,
    val fatGrams: Double,
    val waterMl: Int,
    val waterGoalMl: Int,
    val triggerType: AiDailyTriggerType,
    val meals: List<DailyAiMealSnapshot>,
    val exerciseLogCount: Int,
    val exerciseDurationMinutes: Int,
    val exerciseCalories: Int,
    val exerciseSummaryText: String,
    val dietPreferences: String,
    val allergies: String,
    val dislikedFoods: String,
    val mealRoutine: String
)

data class DailyAiMealSnapshot(
    val mealTypeLabel: String,
    val totalCalories: Int,
    val proteinGrams: Double,
    val carbGrams: Double,
    val fatGrams: Double,
    val entryCount: Int,
    val foodSummary: String
)
