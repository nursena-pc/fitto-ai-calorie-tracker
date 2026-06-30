package com.example.fitto.feature_meal

import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

class AddMealSaveAction(
    private val saveMealEntry: suspend (
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String
    ) -> Unit
) {

    fun canSave(selectedFoods: List<SelectedFoodUiModel>): Boolean {
        return selectedFoods.isNotEmpty()
    }

    suspend fun save(request: AddMealSaveRequest): AddMealSaveResult {
        if (!canSave(request.selectedFoods)) return AddMealSaveResult.EmptySelection

        return runCatching {
            saveMealEntry(
                request.mealType,
                request.selectedFoods,
                request.entryDate
            )
        }.fold(
            onSuccess = {
                AddMealSaveResult.Saved(
                    selectedCount = request.selectedFoods.size,
                    mealType = request.mealType,
                    entryDate = request.entryDate
                )
            },
            onFailure = { throwable ->
                AddMealSaveResult.Failed(throwable)
            }
        )
    }
}

data class AddMealSaveRequest(
    val mealType: MealType,
    val selectedFoods: List<SelectedFoodUiModel>,
    val entryDate: String
)

sealed interface AddMealSaveResult {
    data object EmptySelection : AddMealSaveResult

    data class Saved(
        val selectedCount: Int,
        val mealType: MealType,
        val entryDate: String
    ) : AddMealSaveResult

    data class Failed(val throwable: Throwable) : AddMealSaveResult
}
