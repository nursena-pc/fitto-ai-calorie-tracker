package com.example.fitto.data.local.repository

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MealRepositoryTest {

    @Test
    fun addOperations_areForwardedToCommandSource() = runBlocking {
        val commands = FakeCommandSource()
        val repository = MealRepository(commands, FakeQuerySource())
        val selectedFoods = listOf(selectedFood())

        repository.addManualMealEntry(MealType.BREAKFAST, selectedFoods, "2026-07-05")
        repository.addPlannedMealEntry(
            mealType = MealType.DINNER,
            title = "Plan meal",
            targetCalories = 1800,
            plannedMealCalories = 500,
            suggestedFoodNames = listOf("Rice"),
            planId = "plan-1",
            plannedMealId = "planned-1",
            entryDate = "2026-07-06"
        )

        assertEquals(MealType.BREAKFAST, commands.manualMealType)
        assertSame(selectedFoods, commands.manualFoods)
        assertEquals("2026-07-05", commands.manualDate)
        assertEquals(MealType.DINNER, commands.plannedMealType)
        assertEquals("Plan meal", commands.plannedTitle)
        assertEquals("plan-1", commands.planId)
        assertEquals("planned-1", commands.plannedMealId)
        assertEquals("2026-07-06", commands.plannedDate)
    }

    @Test
    fun mutationOperations_areForwardedToCommandSource() = runBlocking {
        val commands = FakeCommandSource()
        val repository = MealRepository(commands, FakeQuerySource())

        repository.updateMealEntryFoodAmount("food-row", 175.0)
        repository.deleteMealEntryFood("food-row")
        repository.deleteMealEntry("entry-1")
        repository.scaleMealEntry("entry-2", 1.5)

        assertEquals("food-row", commands.updatedFoodRowId)
        assertEquals(175.0, commands.updatedAmount, 0.0001)
        assertEquals("food-row", commands.deletedFoodRowId)
        assertEquals("entry-1", commands.deletedEntryId)
        assertEquals("entry-2", commands.scaledEntryId)
        assertEquals(1.5, commands.scaleMultiplier, 0.0001)
    }

    @Test
    fun explicitDateQueries_areForwardedToQuerySource() = runBlocking {
        val queries = FakeQuerySource()
        val repository = MealRepository(FakeCommandSource(), queries)

        assertSame(queries.mealSummaries, repository.getMealSummariesForDate("2026-07-05"))
        assertSame(queries.nutritionSummary, repository.getNutritionSummaryForDate("2026-07-05"))
        assertSame(
            queries.entryDetails,
            repository.getMealEntryDetailsForDate("2026-07-05", MealType.LUNCH)
        )
        assertEquals("2026-07-05", queries.lastDate)
        assertEquals(MealType.LUNCH, queries.lastMealType)
    }

    @Test
    fun todayMealSummary_isForwardedWithRequestedMealType() = runBlocking {
        val queries = FakeQuerySource()
        val repository = MealRepository(FakeCommandSource(), queries)

        assertSame(queries.mealSummary, repository.getMealSummaryForTodayAndType(MealType.LUNCH))
        assertEquals(MealType.LUNCH, queries.lastMealType)
    }

    private class FakeCommandSource : MealEntryCommandSource {
        var manualMealType: MealType? = null
        var manualFoods: List<SelectedFoodUiModel>? = null
        var manualDate: String? = null
        var plannedMealType: MealType? = null
        var plannedTitle: String? = null
        var planId: String? = null
        var plannedMealId: String? = null
        var plannedDate: String? = null
        var updatedFoodRowId: String? = null
        var updatedAmount: Double = 0.0
        var deletedFoodRowId: String? = null
        var deletedEntryId: String? = null
        var scaledEntryId: String? = null
        var scaleMultiplier: Double = 0.0

        override suspend fun addManualMealEntry(
            mealType: MealType,
            selectedFoods: List<SelectedFoodUiModel>,
            entryDate: String
        ) {
            manualMealType = mealType
            manualFoods = selectedFoods
            manualDate = entryDate
        }

        override suspend fun addPlannedMealEntry(
            mealType: MealType,
            title: String,
            targetCalories: Int,
            plannedMealCalories: Int,
            suggestedFoodNames: List<String>,
            planId: String,
            plannedMealId: String,
            entryDate: String
        ) {
            plannedMealType = mealType
            plannedTitle = title
            this.planId = planId
            this.plannedMealId = plannedMealId
            plannedDate = entryDate
        }

        override suspend fun updateMealEntryFoodAmount(foodRowId: String, newAmount: Double) {
            updatedFoodRowId = foodRowId
            updatedAmount = newAmount
        }

        override suspend fun deleteMealEntryFood(foodRowId: String) {
            deletedFoodRowId = foodRowId
        }

        override suspend fun deleteMealEntry(entryId: String) {
            deletedEntryId = entryId
        }

        override suspend fun scaleMealEntry(entryId: String, multiplier: Double) {
            scaledEntryId = entryId
            scaleMultiplier = multiplier
        }
    }

    private class FakeQuerySource : MealEntryQuerySource {
        val mealSummaries = listOf(DailyMealSummary("lunch", 400, 20.0, 50.0, 10.0))
        val nutritionSummary = DailyNutritionSummary(400, 20.0, 50.0, 10.0)
        val mealSummary = DailyMealSummary("lunch", 400, 20.0, 50.0, 10.0)
        val entryDetails = listOf(
            MealEntryDetailUiModel(
                entryId = "entry-1",
                title = "Lunch",
                sourceType = "manual",
                foodSummary = "Rice (100 g)",
                totalCalories = 400,
                totalProtein = 20.0,
                totalCarbs = 50.0,
                totalFat = 10.0,
                createdAt = 1L,
                foodCount = 1
            )
        )
        var lastDate: String? = null
        var lastMealType: MealType? = null

        override suspend fun getAllFoods(): List<FoodEntity> = emptyList()

        override suspend fun getFoodsForMealEntry(entryId: String): List<MealEntryFoodUiModel> {
            return emptyList()
        }

        override suspend fun getMealSummariesForDate(dateIso: String): List<DailyMealSummary> {
            lastDate = dateIso
            return mealSummaries
        }

        override suspend fun getNutritionSummaryForDate(dateIso: String): DailyNutritionSummary {
            lastDate = dateIso
            return nutritionSummary
        }

        override suspend fun getMealSummaryForDateAndType(
            dateIso: String,
            mealType: MealType
        ): DailyMealSummary {
            lastDate = dateIso
            lastMealType = mealType
            return mealSummary
        }

        override suspend fun getMealEntryDetailsForDate(
            dateIso: String,
            mealType: MealType
        ): List<MealEntryDetailUiModel> {
            lastDate = dateIso
            lastMealType = mealType
            return entryDetails
        }
    }

    private fun selectedFood(): SelectedFoodUiModel {
        return SelectedFoodUiModel(
            id = "selected-1",
            foodId = "food-1",
            name = "Food",
            amountLabel = "100 g",
            amountGrams = 100.0,
            calories = 100,
            protein = 10.0,
            carbs = 15.0,
            fat = 2.0,
            emoji = ""
        )
    }
}
