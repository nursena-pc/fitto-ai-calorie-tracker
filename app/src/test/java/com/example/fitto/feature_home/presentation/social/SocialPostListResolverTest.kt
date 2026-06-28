package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialPostListResolverTest {

    @Test
    fun profilePosts_returnsPublishedPostsNewestFirst() {
        val posts = listOf(
            post(id = "old", createdAt = 10L),
            post(id = "draft", createdAt = 30L, isDraft = true),
            post(id = "new", createdAt = 20L)
        )

        assertEquals(
            listOf("new", "old"),
            SocialPostListResolver.profilePosts(posts).map { it.id }
        )
    }

    @Test
    fun savedPosts_returnsPublishedPostsNewestFirst() {
        val posts = listOf(
            post(id = "new-draft", createdAt = 30L, isDraft = true),
            post(id = "old-published", createdAt = 10L),
            post(id = "new-published", createdAt = 20L)
        )

        assertEquals(
            listOf("new-published", "old-published"),
            SocialPostListResolver.savedPosts(posts).map { it.id }
        )
    }

    @Test
    fun draftPosts_returnsDraftPostsNewestFirst() {
        val posts = listOf(
            post(id = "published", createdAt = 30L),
            post(id = "old-draft", createdAt = 10L, isDraft = true),
            post(id = "new-draft", createdAt = 20L, isDraft = true)
        )

        assertEquals(
            listOf("new-draft", "old-draft"),
            SocialPostListResolver.draftPosts(posts).map { it.id }
        )
    }

    @Test
    fun visiblePost_returnsPublishedPostForAnyUser() {
        val post = post(id = "published", userId = "owner")

        assertEquals(
            post,
            SocialPostListResolver.visiblePost(
                post = post,
                currentUserId = "viewer"
            )
        )
    }

    @Test
    fun visiblePost_returnsOwnDraft() {
        val post = post(id = "draft", userId = "owner", isDraft = true)

        assertEquals(
            post,
            SocialPostListResolver.visiblePost(
                post = post,
                currentUserId = "owner"
            )
        )
    }

    @Test
    fun visiblePost_hidesOtherUsersDrafts() {
        assertNull(
            SocialPostListResolver.visiblePost(
                post = post(id = "draft", userId = "owner", isDraft = true),
                currentUserId = "viewer"
            )
        )
    }

    @Test
    fun visiblePost_keepsNullPostNull() {
        assertNull(
            SocialPostListResolver.visiblePost(
                post = null,
                currentUserId = "viewer"
            )
        )
    }

    private fun post(
        id: String,
        userId: String = "user-1",
        createdAt: Long = 0L,
        isDraft: Boolean = false
    ): SocialPost {
        return SocialPost(
            id = id,
            userId = userId,
            createdAt = createdAt,
            isDraft = isDraft
        )
    }
}
