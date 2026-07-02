package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealFoodSearchActionTest {

    @Test
    fun `blank query shows recent foods without loading suggestions`() = runBlocking {
        val recentFood = food(id = "recent")
        var localSearchCalled = false
        val action = action(
            recentFoods = listOf(recentFood),
            onLocalSearch = { _, _, _ ->
                localSearchCalled = true
                emptyList()
            }
        )

        val result = action.search(request(normalizedQuery = ""))

        assertEquals(listOf(recentFood), result.foods)
        assertEquals(AddMealFoodSection.RECENT, result.section)
        assertEquals(AddMealEmptySearchFeedback.NONE, result.emptyFeedback)
        assertEquals(AddMealOnlinePagingState.Reset, result.pagingState)
        assertFalse(localSearchCalled)
    }

    @Test
    fun `blank query falls back to meal suggestions when recent foods are empty`() = runBlocking {
        val suggestedFood = food(id = "suggested")
        var capturedQuery: String? = null
        var capturedMealType: String? = null
        var capturedLimit: Int? = null
        val action = action(
            onLocalSearch = { query, mealType, limit ->
                capturedQuery = query
                capturedMealType = mealType
                capturedLimit = limit
                listOf(suggestedFood)
            }
        )

        val result = action.search(
            request(
                normalizedQuery = "",
                mealType = "lunch",
                localFoodsLimit = 24
            )
        )

        assertEquals(listOf(suggestedFood), result.foods)
        assertEquals(AddMealFoodSection.SUGGESTED, result.section)
        assertEquals("", capturedQuery)
        assertEquals("lunch", capturedMealType)
        assertEquals(24, capturedLimit)
    }

    @Test
    fun `local-only query returns local results and resets online paging`() = runBlocking {
        val localFood = food(id = "local")
        var onlineSearchCalled = false
        val action = action(
            onLocalSearch = { _, _, _ -> listOf(localFood) },
            onOnlineSearch = { _, _, _, _ ->
                onlineSearchCalled = true
                emptyList()
            }
        )

        val result = action.search(
            request(
                normalizedQuery = "banana",
                allowOnlineSearch = false
            )
        )

        assertEquals(listOf(localFood), result.foods)
        assertEquals(AddMealFoodSection.RESULTS, result.section)
        assertEquals(AddMealEmptySearchFeedback.NONE, result.emptyFeedback)
        assertEquals(AddMealOnlinePagingState.Reset, result.pagingState)
        assertFalse(onlineSearchCalled)
    }

    @Test
    fun `empty local-only query result asks user to submit online search`() = runBlocking {
        val action = action()

        val result = action.search(
            request(
                normalizedQuery = "banana",
                allowOnlineSearch = false
            )
        )

        assertTrue(result.foods.isEmpty())
        assertEquals(AddMealFoodSection.SUGGESTED, result.section)
        assertEquals(
            AddMealEmptySearchFeedback.PRESS_SEARCH_FOR_MORE_RESULTS,
            result.emptyFeedback
        )
        assertEquals(AddMealOnlinePagingState.Reset, result.pagingState)
    }

    @Test
    fun `online query merges foods and starts paging from page two`() = runBlocking {
        val localFood = food(id = "banana", name = "Local banana")
        val duplicateOnlineFood = food(id = "banana", name = "Online banana")
        val onlineFood = food(id = "apple", name = "Online apple")
        var capturedOnlinePage: Int? = null
        var capturedOnlineLimit: Int? = null
        val action = action(
            onLocalSearch = { _, _, _ -> listOf(localFood) },
            onOnlineSearch = { _, _, limit, pageNumber ->
                capturedOnlineLimit = limit
                capturedOnlinePage = pageNumber
                listOf(duplicateOnlineFood, onlineFood)
            }
        )

        val result = action.search(
            request(
                normalizedQuery = "banana",
                allowOnlineSearch = true,
                onlinePageSize = 2
            )
        )

        assertEquals(listOf("banana", "apple"), result.foods.map { it.id })
        assertSame(localFood, result.foods.first())
        assertEquals(AddMealFoodSection.RESULTS, result.section)
        assertEquals(AddMealEmptySearchFeedback.NONE, result.emptyFeedback)
        assertEquals(2, capturedOnlineLimit)
        assertEquals(1, capturedOnlinePage)
        assertEquals(
            AddMealOnlinePagingState.Active(
                query = "banana",
                nextPageNumber = 2,
                canLoadMore = true
            ),
            result.pagingState
        )
    }

    @Test
    fun `empty online result reports no food and disables further paging`() = runBlocking {
        val action = action()

        val result = action.search(
            request(
                normalizedQuery = "banana",
                allowOnlineSearch = true
            )
        )

        assertEquals(AddMealEmptySearchFeedback.NO_ONLINE_FOOD, result.emptyFeedback)
        assertEquals(
            AddMealOnlinePagingState.Active(
                query = "banana",
                nextPageNumber = 2,
                canLoadMore = false
            ),
            result.pagingState
        )
    }

    @Test
    fun `short explicitly submitted query keeps existing paging state`() = runBlocking {
        val localFood = food(id = "kiwi")
        var onlineSearchCalled = false
        val action = action(
            onLocalSearch = { _, _, _ -> listOf(localFood) },
            onOnlineSearch = { _, _, _, _ ->
                onlineSearchCalled = true
                emptyList()
            }
        )

        val result = action.search(
            request(
                normalizedQuery = "ki",
                allowOnlineSearch = true
            )
        )

        assertEquals(listOf(localFood), result.foods)
        assertEquals(AddMealOnlinePagingState.Keep, result.pagingState)
        assertFalse(onlineSearchCalled)
    }

    private fun action(
        recentFoods: List<FoodEntity> = emptyList(),
        onLocalSearch: suspend (String, String, Int) -> List<FoodEntity> = { _, _, _ ->
            emptyList()
        },
        onOnlineSearch: suspend (String, String, Int, Int) -> List<FoodEntity> = {
                _, _, _, _ ->
            emptyList()
        }
    ): AddMealFoodSearchAction {
        return AddMealFoodSearchAction(
            getRecentFoods = { recentFoods },
            searchLocalFoods = onLocalSearch,
            searchOnlineFoods = onOnlineSearch
        )
    }

    private fun request(
        normalizedQuery: String,
        mealType: String = "breakfast",
        allowOnlineSearch: Boolean = false,
        recentFoodsLimit: Int = 12,
        localFoodsLimit: Int = 40,
        onlinePageSize: Int = 8
    ): AddMealFoodSearchRequest {
        return AddMealFoodSearchRequest(
            normalizedQuery = normalizedQuery,
            mealType = mealType,
            allowOnlineSearch = allowOnlineSearch,
            recentFoodsLimit = recentFoodsLimit,
            localFoodsLimit = localFoodsLimit,
            onlinePageSize = onlinePageSize
        )
    }

    private fun food(
        id: String,
        name: String = id
    ): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = name,
            nameEn = null,
            category = "test",
            defaultUnit = "g",
            defaultServingAmount = 100.0,
            caloriesPer100g = 100.0,
            proteinPer100g = 10.0,
            carbsPer100g = 15.0,
            fatPer100g = 3.0,
            fiberPer100g = null,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = ""
        )
    }
}
