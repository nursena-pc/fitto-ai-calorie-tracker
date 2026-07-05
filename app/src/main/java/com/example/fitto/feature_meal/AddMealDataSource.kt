package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

internal interface AddMealDataSource {
    suspend fun initializeFoodCatalog()

    suspend fun getRecentFoods(limit: Int): List<FoodEntity>

    suspend fun searchLocalFoods(
        query: String,
        mealType: String,
        limit: Int
    ): List<FoodEntity>

    suspend fun searchOnlineFoods(
        query: String,
        mealType: String,
        limit: Int,
        pageNumber: Int
    ): List<FoodEntity>

    suspend fun createCustomFood(request: AddMealCustomFoodRequest): FoodEntity

    suspend fun getFoodById(foodId: String): FoodEntity?

    suspend fun getFavoriteFoods(limit: Int): List<FoodEntity>

    suspend fun getFavoriteFoodIds(limit: Int): List<String>

    suspend fun toggleFavoriteFood(foodId: String): Boolean

    suspend fun addManualMealEntry(
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String
    )
}
