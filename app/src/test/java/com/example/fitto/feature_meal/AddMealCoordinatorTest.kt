package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealCoordinatorTest {

    @Test
    fun search_initializesCatalogOnceAndLoadsRecentFoods() = runBlocking {
        val source = FakeAddMealDataSource(recentFoods = listOf(food("recent")))
        val coordinator = coordinator(source)
        val request = AddMealFoodSearchRequest(
            normalizedQuery = "",
            mealType = MealType.BREAKFAST.argumentValue,
            allowOnlineSearch = false
        )

        val first = coordinator.search(request)
        val second = coordinator.search(request)

        assertEquals(1, source.initializeCalls)
        assertEquals(2, source.recentFoodCalls)
        assertEquals(listOf("recent"), first.foods.map { it.id })
        assertEquals(listOf("recent"), second.foods.map { it.id })
    }

    @Test
    fun search_withOnlineRequestForwardsQueryAndMergesResults() = runBlocking {
        val source = FakeAddMealDataSource(
            localFoods = listOf(food("local")),
            onlineFoods = listOf(food("online"))
        )
        val coordinator = coordinator(source)

        val result = coordinator.search(
            AddMealFoodSearchRequest(
                normalizedQuery = "apple",
                mealType = MealType.SNACK.argumentValue,
                allowOnlineSearch = true,
                onlinePageSize = 8
            )
        )

        assertEquals(SearchCall("apple", MealType.SNACK.argumentValue, 40), source.localSearchCall)
        assertEquals(
            OnlineSearchCall("apple", MealType.SNACK.argumentValue, 8, 1),
            source.onlineSearchCall
        )
        assertEquals(listOf("local", "online"), result.foods.map { it.id })
        assertTrue(result.pagingState is AddMealOnlinePagingState.Active)
    }

    @Test
    fun loadMore_forwardsPageAndReturnsMergedFoods() = runBlocking {
        val source = FakeAddMealDataSource(onlineFoods = listOf(food("page-2")))
        val coordinator = coordinator(source)

        val result = coordinator.loadMore(
            AddMealLoadMoreRequest(
                query = "apple",
                mealType = MealType.BREAKFAST.argumentValue,
                currentFoods = listOf(food("page-1")),
                currentPage = 2,
                pageSize = 1
            )
        )

        assertEquals(
            OnlineSearchCall("apple", MealType.BREAKFAST.argumentValue, 1, 2),
            source.onlineSearchCall
        )
        assertTrue(result is AddMealLoadMoreActionResult.Loaded)
        result as AddMealLoadMoreActionResult.Loaded
        assertEquals(listOf("page-1", "page-2"), result.foods.map { it.id })
        assertEquals(3, result.nextPageNumber)
    }

    @Test
    fun createCustomFood_forwardsTypedRequest() = runBlocking {
        val createdFood = food("custom")
        val source = FakeAddMealDataSource(createdFood = createdFood)
        val coordinator = coordinator(source)
        val request = AddMealCustomFoodRequest(
            name = "Custom",
            servingAmount = 75.0,
            unit = "g",
            caloriesPer100g = 120.0,
            proteinPer100g = 4.0,
            carbsPer100g = 20.0,
            fatPer100g = 2.0,
            mealType = MealType.LUNCH.argumentValue
        )

        val result = coordinator.createCustomFood(request)

        assertSame(request, source.customFoodRequest)
        assertEquals(AddMealCustomFoodResult.Created(createdFood), result)
    }

    @Test
    fun loadDetectedFoods_resolvesKnownFoodsThroughSource() = runBlocking {
        val source = FakeAddMealDataSource(
            foodsById = mapOf("food-1" to food("food-1"))
        )
        val coordinator = coordinator(source)

        val result = coordinator.loadDetectedFoods(
            AddMealDetectedFoodsRequest(listOf("food-1", "missing"))
        )

        assertEquals(listOf("food-1", "missing"), source.requestedFoodIds)
        assertTrue(result is AddMealDetectedFoodsResult.Imported)
        result as AddMealDetectedFoodsResult.Imported
        assertEquals(listOf("food-1"), result.detectedFoods.map { it.foodId })
    }

    @Test
    fun favorites_operationsAreRoutedThroughSource() = runBlocking {
        val favorite = food("favorite")
        val source = FakeAddMealDataSource(
            favoriteFoods = listOf(favorite),
            favoriteIds = listOf(favorite.id),
            toggleResult = true
        )
        val coordinator = coordinator(source)

        val favorites = coordinator.loadFavorites()
        val favoriteIds = coordinator.loadFavoriteIds()
        val toggle = coordinator.toggleFavorite(favorite.id)

        assertEquals(listOf(favorite), favorites.foods)
        assertFalse(favorites.shouldShowEmptyFeedback)
        assertEquals(setOf(favorite.id), favoriteIds)
        assertEquals(favorite.id, source.toggledFoodId)
        assertEquals(AddMealFavoriteToggleFeedback.ADDED, toggle.feedback)
    }

    @Test
    fun save_forwardsMealRequestAndReturnsSavedResult() = runBlocking {
        val source = FakeAddMealDataSource()
        val coordinator = coordinator(source)
        val selectedFood = selectedFood()
        val request = AddMealSaveRequest(
            mealType = MealType.DINNER,
            selectedFoods = listOf(selectedFood),
            entryDate = "2026-07-05"
        )

        val result = coordinator.save(request)

        assertTrue(coordinator.canSave(request.selectedFoods))
        assertFalse(coordinator.canSave(emptyList()))
        assertEquals(request, source.savedMealRequest)
        assertEquals(
            AddMealSaveResult.Saved(
                selectedCount = 1,
                mealType = MealType.DINNER,
                entryDate = "2026-07-05"
            ),
            result
        )
    }

    private fun coordinator(source: AddMealDataSource): AddMealCoordinator {
        return AddMealCoordinator(
            dataSource = source,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    private fun food(id: String): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = id,
            nameEn = id,
            category = "test",
            defaultUnit = "g",
            defaultServingAmount = 100.0,
            caloriesPer100g = 100.0,
            proteinPer100g = 10.0,
            carbsPer100g = 15.0,
            fatPer100g = 2.0,
            fiberPer100g = 1.0,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = MealType.BREAKFAST.argumentValue,
            createdAt = 1L,
            updatedAt = 1L
        )
    }

    private fun selectedFood(): SelectedFoodUiModel {
        return SelectedFoodUiModel(
            id = "selected-1",
            foodId = "food-1",
            name = "Food",
            amountLabel = "100 g",
            amountGrams = 100.0,
            calories = 100,
            protein = 10.0,
            carbs = 15.0,
            fat = 2.0,
            emoji = ""
        )
    }

    private data class SearchCall(
        val query: String,
        val mealType: String,
        val limit: Int
    )

    private data class OnlineSearchCall(
        val query: String,
        val mealType: String,
        val limit: Int,
        val pageNumber: Int
    )

    private class FakeAddMealDataSource(
        private val recentFoods: List<FoodEntity> = emptyList(),
        private val localFoods: List<FoodEntity> = emptyList(),
        private val onlineFoods: List<FoodEntity> = emptyList(),
        private val createdFood: FoodEntity = testFood("created"),
        private val foodsById: Map<String, FoodEntity> = emptyMap(),
        private val favoriteFoods: List<FoodEntity> = emptyList(),
        private val favoriteIds: List<String> = emptyList(),
        private val toggleResult: Boolean = false
    ) : AddMealDataSource {
        var initializeCalls: Int = 0
        var recentFoodCalls: Int = 0
        var localSearchCall: SearchCall? = null
        var onlineSearchCall: OnlineSearchCall? = null
        var customFoodRequest: AddMealCustomFoodRequest? = null
        val requestedFoodIds = mutableListOf<String>()
        var toggledFoodId: String? = null
        var savedMealRequest: AddMealSaveRequest? = null

        override suspend fun initializeFoodCatalog() {
            initializeCalls += 1
        }

        override suspend fun getRecentFoods(limit: Int): List<FoodEntity> {
            recentFoodCalls += 1
            return recentFoods
        }

        override suspend fun searchLocalFoods(
            query: String,
            mealType: String,
            limit: Int
        ): List<FoodEntity> {
            localSearchCall = SearchCall(query, mealType, limit)
            return localFoods
        }

        override suspend fun searchOnlineFoods(
            query: String,
            mealType: String,
            limit: Int,
            pageNumber: Int
        ): List<FoodEntity> {
            onlineSearchCall = OnlineSearchCall(query, mealType, limit, pageNumber)
            return onlineFoods
        }

        override suspend fun createCustomFood(request: AddMealCustomFoodRequest): FoodEntity {
            customFoodRequest = request
            return createdFood
        }

        override suspend fun getFoodById(foodId: String): FoodEntity? {
            requestedFoodIds += foodId
            return foodsById[foodId]
        }

        override suspend fun getFavoriteFoods(limit: Int): List<FoodEntity> = favoriteFoods

        override suspend fun getFavoriteFoodIds(limit: Int): List<String> = favoriteIds

        override suspend fun toggleFavoriteFood(foodId: String): Boolean {
            toggledFoodId = foodId
            return toggleResult
        }

        override suspend fun addManualMealEntry(
            mealType: MealType,
            selectedFoods: List<SelectedFoodUiModel>,
            entryDate: String
        ) {
            savedMealRequest = AddMealSaveRequest(mealType, selectedFoods, entryDate)
        }

        private companion object {
            fun testFood(id: String): FoodEntity {
                return FoodEntity(
                    id = id,
                    nameTr = id,
                    nameEn = id,
                    category = "test",
                    defaultUnit = "g",
                    defaultServingAmount = 100.0,
                    caloriesPer100g = 100.0,
                    proteinPer100g = 10.0,
                    carbsPer100g = 15.0,
                    fatPer100g = 2.0,
                    fiberPer100g = 1.0,
                    source = "test",
                    isVerified = true,
                    allergens = "",
                    dietTags = "",
                    mealTypes = MealType.BREAKFAST.argumentValue,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            }
        }
    }
}
