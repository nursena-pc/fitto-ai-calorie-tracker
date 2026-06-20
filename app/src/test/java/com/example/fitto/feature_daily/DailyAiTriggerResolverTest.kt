package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiTriggerResolverTest {

    @Test
    fun resolve_mapsMealSnapshotsToTriggerPolicy() {
        val trigger = DailyAiTriggerResolver.resolve(
            totalCalories = 1000,
            targetCalories = 2000,
            waterMl = 2000,
            waterGoalMl = 2000,
            proteinGrams = 80.0,
            proteinTargetGrams = 100,
            meals = listOf(
                mealSnapshot(
                    mealTypeLabel = "Lunch",
                    totalCalories = 150,
                    proteinGrams = 12.0
                )
            ),
            exerciseLogCount = 1,
            snackMealLabel = SNACK_MEAL_LABEL
        )

        assertEquals(AiDailyTriggerType.INSUFFICIENT_MEAL, trigger)
    }

    @Test
    fun resolve_usesSnackLabelWhenIgnoringSnackForMainMealChecks() {
        val trigger = DailyAiTriggerResolver.resolve(
            totalCalories = 1000,
            targetCalories = 2000,
            waterMl = 2000,
            waterGoalMl = 2000,
            proteinGrams = 80.0,
            proteinTargetGrams = 100,
            meals = listOf(
                mealSnapshot(
                    mealTypeLabel = SNACK_MEAL_LABEL,
                    totalCalories = 120,
                    proteinGrams = 2.0
                ),
                mealSnapshot(
                    mealTypeLabel = "Dinner",
                    totalCalories = 520,
                    proteinGrams = 30.0
                )
            ),
            exerciseLogCount = 1,
            snackMealLabel = SNACK_MEAL_LABEL
        )

        assertEquals(AiDailyTriggerType.NORMAL, trigger)
    }

    private fun mealSnapshot(
        mealTypeLabel: String,
        totalCalories: Int,
        proteinGrams: Double
    ) = DailyAiMealSnapshot(
        mealTypeLabel = mealTypeLabel,
        totalCalories = totalCalories,
        proteinGrams = proteinGrams,
        carbGrams = 0.0,
        fatGrams = 0.0,
        entryCount = 1,
        foodSummary = "Food"
    )

    private companion object {
        const val SNACK_MEAL_LABEL = "Snack"
    }
}
