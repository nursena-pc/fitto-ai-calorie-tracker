package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialModerationCoordinatorTest {

    @Test
    fun getBlockedUsers_routesToQuerySource() = runBlocking {
        val expected = listOf(SocialUser(id = "blocked-1", userName = "blocked"))
        val querySource = FakeQuerySource(blockedUsers = expected)
        val coordinator = createCoordinator(querySource = querySource)

        val result = coordinator.getBlockedUsers()

        assertSame(expected, result)
        assertTrue(querySource.blockedUsersRequested)
    }

    @Test
    fun isUserBlocked_routesTargetToQuerySource() = runBlocking {
        val querySource = FakeQuerySource(isBlocked = true)
        val coordinator = createCoordinator(querySource = querySource)

        val result = coordinator.isUserBlocked("target-check")

        assertTrue(result)
        assertEquals("target-check", querySource.blockedTargetUserId)
    }

    @Test
    fun getDirectThreadBlockState_routesIdentifiersToQuerySource() = runBlocking {
        val expected = DirectThreadBlockState(
            isBlocked = true,
            hiddenRanges = listOf(
                MessageHiddenRange(
                    userId = "target-1",
                    startAt = 10L,
                    endAt = null
                )
            )
        )
        val querySource = FakeQuerySource(directThreadBlockState = expected)
        val coordinator = createCoordinator(querySource = querySource)

        val result = coordinator.getDirectThreadBlockState(
            threadId = "thread-1",
            targetUserId = "target-1"
        )

        assertSame(expected, result)
        assertEquals("thread-1", querySource.blockStateThreadId)
        assertEquals("target-1", querySource.blockStateTargetUserId)
    }

    @Test
    fun observeDirectThreadBlockState_routesIdentifiersToQuerySource() {
        val expectedFlow = flowOf(SocialDirectThreadBlockStateResolver.empty())
        val querySource = FakeQuerySource(blockStateFlow = expectedFlow)
        val coordinator = createCoordinator(querySource = querySource)

        val result = coordinator.observeDirectThreadBlockState(
            threadId = "thread-observe",
            targetUserId = "target-observe"
        )

        assertSame(expectedFlow, result)
        assertEquals("thread-observe", querySource.observedThreadId)
        assertEquals("target-observe", querySource.observedTargetUserId)
    }

    @Test
    fun reportPost_routesPostAndReasonToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)

        coordinator.reportPost(
            postId = "post-1",
            reason = "spam"
        )

        assertEquals("post-1", commandSource.reportedPostId)
        assertEquals("spam", commandSource.postReportReason)
    }

    @Test
    fun blockUser_routesCompleteContextToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)

        coordinator.blockUser(
            targetUserId = "target-block",
            threadId = "thread-block",
            previewText = "Son mesaj",
            previewCreatedAt = 42L
        )

        assertEquals("target-block", commandSource.blockedTargetUserId)
        assertEquals("thread-block", commandSource.blockedThreadId)
        assertEquals("Son mesaj", commandSource.blockedPreviewText)
        assertEquals(42L, commandSource.blockedPreviewCreatedAt)
    }

    @Test
    fun unblockUser_routesTargetAndOptionalThreadToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)

        coordinator.unblockUser(
            targetUserId = "target-unblock",
            threadId = "thread-unblock"
        )

        assertEquals("target-unblock", commandSource.unblockedTargetUserId)
        assertEquals("thread-unblock", commandSource.unblockedThreadId)
    }

    @Test
    fun reportUser_routesTargetThreadAndReasonToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)

        coordinator.reportUser(
            targetUserId = "target-report",
            threadId = "thread-report",
            reason = "abuse"
        )

        assertEquals("target-report", commandSource.reportedTargetUserId)
        assertEquals("thread-report", commandSource.reportedThreadId)
        assertEquals("abuse", commandSource.userReportReason)
    }

    private fun createCoordinator(
        querySource: FakeQuerySource = FakeQuerySource(),
        commandSource: FakeCommandSource = FakeCommandSource()
    ): SocialModerationCoordinator {
        return SocialModerationCoordinator(
            querySource = querySource,
            commandSource = commandSource
        )
    }

    private class FakeQuerySource(
        private val blockedUsers: List<SocialUser> = emptyList(),
        private val isBlocked: Boolean = false,
        private val directThreadBlockState: DirectThreadBlockState =
            SocialDirectThreadBlockStateResolver.empty(),
        private val blockStateFlow: Flow<DirectThreadBlockState> =
            flowOf(SocialDirectThreadBlockStateResolver.empty())
    ) : SocialModerationQuerySource {
        var blockedUsersRequested: Boolean = false
        var blockedTargetUserId: String? = null
        var blockStateThreadId: String? = null
        var blockStateTargetUserId: String? = null
        var observedThreadId: String? = null
        var observedTargetUserId: String? = null

        override suspend fun getBlockedUsers(): List<SocialUser> {
            blockedUsersRequested = true
            return blockedUsers
        }

        override suspend fun isUserBlocked(targetUserId: String): Boolean {
            blockedTargetUserId = targetUserId
            return isBlocked
        }

        override suspend fun getDirectThreadBlockState(
            threadId: String,
            targetUserId: String
        ): DirectThreadBlockState {
            blockStateThreadId = threadId
            blockStateTargetUserId = targetUserId
            return directThreadBlockState
        }

        override fun observeDirectThreadBlockState(
            threadId: String,
            targetUserId: String
        ): Flow<DirectThreadBlockState> {
            observedThreadId = threadId
            observedTargetUserId = targetUserId
            return blockStateFlow
        }
    }

    private class FakeCommandSource : SocialModerationCommandSource {
        var reportedPostId: String? = null
        var postReportReason: String? = null
        var blockedTargetUserId: String? = null
        var blockedThreadId: String? = null
        var blockedPreviewText: String? = null
        var blockedPreviewCreatedAt: Long? = null
        var unblockedTargetUserId: String? = null
        var unblockedThreadId: String? = null
        var reportedTargetUserId: String? = null
        var reportedThreadId: String? = null
        var userReportReason: String? = null

        override suspend fun reportPost(
            postId: String,
            reason: String
        ) {
            reportedPostId = postId
            postReportReason = reason
        }

        override suspend fun blockUser(
            targetUserId: String,
            threadId: String?,
            previewText: String?,
            previewCreatedAt: Long?
        ) {
            blockedTargetUserId = targetUserId
            blockedThreadId = threadId
            blockedPreviewText = previewText
            blockedPreviewCreatedAt = previewCreatedAt
        }

        override suspend fun unblockUser(
            targetUserId: String,
            threadId: String?
        ) {
            unblockedTargetUserId = targetUserId
            unblockedThreadId = threadId
        }

        override suspend fun reportUser(
            targetUserId: String,
            threadId: String,
            reason: String
        ) {
            reportedTargetUserId = targetUserId
            reportedThreadId = threadId
            userReportReason = reason
        }
    }
}
