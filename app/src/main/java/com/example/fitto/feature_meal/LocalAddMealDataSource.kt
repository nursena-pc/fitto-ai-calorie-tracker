package com.example.fitto.feature_meal

import android.content.Context
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.repository.FoodRepository
import com.example.fitto.data.local.repository.MealRepository
import com.example.fitto.data.seed.SeedDataImporter
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

internal class LocalAddMealDataSource(
    context: Context,
    private val foodRepository: FoodRepository = FoodRepository(context.applicationContext),
    private val mealRepository: MealRepository = MealRepository(context.applicationContext)
) : AddMealDataSource {

    private val appContext = context.applicationContext

    override suspend fun initializeFoodCatalog() {
        SeedDataImporter.importFoodSeedIfNeeded(appContext)
    }

    override suspend fun getRecentFoods(limit: Int): List<FoodEntity> {
        return foodRepository.getRecentFoods(limit)
    }

    override suspend fun searchLocalFoods(
        query: String,
        mealType: String,
        limit: Int
    ): List<FoodEntity> {
        return foodRepository.searchFoods(
            query = query,
            mealType = mealType,
            limit = limit
        )
    }

    override suspend fun searchOnlineFoods(
        query: String,
        mealType: String,
        limit: Int,
        pageNumber: Int
    ): List<FoodEntity> {
        return foodRepository.searchUsdaFoods(
            query = query,
            mealType = mealType,
            limit = limit,
            pageNumber = pageNumber
        )
    }

    override suspend fun createCustomFood(request: AddMealCustomFoodRequest): FoodEntity {
        return foodRepository.createCustomFood(
            name = request.name,
            defaultServingAmount = request.servingAmount,
            defaultUnit = request.unit,
            caloriesPer100g = request.caloriesPer100g,
            proteinPer100g = request.proteinPer100g,
            carbsPer100g = request.carbsPer100g,
            fatPer100g = request.fatPer100g,
            mealType = request.mealType
        )
    }

    override suspend fun getFoodById(foodId: String): FoodEntity? {
        return foodRepository.getFoodById(foodId)
    }

    override suspend fun getFavoriteFoods(limit: Int): List<FoodEntity> {
        return foodRepository.getFavoriteFoods(limit)
    }

    override suspend fun getFavoriteFoodIds(limit: Int): List<String> {
        return foodRepository.getFavoriteFoodIds(limit)
    }

    override suspend fun toggleFavoriteFood(foodId: String): Boolean {
        return foodRepository.toggleFavoriteFood(foodId)
    }

    override suspend fun addManualMealEntry(
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String
    ) {
        mealRepository.addManualMealEntry(
            mealType = mealType,
            selectedFoods = selectedFoods,
            entryDate = entryDate
        )
    }
}
