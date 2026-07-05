package com.example.fitto.data.local.repository

import com.example.fitto.data.local.entity.MealEntryEntity
import com.example.fitto.data.local.entity.MealEntryFoodEntity
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealEntryPayloadFactoryTest {

    @Test
    fun createManual_buildsEntryAndFoodRowsWithSharedTimestamp() {
        val ids = ArrayDeque(listOf("entry-1", "row-1", "row-2"))
        val factory = MealEntryPayloadFactory(
            idFactory = { ids.removeFirst() },
            clock = { 1234L }
        )

        val payload = factory.createManual(
            userId = "user-1",
            mealType = MealType.LUNCH,
            selectedFoods = listOf(
                selectedFood("food-1", "Apple", 50, 1.24, 10.04, 0.26),
                selectedFood("food-2", "Yogurt", 100, 5.55, 8.88, 3.33)
            ),
            entryDate = "2026-07-05"
        )!!

        assertEquals("entry-1", payload.entry.id)
        assertEquals("user-1", payload.entry.userId)
        assertEquals("Apple, Yogurt", payload.entry.title)
        assertEquals(150, payload.entry.totalCalories)
        assertEquals(6.8, payload.entry.totalProtein, 0.0001)
        assertEquals(18.9, payload.entry.totalCarbs, 0.0001)
        assertEquals(3.6, payload.entry.totalFat, 0.0001)
        assertEquals(1234L, payload.entry.createdAt)
        assertEquals(listOf("row-1", "row-2"), payload.foods.map { it.id })
        assertEquals(listOf(1234L, 1234L), payload.foods.map { it.createdAt })
    }

    @Test
    fun createManual_returnsNullForEmptySelection() {
        val factory = MealEntryPayloadFactory()

        assertNull(
            factory.createManual(
                userId = "user-1",
                mealType = MealType.BREAKFAST,
                selectedFoods = emptyList(),
                entryDate = "2026-07-05"
            )
        )
    }

    @Test
    fun updateFoodAmount_scalesNutritionAndPreservesUnit() {
        val factory = MealEntryPayloadFactory()
        val row = foodRow(
            amountGrams = 100.0,
            amountLabel = "100 ml",
            calories = 120,
            protein = 4.0,
            carbs = 20.0,
            fat = 2.0
        )

        val updated = factory.updateFoodAmount(row, 250.0)!!

        assertEquals(250.0, updated.amountGrams, 0.0001)
        assertEquals("250 ml", updated.amountLabel)
        assertEquals(300, updated.calories)
        assertEquals(10.0, updated.protein, 0.0001)
        assertEquals(50.0, updated.carbs, 0.0001)
        assertEquals(5.0, updated.fat, 0.0001)
    }

    @Test
    fun updateFoodAmount_returnsNullWhenOriginalAmountIsNotPositive() {
        val factory = MealEntryPayloadFactory()

        assertNull(factory.updateFoodAmount(foodRow(amountGrams = 0.0), 100.0))
    }

    @Test
    fun scaleFoods_clampsMultiplierAndRecalculatesEntry() {
        val factory = MealEntryPayloadFactory(clock = { 9000L })
        val entry = MealEntryEntity(
            id = "entry-1",
            userId = "user-1",
            entryDate = "2026-07-05",
            mealType = MealType.DINNER.argumentValue,
            title = "Old",
            totalCalories = 0,
            totalProtein = 0.0,
            totalCarbs = 0.0,
            totalFat = 0.0,
            sourceType = MealEntryEntity.SOURCE_MANUAL,
            sourcePlanId = null,
            sourcePlannedMealId = null,
            createdAt = 1L,
            updatedAt = 1L
        )
        val foods = listOf(
            foodRow(id = "row-1", foodName = "Rice", amountGrams = 100.0, calories = 100),
            foodRow(id = "row-2", foodName = "Chicken", amountGrams = 50.0, calories = 80)
        )

        val scaled = factory.scaleFoods(foods, 3.0)
        val recalculated = factory.recalculateEntry(entry, scaled)

        assertEquals(listOf(200.0, 100.0), scaled.map { it.amountGrams })
        assertEquals(360, recalculated.totalCalories)
        assertEquals("Rice, Chicken", recalculated.title)
        assertEquals(9000L, recalculated.updatedAt)
    }

    private fun selectedFood(
        id: String,
        name: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double
    ): SelectedFoodUiModel {
        return SelectedFoodUiModel(
            id = "selected-$id",
            foodId = id,
            name = name,
            amountLabel = "100 g",
            amountGrams = 100.0,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            emoji = ""
        )
    }

    private fun foodRow(
        id: String = "row-1",
        foodName: String = "Food",
        amountGrams: Double = 100.0,
        amountLabel: String = "100 g",
        calories: Int = 100,
        protein: Double = 10.0,
        carbs: Double = 15.0,
        fat: Double = 2.0
    ): MealEntryFoodEntity {
        return MealEntryFoodEntity(
            id = id,
            mealEntryId = "entry-1",
            foodId = "food-1",
            foodName = foodName,
            amountLabel = amountLabel,
            amountGrams = amountGrams,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            createdAt = 1L
        )
    }
}
