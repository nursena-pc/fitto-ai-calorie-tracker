package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity

internal class AddMealLoadMoreAction(
    private val stateResolver: AddMealSearchStateResolver = AddMealSearchStateResolver(),
    private val searchOnlineFoods: suspend (
        query: String,
        mealType: String,
        limit: Int,
        pageNumber: Int
    ) -> List<FoodEntity>
) {

    suspend fun load(request: AddMealLoadMoreRequest): AddMealLoadMoreActionResult {
        val newFoods = runCatching {
            searchOnlineFoods(
                request.query,
                request.mealType,
                request.pageSize,
                request.currentPage
            )
        }.getOrElse {
            emptyList()
        }

        val pagingResult = stateResolver.loadMoreResult(
            currentPage = request.currentPage,
            fetchedCount = newFoods.size,
            pageSize = request.pageSize
        )

        if (stateResolver.shouldShowNoMoreFoodsFeedback(pagingResult)) {
            return AddMealLoadMoreActionResult.NoMoreFoods(
                canLoadMore = pagingResult.canLoadMore
            )
        }

        return AddMealLoadMoreActionResult.Loaded(
            foods = stateResolver.mergeFoods(request.currentFoods, newFoods),
            nextPageNumber = pagingResult.nextPageNumber,
            canLoadMore = pagingResult.canLoadMore
        )
    }
}

internal data class AddMealLoadMoreRequest(
    val query: String,
    val mealType: String,
    val currentFoods: List<FoodEntity>,
    val currentPage: Int,
    val pageSize: Int
)

internal sealed interface AddMealLoadMoreActionResult {
    data class Loaded(
        val foods: List<FoodEntity>,
        val nextPageNumber: Int,
        val canLoadMore: Boolean
    ) : AddMealLoadMoreActionResult

    data class NoMoreFoods(
        val canLoadMore: Boolean
    ) : AddMealLoadMoreActionResult
}
