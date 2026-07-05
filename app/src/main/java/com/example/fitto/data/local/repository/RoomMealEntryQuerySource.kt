package com.example.fitto.data.local.repository

import com.example.fitto.data.local.dao.FoodDao
import com.example.fitto.data.local.dao.MealDao
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel
import com.example.fitto.feature_meal.model.MealType
import kotlin.math.roundToInt

internal class RoomMealEntryQuerySource(
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
    private val userIdProvider: () -> String
) : MealEntryQuerySource {

    override suspend fun getAllFoods(): List<FoodEntity> {
        return foodDao.getAllFoods(userIdProvider())
    }

    override suspend fun getFoodsForMealEntry(entryId: String): List<MealEntryFoodUiModel> {
        return mealDao.getMealEntryFoodsByMealEntryId(userIdProvider(), entryId).map { food ->
            MealEntryFoodUiModel(
                id = food.id,
                mealEntryId = food.mealEntryId,
                foodId = food.foodId,
                foodName = food.foodName,
                amountLabel = food.amountLabel,
                amountGrams = food.amountGrams,
                calories = food.calories,
                protein = food.protein,
                carbs = food.carbs,
                fat = food.fat
            )
        }
    }

    override suspend fun getMealSummariesForDate(dateIso: String): List<DailyMealSummary> {
        return mealDao.getMealSummariesForDate(userIdProvider(), dateIso)
    }

    override suspend fun getNutritionSummaryForDate(dateIso: String): DailyNutritionSummary {
        return mealDao.getNutritionSummaryForDate(userIdProvider(), dateIso)
    }

    override suspend fun getMealSummaryForDateAndType(
        dateIso: String,
        mealType: MealType
    ): DailyMealSummary {
        return mealDao.getMealSummaryForDateAndMealType(
            userId = userIdProvider(),
            date = dateIso,
            mealType = mealType.argumentValue
        ) ?: DailyMealSummary(
            mealType = mealType.argumentValue,
            totalCalories = 0,
            totalProtein = 0.0,
            totalCarbs = 0.0,
            totalFat = 0.0
        )
    }

    override suspend fun getMealEntryDetailsForDate(
        dateIso: String,
        mealType: MealType
    ): List<MealEntryDetailUiModel> {
        return mealDao.getMealEntryFoodDetailsForDateAndMealType(
            userId = userIdProvider(),
            date = dateIso,
            mealType = mealType.argumentValue
        )
            .groupBy { it.mealEntryId }
            .map { (_, foods) ->
                val first = foods.first()
                MealEntryDetailUiModel(
                    entryId = first.mealEntryId,
                    title = first.entryTitle,
                    sourceType = first.sourceType,
                    foodSummary = foods.joinToString(separator = ", ") { food ->
                        "${food.foodName} (${food.amountLabel})"
                    },
                    totalCalories = foods.sumOf { it.calories },
                    totalProtein = foods.sumOf { it.protein }.roundOneDecimal(),
                    totalCarbs = foods.sumOf { it.carbs }.roundOneDecimal(),
                    totalFat = foods.sumOf { it.fat }.roundOneDecimal(),
                    createdAt = foods.minOf { it.createdAt },
                    foodCount = foods.size
                )
            }
            .sortedByDescending(MealEntryDetailUiModel::createdAt)
    }

    private fun Double.roundOneDecimal(): Double = (this * 10.0).roundToInt() / 10.0
}
