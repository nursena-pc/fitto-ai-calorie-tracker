package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity

internal class AddMealFoodSearchAction(
    private val getRecentFoods: suspend (limit: Int) -> List<FoodEntity>,
    private val searchLocalFoods: suspend (
        query: String,
        mealType: String,
        limit: Int
    ) -> List<FoodEntity>,
    private val searchOnlineFoods: suspend (
        query: String,
        mealType: String,
        limit: Int,
        pageNumber: Int
    ) -> List<FoodEntity>,
    private val stateResolver: AddMealSearchStateResolver = AddMealSearchStateResolver()
) {

    suspend fun search(request: AddMealFoodSearchRequest): AddMealFoodSearchResult {
        return if (request.normalizedQuery.isBlank()) {
            loadInitialFoods(request)
        } else {
            searchByQuery(request)
        }
    }

    private suspend fun loadInitialFoods(
        request: AddMealFoodSearchRequest
    ): AddMealFoodSearchResult {
        val recentFoods = getRecentFoods(request.recentFoodsLimit)
        val showingRecentFoods = recentFoods.isNotEmpty()
        val foods = if (showingRecentFoods) {
            recentFoods
        } else {
            searchLocalFoods(
                "",
                request.mealType,
                request.localFoodsLimit
            )
        }

        return buildResult(
            request = request,
            foods = foods,
            showingRecentFoods = showingRecentFoods,
            showingSuggestedFoods = !showingRecentFoods,
            showingSearchResults = false,
            onlineSearchAttempted = false,
            pagingState = pagingStateWithoutOnlineSearch(request.allowOnlineSearch)
        )
    }

    private suspend fun searchByQuery(
        request: AddMealFoodSearchRequest
    ): AddMealFoodSearchResult {
        val localFoods = searchLocalFoods(
            request.normalizedQuery,
            request.mealType,
            request.localFoodsLimit
        )
        val shouldSearchOnline = stateResolver.shouldSearchOnline(
            normalizedQuery = request.normalizedQuery,
            allowOnlineSearch = request.allowOnlineSearch
        )

        if (!shouldSearchOnline) {
            return buildResult(
                request = request,
                foods = localFoods,
                showingRecentFoods = false,
                showingSuggestedFoods = false,
                showingSearchResults = localFoods.isNotEmpty(),
                onlineSearchAttempted = false,
                pagingState = pagingStateWithoutOnlineSearch(request.allowOnlineSearch)
            )
        }

        val onlineFoods = searchOnlineFoods(
            request.normalizedQuery,
            request.mealType,
            request.onlinePageSize,
            FIRST_ONLINE_PAGE
        )

        return buildResult(
            request = request,
            foods = stateResolver.mergeFoods(localFoods, onlineFoods),
            showingRecentFoods = false,
            showingSuggestedFoods = false,
            showingSearchResults = true,
            onlineSearchAttempted = true,
            pagingState = AddMealOnlinePagingState.Active(
                query = request.normalizedQuery,
                nextPageNumber = FIRST_ONLINE_PAGE + 1,
                canLoadMore = stateResolver.canLoadMore(
                    fetchedCount = onlineFoods.size,
                    pageSize = request.onlinePageSize
                )
            )
        )
    }

    private fun buildResult(
        request: AddMealFoodSearchRequest,
        foods: List<FoodEntity>,
        showingRecentFoods: Boolean,
        showingSuggestedFoods: Boolean,
        showingSearchResults: Boolean,
        onlineSearchAttempted: Boolean,
        pagingState: AddMealOnlinePagingState
    ): AddMealFoodSearchResult {
        return AddMealFoodSearchResult(
            foods = foods,
            section = stateResolver.sectionFor(
                showingRecentFoods = showingRecentFoods,
                showingSuggestedFoods = showingSuggestedFoods,
                showingSearchResults = showingSearchResults
            ),
            emptyFeedback = stateResolver.emptyFeedback(
                foods = foods,
                normalizedQuery = request.normalizedQuery,
                onlineSearchAttempted = onlineSearchAttempted,
                allowOnlineSearch = request.allowOnlineSearch
            ),
            pagingState = pagingState
        )
    }

    private fun pagingStateWithoutOnlineSearch(
        allowOnlineSearch: Boolean
    ): AddMealOnlinePagingState {
        return if (stateResolver.shouldResetOnlinePaging(allowOnlineSearch)) {
            AddMealOnlinePagingState.Reset
        } else {
            AddMealOnlinePagingState.Keep
        }
    }

    private companion object {
        private const val FIRST_ONLINE_PAGE = 1
    }
}

internal data class AddMealFoodSearchRequest(
    val normalizedQuery: String,
    val mealType: String,
    val allowOnlineSearch: Boolean,
    val recentFoodsLimit: Int = 12,
    val localFoodsLimit: Int = 40,
    val onlinePageSize: Int = 8
)

internal data class AddMealFoodSearchResult(
    val foods: List<FoodEntity>,
    val section: AddMealFoodSection,
    val emptyFeedback: AddMealEmptySearchFeedback,
    val pagingState: AddMealOnlinePagingState
)

internal sealed interface AddMealOnlinePagingState {
    data object Keep : AddMealOnlinePagingState
    data object Reset : AddMealOnlinePagingState

    data class Active(
        val query: String,
        val nextPageNumber: Int,
        val canLoadMore: Boolean
    ) : AddMealOnlinePagingState
}
