package com.example.fitto.feature_daily

import kotlin.math.roundToInt

object DailyWeeklyCalorieProgressCalculator {

    data class Summary(
        val daysWithFood: Int,
        val averageProgress: Int,
        val progressBarPercent: Int,
        val days: List<DayProgress>
    )

    data class DayProgress(
        val calories: Int,
        val progressPercent: Int,
        val heightPx: Int,
        val alpha: Float
    ) {
        val hasFood: Boolean = calories > 0
    }

    fun calculate(
        dailyCalories: List<Int>,
        targetCalories: Int,
        minHeightPx: Int,
        maxHeightPx: Int
    ): Summary {
        val safeTargetCalories = targetCalories.coerceAtLeast(1)
        val normalizedCalories = dailyCalories.map { calories ->
            calories.coerceAtLeast(0)
        }
        val daysWithFood = normalizedCalories.count { it > 0 }
        val averageProgress = calculateAverageProgress(
            calories = normalizedCalories,
            targetCalories = safeTargetCalories,
            daysWithFood = daysWithFood
        )

        return Summary(
            daysWithFood = daysWithFood,
            averageProgress = averageProgress,
            progressBarPercent = averageProgress.coerceIn(0, 100),
            days = normalizedCalories.map { calories ->
                calculateDayProgress(
                    calories = calories,
                    targetCalories = safeTargetCalories,
                    minHeightPx = minHeightPx,
                    maxHeightPx = maxHeightPx
                )
            }
        )
    }

    private fun calculateAverageProgress(
        calories: List<Int>,
        targetCalories: Int,
        daysWithFood: Int
    ): Int {
        if (daysWithFood == 0) return 0

        return calories
            .filter { it > 0 }
            .map { dailyCalories ->
                progressPercent(
                    calories = dailyCalories,
                    targetCalories = targetCalories
                ).toDouble()
            }
            .average()
            .roundToInt()
    }

    private fun calculateDayProgress(
        calories: Int,
        targetCalories: Int,
        minHeightPx: Int,
        maxHeightPx: Int
    ): DayProgress {
        val progressPercent = progressPercent(
            calories = calories,
            targetCalories = targetCalories
        )
        val visualProgress = progressPercent.coerceIn(0, 100)
        val targetHeight = if (calories > 0) {
            minHeightPx + (((maxHeightPx - minHeightPx) * visualProgress) / 100)
        } else {
            minHeightPx
        }

        return DayProgress(
            calories = calories,
            progressPercent = progressPercent,
            heightPx = targetHeight,
            alpha = if (calories > 0) 1f else EMPTY_DAY_ALPHA
        )
    }

    private fun progressPercent(
        calories: Int,
        targetCalories: Int
    ): Int {
        return ((calories.toDouble() / targetCalories.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, MAX_PROGRESS_PERCENT)
    }

    private const val MAX_PROGRESS_PERCENT = 150
    private const val EMPTY_DAY_ALPHA = 0.25f
}
