package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialDirectThreadBlockStateResolverTest {

    @Test
    fun empty_returnsUnblockedStateWithNoHiddenRanges() {
        val state = SocialDirectThreadBlockStateResolver.empty()

        assertFalse(state.isBlocked)
        assertEquals(emptyList<MessageHiddenRange>(), state.hiddenRanges)
    }

    @Test
    fun isBlocked_returnsFalseWhenTargetIsBlank() {
        assertFalse(
            SocialDirectThreadBlockStateResolver.isBlocked(
                blockedUserIds = listOf("user-1"),
                targetUserId = "   "
            )
        )
    }

    @Test
    fun isBlocked_readsStringValuesFromBlockedUserList() {
        assertTrue(
            SocialDirectThreadBlockStateResolver.isBlocked(
                blockedUserIds = listOf("user-1", 42, "user-2"),
                targetUserId = "user-2"
            )
        )
    }

    @Test
    fun resolve_combinesCompletedAndActiveRanges() {
        val completedRange = MessageHiddenRange(
            userId = "user-2",
            startAt = 10L,
            endAt = 20L
        )
        val activeRange = MessageHiddenRange(
            userId = "user-2",
            startAt = 30L,
            endAt = null
        )

        val state = SocialDirectThreadBlockStateResolver.resolve(
            blockedUserIds = listOf("user-2"),
            targetUserId = "user-2",
            completedRanges = listOf(completedRange),
            activeRange = activeRange
        )

        assertTrue(state.isBlocked)
        assertEquals(listOf(completedRange, activeRange), state.hiddenRanges)
    }
}
