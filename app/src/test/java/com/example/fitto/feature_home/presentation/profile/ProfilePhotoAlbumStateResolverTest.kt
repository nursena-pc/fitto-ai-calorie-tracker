package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePhotoAlbumStateResolverTest {

    private val resolver = ProfilePhotoAlbumStateResolver()
    private val items = listOf(
        albumItem(id = "one", dateKey = "2026-01-01"),
        albumItem(id = "two", dateKey = "2026-01-02"),
        albumItem(id = "undated", dateKey = ProfilePhotoAlbumFilter.UNDATED)
    )

    @Test
    fun `all filter keeps every item and hides filter state`() {
        val state = resolver.resolve(items, ProfilePhotoAlbumFilter.ALL)

        assertEquals(items, state.visibleItems)
        assertFalse(state.hasActiveDateFilter)
        assertFalse(state.showEmptyState)
        assertTrue(state.showPhotoGrid)
    }

    @Test
    fun `date filter keeps only matching items`() {
        val state = resolver.resolve(items, "2026-01-02")

        assertEquals(listOf("two"), state.visibleItems.map { item -> item.id })
        assertTrue(state.hasActiveDateFilter)
        assertFalse(state.showEmptyState)
    }

    @Test
    fun `empty filtered result displays empty state`() {
        val state = resolver.resolve(items, "2026-02-01")

        assertTrue(state.visibleItems.isEmpty())
        assertTrue(state.showEmptyState)
        assertFalse(state.showPhotoGrid)
    }

    private fun albumItem(id: String, dateKey: String) = ProfilePhotoAlbumItem(
        id = id,
        imageUrl = "$id.jpg",
        sourceTitle = id,
        dateKey = dateKey,
        dateText = dateKey,
        createdAt = 1L
    )
}
