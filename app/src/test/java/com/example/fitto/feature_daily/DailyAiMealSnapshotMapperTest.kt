package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiMealSnapshotMapperTest {

    @Test
    fun buildMeals_usesDetailsWhenAvailable() {
        val meals = DailyAiMealSnapshotMapper.buildMeals(
            mealDefinitions = listOf(mealDefinition()),
            mealSummariesByType = mapOf(
                "lunch" to mealSummary(totalCalories = 900, protein = 40.0)
            ),
            mealDetailsByType = mapOf(
                "lunch" to listOf(
                    mealDetail(
                        title = "Chicken bowl",
                        foodSummary = "Chicken, rice",
                        totalCalories = 500,
                        protein = 32.0,
                        createdAt = 2L
                    ),
                    mealDetail(
                        title = "Soup",
                        foodSummary = "Lentil soup",
                        totalCalories = 150,
                        protein = 8.0,
                        createdAt = 1L
                    )
                )
            ),
            unavailableDetailsText = "No details",
            maxFoodSummaryChars = 360
        )

        val meal = meals.single()
        assertEquals("Lunch", meal.mealTypeLabel)
        assertEquals(650, meal.totalCalories)
        assertEquals(40.0, meal.proteinGrams, 0.001)
        assertEquals(2, meal.entryCount)
        assertEquals("Soup: Lentil soup; Chicken bowl: Chicken, rice", meal.foodSummary)
    }

    @Test
    fun buildMeals_fallsBackToSummaryWhenDetailsHaveNoCalories() {
        val meals = DailyAiMealSnapshotMapper.buildMeals(
            mealDefinitions = listOf(mealDefinition()),
            mealSummariesByType = mapOf(
                "lunch" to mealSummary(totalCalories = 700, protein = 25.0)
            ),
            mealDetailsByType = mapOf(
                "lunch" to listOf(mealDetail(totalCalories = 0, protein = 0.0))
            ),
            unavailableDetailsText = "No details",
            maxFoodSummaryChars = 360
        )

        val meal = meals.single()
        assertEquals(700, meal.totalCalories)
        assertEquals(0.0, meal.proteinGrams, 0.001)
        assertEquals(1, meal.entryCount)
    }

    @Test
    fun buildMeals_skipsMealsWithoutCalories() {
        val meals = DailyAiMealSnapshotMapper.buildMeals(
            mealDefinitions = listOf(mealDefinition()),
            mealSummariesByType = emptyMap(),
            mealDetailsByType = emptyMap(),
            unavailableDetailsText = "No details",
            maxFoodSummaryChars = 360
        )

        assertEquals(emptyList<DailyAiMealSnapshot>(), meals)
    }

    @Test
    fun buildMeals_usesFallbackTextAndTruncatesFoodSummary() {
        val meals = DailyAiMealSnapshotMapper.buildMeals(
            mealDefinitions = listOf(mealDefinition()),
            mealSummariesByType = emptyMap(),
            mealDetailsByType = mapOf(
                "lunch" to listOf(
                    mealDetail(
                        title = "",
                        foodSummary = "",
                        totalCalories = 300,
                        protein = 10.0
                    )
                )
            ),
            unavailableDetailsText = "No details",
            maxFoodSummaryChars = 10
        )

        assertEquals("Lunch: No ", meals.single().foodSummary)
    }

    private fun mealDefinition() = DailyAiMealSnapshotMapper.MealDefinition(
        key = "lunch",
        label = "Lunch"
    )

    private fun mealSummary(
        totalCalories: Int,
        protein: Double
    ) = DailyMealSummary(
        mealType = "lunch",
        totalCalories = totalCalories,
        totalProtein = protein,
        totalCarbs = 50.0,
        totalFat = 20.0
    )

    private fun mealDetail(
        title: String = "Entry",
        foodSummary: String = "Food",
        totalCalories: Int,
        protein: Double,
        createdAt: Long = 0L
    ) = MealEntryDetailUiModel(
        entryId = "entry-$createdAt",
        title = title,
        sourceType = "manual",
        foodSummary = foodSummary,
        totalCalories = totalCalories,
        totalProtein = protein,
        totalCarbs = 20.0,
        totalFat = 10.0,
        createdAt = createdAt,
        foodCount = 1
    )
}
