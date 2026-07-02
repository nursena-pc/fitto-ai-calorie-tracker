package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_exercise.data.ExerciseDailySummary
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_health.HealthConnectDailySummary
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealType

internal class DailySummaryLoadAction(
    private val loadProfile: suspend () -> UserProfileEntity?,
    private val getMealSummariesForDate: suspend (dateIso: String) -> List<DailyMealSummary>,
    private val getNutritionSummaryForDate: suspend (dateIso: String) -> DailyNutritionSummary,
    private val getExerciseSummaryForDate: suspend (dateIso: String) -> ExerciseDailySummary,
    private val getExerciseLogsForDate: suspend (dateIso: String) -> List<ExerciseLogLocalModel>,
    private val getMealEntryDetailsForDate: suspend (
        dateIso: String,
        mealType: MealType
    ) -> List<MealEntryDetailUiModel>,
    private val resolveNutritionTargets: (UserProfileEntity?) -> DailyNutritionTargetResolver.Targets
) {

    suspend fun load(request: DailySummaryLoadRequest): DailySummaryLoadResult {
        val profile = loadProfile()
        val mealSummaries = getMealSummariesForDate(request.dateIso)
        val nutritionSummary = getNutritionSummaryForDate(request.dateIso)
        val exerciseSummary = getExerciseSummaryForDate(request.dateIso)
        val exerciseLogs = getExerciseLogsForDate(request.dateIso)
        val mealDetailsByType = request.mealTypes.associateWith { mealType ->
            getMealEntryDetailsForDate(request.dateIso, mealType)
        }
        val nutritionTargets = resolveNutritionTargets(profile)
        val resolvedWeightKg = DailyProfileMetricsResolver.resolveWeightKg(profile)
        val displayWeightKg = resolvedWeightKg ?: request.fallbackWeightKg
        val healthActiveCalories = DailyHealthActiveCaloriesResolver.resolve(
            isAvailable = request.healthConnectSummary.isAvailable,
            hasPermission = request.healthConnectSummary.hasPermission,
            activeCaloriesKcal = request.healthConnectSummary.activeCaloriesKcal
        )
        val summaryState = DailySummaryUiStateResolver.resolve(
            nutritionSummary = nutritionSummary,
            nutritionTargets = nutritionTargets,
            exerciseSummary = exerciseSummary,
            healthActiveCalories = healthActiveCalories,
            displaySteps = request.displaySteps,
            weightKg = displayWeightKg
        )

        return DailySummaryLoadResult(
            profile = profile,
            nutritionTargets = nutritionTargets,
            mealSummariesByType = mealSummaries.associateBy { summary -> summary.mealType },
            mealCalories = DailyMealCaloriesResolver.resolve(mealSummaries),
            mealDetailsByType = mealDetailsByType,
            exerciseLogs = exerciseLogs,
            exerciseLogCount = exerciseSummary.logCount,
            resolvedWeightKg = resolvedWeightKg,
            displayWeightKg = displayWeightKg,
            summaryState = summaryState
        )
    }
}

internal data class DailySummaryLoadRequest(
    val dateIso: String,
    val healthConnectSummary: HealthConnectDailySummary,
    val displaySteps: Long,
    val fallbackWeightKg: Double,
    val mealTypes: List<MealType> = MealType.entries
)

internal data class DailySummaryLoadResult(
    val profile: UserProfileEntity?,
    val nutritionTargets: DailyNutritionTargetResolver.Targets,
    val mealSummariesByType: Map<String, DailyMealSummary>,
    val mealCalories: DailyMealCaloriesResolver.Calories,
    val mealDetailsByType: Map<MealType, List<MealEntryDetailUiModel>>,
    val exerciseLogs: List<ExerciseLogLocalModel>,
    val exerciseLogCount: Int,
    val resolvedWeightKg: Double?,
    val displayWeightKg: Double,
    val summaryState: DailySummaryUiStateResolver.State
)
