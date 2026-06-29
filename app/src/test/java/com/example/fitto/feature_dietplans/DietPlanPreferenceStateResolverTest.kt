package com.example.fitto.feature_dietplans

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DietPlanPreferenceStateResolverTest {

    @Test
    fun customFastingHoursOrNull_returnsNullForSentinelValue() {
        assertNull(DietPlanPreferenceStateResolver.customFastingHoursOrNull(-1))
    }

    @Test
    fun storedCustomFastingHours_returnsSentinelWhenValueMissing() {
        assertEquals(-1, DietPlanPreferenceStateResolver.storedCustomFastingHours(null))
        assertEquals(18, DietPlanPreferenceStateResolver.storedCustomFastingHours(18))
    }

    @Test
    fun currentPlanDay_returnsFirstDayWhenDurationOrStartInvalid() {
        assertEquals(
            1,
            DietPlanPreferenceStateResolver.currentPlanDay(
                durationDays = 0,
                startedAt = dateMillis(2026, Calendar.JANUARY, 1),
                nowMillis = dateMillis(2026, Calendar.JANUARY, 3)
            )
        )
        assertEquals(
            1,
            DietPlanPreferenceStateResolver.currentPlanDay(
                durationDays = 7,
                startedAt = 0L,
                nowMillis = dateMillis(2026, Calendar.JANUARY, 3)
            )
        )
    }

    @Test
    fun currentPlanDay_wrapsElapsedDaysByPlanDuration() {
        val result = DietPlanPreferenceStateResolver.currentPlanDay(
            durationDays = 3,
            startedAt = dateMillis(2026, Calendar.JANUARY, 1),
            nowMillis = dateMillis(2026, Calendar.JANUARY, 5)
        )

        assertEquals(2, result)
    }

    @Test
    fun shouldMarkMealTypeCompleted_matchesMealTypeIgnoringCase() {
        assertTrue(
            DietPlanPreferenceStateResolver.shouldMarkMealTypeCompleted(
                mealType = MealType.BREAKFAST,
                plannedMealId = "plan_day_1_BREAKFAST_main"
            )
        )
        assertFalse(
            DietPlanPreferenceStateResolver.shouldMarkMealTypeCompleted(
                mealType = MealType.DINNER,
                plannedMealId = "plan_day_1_lunch_main"
            )
        )
    }

    @Test
    fun toggleFavorite_addsMissingPlan() {
        val result = DietPlanPreferenceStateResolver.toggleFavorite(
            favoriteIds = setOf("plan_a"),
            planId = "plan_b"
        )

        assertTrue(result.isFavorite)
        assertEquals(setOf("plan_a", "plan_b"), result.favoriteIds)
    }

    @Test
    fun toggleFavorite_removesExistingPlan() {
        val result = DietPlanPreferenceStateResolver.toggleFavorite(
            favoriteIds = setOf("plan_a", "plan_b"),
            planId = "plan_b"
        )

        assertFalse(result.isFavorite)
        assertEquals(setOf("plan_a"), result.favoriteIds)
    }

    @Test
    fun recentSearches_trimsAndDropsBlankValues() {
        val encoded = DietPlanPreferenceStateResolver.encodeRecentSearches(
            listOf("  keto  ", "", " protein ")
        )

        assertEquals(
            listOf("keto", "protein"),
            DietPlanPreferenceStateResolver.recentSearches(encoded)
        )
    }

    @Test
    fun updatedRecentSearches_movesQueryToFrontAndDeduplicatesIgnoringCase() {
        val result = DietPlanPreferenceStateResolver.updatedRecentSearches(
            currentSearches = listOf("keto", "protein", "vegan"),
            query = " Protein "
        )

        assertEquals(listOf("Protein", "keto", "vegan"), result)
    }

    @Test
    fun updatedRecentSearches_limitsResultsToSixAndIgnoresBlankQueries() {
        val currentSearches = listOf("a", "b", "c", "d", "e", "f")

        assertNull(
            DietPlanPreferenceStateResolver.updatedRecentSearches(
                currentSearches = currentSearches,
                query = "   "
            )
        )
        assertEquals(
            listOf("new", "a", "b", "c", "d", "e"),
            DietPlanPreferenceStateResolver.updatedRecentSearches(
                currentSearches = currentSearches,
                query = "new"
            )
        )
    }

    private fun dateMillis(
        year: Int,
        month: Int,
        day: Int
    ): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
