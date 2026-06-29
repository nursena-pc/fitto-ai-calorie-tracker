package com.example.fitto.feature_dietplans

import com.example.fitto.feature_meal.model.MealType
import java.util.Calendar
import kotlin.math.max

object DietPlanPreferenceStateResolver {
    private const val CUSTOM_HOURS_NOT_SET = -1
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val RECENT_SEARCH_SEPARATOR = "\u001F"
    private const val MAX_RECENT_SEARCH_COUNT = 6

    fun storedCustomFastingHours(customFastingHours: Int?): Int {
        return customFastingHours ?: CUSTOM_HOURS_NOT_SET
    }

    fun customFastingHoursOrNull(storedValue: Int): Int? {
        return if (storedValue == CUSTOM_HOURS_NOT_SET) null else storedValue
    }

    fun currentPlanDay(
        durationDays: Int,
        startedAt: Long,
        nowMillis: Long
    ): Int {
        if (durationDays <= 0 || startedAt <= 0L) return 1

        val elapsedDays = max(0, daysBetween(startedAt, nowMillis))
        return (elapsedDays % durationDays) + 1
    }

    fun shouldMarkMealTypeCompleted(
        mealType: MealType,
        plannedMealId: String
    ): Boolean {
        return plannedMealId.contains(mealType.argumentValue, ignoreCase = true)
    }

    fun toggleFavorite(
        favoriteIds: Set<String>,
        planId: String
    ): DietPlanFavoriteToggleResolution {
        val updatedFavorites = favoriteIds.toMutableSet()
        val isFavoriteNow = if (updatedFavorites.contains(planId)) {
            updatedFavorites.remove(planId)
            false
        } else {
            updatedFavorites.add(planId)
            true
        }

        return DietPlanFavoriteToggleResolution(
            favoriteIds = updatedFavorites,
            isFavorite = isFavoriteNow
        )
    }

    fun recentSearches(rawValue: String?): List<String> {
        return rawValue
            .orEmpty()
            .split(RECENT_SEARCH_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun updatedRecentSearches(
        currentSearches: List<String>,
        query: String
    ): List<String>? {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return null

        return (listOf(cleanQuery) + currentSearches.filterNot {
            it.equals(cleanQuery, ignoreCase = true)
        }).take(MAX_RECENT_SEARCH_COUNT)
    }

    fun encodeRecentSearches(searches: List<String>): String {
        return searches.joinToString(RECENT_SEARCH_SEPARATOR)
    }

    private fun daysBetween(startMillis: Long, endMillis: Long): Int {
        val start = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return ((end.timeInMillis - start.timeInMillis) / DAY_MILLIS).toInt()
    }
}

data class DietPlanFavoriteToggleResolution(
    val favoriteIds: Set<String>,
    val isFavorite: Boolean
)
