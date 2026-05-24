package com.example.fitto.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fitto.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query(
        """
        SELECT *
        FROM user_profiles
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getProfile(userId: String): UserProfileEntity?

    @Query(
        """
        SELECT *
        FROM user_profiles
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Query(
        """
        DELETE FROM user_profiles
        WHERE user_id = :userId
        """
    )
    suspend fun deleteProfile(userId: String)

    @Query("DELETE FROM user_profiles")
    suspend fun clearProfiles()
}