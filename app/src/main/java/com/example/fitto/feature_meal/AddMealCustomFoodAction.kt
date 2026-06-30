package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity

internal class AddMealCustomFoodAction(
    private val createFood: suspend (AddMealCustomFoodRequest) -> FoodEntity
) {
    suspend fun create(request: AddMealCustomFoodRequest): AddMealCustomFoodResult {
        return runCatching {
            AddMealCustomFoodResult.Created(createFood(request))
        }.getOrElse { throwable ->
            AddMealCustomFoodResult.Failed(throwable)
        }
    }
}

internal data class AddMealCustomFoodRequest(
    val name: String,
    val servingAmount: Double,
    val unit: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val mealType: String
)

internal sealed interface AddMealCustomFoodResult {
    data class Created(val food: FoodEntity) : AddMealCustomFoodResult
    data class Failed(val throwable: Throwable) : AddMealCustomFoodResult
}
