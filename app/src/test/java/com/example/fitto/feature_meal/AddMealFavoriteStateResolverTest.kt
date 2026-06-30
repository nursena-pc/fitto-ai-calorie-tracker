package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealFavoriteStateResolverTest {

    private val resolver = AddMealFavoriteStateResolver()

    @Test
    fun `favoriteIdSet converts favorite ids to set`() {
        val result = resolver.favoriteIdSet(listOf("a", "b", "a"))

        assertEquals(setOf("a", "b"), result)
    }

    @Test
    fun `shouldShowEmptyFavoriteFeedback returns true when favorites are empty`() {
        assertTrue(resolver.shouldShowEmptyFavoriteFeedback(emptyList()))
    }

    @Test
    fun `shouldShowEmptyFavoriteFeedback returns false when favorites exist`() {
        assertFalse(
            resolver.shouldShowEmptyFavoriteFeedback(
                listOf(food(id = "banana"))
            )
        )
    }

    @Test
    fun `toggleFeedback returns added when food becomes favorite`() {
        assertEquals(
            AddMealFavoriteToggleFeedback.ADDED,
            resolver.toggleFeedback(isFavoriteAfterToggle = true)
        )
    }

    @Test
    fun `toggleFeedback returns removed when food is removed from favorites`() {
        assertEquals(
            AddMealFavoriteToggleFeedback.REMOVED,
            resolver.toggleFeedback(isFavoriteAfterToggle = false)
        )
    }

    @Test
    fun `shouldRefreshSearchAfterToggle keeps current refresh behavior`() {
        assertTrue(resolver.shouldRefreshSearchAfterToggle())
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
