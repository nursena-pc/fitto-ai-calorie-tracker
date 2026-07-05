package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_health.HealthConnectDailySummary
import java.time.LocalDate

/**
 * Owns the mutable screen state used while rendering the Daily dashboard.
 *
 * Keeping the state in one place makes date-bound async results and active-plan request ordering
 * explicit instead of spreading related mutable fields throughout [DailyFragment].
 */
internal class DailyDashboardStateStore(
    initialDate: LocalDate,
    initialNutritionTargets: DailyDashboardNutritionTargets,
    initialDisplayWeightKg: Double,
    initialWaterGoalMl: Int
) {
    private var state = DailyDashboardScreenState(
        selectedDate = initialDate,
        nutritionTargets = initialNutritionTargets,
        displayWeightKg = initialDisplayWeightKg,
        waterGoalMl = initialWaterGoalMl
    )

    fun current(): DailyDashboardScreenState = state

    fun selectDate(date: LocalDate) {
        state = state.copy(selectedDate = date)
    }

    fun updateDailyCalorieTarget(calories: Int) {
        state = state.copy(
            nutritionTargets = state.nutritionTargets.copy(calories = calories)
        )
    }

    fun isSelectedDate(date: LocalDate): Boolean {
        return state.selectedDate == date
    }

    fun updateProfileMetrics(
        profileWeightKg: Double?,
        displayWeightKg: Double,
        waterGoalMl: Int
    ) {
        state = state.copy(
            profileWeightKg = profileWeightKg,
            displayWeightKg = displayWeightKg,
            waterGoalMl = waterGoalMl
        )
    }

    fun updateWaterMl(waterMl: Int) {
        state = state.copy(waterMl = waterMl)
    }

    fun updateLocalSteps(steps: Long) {
        state = state.copy(localSteps = steps)
    }

    fun updateHealthConnectSummary(
        requestedDate: LocalDate,
        summary: HealthConnectDailySummary
    ): Boolean {
        if (!isSelectedDate(requestedDate)) return false

        state = state.copy(healthConnectSummary = summary)
        return true
    }

    fun updateSummary(
        nutritionTargets: DailyDashboardNutritionTargets,
        profileWeightKg: Double?,
        displayWeightKg: Double
    ): DailyDashboardSummaryStateChange {
        val previousTargets = state.nutritionTargets
        state = state.copy(
            nutritionTargets = nutritionTargets,
            profileWeightKg = profileWeightKg,
            displayWeightKg = displayWeightKg
        )
        return DailyDashboardSummaryStateChange(
            didCalorieTargetChange = previousTargets.calories != nutritionTargets.calories
        )
    }

    fun beginActivePlanLoad(): DailyActivePlanLoadToken {
        val nextGeneration = state.activePlanLoadGeneration + 1L
        state = state.copy(activePlanLoadGeneration = nextGeneration)
        return DailyActivePlanLoadToken(
            generation = nextGeneration,
            targetCalories = state.nutritionTargets.calories
        )
    }

    fun applyActivePlanLoad(
        token: DailyActivePlanLoadToken,
        result: DailyActivePlanLoadResult
    ): Boolean {
        if (token.generation != state.activePlanLoadGeneration) return false

        state = state.copy(
            activePlan = result.activePlan,
            dailyMealPlan = result.dailyPlan
        )
        return true
    }
}

internal data class DailyDashboardScreenState(
    val selectedDate: LocalDate,
    val healthConnectSummary: HealthConnectDailySummary = HealthConnectDailySummary(),
    val profileWeightKg: Double? = null,
    val displayWeightKg: Double,
    val waterMl: Int = 0,
    val waterGoalMl: Int,
    val localSteps: Long = 0L,
    val nutritionTargets: DailyDashboardNutritionTargets,
    val activePlan: ActiveDietPlanUiModel? = null,
    val dailyMealPlan: DailyPlannedMealsUiModel? = null,
    val activePlanLoadGeneration: Long = 0L
)

internal data class DailyDashboardNutritionTargets(
    val calories: Int,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int
)

internal data class DailyDashboardSummaryStateChange(
    val didCalorieTargetChange: Boolean
)

internal data class DailyActivePlanLoadToken(
    val generation: Long,
    val targetCalories: Int
)
