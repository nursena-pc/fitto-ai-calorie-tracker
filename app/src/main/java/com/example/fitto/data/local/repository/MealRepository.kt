package com.example.fitto.data.local.repository

import android.content.Context
import com.example.fitto.data.local.DateKeyFormatter
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

class MealRepository internal constructor(
    private val commandSource: MealEntryCommandSource,
    private val querySource: MealEntryQuerySource
) {

    constructor(context: Context) : this(createSources(context.applicationContext))

    private constructor(sources: MealEntrySources) : this(
        commandSource = sources.commandSource,
        querySource = sources.querySource
    )

    suspend fun addManualMealEntry(
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String = DateKeyFormatter.today()
    ) {
        commandSource.addManualMealEntry(mealType, selectedFoods, entryDate)
    }

    suspend fun addPlannedMealEntry(
        mealType: MealType,
        title: String,
        targetCalories: Int,
        plannedMealCalories: Int,
        suggestedFoodNames: List<String> = emptyList(),
        planId: String,
        plannedMealId: String,
        entryDate: String = DateKeyFormatter.today()
    ) {
        commandSource.addPlannedMealEntry(
            mealType = mealType,
            title = title,
            targetCalories = targetCalories,
            plannedMealCalories = plannedMealCalories,
            suggestedFoodNames = suggestedFoodNames,
            planId = planId,
            plannedMealId = plannedMealId,
            entryDate = entryDate
        )
    }

    suspend fun getAllFoods(): List<FoodEntity> = querySource.getAllFoods()

    suspend fun getFoodsForMealEntry(entryId: String): List<MealEntryFoodUiModel> {
        return querySource.getFoodsForMealEntry(entryId)
    }

    suspend fun updateMealEntryFoodAmount(foodRowId: String, newAmount: Double) {
        commandSource.updateMealEntryFoodAmount(foodRowId, newAmount)
    }

    suspend fun deleteMealEntryFood(foodRowId: String) {
        commandSource.deleteMealEntryFood(foodRowId)
    }

    suspend fun getMealSummariesForToday(): List<DailyMealSummary> {
        return querySource.getMealSummariesForDate(DateKeyFormatter.today())
    }

    suspend fun getMealSummariesForDate(dateIso: String): List<DailyMealSummary> {
        return querySource.getMealSummariesForDate(dateIso)
    }

    suspend fun getNutritionSummaryForDate(dateIso: String): DailyNutritionSummary {
        return querySource.getNutritionSummaryForDate(dateIso)
    }

    suspend fun getNutritionSummaryForToday(): DailyNutritionSummary {
        return querySource.getNutritionSummaryForDate(DateKeyFormatter.today())
    }

    suspend fun getMealSummaryForTodayAndType(mealType: MealType): DailyMealSummary {
        return querySource.getMealSummaryForDateAndType(DateKeyFormatter.today(), mealType)
    }

    suspend fun getMealEntryDetailsForToday(mealType: MealType): List<MealEntryDetailUiModel> {
        return querySource.getMealEntryDetailsForDate(DateKeyFormatter.today(), mealType)
    }

    suspend fun getMealEntryDetailsForDate(
        dateIso: String,
        mealType: MealType
    ): List<MealEntryDetailUiModel> {
        return querySource.getMealEntryDetailsForDate(dateIso, mealType)
    }

    suspend fun deleteMealEntry(entryId: String) {
        commandSource.deleteMealEntry(entryId)
    }

    suspend fun scaleMealEntry(entryId: String, multiplier: Double) {
        commandSource.scaleMealEntry(entryId, multiplier)
    }

    private data class MealEntrySources(
        val commandSource: MealEntryCommandSource,
        val querySource: MealEntryQuerySource
    )

    private companion object {
        const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"

        fun createSources(appContext: Context): MealEntrySources {
            val database = FittoDatabaseProvider.getDatabase(appContext)
            val userIdProvider = {
                UserSessionDataGuard.getActiveUid(appContext) ?: LOCAL_ANONYMOUS_USER_ID
            }

            return MealEntrySources(
                commandSource = RoomMealEntryCommandSource(
                    mealDao = database.mealDao(),
                    foodDao = database.foodDao(),
                    dietPlanDao = database.dietPlanDao(),
                    userIdProvider = userIdProvider
                ),
                querySource = RoomMealEntryQuerySource(
                    mealDao = database.mealDao(),
                    foodDao = database.foodDao(),
                    userIdProvider = userIdProvider
                )
            )
        }
    }
}
