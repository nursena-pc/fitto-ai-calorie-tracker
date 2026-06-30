package com.example.fitto.feature_meal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealLoadMoreStateResolverTest {

    private val resolver = AddMealSearchStateResolver()

    @Test
    fun `load more result keeps current page and disables paging when no foods are fetched`() {
        val result = resolver.loadMoreResult(
            currentPage = 3,
            fetchedCount = 0,
            pageSize = 8
        )

        assertFalse(result.hasNewFoods)
        assertEquals(3, result.nextPageNumber)
        assertFalse(result.canLoadMore)
        assertTrue(resolver.shouldShowNoMoreFoodsFeedback(result))
    }

    @Test
    fun `load more result advances page and allows more when fetched page is full`() {
        val result = resolver.loadMoreResult(
            currentPage = 3,
            fetchedCount = 8,
            pageSize = 8
        )

        assertTrue(result.hasNewFoods)
        assertEquals(4, result.nextPageNumber)
        assertTrue(result.canLoadMore)
        assertFalse(resolver.shouldShowNoMoreFoodsFeedback(result))
    }

    @Test
    fun `load more result advances page and stops paging when fetched page is partial`() {
        val result = resolver.loadMoreResult(
            currentPage = 3,
            fetchedCount = 4,
            pageSize = 8
        )

        assertTrue(result.hasNewFoods)
        assertEquals(4, result.nextPageNumber)
        assertFalse(result.canLoadMore)
    }

    @Test
    fun `load more button is visible only when there are results and another page`() {
        assertFalse(
            resolver.loadMoreUiState(
                canLoadMore = false,
                hasCurrentResults = true,
                isLoadingMore = false
            ).visible
        )
        assertFalse(
            resolver.loadMoreUiState(
                canLoadMore = true,
                hasCurrentResults = false,
                isLoadingMore = false
            ).visible
        )
        assertTrue(
            resolver.loadMoreUiState(
                canLoadMore = true,
                hasCurrentResults = true,
                isLoadingMore = false
            ).visible
        )
    }

    @Test
    fun `load more button is disabled while loading`() {
        assertFalse(
            resolver.loadMoreUiState(
                canLoadMore = true,
                hasCurrentResults = true,
                isLoadingMore = true
            ).enabled
        )
        assertTrue(
            resolver.loadMoreUiState(
                canLoadMore = true,
                hasCurrentResults = true,
                isLoadingMore = false
            ).enabled
        )
    }
}
