package com.example.fitto.data.local.repository

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.entity.PlannedMealFoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannedMealFoodResolverTest {

    @Test
    fun resolve_usesPlannedFoodsAndScalesConfiguredAmounts() {
        val rows = resolver().resolve(
            request = request(targetCalories = 2700),
            mealEntryId = "entry-1",
            createdAt = 100L,
            allFoods = listOf(food(id = "rice", nameTr = "Pirinç", calories = 200.0)),
            plannedFoods = listOf(
                plannedFood(
                    foodId = "rice",
                    amountGrams = 100.0,
                    scalable = true,
                    minAmount = 50.0,
                    maxAmount = 200.0
                )
            )
        )

        assertEquals(1, rows.size)
        assertEquals("rice", rows.single().foodId)
        assertEquals(150.0, rows.single().amountGrams, 0.0001)
        assertEquals(300, rows.single().calories)
    }

    @Test
    fun resolve_matchesTurkishAndPathLikeFoodKeys() {
        val rows = resolver().resolve(
            request = request(),
            mealEntryId = "entry-1",
            createdAt = 100L,
            allFoods = listOf(food(id = "seed/yogurt", nameTr = "Süzme Yoğurt")),
            plannedFoods = listOf(plannedFood(foodId = "meal/suzme-yogurt"))
        )

        assertEquals("seed/yogurt", rows.single().foodId)
    }

    @Test
    fun resolve_scalesSuggestedFoodsToPlannedCaloriesWhenPlanRowsAreMissing() {
        val rows = resolver().resolve(
            request = request(
                plannedMealCalories = 300,
                suggestedFoodNames = listOf("Apple", "Yogurt")
            ),
            mealEntryId = "entry-1",
            createdAt = 100L,
            allFoods = listOf(
                food(id = "apple", nameTr = "Elma", nameEn = "Apple", calories = 100.0),
                food(id = "yogurt", nameTr = "Yoğurt", nameEn = "Yogurt", calories = 100.0)
            ),
            plannedFoods = emptyList()
        )

        assertEquals(2, rows.size)
        assertEquals(300, rows.sumOf { it.calories })
        assertEquals(listOf(150.0, 150.0), rows.map { it.amountGrams })
    }

    @Test
    fun resolve_usesEstimatedRowWhenNoCatalogFoodMatches() {
        val row = resolver().resolve(
            request = request(
                title = "Plan meal",
                plannedMealCalories = 450,
                suggestedFoodNames = listOf("Missing")
            ),
            mealEntryId = "entry-1",
            createdAt = 100L,
            allFoods = emptyList(),
            plannedFoods = emptyList()
        ).single()

        assertNull(row.foodId)
        assertEquals("Plan meal", row.foodName)
        assertEquals("Planlı öğün", row.amountLabel)
        assertEquals(450, row.calories)
        assertEquals(22.5, row.protein, 0.0001)
        assertEquals(56.3, row.carbs, 0.0001)
        assertEquals(15.0, row.fat, 0.0001)
    }

    private fun resolver(): PlannedMealFoodResolver {
        var nextId = 0
        return PlannedMealFoodResolver(
            MealEntryPayloadFactory(idFactory = { "row-${++nextId}" })
        )
    }

    private fun request(
        title: String = "Meal",
        targetCalories: Int = 1800,
        plannedMealCalories: Int = 400,
        suggestedFoodNames: List<String> = emptyList()
    ): PlannedMealFoodRequest {
        return PlannedMealFoodRequest(
            title = title,
            targetCalories = targetCalories,
            plannedMealCalories = plannedMealCalories,
            suggestedFoodNames = suggestedFoodNames
        )
    }

    private fun food(
        id: String,
        nameTr: String,
        nameEn: String? = null,
        calories: Double = 100.0
    ): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = nameTr,
            nameEn = nameEn,
            category = "test",
            defaultUnit = "g",
            defaultServingAmount = 100.0,
            caloriesPer100g = calories,
            proteinPer100g = 10.0,
            carbsPer100g = 15.0,
            fatPer100g = 2.0,
            fiberPer100g = 1.0,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = "",
            createdAt = 1L,
            updatedAt = 1L
        )
    }

    private fun plannedFood(
        foodId: String,
        amountGrams: Double = 100.0,
        scalable: Boolean = false,
        minAmount: Double = 1.0,
        maxAmount: Double = 5000.0
    ): PlannedMealFoodEntity {
        return PlannedMealFoodEntity(
            id = "planned-$foodId",
            plannedMealId = "planned-meal-1",
            foodId = foodId,
            amountGrams = amountGrams,
            displayAmountTr = "",
            scalable = scalable,
            scaleGroup = "",
            minAmountGrams = minAmount,
            maxAmountGrams = maxAmount,
            createdAt = 1L,
            updatedAt = 1L
        )
    }
}
