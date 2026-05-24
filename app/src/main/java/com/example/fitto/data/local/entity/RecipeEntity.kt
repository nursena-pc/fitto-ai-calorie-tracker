package com.example.fitto.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val ownerUid: String?,
    val sourceType: String,
    val sourceRecipeId: String?,
    val sourceUserId: String?,
    val sourcePostId: String?,
    val title: String,
    val description: String,
    val mainCategoryKey: String,
    val subcategoryKey: String,
    val subcategoryKeys: Set<String>,
    val mainIngredientKeys: Set<String>,
    val dietKeys: Set<String>,
    val methodKeys: Set<String>,
    val durationMinutes: Int,
    val durationLabel: String,
    val difficultyKey: String,
    val calories: Int,
    val imageResId: Int,
    val servings: Int,
    val servingDisplay: String,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int,
    val ingredientNames: List<String>,
    val ingredients: List<String>,
    val steps: List<String>,
    val tip: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val SOURCE_TYPE_SEED = "SEED"
        const val SOURCE_TYPE_USER = "USER"
        const val SOURCE_TYPE_COPIED = "COPIED"
        const val SOURCE_TYPE_COMMUNITY = "COMMUNITY"
    }
}
