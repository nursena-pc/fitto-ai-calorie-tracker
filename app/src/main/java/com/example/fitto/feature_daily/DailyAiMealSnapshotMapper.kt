package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel

object DailyAiMealSnapshotMapper {

    data class MealDefinition(
        val key: String,
        val label: String
    )

    fun buildMeals(
        mealDefinitions: List<MealDefinition>,
        mealSummariesByType: Map<String, DailyMealSummary>,
        mealDetailsByType: Map<String, List<MealEntryDetailUiModel>>,
        unavailableDetailsText: String,
        maxFoodSummaryChars: Int
    ): List<DailyAiMealSnapshot> {
        return mealDefinitions.mapNotNull { mealDefinition ->
            val details = mealDetailsByType[mealDefinition.key].orEmpty()
            val summary = mealSummariesByType[mealDefinition.key]

            val totalCalories = details.sumOf { it.totalCalories }
                .takeIf { it > 0 }
                ?: (summary?.totalCalories ?: 0)
            if (totalCalories <= 0) return@mapNotNull null

            val protein = details.sumOf { it.totalProtein }
                .takeIf { details.isNotEmpty() }
                ?: (summary?.totalProtein ?: 0.0)
            val carbs = details.sumOf { it.totalCarbs }
                .takeIf { details.isNotEmpty() }
                ?: (summary?.totalCarbs ?: 0.0)
            val fat = details.sumOf { it.totalFat }
                .takeIf { details.isNotEmpty() }
                ?: (summary?.totalFat ?: 0.0)

            DailyAiMealSnapshot(
                mealTypeLabel = mealDefinition.label,
                totalCalories = totalCalories,
                proteinGrams = protein,
                carbGrams = carbs,
                fatGrams = fat,
                entryCount = details.size.coerceAtLeast(1),
                foodSummary = buildFoodSummary(
                    details = details,
                    mealLabel = mealDefinition.label,
                    unavailableDetailsText = unavailableDetailsText,
                    maxFoodSummaryChars = maxFoodSummaryChars
                )
            )
        }
    }

    private fun buildFoodSummary(
        details: List<MealEntryDetailUiModel>,
        mealLabel: String,
        unavailableDetailsText: String,
        maxFoodSummaryChars: Int
    ): String {
        return details
            .sortedBy { it.createdAt }
            .joinToString(separator = "; ") { detail ->
                val title = detail.title.ifBlank {
                    detail.foodSummary.ifBlank { mealLabel }
                }
                val foods = detail.foodSummary.ifBlank { unavailableDetailsText }
                "$title: $foods"
            }
            .take(maxFoodSummaryChars)
    }
}
