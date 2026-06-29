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
            ?.map { ingredient -> ingredient.trim() }
            ?.filter { ingredient -> ingredient.isNotBlank() }
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
                    ingredient.toIngredientNameForShare()
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

    private fun String.toIngredientNameForShare(): String {
        val tokens = trim().split(Regex("\\s+")).filter { token -> token.isNotBlank() }
        if (tokens.isEmpty()) return trim()

        var nameStart = 0
        if (tokens.first().any { char -> char.isDigit() } || "/" in tokens.first()) {
            nameStart = 1
            while (nameStart < tokens.size && tokens[nameStart].lowercase(TURKISH_LOCALE) in INGREDIENT_AMOUNT_WORDS && nameStart < 5) {
                nameStart++
            }
        }

        return tokens.drop(nameStart).joinToString(" ").ifBlank { trim() }
    }

    private const val MAX_PREVIEW_INGREDIENTS = 8
    private const val RECIPE_SOURCE_TAG_APP = "app_recipe"
    private const val RECIPE_SOURCE_TAG_COMMUNITY = "community_recipe"

    private val TURKISH_LOCALE = java.util.Locale("tr", "TR")

    private val INGREDIENT_AMOUNT_WORDS = setOf(
        "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
        "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
        "avuç", "paket", "kase", "diş"
    )
}
