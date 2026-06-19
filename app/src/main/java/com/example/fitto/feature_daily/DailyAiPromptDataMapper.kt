package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyExerciseInfo
import com.example.fitto.feature_daily.ai.AiDailyMealInfo
import com.example.fitto.feature_daily.ai.AiDailyPromptData
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import java.time.LocalDate

object DailyAiPromptDataMapper {

    fun localStatus(triggerType: AiDailyTriggerType): AiDailyStatusLevel {
        return when (triggerType) {
            AiDailyTriggerType.CALORIE_EXCEEDED -> AiDailyStatusLevel.RED
            AiDailyTriggerType.CALORIE_REACHED,
            AiDailyTriggerType.LOW_WATER,
            AiDailyTriggerType.LOW_PROTEIN,
            AiDailyTriggerType.LOW_DAILY_PROTEIN,
            AiDailyTriggerType.LOW_MEAL_PROTEIN,
            AiDailyTriggerType.INSUFFICIENT_MEAL,
            AiDailyTriggerType.NO_EXERCISE,
            AiDailyTriggerType.NO_MEAL -> AiDailyStatusLevel.YELLOW
            AiDailyTriggerType.NORMAL -> AiDailyStatusLevel.GREEN
        }
    }

    fun shouldAutoRequestAi(
        snapshot: DailyAiSnapshot,
        selectedDate: LocalDate,
        today: LocalDate
    ): Boolean {
        return selectedDate == today &&
            snapshot.triggerType == AiDailyTriggerType.CALORIE_EXCEEDED
    }

    fun toPromptData(
        snapshot: DailyAiSnapshot,
        appLanguage: String,
        goalLabel: String,
        goalGuidance: String
    ): AiDailyPromptData {
        return AiDailyPromptData(
            appLanguage = appLanguage,
            userName = snapshot.userName,
            dateIso = snapshot.dateIso,
            goalLabel = goalLabel,
            goalGuidance = goalGuidance,
            dailyCalorieTarget = snapshot.dailyCalorieTarget,
            totalCalories = snapshot.totalCalories,
            proteinGrams = snapshot.proteinGrams,
            proteinTargetGrams = snapshot.proteinTargetGrams,
            carbGrams = snapshot.carbGrams,
            fatGrams = snapshot.fatGrams,
            waterMl = snapshot.waterMl,
            waterGoalMl = snapshot.waterGoalMl,
            triggerType = snapshot.triggerType,
            localStatus = localStatus(snapshot.triggerType),
            meals = snapshot.meals.map { meal ->
                AiDailyMealInfo(
                    mealName = meal.mealTypeLabel,
                    totalCalories = meal.totalCalories,
                    proteinGrams = meal.proteinGrams,
                    carbGrams = meal.carbGrams,
                    fatGrams = meal.fatGrams,
                    entryCount = meal.entryCount,
                    foodSummary = meal.foodSummary
                )
            },
            exerciseSummary = AiDailyExerciseInfo(
                hasExercise = snapshot.exerciseLogCount > 0,
                summary = snapshot.exerciseSummaryText,
                totalDurationMinutes = snapshot.exerciseDurationMinutes,
                totalCalories = snapshot.exerciseCalories
            ),
            dietPreferences = snapshot.dietPreferences,
            allergies = snapshot.allergies,
            dislikedFoods = snapshot.dislikedFoods,
            mealRoutine = snapshot.mealRoutine
        )
    }
}
