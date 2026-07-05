package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeUiModel

enum class RecipeDetailTab {
    OVERVIEW,
    INGREDIENTS,
    STEPS
}

data class RecipeDetailOrigin(
    val fromCommunity: Boolean = false,
    val communityPostId: String? = null,
    val communityPostOwnerId: String? = null,
    val communityRecipeSourceTag: String? = null
)

data class RecipeDetailState(
    val recipe: RecipeUiModel? = null,
    val origin: RecipeDetailOrigin = RecipeDetailOrigin(),
    val activeTab: RecipeDetailTab = RecipeDetailTab.OVERVIEW,
    val editMode: Boolean = false,
    val shoppingSelectMode: Boolean = false,
    val cookModeEnabled: Boolean = false,
    val servingMultiplier: Int = 1,
    val ingredients: List<RecipeDetailIngredient> = emptyList(),
    val selectedShoppingIndexes: Set<Int> = emptySet()
) {
    val ingredientLines: List<String>
        get() = ingredients.map(RecipeDetailIngredientMapper::toDisplayLine)

    val selectedShoppingLines: List<String>
        get() = selectedShoppingIndexes.sorted().mapNotNull { index ->
            ingredients.getOrNull(index)?.let(RecipeDetailIngredientMapper::toDisplayLine)
        }
}

data class RecipeDetailStateChange(
    val state: RecipeDetailState,
    val editModeStarted: Boolean = false
)

class RecipeDetailController(
    initialOrigin: RecipeDetailOrigin = RecipeDetailOrigin()
) {

    var state: RecipeDetailState = RecipeDetailState(origin = initialOrigin)
        private set

    private var editModeSnapshot: List<RecipeDetailIngredient> = emptyList()

    fun updateOrigin(origin: RecipeDetailOrigin): RecipeDetailState {
        state = state.copy(origin = origin)
        return state
    }

    fun loadRecipe(recipe: RecipeUiModel): RecipeDetailState {
        editModeSnapshot = emptyList()
        state = state.copy(
            recipe = recipe,
            activeTab = RecipeDetailTab.OVERVIEW,
            editMode = false,
            shoppingSelectMode = false,
            cookModeEnabled = false,
            servingMultiplier = 1,
            ingredients = recipe.ingredients.mapIndexed { index, line ->
                RecipeDetailIngredientMapper.fromDisplayLine(line, index)
            },
            selectedShoppingIndexes = emptySet()
        )
        return state
    }

    fun selectTab(tab: RecipeDetailTab): RecipeDetailState {
        state = if (tab == RecipeDetailTab.INGREDIENTS) {
            state.copy(activeTab = tab)
        } else {
            state.copy(
                activeTab = tab,
                shoppingSelectMode = false,
                selectedShoppingIndexes = emptySet()
            )
        }
        return state
    }

    fun increaseServingMultiplier(): RecipeDetailState {
        state = state.copy(servingMultiplier = state.servingMultiplier + 1)
        return state
    }

    fun decreaseServingMultiplier(): RecipeDetailState {
        state = state.copy(
            servingMultiplier = (state.servingMultiplier - 1).coerceAtLeast(MIN_SERVING_MULTIPLIER)
        )
        return state
    }

    fun toggleCookMode(): RecipeDetailState {
        state = state.copy(cookModeEnabled = !state.cookModeEnabled)
        return state
    }

    fun setCookModeEnabled(enabled: Boolean): RecipeDetailState {
        state = state.copy(cookModeEnabled = enabled)
        return state
    }

    fun toggleEditMode(): RecipeDetailStateChange {
        return if (state.editMode) {
            cancelEditMode()
            RecipeDetailStateChange(state = state)
        } else {
            startEditMode()
            RecipeDetailStateChange(state = state, editModeStarted = true)
        }
    }

    fun addIngredient(ingredient: RecipeDetailIngredient): RecipeDetailStateChange {
        val editModeStarted = ensureEditMode()
        val ingredients = state.ingredients + ingredient
        val selectedIndexes = if (state.shoppingSelectMode) {
            state.selectedShoppingIndexes + ingredients.lastIndex
        } else {
            state.selectedShoppingIndexes
        }
        state = state.copy(
            ingredients = ingredients,
            selectedShoppingIndexes = selectedIndexes
        )
        return RecipeDetailStateChange(state = state, editModeStarted = editModeStarted)
    }

    fun updateIngredientAmount(index: Int, amount: String): RecipeDetailIngredient? {
        val ingredient = state.ingredients.getOrNull(index) ?: return null
        val (_, unit) = RecipeDetailIngredientMapper.splitAmountAndUnit(ingredient.amountUnit)
        val updated = RecipeDetailIngredientMapper.withUpdatedAmount(
            ingredient = ingredient,
            amount = amount.trim(),
            unit = unit
        )
        state = state.copy(
            ingredients = state.ingredients.toMutableList().apply {
                this[index] = updated
            }
        )
        return updated
    }

    fun removeIngredient(index: Int): RecipeDetailState {
        if (index !in state.ingredients.indices) return state

        val ingredients = state.ingredients.toMutableList().apply { removeAt(index) }
        state = state.copy(
            ingredients = ingredients,
            selectedShoppingIndexes = if (state.shoppingSelectMode) {
                ingredients.indices.toSet()
            } else {
                emptySet()
            }
        )
        return state
    }

    fun startShoppingSelection(): RecipeDetailState {
        if (state.editMode) cancelEditMode()
        state = state.copy(
            shoppingSelectMode = true,
            selectedShoppingIndexes = state.ingredients.indices.toSet()
        )
        return state
    }

    fun toggleIngredientSelection(index: Int): RecipeDetailState {
        if (!state.shoppingSelectMode || index !in state.ingredients.indices) return state

        val selected = state.selectedShoppingIndexes.toMutableSet()
        if (!selected.add(index)) selected.remove(index)
        state = state.copy(selectedShoppingIndexes = selected)
        return state
    }

    fun completeShoppingSelection(): RecipeDetailState {
        state = state.copy(
            shoppingSelectMode = false,
            selectedShoppingIndexes = emptySet()
        )
        return state
    }

    fun completeEditedRecipeSave(success: Boolean, useEditedVersion: Boolean): RecipeDetailState {
        if (success && useEditedVersion && state.editMode) {
            editModeSnapshot = emptyList()
            state = state.copy(editMode = false)
        }
        return state
    }

    fun nutritionState(): RecipePortionNutritionResolver.State {
        val recipe = state.recipe
        return RecipePortionNutritionResolver.resolve(
            recipeCalories = recipe?.calories ?: 0,
            recipeServings = recipe?.servings ?: 1,
            servingMultiplier = state.servingMultiplier,
            ingredientCalories = state.ingredients.map(RecipeDetailIngredient::estimatedCalories),
            proteinGrams = recipe?.proteinGrams ?: 0,
            carbGrams = recipe?.carbGrams ?: 0,
            fatGrams = recipe?.fatGrams ?: 0
        )
    }

    fun shouldUseCommunityBookAction(): Boolean {
        val recipeId = state.recipe?.id.orEmpty()
        val origin = state.origin
        if (!origin.fromCommunity) return false

        return origin.communityRecipeSourceTag == COMMUNITY_RECIPE_SOURCE_TAG ||
            recipeId.startsWith(COMMUNITY_PREVIEW_PREFIX) ||
            recipeId.startsWith(SHARED_RECIPE_PREFIX)
    }

    private fun ensureEditMode(): Boolean {
        if (state.editMode) return false
        startEditMode()
        return true
    }

    private fun startEditMode() {
        editModeSnapshot = state.ingredients.map { ingredient -> ingredient.copy() }
        state = state.copy(
            editMode = true,
            shoppingSelectMode = false,
            selectedShoppingIndexes = emptySet()
        )
    }

    private fun cancelEditMode() {
        state = state.copy(
            ingredients = editModeSnapshot.map { ingredient -> ingredient.copy() },
            editMode = false,
            shoppingSelectMode = false,
            selectedShoppingIndexes = emptySet()
        )
        editModeSnapshot = emptyList()
    }

    private companion object {
        private const val MIN_SERVING_MULTIPLIER = 1
        private const val COMMUNITY_RECIPE_SOURCE_TAG = "community_recipe"
        private const val COMMUNITY_PREVIEW_PREFIX = "community_preview_"
        private const val SHARED_RECIPE_PREFIX = "shared_"
    }
}
