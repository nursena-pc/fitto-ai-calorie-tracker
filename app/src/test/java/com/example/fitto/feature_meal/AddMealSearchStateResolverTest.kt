package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealSearchStateResolverTest {

    private val resolver = AddMealSearchStateResolver()

    @Test
    fun `normalizes query by trimming surrounding whitespace`() {
        assertEquals("banana", resolver.normalizeQuery("  banana  "))
    }

    @Test
    fun `online search requires explicit permission and at least three chars`() {
        assertFalse(resolver.shouldSearchOnline("ab", allowOnlineSearch = true))
        assertFalse(resolver.shouldSearchOnline("banana", allowOnlineSearch = false))
        assertTrue(resolver.shouldSearchOnline("banana", allowOnlineSearch = true))
    }

    @Test
    fun `merge foods keeps first occurrence when duplicate ids exist`() {
        val first = food(id = "banana", nameTr = "Muz")
        val duplicate = food(id = "banana", nameTr = "Banana Online")
        val second = food(id = "apple", nameTr = "Elma")

        val result = resolver.mergeFoods(
            first = listOf(first),
            second = listOf(duplicate, second)
        )

        assertEquals(listOf("banana", "apple"), result.map { it.id })
        assertEquals("Muz", result.first().nameTr)
    }

    @Test
    fun `load more can start only with nonblank query available page and idle state`() {
        assertFalse(resolver.canStartLoadMore(" ", canLoadMore = true, isLoadingMore = false))
        assertFalse(resolver.canStartLoadMore("oat", canLoadMore = false, isLoadingMore = false))
        assertFalse(resolver.canStartLoadMore("oat", canLoadMore = true, isLoadingMore = true))
        assertTrue(resolver.canStartLoadMore(" oat ", canLoadMore = true, isLoadingMore = false))
    }

    @Test
    fun `can load more when fetched count fills current page`() {
        assertTrue(resolver.canLoadMore(fetchedCount = 8, pageSize = 8))
        assertFalse(resolver.canLoadMore(fetchedCount = 7, pageSize = 8))
    }

    @Test
    fun `section prioritizes recent then suggested then results`() {
        assertEquals(
            AddMealFoodSection.RECENT,
            resolver.sectionFor(
                showingRecentFoods = true,
                showingSuggestedFoods = true,
                showingSearchResults = true
            )
        )
        assertEquals(
            AddMealFoodSection.SUGGESTED,
            resolver.sectionFor(
                showingRecentFoods = false,
                showingSuggestedFoods = true,
                showingSearchResults = true
            )
        )
        assertEquals(
            AddMealFoodSection.RESULTS,
            resolver.sectionFor(
                showingRecentFoods = false,
                showingSuggestedFoods = false,
                showingSearchResults = true
            )
        )
    }

    @Test
    fun `empty feedback preserves online and local search messages`() {
        assertEquals(
            AddMealEmptySearchFeedback.NO_ONLINE_FOOD,
            resolver.emptyFeedback(
                foods = emptyList(),
                normalizedQuery = "oat",
                onlineSearchAttempted = true,
                allowOnlineSearch = true
            )
        )
        assertEquals(
            AddMealEmptySearchFeedback.PRESS_SEARCH_FOR_MORE_RESULTS,
            resolver.emptyFeedback(
                foods = emptyList(),
                normalizedQuery = "oat",
                onlineSearchAttempted = false,
                allowOnlineSearch = false
            )
        )
        assertEquals(
            AddMealEmptySearchFeedback.NONE,
            resolver.emptyFeedback(
                foods = listOf(food()),
                normalizedQuery = "oat",
                onlineSearchAttempted = true,
                allowOnlineSearch = true
            )
        )
    }

    @Test
    fun `online paging resets only after local-only searches`() {
        assertTrue(resolver.shouldResetOnlinePaging(allowOnlineSearch = false))
        assertFalse(resolver.shouldResetOnlinePaging(allowOnlineSearch = true))
    }

    @Test
    fun `next page increments by one`() {
        assertEquals(3, resolver.nextPageAfterLoad(2))
    }

    private fun food(
        id: String = "banana",
        nameTr: String = "Muz"
    ): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = nameTr,
            nameEn = null,
            category = "fruit",
            defaultUnit = "g",
            defaultServingAmount = 100.0,
            caloriesPer100g = 89.0,
            proteinPer100g = 1.1,
            carbsPer100g = 22.8,
            fatPer100g = 0.3,
            fiberPer100g = null,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = ""
        )
    }
}
