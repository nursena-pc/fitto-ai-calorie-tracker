package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeDetailControllerTest {

    @Test
    fun loadRecipe_mapsIngredientsAndResetsTransientState() {
        val origin = RecipeDetailOrigin(fromCommunity = true, communityPostId = "post-1")
        val controller = RecipeDetailController(origin)
        controller.setCookModeEnabled(true)
        controller.increaseServingMultiplier()

        val state = controller.loadRecipe(recipe())

        assertEquals(origin, state.origin)
        assertEquals("recipe-1", state.recipe?.id)
        assertEquals(RecipeDetailTab.OVERVIEW, state.activeTab)
        assertEquals(1, state.servingMultiplier)
        assertFalse(state.editMode)
        assertFalse(state.shoppingSelectMode)
        assertFalse(state.cookModeEnabled)
        assertEquals(listOf("Havuç", "Yoğurt"), state.ingredients.map { it.name })
    }

    @Test
    fun servingMultiplier_neverDropsBelowOneAndFeedsNutritionState() {
        val controller = loadedController()

        controller.decreaseServingMultiplier()
        assertEquals(1, controller.state.servingMultiplier)

        controller.increaseServingMultiplier()
        val nutrition = controller.nutritionState()

        assertEquals(2, controller.state.servingMultiplier)
        assertEquals(4, nutrition.activeServings)
        assertEquals(1200, nutrition.activeTotalCalories)
    }

    @Test
    fun editMode_cancelRestoresIngredientSnapshot() {
        val controller = loadedController()

        val started = controller.toggleEditMode()
        val updated = controller.updateIngredientAmount(index = 0, amount = "3")
        val cancelled = controller.toggleEditMode()

        assertTrue(started.editModeStarted)
        assertEquals("3 adet", updated?.amountUnit)
        assertFalse(cancelled.state.editMode)
        assertEquals("1 adet", cancelled.state.ingredients.first().amountUnit)
    }

    @Test
    fun addIngredient_startsEditModeOnlyOnce() {
        val controller = loadedController()
        val ingredient = ingredient(id = "new", name = "Biber")

        val first = controller.addIngredient(ingredient)
        val second = controller.addIngredient(ingredient.copy(id = "new-2"))

        assertTrue(first.editModeStarted)
        assertFalse(second.editModeStarted)
        assertTrue(second.state.editMode)
        assertEquals(4, second.state.ingredients.size)
    }

    @Test
    fun shoppingSelection_cancelsEditsAndSelectsRestoredIngredients() {
        val controller = loadedController()
        controller.toggleEditMode()
        controller.addIngredient(ingredient(id = "temporary", name = "Geçici"))

        val state = controller.startShoppingSelection()

        assertFalse(state.editMode)
        assertTrue(state.shoppingSelectMode)
        assertEquals(2, state.ingredients.size)
        assertEquals(setOf(0, 1), state.selectedShoppingIndexes)
    }

    @Test
    fun shoppingSelection_buildsSortedDisplayLines() {
        val controller = loadedController()
        controller.startShoppingSelection()

        controller.toggleIngredientSelection(0)
        var state = controller.state
        assertEquals(setOf(1), state.selectedShoppingIndexes)
        assertEquals(listOf("200 g Yoğurt"), state.selectedShoppingLines)

        controller.toggleIngredientSelection(0)
        state = controller.state
        assertEquals(listOf("1 adet Havuç", "200 g Yoğurt"), state.selectedShoppingLines)
    }

    @Test
    fun leavingIngredientsTab_clearsShoppingSelection() {
        val controller = loadedController()
        controller.selectTab(RecipeDetailTab.INGREDIENTS)
        controller.startShoppingSelection()

        val state = controller.selectTab(RecipeDetailTab.STEPS)

        assertEquals(RecipeDetailTab.STEPS, state.activeTab)
        assertFalse(state.shoppingSelectMode)
        assertTrue(state.selectedShoppingIndexes.isEmpty())
    }

    @Test
    fun removeIngredient_reselectsRemainingItemsDuringShoppingMode() {
        val controller = loadedController()
        controller.startShoppingSelection()
        controller.toggleIngredientSelection(1)

        val state = controller.removeIngredient(0)

        assertEquals(1, state.ingredients.size)
        assertEquals(setOf(0), state.selectedShoppingIndexes)
    }

    @Test
    fun completeShoppingSelection_clearsSelectionState() {
        val controller = loadedController()
        controller.startShoppingSelection()

        val state = controller.completeShoppingSelection()

        assertFalse(state.shoppingSelectMode)
        assertTrue(state.selectedShoppingIndexes.isEmpty())
    }

    @Test
    fun successfulEditedSave_keepsEditsAndClosesEditMode() {
        val controller = loadedController()
        controller.toggleEditMode()
        controller.updateIngredientAmount(index = 0, amount = "2")

        val state = controller.completeEditedRecipeSave(
            success = true,
            useEditedVersion = true
        )

        assertFalse(state.editMode)
        assertEquals("2 adet", state.ingredients.first().amountUnit)
    }

    @Test
    fun failedEditedSave_keepsEditableState() {
        val controller = loadedController()
        controller.toggleEditMode()
        controller.updateIngredientAmount(index = 0, amount = "2")

        val state = controller.completeEditedRecipeSave(
            success = false,
            useEditedVersion = true
        )

        assertTrue(state.editMode)
        assertEquals("2 adet", state.ingredients.first().amountUnit)
    }

    @Test
    fun communityBookAction_requiresCommunityOriginAndRecognizedSource() {
        val regular = loadedController()
        assertFalse(regular.shouldUseCommunityBookAction())

        val tagged = RecipeDetailController(
            RecipeDetailOrigin(
                fromCommunity = true,
                communityRecipeSourceTag = "community_recipe"
            )
        ).apply { loadRecipe(recipe()) }
        assertTrue(tagged.shouldUseCommunityBookAction())

        val preview = RecipeDetailController(
            RecipeDetailOrigin(fromCommunity = true)
        ).apply { loadRecipe(recipe(id = "community_preview_42")) }
        assertTrue(preview.shouldUseCommunityBookAction())
    }

    @Test
    fun cookMode_canBeToggledAndSetExplicitly() {
        val controller = loadedController()

        assertTrue(controller.toggleCookMode().cookModeEnabled)
        assertFalse(controller.toggleCookMode().cookModeEnabled)
        assertTrue(controller.setCookModeEnabled(true).cookModeEnabled)
    }

    @Test
    fun invalidIngredientIndex_isIgnored() {
        val controller = loadedController()
        val before = controller.state

        assertNull(controller.updateIngredientAmount(index = 99, amount = "2"))
        assertEquals(before, controller.removeIngredient(index = 99))
    }

    private fun loadedController(): RecipeDetailController {
        return RecipeDetailController().apply { loadRecipe(recipe()) }
    }

    private fun recipe(id: String = "recipe-1"): RecipeUiModel {
        return RecipeUiModel(
            id = id,
            title = "Test Tarifi",
            description = "Açıklama",
            mainCategoryKey = "main",
            subcategoryKey = "sub",
            mainIngredientKeys = emptySet(),
            dietKeys = emptySet(),
            methodKeys = emptySet(),
            durationMinutes = 30,
            durationLabel = "30 dk",
            difficultyKey = "easy",
            calories = 300,
            imageResId = 0,
            servings = 2,
            servingDisplay = "2 kişilik",
            proteinGrams = 10,
            carbGrams = 20,
            fatGrams = 5,
            ingredients = listOf("1 adet havuç", "200 g yoğurt"),
            steps = listOf("Karıştır")
        )
    }

    private fun ingredient(
        id: String,
        name: String
    ): RecipeDetailIngredient {
        return RecipeDetailIngredient(
            id = id,
            icon = "🥄",
            name = name,
            amountUnit = "",
            estimatedCalories = 40
        )
    }
}
