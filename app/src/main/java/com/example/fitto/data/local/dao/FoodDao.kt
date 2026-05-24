package com.example.fitto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitto.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoods(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(food: FoodEntity)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun getFoodCount(): Int

    @Query(
        """
        SELECT * FROM foods
        WHERE owner_user_id IS NULL OR owner_user_id = :userId
        ORDER BY name_tr COLLATE NOCASE ASC
        """
    )
    fun observeAllFoods(userId: String): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT *
        FROM foods
        WHERE id IN (:foodIds)
          AND (owner_user_id IS NULL OR owner_user_id = :userId)
        """
    )
    suspend fun getFoodsByIds(
        userId: String,
        foodIds: List<String>
    ): List<FoodEntity>

    @Query(
        """
        SELECT * FROM foods
        WHERE (owner_user_id IS NULL OR owner_user_id = :userId)
          AND (
                name_tr LIKE '%' || :query || '%'
             OR name_en LIKE '%' || :query || '%'
             OR category LIKE '%' || :query || '%'
          )
        ORDER BY name_tr COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun searchFoods(
        userId: String,
        query: String,
        limit: Int = 50
    ): List<FoodEntity>

    @Query(
        """
        SELECT * FROM foods
        WHERE id = :foodId
          AND (owner_user_id IS NULL OR owner_user_id = :userId)
        LIMIT 1
        """
    )
    suspend fun getFoodById(
        userId: String,
        foodId: String
    ): FoodEntity?

    @Query(
        """
        SELECT * FROM foods
        WHERE (owner_user_id IS NULL OR owner_user_id = :userId)
          AND diet_tags LIKE '%' || :dietTag || '%'
        ORDER BY name_tr COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun getFoodsByDietTag(
        userId: String,
        dietTag: String,
        limit: Int = 100
    ): List<FoodEntity>

    @Query(
        """
        SELECT * FROM foods
        WHERE (owner_user_id IS NULL OR owner_user_id = :userId)
          AND meal_types LIKE '%' || :mealType || '%'
        ORDER BY name_tr COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun getFoodsByMealType(
        userId: String,
        mealType: String,
        limit: Int = 100
    ): List<FoodEntity>

    @Query("DELETE FROM foods")
    suspend fun clearFoods()

    @Query("DELETE FROM foods WHERE owner_user_id = :userId")
    suspend fun clearFoodsForOwner(userId: String)

    @Query(
        """
        SELECT * FROM foods
        WHERE owner_user_id IS NULL OR owner_user_id = :userId
        """
    )
    suspend fun getAllFoods(userId: String): List<FoodEntity>
}
