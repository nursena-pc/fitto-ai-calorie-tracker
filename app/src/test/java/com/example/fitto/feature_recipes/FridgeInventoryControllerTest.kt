package com.example.fitto.feature_recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FridgeInventoryControllerTest {

    @Test
    fun emptyInitialInventory_usesDefaultIngredients() {
        val controller = FridgeInventoryController(emptyList())

        assertEquals(6, controller.ingredients.size)
        assertEquals("Domates", controller.ingredients.first().name)
    }

    @Test
    fun storedInventory_isPreservedWithoutDefaults() {
        val stored = listOf(FridgeIngredient("custom", "Biber", "2", "adet"))

        val controller = FridgeInventoryController(stored)

        assertEquals(stored, controller.ingredients)
    }

    @Test
    fun search_filtersIngredientsUsingTurkishLocale() {
        val controller = FridgeInventoryController(
            listOf(
                FridgeIngredient("1", "Soğan", "1", "adet"),
                FridgeIngredient("2", "Patates", "2", "adet")
            )
        )

        val result = controller.updateSearchQuery("SOĞ")

        assertEquals(listOf("Soğan"), result.map { it.name })
    }

    @Test
    fun blankSearch_returnsAllIngredients() {
        val ingredients = listOf(
            FridgeIngredient("1", "Domates", "1", "adet"),
            FridgeIngredient("2", "Patates", "2", "adet")
        )
        val controller = FridgeInventoryController(ingredients)
        controller.updateSearchQuery("dom")

        val result = controller.updateSearchQuery("   ")

        assertEquals(ingredients, result)
    }

    @Test
    fun addingIngredient_usesGeneratedIdAndTrimsValues() {
        val controller = FridgeInventoryController(
            initialIngredients = listOf(FridgeIngredient("1", "Domates", "1", "adet")),
            idFactory = { "generated" }
        )

        val ingredient = controller.upsertIngredient(
            existingId = null,
            input = FridgeIngredientInput(" Biber ", " 3 ", " adet ")
        )

        assertEquals(FridgeIngredient("generated", "Biber", "3", "adet"), ingredient)
        assertEquals("generated", controller.ingredients.last().id)
    }

    @Test
    fun editingIngredient_replacesMatchingItemInPlace() {
        val controller = FridgeInventoryController(
            listOf(
                FridgeIngredient("1", "Domates", "1", "adet"),
                FridgeIngredient("2", "Patates", "2", "adet")
            )
        )

        controller.upsertIngredient(
            existingId = "1",
            input = FridgeIngredientInput("Domates", "4", "adet")
        )

        assertEquals("4", controller.ingredients.first().amount)
        assertEquals(listOf("1", "2"), controller.ingredients.map { it.id })
    }

    @Test
    fun editingMissingIngredient_isIgnored() {
        val controller = FridgeInventoryController(
            listOf(FridgeIngredient("1", "Domates", "1", "adet"))
        )

        val result = controller.upsertIngredient(
            existingId = "missing",
            input = FridgeIngredientInput("Biber", "2", "adet")
        )

        assertNull(result)
        assertEquals(listOf("1"), controller.ingredients.map { it.id })
    }

    @Test
    fun blankIngredientName_isRejected() {
        val controller = FridgeInventoryController(emptyList())
        val before = controller.ingredients

        val result = controller.upsertIngredient(
            existingId = null,
            input = FridgeIngredientInput(" ", "1", "adet")
        )

        assertNull(result)
        assertEquals(before, controller.ingredients)
    }

    @Test
    fun removeIngredient_reportsWhetherItemWasRemoved() {
        val controller = FridgeInventoryController(
            listOf(FridgeIngredient("1", "Domates", "1", "adet"))
        )

        assertTrue(controller.removeIngredient("1"))
        assertFalse(controller.removeIngredient("missing"))
        assertTrue(controller.ingredients.isEmpty())
    }
}
