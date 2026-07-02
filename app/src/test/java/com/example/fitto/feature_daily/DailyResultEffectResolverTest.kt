package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyResultEffectResolverTest {

    @Test
    fun `entry date resolves valid date selection`() {
        val effect = DailyResultEffectResolver.resolveEntryDate(
            currentDate = LocalDate.of(2026, 6, 20),
            entryDate = "2026-06-18"
        )

        assertEquals(LocalDate.of(2026, 6, 18), effect.selection?.date)
    }

    @Test
    fun `entry date ignores blank date`() {
        val effect = DailyResultEffectResolver.resolveEntryDate(
            currentDate = LocalDate.of(2026, 6, 20),
            entryDate = "   "
        )

        assertNull(effect.selection)
    }

    @Test
    fun `diet plan changed refreshes active plan and meal totals`() {
        assertEquals(
            DailyDashboardRefreshPlans.DIET_PLAN_CHANGED,
            DailyResultEffectResolver.resolveDietPlanChanged()
        )
    }

    @Test
    fun `meal added resolves meal type date and meal refresh plan`() {
        val effect = DailyResultEffectResolver.resolveMealAdded(
            currentDate = LocalDate.of(2026, 6, 20),
            mealTypeArgument = "lunch",
            entryDate = "2026-06-19"
        )

        assertEquals(MealType.LUNCH, effect.mealType)
        assertEquals(LocalDate.of(2026, 6, 19), effect.dateSelection?.date)
        assertEquals(DailyDashboardRefreshPlans.MEAL_CHANGED, effect.refreshPlan)
    }

    @Test
    fun `meal added falls back to breakfast for unknown meal type`() {
        val effect = DailyResultEffectResolver.resolveMealAdded(
            currentDate = LocalDate.of(2026, 6, 20),
            mealTypeArgument = "unknown",
            entryDate = null
        )

        assertEquals(MealType.BREAKFAST, effect.mealType)
        assertNull(effect.dateSelection)
    }

    @Test
    fun `meal detail changed uses meal refresh plan`() {
        assertEquals(
            DailyDashboardRefreshPlans.MEAL_CHANGED,
            DailyResultEffectResolver.resolveMealDetailChanged()
        )
    }

    @Test
    fun `exercise added resolves date and activity refresh plan`() {
        val effect = DailyResultEffectResolver.resolveExerciseAdded(
            currentDate = LocalDate.of(2026, 6, 20),
            entryDate = "2026-06-17"
        )

        assertNotNull(effect.dateSelectionEffect.selection)
        assertEquals(
            LocalDate.of(2026, 6, 17),
            effect.dateSelectionEffect.selection?.date
        )
        assertEquals(DailyDashboardRefreshPlans.EXERCISE_CHANGED, effect.refreshPlan)
    }
}
