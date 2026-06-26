package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAiPromptDataMapperTest {

    @Test
    fun localStatus_mapsTriggerTypesToStatusLevels() {
        assertEquals(
            AiDailyStatusLevel.RED,
            DailyAiPromptDataMapper.localStatus(AiDailyTriggerType.CALORIE_EXCEEDED)
        )
        assertEquals(
            AiDailyStatusLevel.YELLOW,
            DailyAiPromptDataMapper.localStatus(AiDailyTriggerType.LOW_WATER)
        )
        assertEquals(
            AiDailyStatusLevel.GREEN,
            DailyAiPromptDataMapper.localStatus(AiDailyTriggerType.NORMAL)
        )
    }

    @Test
    fun shouldAutoRequestAi_onlyForTodayCalorieExceeded() {
        val today = LocalDate.of(2026, 6, 19)

        assertTrue(
            DailyAiPromptDataMapper.shouldAutoRequestAi(
                snapshot = dailyAiSnapshot(triggerType = AiDailyTriggerType.CALORIE_EXCEEDED),
                selectedDate = today,
                today = today
            )
        )
        assertFalse(
            DailyAiPromptDataMapper.shouldAutoRequestAi(
                snapshot = dailyAiSnapshot(triggerType = AiDailyTriggerType.CALORIE_EXCEEDED),
                selectedDate = today.minusDays(1),
                today = today
            )
        )
        assertFalse(
            DailyAiPromptDataMapper.shouldAutoRequestAi(
                snapshot = dailyAiSnapshot(triggerType = AiDailyTriggerType.NORMAL),
                selectedDate = today,
                today = today
            )
        )
    }

    @Test
    fun toPromptData_mapsSnapshotFields() {
        val promptData = DailyAiPromptDataMapper.toPromptData(
            snapshot = dailyAiSnapshot(),
            appLanguage = "tr",
            goalLabel = "Kilo vermek",
            goalGuidance = "Nazik yorumla"
        )

        assertEquals("tr", promptData.appLanguage)
        assertEquals("Nursena", promptData.userName)
        assertEquals("Kilo vermek", promptData.goalLabel)
        assertEquals("Nazik yorumla", promptData.goalGuidance)
        assertEquals(AiDailyTriggerType.LOW_WATER, promptData.triggerType)
        assertEquals(AiDailyStatusLevel.YELLOW, promptData.localStatus)
        assertEquals(1, promptData.meals.size)
        assertEquals("Lunch", promptData.meals.first().mealName)
        assertEquals(true, promptData.exerciseSummary.hasExercise)
        assertEquals(35, promptData.exerciseSummary.totalDurationMinutes)
        assertEquals(180, promptData.exerciseSummary.totalCalories)
    }

    private fun dailyAiSnapshot(
        triggerType: AiDailyTriggerType = AiDailyTriggerType.LOW_WATER
    ): DailyAiSnapshot {
        return DailyAiSnapshot(
            dateIso = "2026-06-19",
            userName = "Nursena",
            goalType = "LOSE_WEIGHT",
            dailyCalorieTarget = 1800,
            totalCalories = 1500,
            proteinGrams = 80.0,
            proteinTargetGrams = 120,
            carbGrams = 180.0,
            fatGrams = 45.0,
            waterMl = 600,
            waterGoalMl = 2500,
            triggerType = triggerType,
            meals = listOf(
                DailyAiMealSnapshot(
                    mealTypeLabel = "Lunch",
                    totalCalories = 650,
                    proteinGrams = 35.0,
                    carbGrams = 70.0,
                    fatGrams = 20.0,
                    entryCount = 2,
                    foodSummary = "Chicken salad"
                )
            ),
            exerciseLogCount = 1,
            exerciseDurationMinutes = 35,
            exerciseCalories = 180,
            exerciseSummaryText = "Walk",
            dietPreferences = "Balanced",
            allergies = "",
            dislikedFoods = "",
            mealRoutine = "Three meals"
        )
    }
}
