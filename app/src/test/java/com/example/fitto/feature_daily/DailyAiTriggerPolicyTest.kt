package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiTriggerPolicyTest {

    @Test
    fun resolve_withoutMeals_returnsNoMeal() {
        val trigger = resolveTrigger(meals = emptyList())

        assertEquals(AiDailyTriggerType.NO_MEAL, trigger)
    }

    @Test
    fun resolve_whenCaloriesExceedRedThreshold_prioritizesCalorieExceeded() {
        val trigger = resolveTrigger(
            totalCalories = 2500,
            targetCalories = 2000,
            meals = listOf(mainMeal(totalCalories = 120, proteinGrams = 3.0))
        )

        assertEquals(AiDailyTriggerType.CALORIE_EXCEEDED, trigger)
    }

    @Test
    fun resolve_whenMainMealIsTooLight_returnsInsufficientMeal() {
        val trigger = resolveTrigger(
            meals = listOf(mainMeal(totalCalories = 150, proteinGrams = 12.0))
        )

        assertEquals(AiDailyTriggerType.INSUFFICIENT_MEAL, trigger)
    }

    @Test
    fun resolve_ignoresSnackForMainMealChecks() {
        val trigger = resolveTrigger(
            waterMl = 2000,
            proteinGrams = 80.0,
            proteinTargetGrams = 100,
            meals = listOf(
                snackMeal(totalCalories = 120, proteinGrams = 2.0),
                mainMeal(totalCalories = 500, proteinGrams = 30.0)
            ),
            exerciseLogCount = 1
        )

        assertEquals(AiDailyTriggerType.NORMAL, trigger)
    }

    @Test
    fun resolve_whenWaterBelowHalfGoal_returnsLowWater() {
        val trigger = resolveTrigger(
            waterMl = 900,
            waterGoalMl = 2000,
            proteinGrams = 80.0,
            meals = listOf(mainMeal(totalCalories = 500, proteinGrams = 30.0)),
            exerciseLogCount = 1
        )

        assertEquals(AiDailyTriggerType.LOW_WATER, trigger)
    }

    private fun resolveTrigger(
        totalCalories: Int = 1000,
        targetCalories: Int = 2000,
        waterMl: Int = 2000,
        waterGoalMl: Int = 2000,
        proteinGrams: Double = 80.0,
        proteinTargetGrams: Int = 100,
        meals: List<DailyAiTriggerPolicy.Meal> = listOf(mainMeal()),
        exerciseLogCount: Int = 1
    ): AiDailyTriggerType {
        return DailyAiTriggerPolicy.resolve(
            totalCalories = totalCalories,
            targetCalories = targetCalories,
            waterMl = waterMl,
            waterGoalMl = waterGoalMl,
            proteinGrams = proteinGrams,
            proteinTargetGrams = proteinTargetGrams,
            meals = meals,
            exerciseLogCount = exerciseLogCount,
            snackMealLabel = SNACK_LABEL
        )
    }

    private fun mainMeal(
        totalCalories: Int = 500,
        proteinGrams: Double = 30.0
    ) = DailyAiTriggerPolicy.Meal(
        mealTypeLabel = "Lunch",
        totalCalories = totalCalories,
        proteinGrams = proteinGrams
    )

    private fun snackMeal(
        totalCalories: Int,
        proteinGrams: Double
    ) = DailyAiTriggerPolicy.Meal(
        mealTypeLabel = SNACK_LABEL,
        totalCalories = totalCalories,
        proteinGrams = proteinGrams
    )

    private companion object {
        const val SNACK_LABEL = "Snack"
    }
}
