package com.example.fitto.feature_dietplans.planning

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDietFoodRulesTest {

    @Test
    fun normalizeForSearch_removesTurkishDiacriticsAndSymbols() {
        assertEquals(
            "ispanak corbasi",
            CustomDietFoodRules.normalizeForSearch("\u0130spanak \u00c7orbas\u0131!")
        )
    }

    @Test
    fun isExcludedBy_matchesNormalizedAliasesAndIgnoresShortText() {
        val yogurt = food(
            id = "strained_yogurt",
            nameTr = "S\u00fczme Yo\u011furt",
            nameEn = "Strained Yogurt",
            category = "dairy"
        )

        assertTrue(CustomDietFoodRules.isExcludedBy(yogurt, listOf("s\u00fct")))
        assertFalse(CustomDietFoodRules.isExcludedBy(yogurt, listOf("s")))
    }

    @Test
    fun isAllowedByPlan_appliesVeganPreference() {
        val tofu = food(
            id = "tofu",
            nameTr = "Tofu",
            category = "plant_protein",
            proteinPer100g = 12.0,
            dietTags = "plant_based|balanced"
        )
        val cheese = food(
            id = "cheese",
            nameTr = "Peynir",
            nameEn = "Cheese",
            category = "dairy",
            proteinPer100g = 18.0,
            dietTags = "vegetarian|balanced"
        )
        val veganPlan = plan(preferences = listOf(CustomDietFoodRules.PREF_VEGAN))

        assertTrue(CustomDietFoodRules.isAllowedByPlan(tofu, veganPlan))
        assertFalse(CustomDietFoodRules.isAllowedByPlan(cheese, veganPlan))
    }

    @Test
    fun isAllowedByPlan_appliesLowCarbPreference() {
        val rice = food(
            id = "rice",
            nameTr = "Pilav",
            nameEn = "Rice",
            category = "grain",
            carbsPer100g = 28.0,
            dietTags = "balanced"
        )
        val avocado = food(
            id = "avocado",
            nameTr = "Avokado",
            nameEn = "Avocado",
            category = "fruit_fat",
            carbsPer100g = 8.0,
            fatPer100g = 15.0,
            dietTags = "keto|balanced"
        )
        val lowCarbPlan = plan(preferences = listOf(CustomDietFoodRules.PREF_LOW_CARB))

        assertFalse(CustomDietFoodRules.isAllowedByPlan(rice, lowCarbPlan))
        assertTrue(CustomDietFoodRules.isAllowedByPlan(avocado, lowCarbPlan))
    }

    @Test
    fun scoreFor_prioritizesProteinFoodsForHighProteinPlans() {
        val chicken = food(
            id = "chicken",
            nameTr = "Tavuk",
            nameEn = "Chicken",
            category = "protein",
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            dietTags = "balanced",
            mealTypes = "lunch|dinner"
        )
        val bread = food(
            id = "bread",
            nameTr = "Ekmek",
            nameEn = "Bread",
            category = "grain",
            proteinPer100g = 7.0,
            carbsPer100g = 50.0,
            dietTags = "balanced",
            mealTypes = "lunch|dinner"
        )
        val highProteinPlan = plan(
            goalType = "MUSCLE",
            preferences = listOf(CustomDietFoodRules.PREF_HIGH_PROTEIN)
        )

        assertTrue(
            CustomDietFoodRules.scoreFor(chicken, highProteinPlan, MealType.LUNCH) >
                CustomDietFoodRules.scoreFor(bread, highProteinPlan, MealType.LUNCH)
        )
    }

    @Test
    fun displayName_prefersEnglishOnlyWhenRequested() {
        val oat = food(
            id = "oat",
            nameTr = "Yulaf",
            nameEn = "Oat",
            category = "grain"
        )
        val localOnlyFood = food(
            id = "local",
            nameTr = "Mercimek",
            nameEn = " ",
            category = "legume"
        )

        assertEquals("Oat", CustomDietFoodRules.displayName(oat, preferEnglish = true))
        assertEquals("Yulaf", CustomDietFoodRules.displayName(oat, preferEnglish = false))
        assertEquals("Mercimek", CustomDietFoodRules.displayName(localOnlyFood, preferEnglish = true))
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
