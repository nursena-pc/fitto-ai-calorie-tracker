package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialFollowPayloadResolverTest {

    @Test
    fun shouldSkipFollowAction_skipsBlankOrSelfTargets() {
        assertTrue(SocialFollowPayloadResolver.shouldSkipFollowAction(null, "target"))
        assertTrue(SocialFollowPayloadResolver.shouldSkipFollowAction("", "target"))
        assertTrue(SocialFollowPayloadResolver.shouldSkipFollowAction("user-1", ""))
        assertTrue(SocialFollowPayloadResolver.shouldSkipFollowAction("user-1", "user-1"))
        assertFalse(SocialFollowPayloadResolver.shouldSkipFollowAction("user-1", "user-2"))
    }

    @Test
    fun followDocumentId_usesFollowerAndFollowingIds() {
        assertEquals(
            "follower-1_following-1",
            SocialFollowPayloadResolver.followDocumentId(
                followerId = "follower-1",
                followingId = "following-1"
            )
        )
    }

    @Test
    fun isDeletedOrMissing_detectsMissingOrDeletedUser() {
        assertTrue(SocialFollowPayloadResolver.isDeletedOrMissing(exists = false, isDeleted = false))
        assertTrue(SocialFollowPayloadResolver.isDeletedOrMissing(exists = true, isDeleted = true))
        assertFalse(SocialFollowPayloadResolver.isDeletedOrMissing(exists = true, isDeleted = false))
        assertFalse(SocialFollowPayloadResolver.isDeletedOrMissing(exists = true, isDeleted = null))
    }

    @Test
    fun createFollowPayload_includesFollowerFollowingAndCreatedAt() {
        assertEquals(
            mapOf(
                "followerId" to "user-1",
                "followingId" to "user-2",
                "createdAt" to 123L
            ),
            SocialFollowPayloadResolver.createFollowPayload(
                followerId = "user-1",
                followingId = "user-2",
                createdAt = 123L
            )
        )
    }

    @Test
    fun deletedUserCountResetPayload_resetsFollowerAndFollowingCounts() {
        assertEquals(
            mapOf(
                "followersCount" to 0,
                "followingCount" to 0
            ),
            SocialFollowPayloadResolver.deletedUserCountResetPayload()
        )
    }

    @Test
    fun decrementCountPayload_decrementsWithoutGoingNegative() {
        assertEquals(
            mapOf(
                "followersCount" to 4L,
                "updatedAt" to 500L
            ),
            SocialFollowPayloadResolver.decrementCountPayload(
                field = "followersCount",
                currentCount = 5L,
                updatedAt = 500L
            )
        )

        assertEquals(
            mapOf(
                "followingCount" to 0L,
                "updatedAt" to 600L
            ),
            SocialFollowPayloadResolver.decrementCountPayload(
                field = "followingCount",
                currentCount = null,
                updatedAt = 600L
            )
        )
    }
}
