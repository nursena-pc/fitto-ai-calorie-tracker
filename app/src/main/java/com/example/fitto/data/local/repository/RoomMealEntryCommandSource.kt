package com.example.fitto.data.local.repository

import com.example.fitto.data.local.dao.DietPlanDao
import com.example.fitto.data.local.dao.FoodDao
import com.example.fitto.data.local.dao.MealDao
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import java.util.UUID

internal class RoomMealEntryCommandSource(
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
    private val dietPlanDao: DietPlanDao,
    private val userIdProvider: () -> String,
    private val payloadFactory: MealEntryPayloadFactory = MealEntryPayloadFactory(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
    plannedMealFoodResolver: PlannedMealFoodResolver? = null
) : MealEntryCommandSource {

    private val plannedMealFoodResolver = plannedMealFoodResolver
        ?: PlannedMealFoodResolver(payloadFactory)

    override suspend fun addManualMealEntry(
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String
    ) {
        val payload = payloadFactory.createManual(
            userId = userIdProvider(),
            mealType = mealType,
            selectedFoods = selectedFoods,
            entryDate = entryDate
        ) ?: return

        mealDao.insertMealEntryWithFoods(payload.entry, payload.foods)
    }

    override suspend fun addPlannedMealEntry(
        mealType: MealType,
        title: String,
        targetCalories: Int,
        plannedMealCalories: Int,
        suggestedFoodNames: List<String>,
        planId: String,
        plannedMealId: String,
        entryDate: String
    ) {
        val userId = userIdProvider()
        if (mealDao.countEntriesForPlannedMeal(userId, entryDate, plannedMealId) > 0) return

        val now = clock()
        val mealEntryId = idFactory()
        val allFoods = foodDao.getAllFoods(userId)
        val plannedFoods = dietPlanDao.getFoodsForPlannedMeal(plannedMealId)
        val foodRows = plannedMealFoodResolver.resolve(
            request = PlannedMealFoodRequest(
                title = title,
                targetCalories = targetCalories,
                plannedMealCalories = plannedMealCalories,
                suggestedFoodNames = suggestedFoodNames
            ),
            mealEntryId = mealEntryId,
            createdAt = now,
            allFoods = allFoods,
            plannedFoods = plannedFoods
        )
        val payload = payloadFactory.createPlanned(
            userId = userId,
            mealType = mealType,
            entryDate = entryDate,
            planId = planId,
            plannedMealId = plannedMealId,
            mealEntryId = mealEntryId,
            createdAt = now,
            foodRows = foodRows
        )

        mealDao.insertMealEntryWithFoods(payload.entry, payload.foods)
    }

    override suspend fun updateMealEntryFoodAmount(foodRowId: String, newAmount: Double) {
        val userId = userIdProvider()
        val foodRow = mealDao.getMealEntryFoodById(userId, foodRowId) ?: return
        val entry = mealDao.getMealEntryById(userId, foodRow.mealEntryId) ?: return
        val updatedFood = payloadFactory.updateFoodAmount(foodRow, newAmount) ?: return

        mealDao.updateMealEntryFood(updatedFood)
        recalculateMealEntryTotals(userId, entry.id)
    }

    override suspend fun deleteMealEntryFood(foodRowId: String) {
        val userId = userIdProvider()
        val foodRow = mealDao.getMealEntryFoodById(userId, foodRowId) ?: return
        val entryId = foodRow.mealEntryId

        mealDao.deleteMealEntryFoodById(userId, foodRowId)

        val remainingFoods = mealDao.getMealEntryFoodsByMealEntryId(userId, entryId)
        if (remainingFoods.isEmpty()) {
            mealDao.deleteMealEntryById(userId, entryId)
        } else {
            recalculateMealEntryTotals(userId, entryId)
        }
    }

    override suspend fun deleteMealEntry(entryId: String) {
        mealDao.deleteMealEntryById(userIdProvider(), entryId)
    }

    override suspend fun scaleMealEntry(entryId: String, multiplier: Double) {
        val safeMultiplier = multiplier.coerceIn(MIN_ENTRY_SCALE, MAX_ENTRY_SCALE)
        if (safeMultiplier == 1.0) return

        val userId = userIdProvider()
        val entry = mealDao.getMealEntryById(userId, entryId) ?: return
        val foods = mealDao.getMealEntryFoodsByEntryId(userId, entryId)
        if (foods.isEmpty()) return

        val updatedFoods = payloadFactory.scaleFoods(foods, safeMultiplier)
        val updatedEntry = payloadFactory.recalculateEntry(entry, updatedFoods)

        mealDao.updateMealEntryWithFoods(updatedEntry, updatedFoods)
    }

    private suspend fun recalculateMealEntryTotals(userId: String, entryId: String) {
        val entry = mealDao.getMealEntryById(userId, entryId) ?: return
        val foods = mealDao.getMealEntryFoodsByMealEntryId(userId, entryId)

        if (foods.isEmpty()) {
            mealDao.deleteMealEntryById(userId, entryId)
            return
        }

        mealDao.updateMealEntry(payloadFactory.recalculateEntry(entry, foods))
    }

    private companion object {
        const val MIN_ENTRY_SCALE = 0.5
        const val MAX_ENTRY_SCALE = 2.0
    }
}
