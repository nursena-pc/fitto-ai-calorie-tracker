package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AddMealFoodPresentationResolverTest {

    private val resolver = AddMealFoodPresentationResolver(
        idSuffixProvider = { "fixed" }
    )

    @Test
    fun `maps food lists with localized names and favorite ids`() {
        val models = resolver.toFoodUiModels(
            foods = listOf(
                food(id = "banana", nameTr = "Muz", nameEn = "Banana"),
                food(id = "apple", nameTr = "Elma", nameEn = "Apple")
            ),
            languageCode = "en",
            favoriteIds = setOf("apple")
        )

        assertEquals(listOf("Banana", "Apple"), models.map { it.name })
        assertFalse(models.first().isFavorite)
        assertTrue(models.last().isFavorite)
    }

    @Test
    fun `maps empty food list to empty presentation list`() {
        assertTrue(
            resolver.toFoodUiModels(
                foods = emptyList(),
                languageCode = "tr"
            ).isEmpty()
        )
    }

    @Test
    fun `uses english name when language is english and name exists`() {
        val model = resolver.toFoodUiModel(
            food = food(nameTr = "Muz", nameEn = "Banana"),
            languageCode = "en"
        )

        assertEquals("Banana", model.name)
    }

    @Test
    fun `falls back to turkish name when english name is blank`() {
        val model = resolver.toFoodUiModel(
            food = food(nameTr = "Yumurta", nameEn = " "),
            languageCode = "en"
        )

        assertEquals("Yumurta", model.name)
    }

    @Test
    fun `maps food ui model with default serving fallback and favorite state`() {
        val model = resolver.toFoodUiModel(
            food = food(
                id = "custom-food",
                category = "vegetable",
                defaultServingAmount = 0.0,
                defaultUnit = "g",
                caloriesPer100g = 45.4
            ),
            languageCode = "tr",
            isFavorite = true
        )

        assertEquals("custom-food", model.id)
        assertEquals("100 g", model.serving)
        assertEquals(45, model.calories)
        assertEquals("🥗", model.emoji)
        assertTrue(model.isFavorite)
    }

    @Test
    fun `maps detected selected food with deterministic id and macro totals`() {
        val model = resolver.toSelectedFoodFromDetectedServing(
            food = food(
                id = "chicken-breast",
                nameTr = "Tavuk Göğsü",
                defaultServingAmount = 150.0,
                caloriesPer100g = 165.0,
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 3.6
            )
        )

        assertEquals("chicken-breast-fixed", model.id)
        assertEquals("chicken-breast", model.foodId)
        assertEquals("Tavuk Göğsü", model.name)
        assertEquals("150 g", model.amountLabel)
        assertEquals(248, model.calories)
        assertEquals(46.5, model.protein, 0.01)
        assertEquals(0.0, model.carbs, 0.01)
        assertEquals(5.4, model.fat, 0.01)
        assertEquals("🍗", model.emoji)
    }

    @Test
    fun `format serving label preserves one decimal for fractional amounts`() {
        assertEquals("12.5 g", resolver.formatServingLabel(12.5, "g"))
        assertEquals("12 g", resolver.formatServingLabel(12.0, "g"))
    }

    @Test
    fun `food emoji falls back to plate for unknown food`() {
        assertEquals("🍽️", resolver.foodEmoji(food(id = "mystery", category = "unknown")))
    }

    @Test
    fun `negative serving calories are clamped to zero`() {
        val model = resolver.toFoodUiModel(
            food = food(caloriesPer100g = -20.0),
            languageCode = "tr"
        )

        assertEquals(0, model.calories)
        assertFalse(model.isFavorite)
    }

    private fun food(
        id: String = "banana",
        nameTr: String = "Muz",
        nameEn: String? = "Banana",
        category: String = "fruit",
        defaultUnit: String = "g",
        defaultServingAmount: Double = 100.0,
        caloriesPer100g: Double = 89.0,
        proteinPer100g: Double = 1.1,
        carbsPer100g: Double = 22.8,
        fatPer100g: Double = 0.3
    ): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = nameTr,
            nameEn = nameEn,
            category = category,
            defaultUnit = defaultUnit,
            defaultServingAmount = defaultServingAmount,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            fiberPer100g = null,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = ""
        )
    }
}
