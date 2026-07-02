package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity

internal class AddMealFavoritesAction(
    private val getFavoriteFoods: suspend (limit: Int) -> List<FoodEntity>,
    private val getFavoriteFoodIds: suspend (limit: Int) -> List<String>,
    private val toggleFavoriteFood: suspend (foodId: String) -> Boolean,
    private val stateResolver: AddMealFavoriteStateResolver = AddMealFavoriteStateResolver()
) {

    suspend fun loadFavorites(limit: Int = DEFAULT_FAVORITES_LIMIT): AddMealFavoritesResult {
        val foods = getFavoriteFoods(limit)
        return AddMealFavoritesResult(
            foods = foods,
            shouldShowEmptyFeedback = stateResolver.shouldShowEmptyFavoriteFeedback(foods)
        )
    }

    suspend fun loadFavoriteIds(limit: Int = DEFAULT_FAVORITE_IDS_LIMIT): Set<String> {
        return stateResolver.favoriteIdSet(getFavoriteFoodIds(limit))
    }

    suspend fun toggle(foodId: String): AddMealFavoriteToggleResult {
        val isFavorite = toggleFavoriteFood(foodId)
        return AddMealFavoriteToggleResult(
            feedback = stateResolver.toggleFeedback(isFavorite),
            shouldRefreshSearch = stateResolver.shouldRefreshSearchAfterToggle()
        )
    }

    private companion object {
        private const val DEFAULT_FAVORITES_LIMIT = 50
        private const val DEFAULT_FAVORITE_IDS_LIMIT = 200
    }
}

internal data class AddMealFavoritesResult(
    val foods: List<FoodEntity>,
    val shouldShowEmptyFeedback: Boolean
)

internal data class AddMealFavoriteToggleResult(
    val feedback: AddMealFavoriteToggleFeedback,
    val shouldRefreshSearch: Boolean
)
