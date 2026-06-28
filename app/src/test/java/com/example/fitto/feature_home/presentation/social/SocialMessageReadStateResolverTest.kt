package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMessageReadStateResolverTest {

    @Test
    fun canMarkThreadReadRequiresCurrentUserAndThread() {
        assertFalse(SocialMessageReadStateResolver.canMarkThreadRead(null, "thread-1"))
        assertFalse(SocialMessageReadStateResolver.canMarkThreadRead("", "thread-1"))
        assertFalse(SocialMessageReadStateResolver.canMarkThreadRead("user-1", ""))

        assertTrue(SocialMessageReadStateResolver.canMarkThreadRead("user-1", "thread-1"))
    }

    @Test
    fun directPeerIdReturnsOtherParticipantOnlyForDirectThreads() {
        val peerId = SocialMessageReadStateResolver.directPeerId(
            threadType = SocialRepository.THREAD_TYPE_DIRECT,
            participantIds = listOf("user-1", "user-2"),
            currentUserId = "user-1"
        )

        assertEquals("user-2", peerId)
        assertNull(
            SocialMessageReadStateResolver.directPeerId(
                threadType = SocialRepository.THREAD_TYPE_GROUP,
                participantIds = listOf("user-1", "user-2"),
                currentUserId = "user-1"
            )
        )
    }

    @Test
    fun blockedUserIdsDropsInvalidBlankAndDuplicateValues() {
        val blockedUserIds = SocialMessageReadStateResolver.blockedUserIds(
            listOf(" user-2 ", "", null, 7, "user-2", "user-3")
        )

        assertEquals(listOf("user-2", "user-3"), blockedUserIds)
    }

    @Test
    fun isPeerBlockedRequiresPeerAndMatchingBlockedId() {
        assertTrue(SocialMessageReadStateResolver.isPeerBlocked(listOf("user-2"), "user-2"))
        assertFalse(SocialMessageReadStateResolver.isPeerBlocked(listOf("user-2"), "user-3"))
        assertFalse(SocialMessageReadStateResolver.isPeerBlocked(listOf("user-2"), null))
        assertFalse(SocialMessageReadStateResolver.isPeerBlocked(listOf("user-2"), ""))
    }

    @Test
    fun hiddenRangesAppendsActiveRangeWhenAvailable() {
        val completedRange = MessageHiddenRange(userId = "user-2", startAt = 10L, endAt = 20L)
        val activeRange = MessageHiddenRange(userId = "user-2", startAt = 30L, endAt = null)

        assertEquals(
            listOf(completedRange, activeRange),
            SocialMessageReadStateResolver.hiddenRanges(listOf(completedRange), activeRange)
        )
        assertEquals(
            listOf(completedRange),
            SocialMessageReadStateResolver.hiddenRanges(listOf(completedRange), null)
        )
    }
}
