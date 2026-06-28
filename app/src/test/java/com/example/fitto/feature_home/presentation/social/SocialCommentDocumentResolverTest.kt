package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialCommentDocumentResolverTest {

    @Test
    fun resolve_mapsCommentFieldsAndLikedState() {
        val comment = SocialCommentDocumentResolver.resolve(
            id = "comment-1",
            postId = "post-1",
            currentUserId = "user-2",
            userId = "user-1",
            userName = "Ayşe",
            userPhotoUrl = "photo-url",
            fallbackPhotoUrl = "fallback-url",
            text = "Harika görünüyor",
            parentCommentId = "parent-1",
            likesCount = 3L,
            replyCount = 2L,
            likedBy = listOf("user-2", "user-3", 42),
            createdAt = 123L
        )

        assertEquals("comment-1", comment.id)
        assertEquals("post-1", comment.postId)
        assertEquals("user-1", comment.userId)
        assertEquals("Ayşe", comment.userName)
        assertEquals("photo-url", comment.userPhotoUrl)
        assertEquals("Harika görünüyor", comment.text)
        assertEquals("parent-1", comment.parentCommentId)
        assertEquals(3, comment.likesCount)
        assertEquals(2, comment.replyCount)
        assertTrue(comment.likedByMe)
        assertEquals(123L, comment.createdAt)
    }

    @Test
    fun resolve_usesFallbacksForMissingValues() {
        val comment = SocialCommentDocumentResolver.resolve(
            id = "comment-1",
            postId = "post-1",
            currentUserId = "user-2",
            userId = null,
            userName = "   ",
            userPhotoUrl = null,
            fallbackPhotoUrl = "fallback-url",
            text = null,
            parentCommentId = null,
            likesCount = null,
            replyCount = null,
            likedBy = "invalid",
            createdAt = null
        )

        assertEquals("", comment.userId)
        assertEquals("fitto_user", comment.userName)
        assertEquals("fallback-url", comment.userPhotoUrl)
        assertEquals("", comment.text)
        assertEquals(0, comment.likesCount)
        assertEquals(0, comment.replyCount)
        assertFalse(comment.likedByMe)
        assertEquals(0L, comment.createdAt)
    }

    @Test
    fun resolve_prefersExplicitUserPhotoOverFallback() {
        val comment = SocialCommentDocumentResolver.resolve(
            id = "comment-1",
            postId = "post-1",
            currentUserId = "user-2",
            userId = "user-1",
            userName = "Ayşe",
            userPhotoUrl = "photo-url",
            fallbackPhotoUrl = "fallback-url",
            text = "text",
            parentCommentId = null,
            likesCount = 0L,
            replyCount = 0L,
            likedBy = emptyList<String>(),
            createdAt = 1L
        )

        assertEquals("photo-url", comment.userPhotoUrl)
    }
}
