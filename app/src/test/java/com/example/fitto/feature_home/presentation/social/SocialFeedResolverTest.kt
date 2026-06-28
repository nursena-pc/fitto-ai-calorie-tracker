package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialFeedResolverTest {

    @Test
    fun shouldSkipFeed_skipsBlankCurrentUser() {
        assertTrue(SocialFeedResolver.shouldSkipFeed(""))
        assertTrue(SocialFeedResolver.shouldSkipFeed("   "))
        assertFalse(SocialFeedResolver.shouldSkipFeed("user-1"))
    }

    @Test
    fun followingIds_removesBlankAndDuplicateIds() {
        val ids = SocialFeedResolver.followingIds(
            listOf(" user-1 ", "", null, "user-2", "user-1")
        )

        assertEquals(setOf("user-1", "user-2"), ids)
    }

    @Test
    fun followingLookupChunks_respectsFirestoreWhereInLimit() {
        val ids = (1..23).map { index -> "user-$index" }.toSet()

        val chunks = SocialFeedResolver.followingLookupChunks(ids)

        assertEquals(listOf(10, 10, 3), chunks.map { it.size })
        assertEquals(ids.toList(), chunks.flatten())
    }

    @Test
    fun followingLookupChunks_returnsEmptyForInvalidChunkSize() {
        assertEquals(
            emptyList<List<String>>(),
            SocialFeedResolver.followingLookupChunks(setOf("user-1"), chunkSize = 0)
        )
    }

    @Test
    fun sourcePostMap_keepsLatestDuplicateFromSameSource() {
        val posts = listOf(
            post(id = "same", createdAt = 1L),
            post(id = "same", createdAt = 2L)
        )

        assertEquals(2L, SocialFeedResolver.sourcePostMap(posts).getValue("same").createdAt)
    }

    @Test
    fun mergeSourcePosts_deduplicatesAndSortsNewestFirst() {
        val sources = linkedMapOf(
            "community" to mapOf(
                "old" to post(id = "old", createdAt = 10L),
                "duplicate" to post(id = "duplicate", createdAt = 20L)
            ),
            "my_posts" to mapOf(
                "new" to post(id = "new", createdAt = 30L),
                "duplicate" to post(id = "duplicate", createdAt = 40L)
            )
        )

        assertEquals(
            listOf("duplicate", "new", "old"),
            SocialFeedResolver.mergeSourcePosts(sources).map { it.id }
        )
        assertEquals(40L, SocialFeedResolver.mergeSourcePosts(sources).first().createdAt)
    }

    private fun post(id: String, createdAt: Long): SocialPost {
        return SocialPost(id = id, createdAt = createdAt)
    }
}
