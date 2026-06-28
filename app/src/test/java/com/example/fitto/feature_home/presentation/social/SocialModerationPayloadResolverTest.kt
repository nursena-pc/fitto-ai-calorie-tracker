package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialModerationPayloadResolverTest {

    @Test
    fun shouldSkipTargetAction_skipsBlankAndSelfTargets() {
        assertTrue(SocialModerationPayloadResolver.shouldSkipTargetAction("current", ""))
        assertTrue(SocialModerationPayloadResolver.shouldSkipTargetAction("current", "current"))
        assertFalse(SocialModerationPayloadResolver.shouldSkipTargetAction("current", "target"))
    }

    @Test
    fun createPostReportPayload_preservesPostReportFields() {
        val payload = SocialModerationPayloadResolver.createPostReportPayload(
            postId = "post-1",
            reporterId = "user-1",
            reason = "spam",
            createdAt = 100L
        )

        assertEquals("post-1", payload["postId"])
        assertEquals("user-1", payload["reporterId"])
        assertEquals("spam", payload["reason"])
        assertEquals(100L, payload["createdAt"])
    }

    @Test
    fun createUserReportPayload_preservesUserReportFields() {
        val payload = SocialModerationPayloadResolver.createUserReportPayload(
            targetUserId = "target-1",
            reporterId = "user-1",
            threadId = "thread-1",
            reason = "abuse",
            createdAt = 200L
        )

        assertEquals("user", payload["type"])
        assertEquals("target-1", payload["reportedUserId"])
        assertEquals("user-1", payload["reporterId"])
        assertEquals("thread-1", payload["threadId"])
        assertEquals("abuse", payload["reason"])
        assertEquals(200L, payload["createdAt"])
    }

    @Test
    fun createBlockUserPayload_withoutThreadOnlyBlocksUserAndUpdatesTimestamp() {
        val payload = SocialModerationPayloadResolver.createBlockUserPayload(
            targetUserId = "target-1",
            threadId = null,
            previewText = null,
            previewCreatedAt = null,
            updatedAt = 300L
        )

        assertTrue(payload["blockedUserIds"] is FieldValue)
        assertEquals(300L, payload["updatedAt"])
        assertFalse(payload.containsKey("activeBlockedMessageRanges.thread-1"))
        assertFalse(payload.containsKey("blockedThreadPreviews.thread-1"))
    }

    @Test
    fun createBlockUserPayload_withThreadCreatesHiddenRangeAndPreviewFallback() {
        val payload = SocialModerationPayloadResolver.createBlockUserPayload(
            targetUserId = "target-1",
            threadId = "thread-1",
            previewText = " ",
            previewCreatedAt = null,
            updatedAt = 400L
        )

        @Suppress("UNCHECKED_CAST")
        val range = payload["activeBlockedMessageRanges.thread-1"] as Map<String, Any>
        assertEquals("target-1", range["userId"])
        assertEquals(400L, range["startAt"])

        @Suppress("UNCHECKED_CAST")
        val preview = payload["blockedThreadPreviews.thread-1"] as Map<String, Any>
        assertEquals("Henüz mesaj yok.", preview["text"])
        assertEquals(400L, preview["createdAt"])
    }

    @Test
    fun createUnblockBasePayload_removesUserAndUpdatesTimestamp() {
        val payload = SocialModerationPayloadResolver.createUnblockBasePayload(
            targetUserId = "target-1",
            updatedAt = 500L
        )

        assertTrue(payload["blockedUserIds"] is FieldValue)
        assertEquals(500L, payload["updatedAt"])
    }

    @Test
    fun createCloseBlockedThreadPayload_closesMatchingRangeAndClearsActiveState() {
        val payload = SocialModerationPayloadResolver.createCloseBlockedThreadPayload(
            threadId = "thread-1",
            activeRange = MessageHiddenRange(
                userId = "target-1",
                startAt = 600L,
                endAt = null
            ),
            targetUserId = "target-1",
            updatedAt = 700L
        )

        assertTrue(payload["blockedMessageRanges.thread-1"] is FieldValue)
        assertTrue(payload["activeBlockedMessageRanges.thread-1"] is FieldValue)
        assertTrue(payload["blockedThreadPreviews.thread-1"] is FieldValue)
    }

    @Test
    fun createCloseBlockedThreadPayload_onlyClearsActiveStateForDifferentRangeOwner() {
        val payload = SocialModerationPayloadResolver.createCloseBlockedThreadPayload(
            threadId = "thread-1",
            activeRange = MessageHiddenRange(
                userId = "other-user",
                startAt = 600L,
                endAt = null
            ),
            targetUserId = "target-1",
            updatedAt = 700L
        )

        assertFalse(payload.containsKey("blockedMessageRanges.thread-1"))
        assertTrue(payload["activeBlockedMessageRanges.thread-1"] is FieldValue)
        assertTrue(payload["blockedThreadPreviews.thread-1"] is FieldValue)
    }
}
