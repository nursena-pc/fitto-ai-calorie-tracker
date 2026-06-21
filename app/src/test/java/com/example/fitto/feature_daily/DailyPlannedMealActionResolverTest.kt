package com.example.fitto.feature_daily

import com.example.fitto.R
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.PlannedMealUiModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyPlannedMealActionResolverTest {

    @Test
    fun resolveApplyRequest_returnsNullWhenPlanOrNextMealIsMissing() {
        assertNull(DailyPlannedMealActionResolver.resolveApplyRequest(null))
        assertNull(
            DailyPlannedMealActionResolver.resolveApplyRequest(
                dailyPlan(nextMeal = null)
            )
        )
    }

    @Test
    fun resolveApplyRequest_mapsNextMealToRepositoryRequestData() {
        val request = DailyPlannedMealActionResolver.resolveApplyRequest(
            dailyPlan(
                targetCalories = 2400,
                nextMeal = plannedMeal(
                    id = "lunch",
                    mealType = MealType.LUNCH,
                    baseCalories = 600,
                    foods = listOf("Chicken", "Rice")
                )
            )
        )

        assertEquals(MealType.LUNCH, request?.mealType)
        assertEquals("Lunch bowl", request?.title)
        assertEquals(2400, request?.targetCalories)
        assertEquals(800, request?.plannedMealCalories)
        assertEquals(listOf("Chicken", "Rice"), request?.suggestedFoodNames)
        assertEquals("balanced", request?.planId)
        assertEquals("lunch", request?.plannedMealId)
        assertEquals(R.string.daily_lunch, request?.mealTypeTitleRes)
        assertEquals(
            DailyPlannedMealActionResolver.Completion(
                planId = "balanced",
                dayNumber = 2,
                plannedMealId = "lunch"
            ),
            request?.completion
        )
    }

    @Test
    fun resolveCompletionForMealType_returnsCompletionOnlyWhenNextMealMatches() {
        val plan = dailyPlan(
            nextMeal = plannedMeal(
                id = "snack",
                mealType = MealType.SNACK
            )
        )

        assertNull(
            DailyPlannedMealActionResolver.resolveCompletionForMealType(
                dailyPlan = plan,
                mealType = MealType.LUNCH
            )
        )
        assertEquals(
            DailyPlannedMealActionResolver.Completion(
                planId = "balanced",
                dayNumber = 2,
                plannedMealId = "snack"
            ),
            DailyPlannedMealActionResolver.resolveCompletionForMealType(
                dailyPlan = plan,
                mealType = MealType.SNACK
            )
        )
    }

    private fun dailyPlan(
        targetCalories: Int = 1800,
        nextMeal: PlannedMealUiModel?
    ): DailyPlannedMealsUiModel {
        return DailyPlannedMealsUiModel(
            planId = "balanced",
            planTitle = "Balanced plan",
            planBadge = "7 days",
            planIcon = "icon",
            dayNumber = 2,
            durationDays = 7,
            targetCalories = targetCalories,
            meals = listOf(
                plannedMeal(id = "breakfast", mealType = MealType.BREAKFAST),
                plannedMeal(id = "lunch", mealType = MealType.LUNCH),
                plannedMeal(id = "snack", mealType = MealType.SNACK)
            ),
            nextMeal = nextMeal,
            completedMealIds = emptySet()
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
            title = if (mealType == MealType.LUNCH) "Lunch bowl" else "Meal",
            baseCalories = baseCalories,
            foods = foods
        )
    }
}
