package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.data.local.model.DailyNutritionSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal fun interface ProfileNutritionSummarySource {
    suspend fun load(userId: String, dateIso: String): DailyNutritionSummary
}

internal class ProfileWeeklySummaryLoader(
    private val activeUserId: () -> String?,
    private val nutritionSource: ProfileNutritionSummarySource,
    private val ioDispatcher: CoroutineDispatcher,
    private val today: () -> LocalDate = { LocalDate.now() }
) {
    suspend fun load(): ProfileWeeklySummary = withContext(ioDispatcher) {
        val userId = activeUserId() ?: LOCAL_ANONYMOUS_USER_ID
        val endDate = today()
        val dates = (DAYS_BACK downTo 0).map { offset ->
            endDate.minusDays(offset.toLong())
        }
        val summaries = dates.map { date ->
            nutritionSource.load(userId, date.toString())
        }
        val todaySummary = summaries.lastOrNull()
        val recordedDays = summaries.count { summary -> summary.hasRecordedNutrition() }
        val weeklyCaloriesTotal = summaries.sumOf(DailyNutritionSummary::totalCalories)

        ProfileWeeklySummary(
            todayCalories = todaySummary?.totalCalories ?: 0,
            todayProtein = todaySummary?.totalProtein ?: 0.0,
            todayCarbs = todaySummary?.totalCarbs ?: 0.0,
            todayFat = todaySummary?.totalFat ?: 0.0,
            weeklyTotalCalories = weeklyCaloriesTotal,
            weeklyAverageCalories = if (recordedDays > 0) {
                weeklyCaloriesTotal / recordedDays
            } else {
                0
            },
            recordedDays = recordedDays,
            weeklyProtein = summaries.sumOf(DailyNutritionSummary::totalProtein),
            weeklyCarbs = summaries.sumOf(DailyNutritionSummary::totalCarbs),
            weeklyFat = summaries.sumOf(DailyNutritionSummary::totalFat)
        )
    }

    private fun DailyNutritionSummary.hasRecordedNutrition(): Boolean {
        return totalCalories > 0 || totalProtein > 0.0 || totalCarbs > 0.0 || totalFat > 0.0
    }

    private companion object {
        const val DAYS_BACK = 6
        const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"
    }
}
