package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface FridgeRecipeSuggestionDataSource {
    fun recipes(): List<RecipeUiModel>
}

class FridgeInventoryCoordinator(
    private val dataSource: FridgeRecipeSuggestionDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun findRecipeSuggestions(
        ingredients: Collection<FridgeIngredient>,
        limit: Int = DEFAULT_SUGGESTION_LIMIT
    ): List<RecipeUiModel> {
        if (limit <= 0 || ingredients.isEmpty()) return emptyList()
        val fridgeNames = ingredients
            .map { it.name.lowercase(TURKISH_LOCALE) }
            .filter(String::isNotBlank)
        if (fridgeNames.isEmpty()) return emptyList()

        return withContext(ioDispatcher) {
            dataSource.recipes().filter { recipe ->
                recipe.ingredientNames.any { ingredientName ->
                    val normalized = ingredientName.lowercase(TURKISH_LOCALE)
                    fridgeNames.any { fridgeName ->
                        normalized.contains(fridgeName) || fridgeName.contains(normalized)
                    }
                }
            }.take(limit)
        }
    }

    private companion object {
        private const val DEFAULT_SUGGESTION_LIMIT = 10
        private val TURKISH_LOCALE = Locale("tr", "TR")
    }
}
