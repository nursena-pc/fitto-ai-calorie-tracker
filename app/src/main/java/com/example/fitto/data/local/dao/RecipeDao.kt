package com.example.fitto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitto.data.local.entity.RecipeEntity

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes WHERE sourceType = 'SEED' ORDER BY title COLLATE NOCASE")
    fun getSeedRecipes(): List<RecipeEntity>

    @Query(
        """
        SELECT * FROM recipes
        WHERE sourceType = 'SEED' OR ownerUid = :ownerUid
        ORDER BY sourceType COLLATE NOCASE, title COLLATE NOCASE
        """
    )
    fun getVisibleRecipes(ownerUid: String): List<RecipeEntity>


    @Query(
        """
        SELECT * FROM recipes
        WHERE ownerUid = :ownerUid AND sourceType IN ('USER', 'COPIED')
        ORDER BY updatedAt DESC, title COLLATE NOCASE
        """
    )
    fun getUserRecipes(ownerUid: String): List<RecipeEntity>

    @Query(
        """
        SELECT * FROM recipes
        WHERE ownerUid = :ownerUid AND sourceType = 'COMMUNITY'
        ORDER BY updatedAt DESC, title COLLATE NOCASE
        """
    )
    fun getCommunityRecipes(ownerUid: String): List<RecipeEntity>

    @Query(
        """
        SELECT * FROM recipes
        WHERE ownerUid = :ownerUid AND sourcePostId = :postId
        LIMIT 1
        """
    )
    fun findByOwnerAndSourcePost(ownerUid: String, postId: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    fun findById(recipeId: String): RecipeEntity?

    @Query("SELECT COUNT(*) FROM recipes WHERE sourceType = 'SEED'")
    fun countSeedRecipes(): Int

    @Query("DELETE FROM recipes WHERE sourceType = 'SEED'")
    fun deleteSeedRecipes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertRecipe(recipe: RecipeEntity)

    @Query(
        """
        DELETE FROM recipes
        WHERE id = :recipeId
            AND ownerUid = :ownerUid
            AND sourceType IN ('USER', 'COPIED', 'COMMUNITY')
        """
    )
    fun deleteOwnedBookRecipe(recipeId: String, ownerUid: String): Int

    @Query(
        """
        DELETE FROM recipes
        WHERE ownerUid = :ownerUid
            AND sourceType IN ('USER', 'COPIED', 'COMMUNITY')
        """
    )
    fun deleteOwnedRecipes(ownerUid: String): Int
}
