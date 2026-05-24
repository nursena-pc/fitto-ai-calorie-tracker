package com.example.fitto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.fitto.data.local.entity.DietPlanEntity
import com.example.fitto.data.local.entity.PlannedMealEntity
import com.example.fitto.data.local.entity.PlannedMealFoodEntity

@Dao
interface DietPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDietPlans(plans: List<DietPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlannedMeals(meals: List<PlannedMealEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlannedMealFoods(foods: List<PlannedMealFoodEntity>)

    @Query("SELECT COUNT(*) FROM diet_plans")
    suspend fun getDietPlanCount(): Int

    @Query("SELECT COUNT(*) FROM planned_meals")
    suspend fun getPlannedMealCount(): Int

    @Query("SELECT COUNT(*) FROM planned_meal_foods")
    suspend fun getPlannedMealFoodCount(): Int

    @Query("SELECT * FROM diet_plans ORDER BY is_restrictive ASC, name_tr COLLATE NOCASE ASC")
    suspend fun getDietPlans(): List<DietPlanEntity>

    @Query("SELECT * FROM diet_plans WHERE id = :planId LIMIT 1")
    suspend fun getDietPlanById(planId: String): DietPlanEntity?

    @Query(
        """
        SELECT * FROM planned_meals
        WHERE diet_plan_id = :planId AND day_number = :dayNumber
        ORDER BY order_index ASC
        """
    )
    suspend fun getPlannedMealsForDay(
        planId: String,
        dayNumber: Int
    ): List<PlannedMealEntity>

    @Query(
        """
        SELECT * FROM planned_meal_foods
        WHERE planned_meal_id = :plannedMealId
        ORDER BY id ASC
        """
    )
    suspend fun getFoodsForPlannedMeal(plannedMealId: String): List<PlannedMealFoodEntity>

    @Transaction
    suspend fun replaceDietSeed(
        plans: List<DietPlanEntity>,
        meals: List<PlannedMealEntity>,
        mealFoods: List<PlannedMealFoodEntity>
    ) {
        clearPlannedMealFoods()
        clearPlannedMeals()
        clearDietPlans()
        upsertDietPlans(plans)
        upsertPlannedMeals(meals)
        upsertPlannedMealFoods(mealFoods)
    }

    @Query("DELETE FROM planned_meal_foods")
    suspend fun clearPlannedMealFoods()

    @Query("DELETE FROM planned_meals")
    suspend fun clearPlannedMeals()

    @Query("DELETE FROM diet_plans")
    suspend fun clearDietPlans()
}
