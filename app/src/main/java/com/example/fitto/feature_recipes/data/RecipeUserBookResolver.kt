package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import java.util.Locale

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
        val cleanIngredients = cleanLines(input.draft.ingredients)
        val cleanSteps = cleanLines(input.draft.steps)

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
            durationMinutes = representativeMinutes(input.draft.durationLabel),
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
            ?.let(::cleanLines)
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
        return "user_${ownerUid}_${now}".toSafeRecipeId()
    }

    fun buildCopiedRecipeId(ownerUid: String, sourceId: String, now: Long): String {
        return "copied_${ownerUid}_${sourceId}_${now}".toSafeRecipeId()
    }

    fun cleanTitle(title: String): String {
        return title.trim().ifBlank { DEFAULT_USER_RECIPE_TITLE }
    }

    fun cleanLines(lines: List<String>): List<String> {
        return lines.map { line -> line.trim() }.filter { line -> line.isNotBlank() }
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
            cleanIngredients.map { ingredientLine -> ingredientLine.toIngredientNameForCopy() }
        }
    }

    fun representativeMinutes(durationLabel: String): Int {
        return when (durationLabel) {
            "15 dk altı" -> 10
            "15-30 dk" -> 25
            "30-60 dk" -> 45
            "60 dk üzeri" -> 75
            else -> 30
        }
    }

    private fun String.toSafeRecipeId(): String {
        return replace("/", "_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    private fun String.toIngredientNameForCopy(): String {
        val tokens = trim().split(Regex("\\s+")).filter { token -> token.isNotBlank() }
        if (tokens.isEmpty()) return trim()

        var nameStart = 0
        if (tokens.first().any { char -> char.isDigit() } || "/" in tokens.first()) {
            nameStart = 1
            while (
                nameStart < tokens.size &&
                tokens[nameStart].lowercase(TURKISH_LOCALE) in INGREDIENT_AMOUNT_WORDS &&
                nameStart < MAX_AMOUNT_TOKEN_SCAN
            ) {
                nameStart++
            }
        }

        return tokens.drop(nameStart).joinToString(" ").ifBlank { trim() }
    }

    private const val DEFAULT_USER_RECIPE_TITLE = "Yeni Tarif"
    private const val DEFAULT_USER_RECIPE_DESCRIPTION = "Kullanıcı tarafından eklenen tarif"
    private const val MAX_AMOUNT_TOKEN_SCAN = 5

    private val TURKISH_LOCALE = Locale("tr", "TR")
    private val INGREDIENT_AMOUNT_WORDS = setOf(
        "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
        "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
        "avuç", "paket", "kase", "diş"
    )
}
