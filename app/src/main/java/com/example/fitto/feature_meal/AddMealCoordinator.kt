package com.example.fitto.feature_meal

import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AddMealCoordinator(
    dataSource: AddMealDataSource,
    presentationResolver: AddMealFoodPresentationResolver = AddMealFoodPresentationResolver(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val catalogInitializer = AddMealFoodCatalogInitializer(
        importFoodSeed = dataSource::initializeFoodCatalog
    )
    private val foodSearchAction = AddMealFoodSearchAction(
        getRecentFoods = dataSource::getRecentFoods,
        searchLocalFoods = dataSource::searchLocalFoods,
        searchOnlineFoods = dataSource::searchOnlineFoods
    )
    private val loadMoreAction = AddMealLoadMoreAction(
        searchOnlineFoods = dataSource::searchOnlineFoods
    )
    private val saveAction = AddMealSaveAction(
        saveMealEntry = dataSource::addManualMealEntry
    )
    private val customFoodAction = AddMealCustomFoodAction(
        createFood = dataSource::createCustomFood
    )
    private val detectedFoodsAction = AddMealDetectedFoodsAction(
        getFoodById = dataSource::getFoodById,
        presentationResolver = presentationResolver
    )
    private val favoritesAction = AddMealFavoritesAction(
        getFavoriteFoods = dataSource::getFavoriteFoods,
        getFavoriteFoodIds = dataSource::getFavoriteFoodIds,
        toggleFavoriteFood = dataSource::toggleFavoriteFood
    )

    suspend fun search(request: AddMealFoodSearchRequest): AddMealFoodSearchResult {
        return withContext(ioDispatcher) {
            catalogInitializer.ensureInitialized()
            foodSearchAction.search(request)
        }
    }

    suspend fun loadMore(request: AddMealLoadMoreRequest): AddMealLoadMoreActionResult {
        return withContext(ioDispatcher) {
            loadMoreAction.load(request)
        }
    }

    suspend fun createCustomFood(request: AddMealCustomFoodRequest): AddMealCustomFoodResult {
        return withContext(ioDispatcher) {
            customFoodAction.create(request)
        }
    }

    suspend fun loadDetectedFoods(
        request: AddMealDetectedFoodsRequest
    ): AddMealDetectedFoodsResult {
        return withContext(ioDispatcher) {
            detectedFoodsAction.load(request)
        }
    }

    suspend fun loadFavorites(): AddMealFavoritesResult {
        return withContext(ioDispatcher) {
            favoritesAction.loadFavorites()
        }
    }

    suspend fun loadFavoriteIds(): Set<String> {
        return withContext(ioDispatcher) {
            favoritesAction.loadFavoriteIds()
        }
    }

    suspend fun toggleFavorite(foodId: String): AddMealFavoriteToggleResult {
        return withContext(ioDispatcher) {
            favoritesAction.toggle(foodId)
        }
    }

    fun canSave(selectedFoods: List<SelectedFoodUiModel>): Boolean {
        return saveAction.canSave(selectedFoods)
    }

    suspend fun save(request: AddMealSaveRequest): AddMealSaveResult {
        return withContext(ioDispatcher) {
            saveAction.save(request)
        }
    }
}
