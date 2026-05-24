package com.example.fitto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitto.data.local.entity.FavoriteFoodEntity

@Dao
interface FavoriteFoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(favorite: FavoriteFoodEntity)

    @Query("DELETE FROM favorite_foods WHERE user_id = :userId AND food_id = :foodId")
    suspend fun removeFavorite(userId: String, foodId: String)

    @Query("SELECT COUNT(*) FROM favorite_foods WHERE user_id = :userId AND food_id = :foodId")
    suspend fun countFavorite(userId: String, foodId: String): Int

    @Query(
        """
        SELECT food_id
        FROM favorite_foods
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteFoodIds(userId: String, limit: Int = 50): List<String>

    @Query("DELETE FROM favorite_foods WHERE user_id = :userId")
    suspend fun clearFavoritesForUser(userId: String)

    @Deprecated("Use clearFavoritesForUser so account switching keeps each user's favorites.")
    suspend fun clearFavorites() {
        // Intentionally no-op for account switching. Account deletion should call clearFavoritesForUser.
    }
}
