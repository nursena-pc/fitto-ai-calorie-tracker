package com.example.fitto.feature_dietplans.planning

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDietMealFoodSelectionResolverTest {

    @Test
    fun sourceFoods_removesZeroCalorieAndDisallowedFoods() {
        val tofu = food(
            id = "tofu",
            nameTr = "Tofu",
            category = "plant_protein",
            dietTags = "plant_based|balanced"
        )
        val cheese = food(
            id = "cheese",
            nameTr = "Peynir",
            category = "dairy",
            dietTags = "vegetarian|balanced"
        )
        val water = food(
            id = "water",
            nameTr = "Su",
            category = "drink",
            caloriesPer100g = 0.0,
            dietTags = "plant_based|balanced"
        )
        val plan = plan(preferences = listOf(CustomDietFoodRules.PREF_VEGAN))

        val result = CustomDietMealFoodSelectionResolver.sourceFoods(
            plan = plan,
            allFoods = listOf(tofu, cheese, water)
        )

        assertEquals(listOf("tofu"), result.map { food -> food.id })
    }

    @Test
    fun sourceFoods_fallsBackToNonExcludedFoodsWhenPreferencesRemoveEverything() {
        val cheese = food(
            id = "cheese",
            nameTr = "Peynir",
            category = "dairy",
            dietTags = "vegetarian|balanced"
        )
        val yogurt = food(
            id = "yogurt",
            nameTr = "Yoğurt",
            category = "dairy",
            dietTags = "vegetarian|balanced"
        )
        val plan = plan(
            preferences = listOf(CustomDietFoodRules.PREF_VEGAN),
            exclusions = listOf("yoğurt")
        )

        val result = CustomDietMealFoodSelectionResolver.sourceFoods(
            plan = plan,
            allFoods = listOf(cheese, yogurt)
        )

        assertEquals(listOf("cheese"), result.map { food -> food.id })
    }

    @Test
    fun selectFoodsForLunch_picksProteinCarbVegetableAndFatWithoutDuplicates() {
        val foods = listOf(
            food(
                id = "chicken",
                nameTr = "Tavuk",
                nameEn = "Chicken",
                category = "protein",
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 0.0
            ),
            food(
                id = "rice",
                nameTr = "Pirinç",
                nameEn = "Rice",
                category = "grain",
                proteinPer100g = 0.0,
                carbsPer100g = 28.0,
                fatPer100g = 0.0
            ),
            food(
                id = "broccoli",
                nameTr = "Brokoli",
                nameEn = "Broccoli",
                category = "vegetable",
                proteinPer100g = 0.0,
                carbsPer100g = 0.0,
                fatPer100g = 0.0
            ),
            food(
                id = "almond",
                nameTr = "Badem",
                nameEn = "Almond",
                category = "nut",
                proteinPer100g = 0.0,
                carbsPer100g = 0.0,
                fatPer100g = 50.0
            )
        )

        val result = CustomDietMealFoodSelectionResolver.selectFoodsForMeal(
            plan = plan(),
            mealType = MealType.LUNCH,
            allFoods = foods,
            dayNumber = 1,
            mealIndex = 0
        )

        val selectedIds = result.map { food -> food.id }

        assertEquals("selectedIds = $selectedIds", 4, selectedIds.size)
        assertEquals("selectedIds = $selectedIds", selectedIds.size, selectedIds.toSet().size)
        assertEquals(
            "selectedIds = $selectedIds",
            setOf("chicken", "rice", "broccoli", "almond"),
            selectedIds.toSet()
        )
    }

    @Test
    fun selectFoodsForSnack_addsProteinOnlyForProteinFocusedPlans() {
        val foods = listOf(
            food(id = "apple", nameTr = "Elma", category = "fruit"),
            food(id = "almond", nameTr = "Badem", category = "nut", fatPer100g = 50.0),
            food(id = "yogurt", nameTr = "Yoğurt", category = "dairy", proteinPer100g = 10.0)
        )

        val balancedResult = CustomDietMealFoodSelectionResolver.selectFoodsForMeal(
            plan = plan(),
            mealType = MealType.SNACK,
            allFoods = foods,
            dayNumber = 1,
            mealIndex = 0
        )
        val proteinResult = CustomDietMealFoodSelectionResolver.selectFoodsForMeal(
            plan = plan(preferences = listOf(CustomDietFoodRules.PREF_HIGH_PROTEIN)),
            mealType = MealType.SNACK,
            allFoods = foods,
            dayNumber = 1,
            mealIndex = 0
        )

        assertFalse(balancedResult.map { food -> food.id }.contains("yogurt"))
        assertTrue(proteinResult.map { food -> food.id }.contains("yogurt"))
    }

    @Test
    fun displayNames_usesEnglishFallbackOnlyWhenRequested() {
        val foods = listOf(
            food(id = "oat", nameTr = "Yulaf", nameEn = "Oat", category = "grain"),
            food(id = "lentil", nameTr = "Mercimek", nameEn = " ", category = "legume")
        )

        assertEquals(
            listOf("Oat", "Mercimek"),
            CustomDietMealFoodSelectionResolver.displayNames(
                foods = foods,
                preferEnglish = true
            )
        )
        assertEquals(
            listOf("Yulaf", "Mercimek"),
            CustomDietMealFoodSelectionResolver.displayNames(
                foods = foods,
                preferEnglish = false
            )
        )
    }

    private fun food(
        id: String,
        nameTr: String,
        nameEn: String? = null,
        category: String,
        caloriesPer100g: Double = 100.0,
        proteinPer100g: Double = 4.0,
        carbsPer100g: Double = 10.0,
        fatPer100g: Double = 3.0,
        dietTags: String = "balanced",
        mealTypes: String = "breakfast|snack|lunch|dinner"
    ): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = nameTr,
            nameEn = nameEn,
            category = category,
            defaultUnit = "g",
            defaultServingAmount = 100.0,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            fiberPer100g = null,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = dietTags,
            mealTypes = mealTypes
        )
    }

    private fun plan(
        goalType: String = "BALANCED",
        dailyCalories: Int = 1800,
        preferences: List<String> = emptyList(),
        exclusions: List<String> = emptyList()
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "custom_plan_test",
            title = "Test plan",
            description = "",
            goalType = goalType,
            dailyCalories = dailyCalories,
            mealCount = 4,
            durationDays = 7,
            isPublic = false,
            preferences = preferences,
            exclusions = exclusions,
            mealOrder = listOf("breakfast", "snack", "lunch", "dinner"),
            isDraft = false,
            createdAt = 0L,
            updatedAt = 0L
        )
    }
}
