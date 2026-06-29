package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import com.example.fitto.feature_recipes.model.RecipeUiModel

internal object RecipePresentationResolver {

    fun toUiModels(
        entities: List<RecipeEntity>,
        favoriteIds: Set<String>,
        imageResIdResolver: (RecipeEntity) -> Int
    ): List<RecipeUiModel> {
        return entities.map { entity ->
            toUiModel(
                entity = entity,
                isFavorite = entity.id in favoriteIds,
                imageResId = imageResIdResolver(entity)
            )
        }
    }

    fun favoriteOriginalRecipes(
        entities: List<RecipeEntity>,
        favoriteIds: Set<String>
    ): List<RecipeEntity> {
        return entities.filter { entity ->
            entity.id in favoriteIds && entity.sourceType == RecipeEntity.SOURCE_TYPE_SEED
        }
    }

    fun toUiModel(
        entity: RecipeEntity,
        isFavorite: Boolean,
        imageResId: Int
    ): RecipeUiModel {
        return RecipeUiModel(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            mainCategoryKey = entity.mainCategoryKey,
            subcategoryKey = entity.subcategoryKey,
            subcategoryKeys = entity.subcategoryKeys,
            mainIngredientKeys = entity.mainIngredientKeys,
            dietKeys = entity.dietKeys,
            methodKeys = entity.methodKeys,
            durationMinutes = entity.durationMinutes,
            durationLabel = entity.durationLabel,
            difficultyKey = entity.difficultyKey,
            calories = entity.calories,
            imageResId = imageResId,
            servings = entity.servings,
            servingDisplay = entity.servingDisplay,
            proteinGrams = entity.proteinGrams,
            carbGrams = entity.carbGrams,
            fatGrams = entity.fatGrams,
            ingredientNames = entity.ingredientNames,
            ingredients = entity.ingredients,
            steps = entity.steps,
            tip = entity.tip,
            isFavorite = isFavorite
        )
    }

    fun resolvedImageResId(
        entity: RecipeEntity,
        resourceIdForRawId: (String) -> Int?,
        defaultImageResId: Int
    ): Int {
        val rawSeedId = rawSeedImageId(entity)
        if (rawSeedId != null) {
            return resourceIdForRawId(rawSeedId) ?: defaultImageResId
        }

        return entity.imageResId.takeIf { imageResId -> imageResId != 0 } ?: defaultImageResId
    }

    fun imageResIdForRecipeId(
        recipeId: String,
        resourceIdForRawId: (String) -> Int?,
        defaultImageResId: Int
    ): Int {
        return rawImageIdForRecipeId(recipeId)
            ?.let { rawId -> resourceIdForRawId(rawId) }
            ?: defaultImageResId
    }

    fun imageResIdForRawId(
        rawId: String,
        resourceIdForRawId: (String) -> Int?,
        defaultImageResId: Int
    ): Int {
        return resourceIdForRawId(rawId) ?: defaultImageResId
    }

    fun rawSeedImageId(entity: RecipeEntity): String? {
        return when {
            entity.sourceType == RecipeEntity.SOURCE_TYPE_SEED -> entity.sourceRecipeId ?: entity.id.removePrefix("seed_")
            entity.sourceRecipeId?.toIntOrNull() != null -> entity.sourceRecipeId
            entity.id.startsWith("seed_") -> entity.id.removePrefix("seed_")
            else -> null
        }
    }

    fun rawImageIdForRecipeId(recipeId: String): String? {
        val cleanRecipeId = recipeId.trim()
        return when {
            cleanRecipeId.startsWith("seed_") -> cleanRecipeId.removePrefix("seed_")
            cleanRecipeId.toIntOrNull() != null -> cleanRecipeId
            else -> null
        }
    }

    fun recipeImageResourceName(rawId: String): String? {
        val numericId = rawId.toIntOrNull() ?: return null
        return "recipe_${numericId.toString().padStart(3, '0')}"
    }
}
