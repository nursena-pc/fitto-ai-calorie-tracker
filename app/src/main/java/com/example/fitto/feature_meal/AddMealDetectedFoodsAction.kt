package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

internal class AddMealDetectedFoodsAction(
    private val getFoodById: suspend (foodId: String) -> FoodEntity?,
    private val presentationResolver: AddMealFoodPresentationResolver =
        AddMealFoodPresentationResolver()
) {

    suspend fun load(request: AddMealDetectedFoodsRequest): AddMealDetectedFoodsResult {
        if (request.foodIds.isEmpty()) return AddMealDetectedFoodsResult.NoFoods

        val selectedFoods = request.foodIds.mapNotNull { foodId ->
            getFoodById(foodId)?.let(presentationResolver::toSelectedFoodFromDetectedServing)
        }

        return if (selectedFoods.isEmpty()) {
            AddMealDetectedFoodsResult.NoFoods
        } else {
            AddMealDetectedFoodsResult.Imported(selectedFoods)
        }
    }
}

internal data class AddMealDetectedFoodsRequest(
    val foodIds: List<String>
)

internal sealed interface AddMealDetectedFoodsResult {
    data object NoFoods : AddMealDetectedFoodsResult

    data class Imported(
        val detectedFoods: List<SelectedFoodUiModel>
    ) : AddMealDetectedFoodsResult
}
