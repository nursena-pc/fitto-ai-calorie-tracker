package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialGroupMessageSearchResolverTest {

    @Test
    fun shouldSkipSearchRequiresThreadId() {
        assertTrue(SocialGroupMessageSearchResolver.shouldSkipSearch(""))
        assertTrue(SocialGroupMessageSearchResolver.shouldSkipSearch("   "))

        assertFalse(SocialGroupMessageSearchResolver.shouldSkipSearch("thread-1"))
    }

    @Test
    fun resolveResultSkipsBlankMessages() {
        assertNull(
            SocialGroupMessageSearchResolver.resolveResult(
                messageId = "message-1",
                threadId = "thread-1",
                text = "   ",
                senderName = "nursena",
                senderDisplayName = null,
                createdAt = 10L
            )
        )

        assertNull(
            SocialGroupMessageSearchResolver.resolveResult(
                messageId = "message-1",
                threadId = "thread-1",
                text = null,
                senderName = "nursena",
                senderDisplayName = null,
                createdAt = 10L
            )
        )
    }

    @Test
    fun resolveResultTrimsMessageTextAndKeepsTimestamp() {
        val result = SocialGroupMessageSearchResolver.resolveResult(
            messageId = "message-1",
            threadId = "thread-1",
            text = "  merhaba  ",
            senderName = "nursena",
            senderDisplayName = null,
            createdAt = 25L
        )

        requireNotNull(result)
        assertEquals("message-1", result.messageId)
        assertEquals("thread-1", result.threadId)
        assertEquals("nursena", result.senderName)
        assertEquals("merhaba", result.messageText)
        assertEquals(25L, result.createdAt)
    }

    @Test
    fun resolveResultUsesDisplayNameThenFallbackSenderName() {
        val displayNameResult = SocialGroupMessageSearchResolver.resolveResult(
            messageId = "message-1",
            threadId = "thread-1",
            text = "merhaba",
            senderName = null,
            senderDisplayName = "Nursena",
            createdAt = null
        )

        requireNotNull(displayNameResult)
        assertEquals("Nursena", displayNameResult.senderName)
        assertEquals(0L, displayNameResult.createdAt)

        val fallbackResult = SocialGroupMessageSearchResolver.resolveResult(
            messageId = "message-2",
            threadId = "thread-1",
            text = "selam",
            senderName = null,
            senderDisplayName = null,
            createdAt = null
        )

        requireNotNull(fallbackResult)
        assertEquals("Kullanıcı", fallbackResult.senderName)
    }
}
