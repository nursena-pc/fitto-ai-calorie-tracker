package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealFavoritesActionTest {

    @Test
    fun `load favorites returns foods and forwards requested limit`() = runBlocking {
        var capturedLimit = 0
        val favorite = food(id = "banana")
        val action = action(
            getFavoriteFoods = { limit ->
                capturedLimit = limit
                listOf(favorite)
            }
        )

        val result = action.loadFavorites(limit = 24)

        assertEquals(24, capturedLimit)
        assertEquals(listOf(favorite), result.foods)
        assertFalse(result.shouldShowEmptyFeedback)
    }

    @Test
    fun `empty favorites request empty feedback`() = runBlocking {
        val result = action().loadFavorites()

        assertTrue(result.foods.isEmpty())
        assertTrue(result.shouldShowEmptyFeedback)
    }

    @Test
    fun `favorite ids are deduplicated and requested with limit`() = runBlocking {
        var capturedLimit = 0
        val action = action(
            getFavoriteFoodIds = { limit ->
                capturedLimit = limit
                listOf("banana", "apple", "banana")
            }
        )

        val result = action.loadFavoriteIds(limit = 75)

        assertEquals(75, capturedLimit)
        assertEquals(setOf("banana", "apple"), result)
    }

    @Test
    fun `toggle returns added feedback and requests search refresh`() = runBlocking {
        var capturedFoodId: String? = null
        val action = action(
            toggleFavoriteFood = { foodId ->
                capturedFoodId = foodId
                true
            }
        )

        val result = action.toggle("banana")

        assertEquals("banana", capturedFoodId)
        assertEquals(AddMealFavoriteToggleFeedback.ADDED, result.feedback)
        assertTrue(result.shouldRefreshSearch)
    }

    @Test
    fun `toggle returns removed feedback when repository removes favorite`() = runBlocking {
        val result = action(
            toggleFavoriteFood = { false }
        ).toggle("banana")

        assertEquals(AddMealFavoriteToggleFeedback.REMOVED, result.feedback)
        assertTrue(result.shouldRefreshSearch)
    }

    private fun action(
        getFavoriteFoods: suspend (Int) -> List<FoodEntity> = { emptyList() },
        getFavoriteFoodIds: suspend (Int) -> List<String> = { emptyList() },
        toggleFavoriteFood: suspend (String) -> Boolean = { false }
    ): AddMealFavoritesAction {
        return AddMealFavoritesAction(
            getFavoriteFoods = getFavoriteFoods,
            getFavoriteFoodIds = getFavoriteFoodIds,
            toggleFavoriteFood = toggleFavoriteFood
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
            caloriesPer100g = 50.0,
            proteinPer100g = 1.0,
            carbsPer100g = 2.0,
            fatPer100g = 3.0,
            fiberPer100g = 0.0,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = "breakfast"
        )
    }
}
