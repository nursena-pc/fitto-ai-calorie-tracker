package com.example.fitto.feature_reports

import kotlin.math.roundToInt

object ReportCalorieSummaryResolver {

    data class State(
        val targetCalories: Int,
        val todayCalories: Int,
        val averageCalories: Int,
        val maxCalories: Int,
        val days: List<ReportDay>
    )

    fun resolve(
        days: List<ReportDay>,
        profileTargetCalories: Int,
        defaultTargetCalories: Int
    ): State {
        val targetCalories = profileTargetCalories
            .takeIf { it > 0 }
            ?: defaultTargetCalories

        val calories = days.map { day -> day.calories }
        val averageCalories = calories
            .average()
            .takeIf { average -> !average.isNaN() }
            ?.roundToInt()
            ?: 0

        return State(
            targetCalories = targetCalories,
            todayCalories = days.lastOrNull()?.calories ?: 0,
            averageCalories = averageCalories,
            maxCalories = calories.maxOrNull() ?: 0,
            days = days
        )
    }
}
