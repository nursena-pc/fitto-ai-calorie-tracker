package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMessageThreadListResolverTest {

    @Test
    fun shouldSkipThreadList_skipsBlankUser() {
        assertTrue(SocialMessageThreadListResolver.shouldSkipThreadList(null))
        assertTrue(SocialMessageThreadListResolver.shouldSkipThreadList(""))
        assertTrue(SocialMessageThreadListResolver.shouldSkipThreadList("   "))
        assertFalse(SocialMessageThreadListResolver.shouldSkipThreadList("user-1"))
    }

    @Test
    fun blockedUserIds_removesBlankAndDuplicateIds() {
        val ids = SocialMessageThreadListResolver.blockedUserIds(
            listOf(" user-1 ", "", null, "user-2", "user-1", 5)
        )

        assertEquals(setOf("user-1", "user-2"), ids)
    }

    @Test
    fun isBlockedDirectThread_onlyBlocksDirectThreadsWithBlockedParticipant() {
        val blockedIds = setOf("blocked-user")

        assertTrue(
            SocialMessageThreadListResolver.isBlockedDirectThread(
                thread = thread(isGroup = false, otherParticipantId = "blocked-user"),
                blockedUserIds = blockedIds
            )
        )
        assertFalse(
            SocialMessageThreadListResolver.isBlockedDirectThread(
                thread = thread(isGroup = true, otherParticipantId = "blocked-user"),
                blockedUserIds = blockedIds
            )
        )
        assertFalse(
            SocialMessageThreadListResolver.isBlockedDirectThread(
                thread = thread(isGroup = false, otherParticipantId = "other-user"),
                blockedUserIds = blockedIds
            )
        )
    }

    @Test
    fun resolveBlockedPreview_freezesBlockedDirectThreadPreview() {
        val resolved = SocialMessageThreadListResolver.resolveBlockedPreview(
            thread = thread(
                id = "thread-1",
                isGroup = false,
                otherParticipantId = "blocked-user",
                lastMessage = "new live message",
                timeText = "1 dk",
                unreadCount = 4,
                highlightText = "new"
            ),
            blockedUserIds = setOf("blocked-user"),
            blockedThreadPreviews = mapOf(
                "thread-1" to BlockedThreadPreview(
                    text = "old frozen message",
                    createdAt = 1_000L
                )
            ),
            nowMillis = 61_000L
        )

        assertEquals("old frozen message", resolved.lastMessage)
        assertEquals("1 dk", resolved.timeText)
        assertEquals(0, resolved.unreadCount)
        assertEquals(null, resolved.highlightText)
    }

    @Test
    fun resolveBlockedPreview_usesBlockedFallbackWhenPreviewMissing() {
        val resolved = SocialMessageThreadListResolver.resolveBlockedPreview(
            thread = thread(
                id = "thread-1",
                isGroup = false,
                otherParticipantId = "blocked-user",
                lastMessage = "new live message",
                timeText = "1 dk",
                unreadCount = 2,
                highlightText = "live"
            ),
            blockedUserIds = setOf("blocked-user"),
            blockedThreadPreviews = emptyMap(),
            nowMillis = 61_000L
        )

        assertEquals("Bu kullanıcı engellendi", resolved.lastMessage)
        assertEquals("", resolved.timeText)
        assertEquals(0, resolved.unreadCount)
        assertEquals(null, resolved.highlightText)
    }

    @Test
    fun resolveThreads_sortsByDocumentUpdatedAtNewestFirst() {
        val resolved = SocialMessageThreadListResolver.resolveThreads(
            items = listOf(
                SocialMessageThreadListItem(thread = thread(id = "old"), updatedAt = 10L),
                SocialMessageThreadListItem(thread = thread(id = "new"), updatedAt = 30L),
                SocialMessageThreadListItem(thread = thread(id = "middle"), updatedAt = 20L)
            ),
            blockedUserIds = emptySet(),
            blockedThreadPreviews = emptyMap(),
            nowMillis = 61_000L
        )

        assertEquals(listOf("new", "middle", "old"), resolved.map { it.id })
    }

    private fun thread(
        id: String = "thread-1",
        isGroup: Boolean = false,
        otherParticipantId: String? = "other-user",
        lastMessage: String = "last message",
        timeText: String = "şimdi",
        unreadCount: Int = 0,
        highlightText: String? = null
    ): MessageThreadUiModel {
        return MessageThreadUiModel(
            id = id,
            title = "Thread",
            lastMessage = lastMessage,
            timeText = timeText,
            isGroup = isGroup,
            highlightText = highlightText,
            initials = "T",
            unreadCount = unreadCount,
            otherParticipantId = otherParticipantId
        )
    }
}
