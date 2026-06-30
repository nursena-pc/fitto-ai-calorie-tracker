package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity

enum class AddMealFoodSection {
    RECENT,
    SUGGESTED,
    RESULTS
}

enum class AddMealEmptySearchFeedback {
    NONE,
    NO_ONLINE_FOOD,
    PRESS_SEARCH_FOR_MORE_RESULTS
}

class AddMealSearchStateResolver {

    fun normalizeQuery(query: String): String {
        return query.trim()
    }

    fun shouldSearchOnline(
        normalizedQuery: String,
        allowOnlineSearch: Boolean,
        minQueryLength: Int = MIN_ONLINE_QUERY_LENGTH
    ): Boolean {
        return allowOnlineSearch && normalizedQuery.length >= minQueryLength
    }

    fun shouldShowSearchingTitle(normalizedQuery: String): Boolean {
        return normalizedQuery.length >= MIN_ONLINE_QUERY_LENGTH
    }

    fun mergeFoods(
        first: List<FoodEntity>,
        second: List<FoodEntity>
    ): List<FoodEntity> {
        return buildList {
            addAll(first)
            addAll(second)
        }.distinctBy { food -> food.id }
    }

    fun canLoadMore(fetchedCount: Int, pageSize: Int): Boolean {
        return fetchedCount >= pageSize
    }

    fun canStartLoadMore(
        query: String,
        canLoadMore: Boolean,
        isLoadingMore: Boolean
    ): Boolean {
        return query.trim().isNotBlank() && canLoadMore && !isLoadingMore
    }

    fun nextPageAfterLoad(currentPage: Int): Int {
        return currentPage + 1
    }

    fun shouldResetOnlinePaging(allowOnlineSearch: Boolean): Boolean {
        return !allowOnlineSearch
    }

    fun sectionFor(
        showingRecentFoods: Boolean,
        showingSuggestedFoods: Boolean,
        showingSearchResults: Boolean
    ): AddMealFoodSection {
        return when {
            showingRecentFoods -> AddMealFoodSection.RECENT
            showingSuggestedFoods -> AddMealFoodSection.SUGGESTED
            showingSearchResults -> AddMealFoodSection.RESULTS
            else -> AddMealFoodSection.SUGGESTED
        }
    }

    fun emptyFeedback(
        foods: List<FoodEntity>,
        normalizedQuery: String,
        onlineSearchAttempted: Boolean,
        allowOnlineSearch: Boolean
    ): AddMealEmptySearchFeedback {
        return when {
            foods.isNotEmpty() -> AddMealEmptySearchFeedback.NONE
            onlineSearchAttempted -> AddMealEmptySearchFeedback.NO_ONLINE_FOOD
            normalizedQuery.length >= MIN_ONLINE_QUERY_LENGTH && !allowOnlineSearch -> {
                AddMealEmptySearchFeedback.PRESS_SEARCH_FOR_MORE_RESULTS
            }
            else -> AddMealEmptySearchFeedback.NONE
        }
    }

    private companion object {
        private const val MIN_ONLINE_QUERY_LENGTH = 3
    }
}
