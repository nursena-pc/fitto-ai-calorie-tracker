package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialConversationObservationCoordinatorTest {

    private val presence = PresenceStatusUiModel(
        userId = "user-1",
        isOnline = true,
        lastChanged = 10L
    )
    private val typingUser = TypingUserUiModel(
        uid = "user-2",
        userName = "Nursena",
        photoUrl = "photo",
        updatedAt = 20L
    )
    private val searchResult = GroupMessageSearchResultUiModel(
        messageId = "message-1",
        threadId = "thread-1",
        senderName = "Nursena",
        messageText = "Merhaba",
        createdAt = 30L
    )
    private val sharedContentSummary = ThreadSharedContentSummary(
        mediaCount = 2,
        linkCount = 1,
        fileCount = 3
    )

    @Test
    fun observeUserPresence_routesToRealtimeSource() = runBlocking {
        val realtimeSource = FakeRealtimeSource(presence = presence)
        val coordinator = createCoordinator(realtimeSource = realtimeSource)

        assertEquals(presence, coordinator.observeUserPresence("user-1").first())
        assertEquals("user-1", realtimeSource.observedPresenceUserId)
    }

    @Test
    fun observeTypingUsers_routesToRealtimeSource() = runBlocking {
        val realtimeSource = FakeRealtimeSource(typingUsers = listOf(typingUser))
        val coordinator = createCoordinator(realtimeSource = realtimeSource)

        assertEquals(
            listOf(typingUser),
            coordinator.observeTypingUsers("thread-1").first()
        )
        assertEquals("thread-1", realtimeSource.observedTypingThreadId)
    }

    @Test
    fun setTyping_routesThreadAndTypingStateToRealtimeSource() {
        val realtimeSource = FakeRealtimeSource()
        val coordinator = createCoordinator(realtimeSource = realtimeSource)

        coordinator.setTyping(threadId = "thread-1", isTyping = true)

        assertEquals("thread-1", realtimeSource.updatedTypingThreadId)
        assertEquals(true, realtimeSource.updatedTypingState)
    }

    @Test
    fun observeThreadMessagesForSearch_routesToThreadSource() = runBlocking {
        val threadSource = FakeThreadSource(searchResults = listOf(searchResult))
        val coordinator = createCoordinator(threadSource = threadSource)

        assertEquals(
            listOf(searchResult),
            coordinator.observeThreadMessagesForSearch("thread-1").first()
        )
        assertEquals("thread-1", threadSource.searchedThreadId)
    }

    @Test
    fun observeThreadSharedContentSummary_routesToThreadSource() = runBlocking {
        val threadSource = FakeThreadSource(sharedContentSummary = sharedContentSummary)
        val coordinator = createCoordinator(threadSource = threadSource)

        assertEquals(
            sharedContentSummary,
            coordinator.observeThreadSharedContentSummary("thread-1").first()
        )
        assertEquals("thread-1", threadSource.sharedContentThreadId)
    }

    @Test
    fun observeUnreadMessageCount_routesToThreadSource() = runBlocking {
        val threadSource = FakeThreadSource(unreadCount = 7)
        val coordinator = createCoordinator(threadSource = threadSource)

        assertEquals(7, coordinator.observeUnreadMessageCount().first())
        assertEquals(1, threadSource.unreadObservationCount)
    }

    private fun createCoordinator(
        realtimeSource: FakeRealtimeSource = FakeRealtimeSource(),
        threadSource: FakeThreadSource = FakeThreadSource()
    ): SocialConversationObservationCoordinator {
        return SocialConversationObservationCoordinator(
            realtimeSource = realtimeSource,
            threadSource = threadSource
        )
    }

    private class FakeRealtimeSource(
        private val presence: PresenceStatusUiModel = PresenceStatusUiModel(
            userId = "",
            isOnline = false,
            lastChanged = 0L
        ),
        private val typingUsers: List<TypingUserUiModel> = emptyList()
    ) : SocialRealtimeObservationSource {
        var observedPresenceUserId: String? = null
        var observedTypingThreadId: String? = null
        var updatedTypingThreadId: String? = null
        var updatedTypingState: Boolean? = null

        override fun observeUserPresence(userId: String): Flow<PresenceStatusUiModel> {
            observedPresenceUserId = userId
            return flowOf(presence)
        }

        override fun observeTypingUsers(threadId: String): Flow<List<TypingUserUiModel>> {
            observedTypingThreadId = threadId
            return flowOf(typingUsers)
        }

        override fun setTyping(threadId: String, isTyping: Boolean) {
            updatedTypingThreadId = threadId
            updatedTypingState = isTyping
        }
    }

    private class FakeThreadSource(
        private val searchResults: List<GroupMessageSearchResultUiModel> = emptyList(),
        private val sharedContentSummary: ThreadSharedContentSummary = ThreadSharedContentSummary(),
        private val unreadCount: Int = 0
    ) : SocialThreadObservationSource {
        var searchedThreadId: String? = null
        var sharedContentThreadId: String? = null
        var unreadObservationCount: Int = 0

        override fun observeThreadMessagesForSearch(
            threadId: String
        ): Flow<List<GroupMessageSearchResultUiModel>> {
            searchedThreadId = threadId
            return flowOf(searchResults)
        }

        override fun observeThreadSharedContentSummary(
            threadId: String
        ): Flow<ThreadSharedContentSummary> {
            sharedContentThreadId = threadId
            return flowOf(sharedContentSummary)
        }

        override fun observeUnreadMessageCount(): Flow<Int> {
            unreadObservationCount += 1
            return flowOf(unreadCount)
        }
    }
}
