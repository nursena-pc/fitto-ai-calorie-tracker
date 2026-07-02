package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPlannedMealActionTest {

    @Test
    fun `prepare returns null when next meal is missing`() {
        val action = action()
        val dailyPlan = dailyTestMealPlan(nextMeal = null)

        assertNull(action.prepare(dailyPlan))
    }

    @Test
    fun `execute adds meal with stable date and marks completion`() = runBlocking {
        val calls = mutableListOf<Any>()
        val action = DailyPlannedMealAction(
            addPlannedMealEntry = { request, entryDate ->
                calls += request
                calls += entryDate
            },
            markPlannedMealCompleted = { calls += it }
        )
        val request = action.prepare(
            dailyTestMealPlan(
                nextMeal = dailyTestPlannedMeal(
                    id = "lunch",
                    mealType = MealType.LUNCH,
                    title = "Lunch bowl"
                )
            )
        )!!

        val result = action.execute(request, "2026-07-02")

        assertTrue(result is DailyPlannedMealExecutionResult.Success)
        assertEquals(request, calls[0])
        assertEquals("2026-07-02", calls[1])
        assertEquals(request.completion, calls[2])
        val effect = (result as DailyPlannedMealExecutionResult.Success).effect
        assertTrue(effect.refreshMealTotals)
        assertTrue(effect.refreshActivePlan)
        assertEquals(request.mealTypeTitleRes, effect.mealTypeTitleRes)
    }

    @Test
    fun `failed add returns failure and does not mark completion`() = runBlocking {
        var completionCount = 0
        val action = DailyPlannedMealAction(
            addPlannedMealEntry = { _, _ -> error("save failed") },
            markPlannedMealCompleted = { completionCount += 1 }
        )
        val request = action.prepare(dailyTestMealPlan())!!

        val result = action.execute(request, "2026-07-02")

        assertTrue(result is DailyPlannedMealExecutionResult.Failure)
        assertEquals(0, completionCount)
    }

    @Test
    fun `matching manual meal completes next planned meal`() {
        val completions = mutableListOf<DailyPlannedMealActionResolver.Completion>()
        val action = action { completions += it }
        val dailyPlan = dailyTestMealPlan(
            nextMeal = dailyTestPlannedMeal(
                id = "snack",
                mealType = MealType.SNACK
            )
        )

        val completed = action.completeNextMealIfMatches(
            dailyPlan = dailyPlan,
            mealType = MealType.SNACK
        )

        assertTrue(completed)
        assertEquals(
            listOf(
                DailyPlannedMealActionResolver.Completion(
                    planId = dailyPlan.planId,
                    dayNumber = dailyPlan.dayNumber,
                    plannedMealId = "snack"
                )
            ),
            completions
        )
    }

    @Test
    fun `non matching manual meal leaves progress unchanged`() {
        var completionCount = 0
        val action = action { completionCount += 1 }
        val dailyPlan = dailyTestMealPlan(
            nextMeal = dailyTestPlannedMeal(mealType = MealType.DINNER)
        )

        val completed = action.completeNextMealIfMatches(
            dailyPlan = dailyPlan,
            mealType = MealType.BREAKFAST
        )

        assertFalse(completed)
        assertEquals(0, completionCount)
    }

    private fun action(
        markCompleted: (DailyPlannedMealActionResolver.Completion) -> Unit = {}
    ): DailyPlannedMealAction {
        return DailyPlannedMealAction(
            addPlannedMealEntry = { _, _ -> },
            markPlannedMealCompleted = markCompleted
        )
    }
}
