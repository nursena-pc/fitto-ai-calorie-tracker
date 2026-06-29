package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity

object RecipeUserBookResolver {

    data class UserRecipeEntityInput(
        val draft: UserRecipeDraft,
        val ownerUid: String,
        val now: Long,
        val defaultImageResId: Int
    )

    data class CopyRecipeEntityInput(
        val source: RecipeEntity,
        val ownerUid: String,
        val now: Long,
        val ingredientsOverride: List<String>?,
        val resolvedImageResId: Int
    )

    fun buildUserRecipeEntity(input: UserRecipeEntityInput): RecipeEntity {
        val cleanTitle = cleanTitle(input.draft.title)
        val cleanIngredients = RecipeValueResolver.cleanLines(input.draft.ingredients)
        val cleanSteps = RecipeValueResolver.cleanLines(input.draft.steps)

        return RecipeEntity(
            id = buildUserRecipeId(input.ownerUid, input.now),
            ownerUid = input.ownerUid,
            sourceType = RecipeEntity.SOURCE_TYPE_USER,
            sourceRecipeId = null,
            sourceUserId = null,
            sourcePostId = null,
            title = cleanTitle,
            description = input.draft.description.trim().ifBlank {
                DEFAULT_USER_RECIPE_DESCRIPTION
            },
            mainCategoryKey = input.draft.mainCategoryKey,
            subcategoryKey = "",
            subcategoryKeys = emptySet(),
            mainIngredientKeys = emptySet(),
            dietKeys = emptySet(),
            methodKeys = emptySet(),
            durationMinutes = RecipeValueResolver.representativeMinutes(input.draft.durationLabel),
            durationLabel = input.draft.durationLabel,
            difficultyKey = input.draft.difficultyKey,
            calories = 0,
            imageResId = input.defaultImageResId,
            servings = 1,
            servingDisplay = "1 porsiyon",
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0,
            ingredientNames = cleanIngredients.map { ingredient -> ingredient.substringBefore(" - ").trim() },
            ingredients = cleanIngredients,
            steps = cleanSteps,
            tip = null,
            createdAt = input.now,
            updatedAt = input.now
        )
    }

    fun buildCopiedRecipeEntity(input: CopyRecipeEntityInput): RecipeEntity {
        val source = input.source
        val cleanIngredients = input.ingredientsOverride
            ?.let(RecipeValueResolver::cleanLines)
            ?: source.ingredients

        return source.copy(
            id = buildCopiedRecipeId(
                ownerUid = input.ownerUid,
                sourceId = source.id,
                now = input.now
            ),
            ownerUid = input.ownerUid,
            sourceType = RecipeEntity.SOURCE_TYPE_COPIED,
            sourceRecipeId = source.sourceRecipeId ?: source.id,
            title = copyTitle(source.title, input.ingredientsOverride),
            imageResId = input.resolvedImageResId,
            ingredients = cleanIngredients,
            ingredientNames = copyIngredientNames(
                sourceIngredientNames = source.ingredientNames,
                ingredientsOverride = input.ingredientsOverride,
                cleanIngredients = cleanIngredients
            ),
            calories = source.calories,
            proteinGrams = source.proteinGrams,
            carbGrams = source.carbGrams,
            fatGrams = source.fatGrams,
            createdAt = input.now,
            updatedAt = input.now
        )
    }

    fun buildUserRecipeId(ownerUid: String, now: Long): String {
        return RecipeValueResolver.safeRecipeId("user_${ownerUid}_${now}")
    }

    fun buildCopiedRecipeId(ownerUid: String, sourceId: String, now: Long): String {
        return RecipeValueResolver.safeRecipeId("copied_${ownerUid}_${sourceId}_${now}")
    }

    fun cleanTitle(title: String): String {
        return title.trim().ifBlank { DEFAULT_USER_RECIPE_TITLE }
    }

    fun copyTitle(sourceTitle: String, ingredientsOverride: List<String>?): String {
        return if (ingredientsOverride == null) sourceTitle else "$sourceTitle (Bana Özel)"
    }

    fun copyIngredientNames(
        sourceIngredientNames: List<String>,
        ingredientsOverride: List<String>?,
        cleanIngredients: List<String>
    ): List<String> {
        return if (ingredientsOverride == null) {
            sourceIngredientNames
        } else {
            cleanIngredients.map(RecipeValueResolver::ingredientNameForCopy)
        }
    }



    private const val DEFAULT_USER_RECIPE_TITLE = "Yeni Tarif"
    private const val DEFAULT_USER_RECIPE_DESCRIPTION = "Kullanıcı tarafından eklenen tarif"
}
