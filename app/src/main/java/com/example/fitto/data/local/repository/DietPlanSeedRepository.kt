package com.example.fitto.data.local.repository

import android.content.Context
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.entity.DietPlanEntity
import com.example.fitto.data.local.entity.PlannedMealEntity
import com.example.fitto.data.local.entity.PlannedMealFoodEntity
import com.example.fitto.feature_dietplans.planning.PlannedMealUiModel
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_meal.model.MealType

class DietPlanSeedRepository(context: Context) {

    private val appContext = context.applicationContext
    private val database = FittoDatabaseProvider.getDatabase(appContext)
    private val dietPlanDao = database.dietPlanDao()
    private val foodDao = database.foodDao()

    private fun currentUserId(): String {
        return UserSessionDataGuard.getActiveUid(appContext) ?: LOCAL_ANONYMOUS_USER_ID
    }

    suspend fun getDietPlans(): List<DietPlanEntity> {
        return dietPlanDao.getDietPlans()
    }

    suspend fun getDietPlanById(planId: String): DietPlanEntity? {
        return dietPlanDao.getDietPlanById(planId)
    }

    suspend fun getPlannedMealsForDay(
        planId: String,
        dayNumber: Int
    ): List<PlannedMealEntity> {
        return dietPlanDao.getPlannedMealsForDay(planId, dayNumber)
    }

    suspend fun getFoodsForPlannedMeal(plannedMealId: String): List<PlannedMealFoodEntity> {
        return dietPlanDao.getFoodsForPlannedMeal(plannedMealId)
    }

    suspend fun getPlannedMealUiModelsForDay(
        planId: String,
        dayNumber: Int
    ): List<PlannedMealUiModel> {
        return dietPlanDao.getPlannedMealsForDay(planId, dayNumber).map { meal ->
            val foodNames = dietPlanDao.getFoodsForPlannedMeal(meal.id)
                .map { plannedFood ->
                    foodDao.getFoodById(currentUserId(), plannedFood.foodId)?.nameTr ?: plannedFood.foodId
                }

            PlannedMealUiModel(
                id = meal.id,
                planId = meal.dietPlanId,
                dayNumber = meal.dayNumber,
                mealType = MealType.fromArgument(meal.mealType),
                title = meal.titleTr,
                baseCalories = meal.baseCalories,
                foods = foodNames
            )
        }
    }
}

private const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"
