package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity

object RecipeCommunitySaveResolver {

    enum class SaveDecision {
        FAVORITE_SOURCE,
        STORE_COMMUNITY,
        IGNORE
    }

    data class SaveDecisionInput(
        val ownerUid: String,
        val postUserId: String,
        val recipeTitle: String?,
        val recipeIngredients: List<String>,
        val recipeSteps: List<String>,
        val recipeSourceTag: String?,
        val sourceOwnerUid: String?,
        val sourceType: String?
    )

    data class CommunityRecipeValues(
        val title: String,
        val description: String,
        val durationLabel: String,
        val servingDisplay: String,
        val category: String,
        val calories: Int,
        val ingredients: List<String>,
        val ingredientNames: List<String>,
        val steps: List<String>
    )

    fun resolveSaveDecision(input: SaveDecisionInput): SaveDecision {
        val isMyOwnPost = input.postUserId.isNotBlank() && input.postUserId == input.ownerUid
        val isMyOwnRecipe = input.sourceOwnerUid == input.ownerUid
        val hasPostRecipePayload = !input.recipeTitle.isNullOrBlank() ||
                input.recipeIngredients.isNotEmpty() ||
                input.recipeSteps.isNotEmpty()
        val isAppRecipePost = input.recipeSourceTag == RECIPE_SOURCE_TAG_APP ||
                input.sourceType == RecipeEntity.SOURCE_TYPE_SEED
        val shouldStoreAsCommunity = input.recipeSourceTag == RECIPE_SOURCE_TAG_COMMUNITY ||
                (!isAppRecipePost && !isMyOwnPost && !isMyOwnRecipe && hasPostRecipePayload)

        return when {
            isAppRecipePost -> SaveDecision.FAVORITE_SOURCE
            shouldStoreAsCommunity && !isMyOwnPost && !isMyOwnRecipe -> SaveDecision.STORE_COMMUNITY
            else -> SaveDecision.IGNORE
        }
    }

    fun buildSharedRecipeId(
        ownerUid: String,
        postId: String,
        recipeId: String,
        now: Long
    ): String {
        val safeOwnerUid = RecipeValueResolver.safeRecipeId(ownerUid).trim('_')
        val safeRecipePart = RecipeValueResolver.safeRecipeId(postId.ifBlank { recipeId }).trim('_')
        return "shared_${safeOwnerUid}_${safeRecipePart}_${now}"
    }

    fun buildPreviewRecipeId(
        postId: String,
        recipeId: String
    ): String {
        val safeRecipePart = RecipeValueResolver.safeRecipeId(postId.ifBlank { recipeId }).trim('_')
        return "community_preview_${safeRecipePart}"
    }

    fun cleanLines(lines: List<String>): List<String> {
        return RecipeValueResolver.cleanLines(lines)
    }

    fun resolveCommunityValues(
        recipeTitle: String?,
        recipeDescription: String?,
        recipeDuration: String?,
        recipeServing: String?,
        recipeCalories: Int?,
        recipeCategory: String?,
        recipeIngredients: List<String>,
        recipeSteps: List<String>,
        source: RecipeEntity?,
        defaultDescription: String
    ): CommunityRecipeValues {
        val cleanIngredients = cleanLines(recipeIngredients)
        val cleanSteps = cleanLines(recipeSteps)
        val ingredients = cleanIngredients.ifEmpty { source?.ingredients.orEmpty() }
        val ingredientNames = if (cleanIngredients.isEmpty() && source != null) {
            source.ingredientNames
        } else {
            ingredients.map(RecipeValueResolver::ingredientNameForCopy)
        }
        val steps = cleanSteps.ifEmpty { source?.steps.orEmpty() }

        return CommunityRecipeValues(
            title = recipeTitle.orEmpty().trim().ifBlank { source?.title ?: "Topluluk Tarifi" },
            description = recipeDescription.orEmpty().trim().ifBlank { defaultDescription },
            durationLabel = recipeDuration.orEmpty().trim().ifBlank { source?.durationLabel ?: "15-30 dk" },
            servingDisplay = recipeServing.orEmpty().trim().ifBlank { source?.servingDisplay ?: "1 porsiyon" },
            category = recipeCategory.orEmpty().trim().ifBlank { source?.mainCategoryKey ?: "" },
            calories = recipeCalories ?: source?.calories ?: 0,
            ingredients = ingredients,
            ingredientNames = ingredientNames,
            steps = steps
        )
    }

    fun sourceUserIdForCommunityCopy(postUserId: String, source: RecipeEntity?): String? {
        return postUserId.takeIf { it.isNotBlank() } ?: source?.sourceUserId
    }

    fun sourcePostIdForCommunityCopy(postId: String, source: RecipeEntity?): String? {
        return postId.takeIf { it.isNotBlank() } ?: source?.sourcePostId
    }

    fun sourceRecipeIdForCommunityCopy(recipeId: String, source: RecipeEntity?): String? {
        return source?.let { it.sourceRecipeId ?: it.id } ?: recipeId.takeIf { it.isNotBlank() }
    }

    private const val RECIPE_SOURCE_TAG_APP = "app_recipe"
    private const val RECIPE_SOURCE_TAG_COMMUNITY = "community_recipe"

}
