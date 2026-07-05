package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FridgeInventoryCoordinatorTest {

    @Test
    fun suggestions_matchRecipeIngredientsInBothContainmentDirections() = runBlocking {
        val source = FakeSource(
            listOf(
                recipe("1", "Domatesli Omlet", listOf("Cherry domates", "Yumurta")),
                recipe("2", "Yoğurt", listOf("Süzme yoğurt")),
                recipe("3", "Makarna", listOf("Makarna"))
            )
        )
        val coordinator = FridgeInventoryCoordinator(source, Dispatchers.Unconfined)

        val result = coordinator.findRecipeSuggestions(
            ingredients = listOf(
                FridgeIngredient("a", "Domates", "", ""),
                FridgeIngredient("b", "Süzme yoğurt tam yağlı", "", "")
            )
        )

        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun suggestions_respectLimit() = runBlocking {
        val recipes = (1..12).map { index ->
            recipe(index.toString(), "Tarif $index", listOf("Domates"))
        }
        val coordinator = FridgeInventoryCoordinator(FakeSource(recipes), Dispatchers.Unconfined)

        val result = coordinator.findRecipeSuggestions(
            ingredients = listOf(FridgeIngredient("a", "Domates", "", "")),
            limit = 3
        )

        assertEquals(3, result.size)
    }

    @Test
    fun emptyIngredients_orNonPositiveLimit_skipsSource() = runBlocking {
        val source = FakeSource(listOf(recipe("1", "Tarif", listOf("Domates"))))
        val coordinator = FridgeInventoryCoordinator(source, Dispatchers.Unconfined)

        assertTrue(coordinator.findRecipeSuggestions(emptyList()).isEmpty())
        assertTrue(
            coordinator.findRecipeSuggestions(
                listOf(FridgeIngredient("a", "Domates", "", "")),
                limit = 0
            ).isEmpty()
        )
        assertEquals(0, source.loadCount)
    }

    private fun recipe(
        id: String,
        title: String,
        ingredients: List<String>
    ): RecipeUiModel {
        return RecipeUiModel(
            id = id,
            title = title,
            description = "",
            mainCategoryKey = "main",
            subcategoryKey = "sub",
            mainIngredientKeys = emptySet(),
            dietKeys = emptySet(),
            methodKeys = emptySet(),
            durationMinutes = 30,
            durationLabel = "30 dk",
            difficultyKey = "kolay",
            calories = 100,
            imageResId = 0,
            ingredientNames = ingredients,
            ingredients = ingredients,
            steps = emptyList()
        )
    }

    private class FakeSource(
        private val values: List<RecipeUiModel>
    ) : FridgeRecipeSuggestionDataSource {

        var loadCount: Int = 0

        override fun recipes(): List<RecipeUiModel> {
            loadCount += 1
            return values
        }
    }
}
