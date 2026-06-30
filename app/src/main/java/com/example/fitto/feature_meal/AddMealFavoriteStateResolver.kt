package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity

class AddMealFavoriteStateResolver {

    fun favoriteIdSet(favoriteIds: List<String>): Set<String> {
        return favoriteIds.toSet()
    }

    fun shouldShowEmptyFavoriteFeedback(favoriteFoods: List<FoodEntity>): Boolean {
        return favoriteFoods.isEmpty()
    }

    fun toggleFeedback(isFavoriteAfterToggle: Boolean): AddMealFavoriteToggleFeedback {
        return if (isFavoriteAfterToggle) {
            AddMealFavoriteToggleFeedback.ADDED
        } else {
            AddMealFavoriteToggleFeedback.REMOVED
        }
    }

    fun shouldRefreshSearchAfterToggle(): Boolean {
        return true
    }
}

enum class AddMealFavoriteToggleFeedback {
    ADDED,
    REMOVED
}
