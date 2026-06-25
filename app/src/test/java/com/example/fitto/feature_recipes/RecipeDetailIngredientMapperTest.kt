package com.example.fitto.feature_recipes

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeDetailIngredientMapperTest {

    @Test
    fun fromDisplayLine_extractsAmountUnitNameIconAndCalories() {
        val ingredient = RecipeDetailIngredientMapper.fromDisplayLine(
            line = "2 adet yumurta",
            index = 1
        )

        assertEquals("ingredient_1_${"2 adet yumurta".hashCode()}", ingredient.id)
        assertEquals("🥚", ingredient.icon)
        assertEquals("Yumurta", ingredient.name)
        assertEquals("2 adet", ingredient.amountUnit)
        assertEquals(157, ingredient.estimatedCalories)
    }

    @Test
    fun fromDisplayLine_usesRawLineAsNameWhenAmountIsMissing() {
        val ingredient = RecipeDetailIngredientMapper.fromDisplayLine(
            line = "maydanoz",
            index = 0
        )

        assertEquals("Maydanoz", ingredient.name)
        assertEquals("", ingredient.amountUnit)
        assertEquals(40, ingredient.estimatedCalories)
    }

    @Test
    fun splitAmountAndUnit_separatesAmountFromUnit() {
        assertEquals(
            "1/2" to "su bardağı",
            RecipeDetailIngredientMapper.splitAmountAndUnit("1/2 su bardağı")
        )
        assertEquals(
            "" to "bir tutam",
            RecipeDetailIngredientMapper.splitAmountAndUnit("bir tutam")
        )
    }

    @Test
    fun withUpdatedAmount_recalculatesCaloriesAndDisplayLine() {
        val ingredient = RecipeDetailIngredient(
            id = "id",
            icon = "🥕",
            name = "Havuç",
            amountUnit = "1 adet",
            estimatedCalories = 29
        )

        val updated = RecipeDetailIngredientMapper.withUpdatedAmount(
            ingredient = ingredient,
            amount = "2",
            unit = "adet"
        )

        assertEquals("2 adet", updated.amountUnit)
        assertEquals(57, updated.estimatedCalories)
        assertEquals("2 adet Havuç", RecipeDetailIngredientMapper.toDisplayLine(updated))
    }

    @Test
    fun estimateCalories_supportsFractionalVolume() {
        assertEquals(
            350,
            RecipeDetailIngredientMapper.estimateCalories(
                name = "Pirinç",
                amountUnit = "1/2 su bardağı"
            )
        )
    }
}
