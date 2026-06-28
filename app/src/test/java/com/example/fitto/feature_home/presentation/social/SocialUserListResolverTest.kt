package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialUserListResolverTest {

    @Test
    fun shouldSkipRelationshipLookup_skipsBlankIds() {
        assertTrue(SocialUserListResolver.shouldSkipRelationshipLookup(""))
        assertTrue(SocialUserListResolver.shouldSkipRelationshipLookup("   "))
        assertFalse(SocialUserListResolver.shouldSkipRelationshipLookup("user-1"))
    }

    @Test
    fun followerIds_keepsUniqueNonBlankIdsInOrder() {
        assertEquals(
            listOf("user-1", "user-2", "user-3"),
            SocialUserListResolver.followerIds(
                listOf("user-1", "", null, "user-2", "user-1", "user-3")
            )
        )
    }

    @Test
    fun followingIds_keepsUniqueNonBlankIdsInOrder() {
        assertEquals(
            listOf("target-1", "target-2"),
            SocialUserListResolver.followingIds(
                listOf("target-1", "target-1", "   ", null, "target-2")
            )
        )
    }

    @Test
    fun lookupChunks_usesFirestoreWhereInLimit() {
        val ids = (1..12).map { index -> "user-$index" } + listOf("user-1", "")

        assertEquals(
            listOf(
                (1..10).map { index -> "user-$index" },
                listOf("user-11", "user-12")
            ),
            SocialUserListResolver.lookupChunks(ids)
        )
    }

    @Test
    fun resolveVisibleProfile_skipsDeletedUsers() {
        assertNull(
            SocialUserListResolver.resolveVisibleProfile(
                id = "user-1",
                isDeleted = true,
                userName = "nursena",
                username = null,
                displayName = null,
                name = null,
                realName = null,
                email = null,
                photoUrl = null,
                bio = null,
                followersCount = null,
                followingCount = null
            )
        )
    }

    @Test
    fun resolveVisibleProfile_mapsVisibleUserFields() {
        assertEquals(
            SocialUser(
                id = "user-1",
                userName = "nursena",
                displayName = "Nursena",
                personalName = "Nursena A.",
                email = "nursena@example.com",
                photoUrl = "https://example.com/photo.jpg",
                bio = "bio",
                followersCount = 15,
                followingCount = 9
            ),
            SocialUserListResolver.resolveVisibleProfile(
                id = "user-1",
                isDeleted = false,
                userName = "nursena",
                username = null,
                displayName = "Nursena",
                name = "Nursena A.",
                realName = null,
                email = "nursena@example.com",
                photoUrl = "https://example.com/photo.jpg",
                bio = "bio",
                followersCount = 15L,
                followingCount = 9L
            )
        )
    }

    @Test
    fun orderUsersByRequestedIds_preservesRequestedOrderAndSkipsMissingUsers() {
        val user1 = SocialUser(id = "user-1")
        val user2 = SocialUser(id = "user-2")

        assertEquals(
            listOf(user2, user1),
            SocialUserListResolver.orderUsersByRequestedIds(
                requestedIds = listOf("user-2", "missing", "user-1"),
                usersById = mapOf(
                    user1.id to user1,
                    user2.id to user2
                )
            )
        )
    }
}
