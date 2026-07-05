package com.example.fitto.data.local.repository

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

internal interface MealEntryCommandSource {
    suspend fun addManualMealEntry(
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String
    )

    suspend fun addPlannedMealEntry(
        mealType: MealType,
        title: String,
        targetCalories: Int,
        plannedMealCalories: Int,
        suggestedFoodNames: List<String>,
        planId: String,
        plannedMealId: String,
        entryDate: String
    )

    suspend fun updateMealEntryFoodAmount(foodRowId: String, newAmount: Double)

    suspend fun deleteMealEntryFood(foodRowId: String)

    suspend fun deleteMealEntry(entryId: String)

    suspend fun scaleMealEntry(entryId: String, multiplier: Double)
}

internal interface MealEntryQuerySource {
    suspend fun getAllFoods(): List<FoodEntity>

    suspend fun getFoodsForMealEntry(entryId: String): List<MealEntryFoodUiModel>

    suspend fun getMealSummariesForDate(dateIso: String): List<DailyMealSummary>

    suspend fun getNutritionSummaryForDate(dateIso: String): DailyNutritionSummary

    suspend fun getMealSummaryForDateAndType(
        dateIso: String,
        mealType: MealType
    ): DailyMealSummary

    suspend fun getMealEntryDetailsForDate(
        dateIso: String,
        mealType: MealType
    ): List<MealEntryDetailUiModel>
}
