package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity

object RecipeCommunityShareResolver {

    data class ShareContentInput(
        val comment: String,
        val recipeTitle: String,
        val ingredients: List<String>
    )

    fun cleanShareIngredients(
        ingredientsOverride: List<String>?,
        sourceIngredients: List<String>
    ): List<String> {
        return ingredientsOverride
            ?.let(RecipeValueResolver::cleanLines)
            ?: sourceIngredients
    }

    fun buildShareContent(input: ShareContentInput): String {
        return buildString {
            val cleanComment = input.comment.trim()
            if (cleanComment.isNotBlank()) {
                append(cleanComment)
                append("\n\n")
            }
            append("Denediğim tarifi toplulukla paylaşıyorum: ")
            append(input.recipeTitle)
            if (input.ingredients.isNotEmpty()) {
                append("\n\nMalzemeler: ")
                append(input.ingredients.take(MAX_PREVIEW_INGREDIENTS).joinToString(", ") { ingredient ->
                    RecipeValueResolver.ingredientNameForCopy(ingredient)
                })
            }
        }
    }

    fun sourceTagForRecipe(sourceType: String): String {
        return if (sourceType == RecipeEntity.SOURCE_TYPE_SEED) {
            RECIPE_SOURCE_TAG_APP
        } else {
            RECIPE_SOURCE_TAG_COMMUNITY
        }
    }

    fun servingText(servingDisplay: String, servings: Int): String {
        return servingDisplay.ifBlank { "$servings kişilik" }
    }

    fun tagsForRecipe(mainCategoryKey: String): List<String> {
        return listOf("Tarif", mainCategoryKey)
    }

    private const val MAX_PREVIEW_INGREDIENTS = 8
    private const val RECIPE_SOURCE_TAG_APP = "app_recipe"
    private const val RECIPE_SOURCE_TAG_COMMUNITY = "community_recipe"

}
