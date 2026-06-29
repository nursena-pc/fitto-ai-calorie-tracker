package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity

object RecipeCommunityEntityResolver {

    data class CommunityEntityInput(
        val id: String,
        val ownerUid: String?,
        val source: RecipeEntity?,
        val values: RecipeCommunitySaveResolver.CommunityRecipeValues,
        val postUserId: String,
        val postId: String,
        val recipeId: String,
        val recipeImageResId: Int?,
        val fallbackImageResId: Int,
        val resolvedSourceImageResId: Int?,
        val now: Long
    )

    fun buildCommunityRecipeEntity(input: CommunityEntityInput): RecipeEntity {
        val cleanPostId = input.postId.trim()
        val cleanRecipeId = input.recipeId.trim()
        val source = input.source

        return if (source != null) {
            source.copy(
                id = input.id,
                ownerUid = input.ownerUid,
                sourceType = RecipeEntity.SOURCE_TYPE_COMMUNITY,
                sourceRecipeId = RecipeCommunitySaveResolver.sourceRecipeIdForCommunityCopy(cleanRecipeId, source),
                sourceUserId = RecipeCommunitySaveResolver.sourceUserIdForCommunityCopy(input.postUserId, source),
                sourcePostId = RecipeCommunitySaveResolver.sourcePostIdForCommunityCopy(cleanPostId, source),
                title = input.values.title,
                description = input.values.description,
                calories = input.values.calories,
                imageResId = input.resolvedSourceImageResId ?: source.imageResId,
                ingredients = input.values.ingredients,
                ingredientNames = input.values.ingredientNames,
                steps = input.values.steps,
                createdAt = input.now,
                updatedAt = input.now
            )
        } else {
            RecipeEntity(
                id = input.id,
                ownerUid = input.ownerUid,
                sourceType = RecipeEntity.SOURCE_TYPE_COMMUNITY,
                sourceRecipeId = RecipeCommunitySaveResolver.sourceRecipeIdForCommunityCopy(cleanRecipeId, null),
                sourceUserId = RecipeCommunitySaveResolver.sourceUserIdForCommunityCopy(input.postUserId, null),
                sourcePostId = RecipeCommunitySaveResolver.sourcePostIdForCommunityCopy(cleanPostId, null),
                title = input.values.title,
                description = input.values.description,
                mainCategoryKey = input.values.category,
                subcategoryKey = "",
                subcategoryKeys = emptySet(),
                mainIngredientKeys = emptySet(),
                dietKeys = emptySet(),
                methodKeys = emptySet(),
                durationMinutes = RecipeValueResolver.representativeMinutes(input.values.durationLabel),
                durationLabel = input.values.durationLabel,
                difficultyKey = DEFAULT_COMMUNITY_DIFFICULTY_KEY,
                calories = input.values.calories,
                imageResId = input.recipeImageResId ?: input.fallbackImageResId,
                servings = DEFAULT_COMMUNITY_SERVINGS,
                servingDisplay = input.values.servingDisplay,
                proteinGrams = DEFAULT_COMMUNITY_MACRO_GRAMS,
                carbGrams = DEFAULT_COMMUNITY_MACRO_GRAMS,
                fatGrams = DEFAULT_COMMUNITY_MACRO_GRAMS,
                ingredientNames = input.values.ingredientNames,
                ingredients = input.values.ingredients,
                steps = input.values.steps,
                tip = null,
                createdAt = input.now,
                updatedAt = input.now
            )
        }
    }

    private const val DEFAULT_COMMUNITY_DIFFICULTY_KEY = "kolay"
    private const val DEFAULT_COMMUNITY_SERVINGS = 1
    private const val DEFAULT_COMMUNITY_MACRO_GRAMS = 0
}
