package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialPostInteractionResolverTest {

    @Test
    fun toggleSavePayload_addsUserWhenMissing() {
        val payload = SocialPostInteractionResolver.toggleSavePayload(
            savedByValue = listOf("user-1"),
            userId = "user-2"
        )

        assertEquals(
            listOf("user-1", "user-2"),
            payload["savedBy"]
        )
    }

    @Test
    fun toggleSavePayload_removesUserWhenPresent() {
        val payload = SocialPostInteractionResolver.toggleSavePayload(
            savedByValue = listOf("user-1", "user-2"),
            userId = "user-2"
        )

        assertEquals(
            listOf("user-1"),
            payload["savedBy"]
        )
    }

    @Test
    fun toggleLikePayload_addsUserAndIncrementsLikes() {
        val payload = SocialPostInteractionResolver.toggleLikePayload(
            likedByValue = listOf("user-1", 42),
            currentLikes = 1,
            userId = "user-2"
        )

        assertEquals(
            listOf("user-1", "user-2"),
            payload["likedBy"]
        )
        assertEquals(2L, payload["likesCount"])
    }

    @Test
    fun toggleLikePayload_removesUserAndDecrementsLikes() {
        val payload = SocialPostInteractionResolver.toggleLikePayload(
            likedByValue = listOf("user-1", "user-2"),
            currentLikes = 2,
            userId = "user-2"
        )

        assertEquals(
            listOf("user-1"),
            payload["likedBy"]
        )
        assertEquals(1L, payload["likesCount"])
    }

    @Test
    fun toggleLikePayload_neverReturnsNegativeLikes() {
        val payload = SocialPostInteractionResolver.toggleLikePayload(
            likedByValue = listOf("user-1"),
            currentLikes = 0,
            userId = "user-1"
        )

        assertEquals(emptyList<String>(), payload["likedBy"])
        assertEquals(0L, payload["likesCount"])
    }
}
