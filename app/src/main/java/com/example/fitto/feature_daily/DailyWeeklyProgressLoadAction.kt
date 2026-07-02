package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.UserProfileEntity
import java.time.LocalDate

internal class DailyWeeklyProgressLoadAction(
    private val loadProfile: suspend () -> UserProfileEntity?,
    private val getCaloriesForDate: suspend (dateIso: String) -> Int,
    private val resolveNutritionTargets: (UserProfileEntity?) -> DailyNutritionTargetResolver.Targets
) {

    suspend fun load(request: DailyWeeklyProgressLoadRequest): DailyWeeklyProgressLoadResult {
        val profile = loadProfile()
        val targetCalories = resolveNutritionTargets(profile)
            .dailyCalories
            .coerceAtLeast(1)
        val days = (request.dayCount - 1 downTo 0).map { offset ->
            request.selectedDate.minusDays(offset.toLong())
        }
        val dailyCalories = days.map { date ->
            getCaloriesForDate(date.toString()).coerceAtLeast(0)
        }
        val progress = DailyWeeklyCalorieProgressCalculator.calculate(
            dailyCalories = dailyCalories,
            targetCalories = targetCalories,
            minHeightPx = request.minHeightPx,
            maxHeightPx = request.maxHeightPx
        )

        return DailyWeeklyProgressLoadResult(
            days = days,
            targetCalories = targetCalories,
            progress = progress
        )
    }
}

internal data class DailyWeeklyProgressLoadRequest(
    val selectedDate: LocalDate,
    val minHeightPx: Int,
    val maxHeightPx: Int,
    val dayCount: Int = 7
) {
    init {
        require(dayCount > 0) { "dayCount must be greater than zero" }
    }
}

internal data class DailyWeeklyProgressLoadResult(
    val days: List<LocalDate>,
    val targetCalories: Int,
    val progress: DailyWeeklyCalorieProgressCalculator.Summary
)
