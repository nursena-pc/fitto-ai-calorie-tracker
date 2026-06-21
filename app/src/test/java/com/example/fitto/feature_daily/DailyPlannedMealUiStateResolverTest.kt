package com.example.fitto.feature_daily

import com.example.fitto.R
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.PlannedMealUiModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyPlannedMealUiStateResolverTest {

    @Test
    fun resolve_returnsCompletedStateWhenThereIsNoNextMeal() {
        val plan = dailyPlan(
            nextMeal = null,
            completedMealIds = setOf("breakfast", "lunch")
        )

        val state = DailyPlannedMealUiStateResolver.resolve(plan)

        assertEquals("Balanced plan", state.planTitle)
        assertEquals("icon", state.planIcon)
        assertEquals(2, state.dayNumber)
        assertEquals(7, state.durationDays)
        assertEquals(1800, state.targetCalories)
        assertEquals(2, state.completedMealCount)
        assertEquals(2, state.totalMealCount)
        assertEquals(100, state.progressPercent)
        assertEquals(
            DailyPlannedMealUiStateResolver.NextMealState.Completed,
            state.nextMealState
        )
    }

    @Test
    fun resolve_returnsAvailableStateForNextMeal() {
        val nextMeal = plannedMeal(
            id = "lunch",
            mealType = MealType.LUNCH,
            baseCalories = 600,
            foods = listOf("Chicken", "Rice")
        )
        val plan = dailyPlan(
            targetCalories = 1800,
            nextMeal = nextMeal,
            completedMealIds = setOf("breakfast")
        )

        val state = DailyPlannedMealUiStateResolver.resolve(plan)
        val nextMealState = state.nextMealState
            as DailyPlannedMealUiStateResolver.NextMealState.Available

        assertEquals(R.string.daily_lunch, nextMealState.mealTypeTitleRes)
        assertEquals(600, nextMealState.calories)
        assertEquals("Lunch bowl", nextMealState.mealTitle)
        assertEquals("Chicken, Rice", nextMealState.foodSummary)
        assertEquals(50, state.progressPercent)
    }

    private fun dailyPlan(
        targetCalories: Int = 1800,
        nextMeal: PlannedMealUiModel?,
        completedMealIds: Set<String>
    ): DailyPlannedMealsUiModel {
        val meals = listOf(
            plannedMeal(id = "breakfast", mealType = MealType.BREAKFAST),
            plannedMeal(id = "lunch", mealType = MealType.LUNCH)
        )

        return DailyPlannedMealsUiModel(
            planId = "balanced",
            planTitle = "Balanced plan",
            planBadge = "7 days",
            planIcon = "icon",
            dayNumber = 2,
            durationDays = 7,
            targetCalories = targetCalories,
            meals = meals,
            nextMeal = nextMeal,
            completedMealIds = completedMealIds
        )
    }

    private fun plannedMeal(
        id: String,
        mealType: MealType,
        baseCalories: Int = 450,
        foods: List<String> = listOf("Egg", "Toast")
    ): PlannedMealUiModel {
        return PlannedMealUiModel(
            id = id,
            planId = "balanced",
            dayNumber = 2,
            mealType = mealType,
            title = if (mealType == MealType.LUNCH) "Lunch bowl" else "Breakfast plate",
            baseCalories = baseCalories,
            foods = foods
        )
    }
}
