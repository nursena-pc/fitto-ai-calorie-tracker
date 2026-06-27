package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val effect = DailyResultEffectResolver.resolveDietPlanChanged()

        assertTrue(effect.shouldRenderActivePlan)
        assertTrue(effect.shouldRenderMealTotals)
        assertFalse(effect.shouldRenderWeeklyCalorieProgress)
        assertFalse(effect.shouldLoadHealthConnectSummary)
    }

    @Test
    fun `meal added resolves meal type date and refreshes daily summaries`() {
        val effect = DailyResultEffectResolver.resolveMealAdded(
            currentDate = LocalDate.of(2026, 6, 20),
            mealTypeArgument = "lunch",
            entryDate = "2026-06-19"
        )

        assertEquals(MealType.LUNCH, effect.mealType)
        assertEquals(LocalDate.of(2026, 6, 19), effect.dateSelection?.date)
        assertTrue(effect.refreshEffect.shouldRenderMealTotals)
        assertTrue(effect.refreshEffect.shouldRenderActivePlan)
        assertTrue(effect.refreshEffect.shouldRenderWeeklyCalorieProgress)
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
    fun `meal detail changed refreshes meal totals active plan and weekly calories`() {
        val effect = DailyResultEffectResolver.resolveMealDetailChanged()

        assertTrue(effect.shouldRenderMealTotals)
        assertTrue(effect.shouldRenderActivePlan)
        assertTrue(effect.shouldRenderWeeklyCalorieProgress)
    }

    @Test
    fun `exercise added resolves date and refreshes health summary meal totals and active plan`() {
        val (dateEffect, refreshEffect) = DailyResultEffectResolver.resolveExerciseAdded(
            currentDate = LocalDate.of(2026, 6, 20),
            entryDate = "2026-06-17"
        )

        assertNotNull(dateEffect.selection)
        assertEquals(LocalDate.of(2026, 6, 17), dateEffect.selection?.date)
        assertTrue(refreshEffect.shouldLoadHealthConnectSummary)
        assertTrue(refreshEffect.shouldRenderMealTotals)
        assertTrue(refreshEffect.shouldRenderActivePlan)
        assertFalse(refreshEffect.shouldRenderWeeklyCalorieProgress)
    }
}
