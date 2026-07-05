package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

/**
 * Owns mutable state for the add-meal screen.
 *
 * Food content generations keep delayed search and load-more results from replacing a newer
 * screen request. Selection and save state live alongside paging state so [AddMealFragment]
 * only renders snapshots and forwards user events.
 */
internal class AddMealScreenStateStore(
    private val selectionStateResolver: AddMealSelectionStateResolver =
        AddMealSelectionStateResolver(),
    private val searchStateResolver: AddMealSearchStateResolver = AddMealSearchStateResolver()
) {
    private var state = AddMealScreenState()

    fun current(): AddMealScreenState = state

    fun beginFoodContentLoad(): AddMealFoodContentToken {
        val nextGeneration = state.foodContentGeneration + 1L
        state = state.copy(
            foodContentGeneration = nextGeneration,
            onlinePaging = state.onlinePaging.copy(isLoadingMore = false)
        )
        return AddMealFoodContentToken(nextGeneration)
    }

    fun applySearchResult(
        token: AddMealFoodContentToken,
        result: AddMealFoodSearchResult
    ): Boolean {
        if (!isCurrent(token)) return false

        state = state.copy(
            currentFoodResults = result.foods,
            onlinePaging = resolvePagingState(
                current = state.onlinePaging,
                update = result.pagingState
            )
        )
        return true
    }

    fun applyFavoriteFoods(
        token: AddMealFoodContentToken,
        foods: List<FoodEntity>
    ): Boolean {
        if (!isCurrent(token)) return false

        state = state.copy(
            currentFoodResults = foods,
            onlinePaging = AddMealOnlinePagingScreenState()
        )
        return true
    }

    fun beginLoadMore(): AddMealLoadMoreToken? {
        val paging = state.onlinePaging
        if (!searchStateResolver.canStartLoadMore(
                query = paging.query,
                canLoadMore = paging.canLoadMore,
                isLoadingMore = paging.isLoadingMore
            )
        ) {
            return null
        }

        state = state.copy(
            onlinePaging = paging.copy(isLoadingMore = true)
        )
        return AddMealLoadMoreToken(
            foodContentGeneration = state.foodContentGeneration,
            query = paging.query.trim(),
            currentFoods = state.currentFoodResults,
            currentPage = paging.nextPageNumber
        )
    }

    fun applyLoadMoreResult(
        token: AddMealLoadMoreToken,
        result: AddMealLoadMoreActionResult
    ): Boolean {
        if (!isCurrent(token)) return false

        val paging = state.onlinePaging
        state = when (result) {
            is AddMealLoadMoreActionResult.NoMoreFoods -> {
                state.copy(
                    onlinePaging = paging.copy(
                        canLoadMore = result.canLoadMore,
                        isLoadingMore = false
                    )
                )
            }
            is AddMealLoadMoreActionResult.Loaded -> {
                state.copy(
                    currentFoodResults = result.foods,
                    onlinePaging = paging.copy(
                        nextPageNumber = result.nextPageNumber,
                        canLoadMore = result.canLoadMore,
                        isLoadingMore = false
                    )
                )
            }
        }
        return true
    }

    fun loadMoreUiState(): AddMealLoadMoreUiState {
        return searchStateResolver.loadMoreUiState(
            canLoadMore = state.onlinePaging.canLoadMore,
            hasCurrentResults = state.currentFoodResults.isNotEmpty(),
            isLoadingMore = state.onlinePaging.isLoadingMore
        )
    }

    fun addSelectedFood(selectedFood: SelectedFoodUiModel) {
        state = state.copy(
            selectedFoods = selectionStateResolver.addSelectedFood(
                currentSelection = state.selectedFoods,
                selectedFood = selectedFood
            )
        )
    }

    fun addDetectedFoods(detectedFoods: List<SelectedFoodUiModel>) {
        state = state.copy(
            selectedFoods = selectionStateResolver.addDetectedFoods(
                currentSelection = state.selectedFoods,
                detectedFoods = detectedFoods
            )
        )
    }

    fun removeSelectedFood(food: SelectedFoodUiModel) {
        state = state.copy(
            selectedFoods = selectionStateResolver.removeSelectedFood(
                currentSelection = state.selectedFoods,
                food = food
            )
        )
    }

    fun beginSave(): AddMealSaveSnapshot? {
        if (state.isSaving || !selectionStateResolver.canSave(state.selectedFoods)) return null

        state = state.copy(isSaving = true)
        return AddMealSaveSnapshot(selectedFoods = state.selectedFoods)
    }

    fun finishSave() {
        state = state.copy(isSaving = false)
    }

    fun selectionUiState(): AddMealSelectionUiState {
        val selectionState = selectionStateResolver.resolveUiState(state.selectedFoods)
        return if (state.isSaving) {
            selectionState.copy(
                saveButtonEnabled = false,
                saveButtonAlpha = SAVING_BUTTON_ALPHA
            )
        } else {
            selectionState
        }
    }

    private fun isCurrent(token: AddMealFoodContentToken): Boolean {
        return token.generation == state.foodContentGeneration
    }

    private fun isCurrent(token: AddMealLoadMoreToken): Boolean {
        return token.foodContentGeneration == state.foodContentGeneration &&
            token.query == state.onlinePaging.query.trim()
    }

    private fun resolvePagingState(
        current: AddMealOnlinePagingScreenState,
        update: AddMealOnlinePagingState
    ): AddMealOnlinePagingScreenState {
        return when (update) {
            AddMealOnlinePagingState.Keep -> current.copy(isLoadingMore = false)
            AddMealOnlinePagingState.Reset -> AddMealOnlinePagingScreenState()
            is AddMealOnlinePagingState.Active -> AddMealOnlinePagingScreenState(
                query = update.query,
                nextPageNumber = update.nextPageNumber,
                canLoadMore = update.canLoadMore
            )
        }
    }

    private companion object {
        private const val SAVING_BUTTON_ALPHA = 0.65f
    }
}

internal data class AddMealScreenState(
    val currentFoodResults: List<FoodEntity> = emptyList(),
    val selectedFoods: List<SelectedFoodUiModel> = emptyList(),
    val onlinePaging: AddMealOnlinePagingScreenState = AddMealOnlinePagingScreenState(),
    val isSaving: Boolean = false,
    val foodContentGeneration: Long = 0L
)

internal data class AddMealOnlinePagingScreenState(
    val query: String = "",
    val nextPageNumber: Int = 2,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false
)

internal data class AddMealFoodContentToken(
    val generation: Long
)

internal data class AddMealLoadMoreToken(
    val foodContentGeneration: Long,
    val query: String,
    val currentFoods: List<FoodEntity>,
    val currentPage: Int
)

internal data class AddMealSaveSnapshot(
    val selectedFoods: List<SelectedFoodUiModel>
)
