package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialPostDraftUpdateResolverTest {

    @Test
    fun createUpdatePayload_keepsDraftPrivateAndUnpublished() {
        val payload = SocialPostDraftUpdateResolver.createUpdatePayload(
            content = "  draft body  ",
            imageUrl = "image-url",
            mealInfo = "  breakfast  ",
            calories = 320,
            allowComments = false,
            visibility = POST_VISIBILITY_COMMUNITY,
            tags = listOf("draft", "meal"),
            publish = false,
            updatedAt = 100L
        )

        assertEquals("draft body", payload["content"])
        assertEquals("image-url", payload["imageUrl"])
        assertEquals("breakfast", payload["mealInfo"])
        assertEquals(320, payload["calories"])
        assertEquals(false, payload["allowComments"])
        assertEquals(POST_VISIBILITY_PRIVATE, payload["visibility"])
        assertEquals(listOf("draft", "meal"), payload["tags"])
        assertEquals(true, payload["isDraft"])
        assertEquals(100L, payload["updatedAt"])
        assertFalse(payload.containsKey("publishedAt"))
    }

    @Test
    fun createUpdatePayload_publishesWithRequestedVisibility() {
        val payload = SocialPostDraftUpdateResolver.createUpdatePayload(
            content = "  publish body  ",
            imageUrl = null,
            mealInfo = null,
            calories = null,
            allowComments = true,
            visibility = POST_VISIBILITY_FOLLOWERS,
            tags = emptyList(),
            publish = true,
            updatedAt = 200L
        )

        assertEquals("publish body", payload["content"])
        assertEquals(POST_VISIBILITY_FOLLOWERS, payload["visibility"])
        assertEquals(false, payload["isDraft"])
        assertEquals(200L, payload["updatedAt"])
        assertEquals(200L, payload["publishedAt"])
    }

    @Test
    fun createUpdatePayload_publishesBlankVisibilityAsCommunity() {
        val payload = SocialPostDraftUpdateResolver.createUpdatePayload(
            content = "content",
            imageUrl = null,
            mealInfo = null,
            calories = null,
            allowComments = true,
            visibility = "   ",
            tags = emptyList(),
            publish = true,
            updatedAt = 300L
        )

        assertEquals(POST_VISIBILITY_COMMUNITY, payload["visibility"])
        assertTrue(payload.containsKey("publishedAt"))
    }
}
