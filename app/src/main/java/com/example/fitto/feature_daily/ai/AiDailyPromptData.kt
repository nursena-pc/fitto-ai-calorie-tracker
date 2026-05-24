package com.example.fitto.feature_daily.ai

data class AiDailyPromptData(
    val appLanguage: String,
    val userName: String,
    val dateIso: String,
    val goalLabel: String,
    val goalGuidance: String,
    val dailyCalorieTarget: Int,
    val totalCalories: Int,
    val proteinGrams: Double,
    val proteinTargetGrams: Int,
    val carbGrams: Double,
    val fatGrams: Double,
    val waterMl: Int,
    val waterGoalMl: Int,
    val triggerType: AiDailyTriggerType,
    val localStatus: AiDailyStatusLevel,
    val meals: List<AiDailyMealInfo>,
    val exerciseSummary: AiDailyExerciseInfo,
    val dietPreferences: String,
    val allergies: String,
    val dislikedFoods: String,
    val mealRoutine: String
)

data class AiDailyMealInfo(
    val mealName: String,
    val totalCalories: Int,
    val proteinGrams: Double,
    val carbGrams: Double,
    val fatGrams: Double,
    val entryCount: Int,
    val foodSummary: String
)

data class AiDailyExerciseInfo(
    val hasExercise: Boolean,
    val summary: String,
    val totalDurationMinutes: Int,
    val totalCalories: Int
)
