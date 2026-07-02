package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealLoadMoreActionTest {

    @Test
    fun `full page merges foods and keeps paging enabled`() = runBlocking {
        val existingFood = food(id = "banana", name = "Local banana")
        val duplicateFood = food(id = "banana", name = "Online banana")
        val newFood = food(id = "apple")
        val action = action(
            returnedFoods = listOf(duplicateFood, newFood)
        )

        val result = action.load(
            request(
                currentFoods = listOf(existingFood),
                currentPage = 2,
                pageSize = 2
            )
        ) as AddMealLoadMoreActionResult.Loaded

        assertEquals(listOf("banana", "apple"), result.foods.map { it.id })
        assertSame(existingFood, result.foods.first())
        assertEquals(3, result.nextPageNumber)
        assertTrue(result.canLoadMore)
    }

    @Test
    fun `partial page advances page and disables further paging`() = runBlocking {
        val action = action(returnedFoods = listOf(food(id = "pear")))

        val result = action.load(
            request(
                currentPage = 4,
                pageSize = 8
            )
        ) as AddMealLoadMoreActionResult.Loaded

        assertEquals(listOf("pear"), result.foods.map { it.id })
        assertEquals(5, result.nextPageNumber)
        assertFalse(result.canLoadMore)
    }

    @Test
    fun `empty page returns no more foods without advancing page`() = runBlocking {
        val result = action().load(
            request(
                currentPage = 3,
                pageSize = 8
            )
        )

        assertEquals(
            AddMealLoadMoreActionResult.NoMoreFoods(canLoadMore = false),
            result
        )
    }

    @Test
    fun `online search failure preserves existing no more foods behavior`() = runBlocking {
        val action = AddMealLoadMoreAction { _, _, _, _ ->
            error("network unavailable")
        }

        val result = action.load(request())

        assertEquals(
            AddMealLoadMoreActionResult.NoMoreFoods(canLoadMore = false),
            result
        )
    }

    @Test
    fun `request values are passed to online search`() = runBlocking {
        var capturedQuery: String? = null
        var capturedMealType: String? = null
        var capturedLimit: Int? = null
        var capturedPage: Int? = null
        val action = AddMealLoadMoreAction { query, mealType, limit, pageNumber ->
            capturedQuery = query
            capturedMealType = mealType
            capturedLimit = limit
            capturedPage = pageNumber
            listOf(food(id = "oat"))
        }

        action.load(
            request(
                query = "oat meal",
                mealType = "dinner",
                currentPage = 6,
                pageSize = 12
            )
        )

        assertEquals("oat meal", capturedQuery)
        assertEquals("dinner", capturedMealType)
        assertEquals(12, capturedLimit)
        assertEquals(6, capturedPage)
    }

    private fun action(
        returnedFoods: List<FoodEntity> = emptyList()
    ): AddMealLoadMoreAction {
        return AddMealLoadMoreAction { _, _, _, _ -> returnedFoods }
    }

    private fun request(
        query: String = "banana",
        mealType: String = "breakfast",
        currentFoods: List<FoodEntity> = emptyList(),
        currentPage: Int = 2,
        pageSize: Int = 8
    ): AddMealLoadMoreRequest {
        return AddMealLoadMoreRequest(
            query = query,
            mealType = mealType,
            currentFoods = currentFoods,
            currentPage = currentPage,
            pageSize = pageSize
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
