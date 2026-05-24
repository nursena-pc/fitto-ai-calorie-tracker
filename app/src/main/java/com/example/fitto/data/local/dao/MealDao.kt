package com.example.fitto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.fitto.data.local.entity.MealEntryEntity
import com.example.fitto.data.local.entity.MealEntryFoodEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.data.local.model.MealEntryFoodDetail

@Dao
abstract class MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMealEntry(entry: MealEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMealEntryFoods(foods: List<MealEntryFoodEntity>)

    @Update
    abstract suspend fun updateMealEntry(entry: MealEntryEntity)

    @Update
    protected abstract suspend fun updateMealEntryFoods(foods: List<MealEntryFoodEntity>)

    @Update
    abstract suspend fun updateMealEntryFood(food: MealEntryFoodEntity)

    @Transaction
    open suspend fun insertMealEntryWithFoods(
        entry: MealEntryEntity,
        foods: List<MealEntryFoodEntity>
    ) {
        insertMealEntry(entry)
        if (foods.isNotEmpty()) {
            insertMealEntryFoods(foods)
        }
    }

    @Transaction
    open suspend fun updateMealEntryWithFoods(
        entry: MealEntryEntity,
        foods: List<MealEntryFoodEntity>
    ) {
        updateMealEntry(entry)
        if (foods.isNotEmpty()) {
            updateMealEntryFoods(foods)
        }
    }

    @Query(
        """
        SELECT
            meal_type AS mealType,
            COALESCE(SUM(total_calories), 0) AS totalCalories,
            COALESCE(SUM(total_protein), 0) AS totalProtein,
            COALESCE(SUM(total_carbs), 0) AS totalCarbs,
            COALESCE(SUM(total_fat), 0) AS totalFat
        FROM meal_entries
        WHERE user_id = :userId
          AND entry_date = :date
        GROUP BY meal_type
        """
    )
    abstract suspend fun getMealSummariesForDate(
        userId: String,
        date: String
    ): List<DailyMealSummary>

    @Query(
        """
        SELECT
            COALESCE(SUM(total_calories), 0) AS totalCalories,
            COALESCE(SUM(total_protein), 0) AS totalProtein,
            COALESCE(SUM(total_carbs), 0) AS totalCarbs,
            COALESCE(SUM(total_fat), 0) AS totalFat
        FROM meal_entries
        WHERE user_id = :userId
          AND entry_date = :date
        """
    )
    abstract suspend fun getNutritionSummaryForDate(
        userId: String,
        date: String
    ): DailyNutritionSummary

    @Query(
        """
        SELECT COUNT(*)
        FROM meal_entries
        WHERE user_id = :userId
          AND entry_date = :date
          AND source_planned_meal_id = :plannedMealId
        """
    )
    abstract suspend fun countEntriesForPlannedMeal(
        userId: String,
        date: String,
        plannedMealId: String
    ): Int

    @Query(
        """
        SELECT
            meal_type AS mealType,
            COALESCE(SUM(total_calories), 0) AS totalCalories,
            COALESCE(SUM(total_protein), 0) AS totalProtein,
            COALESCE(SUM(total_carbs), 0) AS totalCarbs,
            COALESCE(SUM(total_fat), 0) AS totalFat
        FROM meal_entries
        WHERE user_id = :userId
          AND entry_date = :date
          AND meal_type = :mealType
        GROUP BY meal_type
        """
    )
    abstract suspend fun getMealSummaryForDateAndMealType(
        userId: String,
        date: String,
        mealType: String
    ): DailyMealSummary?

    @Query(
        """
        SELECT
            meal_entries.id AS meal_entry_id,
            meal_entries.title AS entry_title,
            meal_entries.source_type AS source_type,
            meal_entry_foods.food_id AS food_id,
            meal_entry_foods.food_name AS food_name,
            meal_entry_foods.amount_label AS amount_label,
            meal_entry_foods.amount_grams AS amount_grams,
            meal_entry_foods.calories AS calories,
            meal_entry_foods.protein AS protein,
            meal_entry_foods.carbs AS carbs,
            meal_entry_foods.fat AS fat,
            meal_entry_foods.created_at AS created_at
        FROM meal_entry_foods
        INNER JOIN meal_entries ON meal_entries.id = meal_entry_foods.meal_entry_id
        WHERE meal_entries.user_id = :userId
          AND meal_entries.entry_date = :date
          AND meal_entries.meal_type = :mealType
        ORDER BY meal_entries.created_at DESC, meal_entry_foods.created_at ASC
        """
    )
    abstract suspend fun getMealEntryFoodDetailsForDateAndMealType(
        userId: String,
        date: String,
        mealType: String
    ): List<MealEntryFoodDetail>

    @Query(
        """
        SELECT *
        FROM meal_entries
        WHERE user_id = :userId
          AND id = :entryId
        LIMIT 1
        """
    )
    abstract suspend fun getMealEntryById(
        userId: String,
        entryId: String
    ): MealEntryEntity?

    @Query(
        """
        SELECT meal_entry_foods.*
        FROM meal_entry_foods
        INNER JOIN meal_entries ON meal_entries.id = meal_entry_foods.meal_entry_id
        WHERE meal_entries.user_id = :userId
          AND meal_entry_foods.meal_entry_id = :entryId
        ORDER BY meal_entry_foods.created_at ASC
        """
    )
    abstract suspend fun getMealEntryFoodsByEntryId(
        userId: String,
        entryId: String
    ): List<MealEntryFoodEntity>

    @Query(
        """
        SELECT meal_entry_foods.*
        FROM meal_entry_foods
        INNER JOIN meal_entries ON meal_entries.id = meal_entry_foods.meal_entry_id
        WHERE meal_entries.user_id = :userId
          AND meal_entry_foods.meal_entry_id = :mealEntryId
        ORDER BY meal_entry_foods.created_at ASC
        """
    )
    abstract suspend fun getMealEntryFoodsByMealEntryId(
        userId: String,
        mealEntryId: String
    ): List<MealEntryFoodEntity>

    @Query(
        """
        SELECT meal_entry_foods.*
        FROM meal_entry_foods
        INNER JOIN meal_entries ON meal_entries.id = meal_entry_foods.meal_entry_id
        WHERE meal_entries.user_id = :userId
          AND meal_entry_foods.id = :foodRowId
        LIMIT 1
        """
    )
    abstract suspend fun getMealEntryFoodById(
        userId: String,
        foodRowId: String
    ): MealEntryFoodEntity?

    @Query(
        """
        DELETE FROM meal_entry_foods
        WHERE id = :foodRowId
          AND meal_entry_id IN (
              SELECT id FROM meal_entries WHERE user_id = :userId
          )
        """
    )
    abstract suspend fun deleteMealEntryFoodById(
        userId: String,
        foodRowId: String
    )

    @Query(
        """
        DELETE FROM meal_entries
        WHERE user_id = :userId
          AND id = :entryId
        """
    )
    abstract suspend fun deleteMealEntryById(
        userId: String,
        entryId: String
    )

    @Query(
        """
        SELECT food_id
        FROM meal_entry_foods
        INNER JOIN meal_entries ON meal_entries.id = meal_entry_foods.meal_entry_id
        WHERE meal_entries.user_id = :userId
          AND food_id IS NOT NULL
          AND food_id != ''
        GROUP BY food_id
        ORDER BY MAX(meal_entry_foods.created_at) DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getRecentFoodIds(
        userId: String,
        limit: Int = 12
    ): List<String>

    @Query("DELETE FROM meal_entry_foods WHERE meal_entry_id IN (SELECT id FROM meal_entries WHERE user_id = :userId)")
    protected abstract suspend fun clearMealEntryFoodsForUser(userId: String)

    @Query("DELETE FROM meal_entries WHERE user_id = :userId")
    abstract suspend fun clearMealEntriesForUser(userId: String)

    @Transaction
    open suspend fun clearMealLogsForUser(userId: String) {
        clearMealEntryFoodsForUser(userId)
        clearMealEntriesForUser(userId)
    }

    @Deprecated("Use clearMealLogsForUser so different accounts keep their own local history.")
    @Transaction
    open suspend fun clearAllMealLogs() {
        // Intentionally no-op for account switching. Profile/account deletion should call clearMealLogsForUser.
    }
}
