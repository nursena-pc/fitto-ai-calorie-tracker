package com.example.fitto.feature_meal

import com.example.fitto.feature_meal.model.SelectedFoodUiModel

class AddMealSelectionStateResolver {

    fun addSelectedFood(
        currentSelection: List<SelectedFoodUiModel>,
        selectedFood: SelectedFoodUiModel
    ): List<SelectedFoodUiModel> {
        return currentSelection + selectedFood
    }

    fun addDetectedFoods(
        currentSelection: List<SelectedFoodUiModel>,
        detectedFoods: List<SelectedFoodUiModel>
    ): List<SelectedFoodUiModel> {
        if (detectedFoods.isEmpty()) return currentSelection
        return currentSelection + detectedFoods
    }

    fun removeSelectedFood(
        currentSelection: List<SelectedFoodUiModel>,
        food: SelectedFoodUiModel
    ): List<SelectedFoodUiModel> {
        return currentSelection.filterNot { selectedFood ->
            selectedFood.id == food.id
        }
    }

    fun canSave(selection: List<SelectedFoodUiModel>): Boolean {
        return selection.isNotEmpty()
    }

    fun resolveUiState(selection: List<SelectedFoodUiModel>): AddMealSelectionUiState {
        val isEmpty = selection.isEmpty()
        return AddMealSelectionUiState(
            selectedCount = selection.size,
            totalCalories = selection.sumOf { food -> food.calories },
            selectedListVisible = !isEmpty,
            emptyStateVisible = isEmpty,
            saveButtonEnabled = !isEmpty,
            saveButtonAlpha = if (isEmpty) DISABLED_BUTTON_ALPHA else ENABLED_BUTTON_ALPHA
        )
    }

    private companion object {
        private const val ENABLED_BUTTON_ALPHA = 1f
        private const val DISABLED_BUTTON_ALPHA = 0.55f
    }
}

data class AddMealSelectionUiState(
    val selectedCount: Int,
    val totalCalories: Int,
    val selectedListVisible: Boolean,
    val emptyStateVisible: Boolean,
    val saveButtonEnabled: Boolean,
    val saveButtonAlpha: Float
)
