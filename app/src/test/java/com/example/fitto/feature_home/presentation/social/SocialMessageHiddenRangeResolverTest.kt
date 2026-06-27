package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMessageHiddenRangeResolverTest {

    @Test
    fun readBlockedThreadPreviews_readsValidPreviewMap() {
        val value = mapOf(
            "thread-1" to mapOf(
                "text" to "Merhaba",
                "createdAt" to 123L
            ),
            "thread-2" to mapOf(
                "createdAt" to 456
            ),
            42 to mapOf("text" to "ignored"),
            "thread-3" to "invalid"
        )

        assertEquals(
            mapOf(
                "thread-1" to BlockedThreadPreview(
                    text = "Merhaba",
                    createdAt = 123L
                ),
                "thread-2" to BlockedThreadPreview(
                    text = "Henüz mesaj yok.",
                    createdAt = 456L
                )
            ),
            SocialMessageHiddenRangeResolver.readBlockedThreadPreviews(value)
        )
    }

    @Test
    fun readBlockedThreadPreviews_returnsEmptyMapForInvalidValue() {
        assertEquals(
            emptyMap<String, BlockedThreadPreview>(),
            SocialMessageHiddenRangeResolver.readBlockedThreadPreviews("invalid")
        )
    }

    @Test
    fun readMessageHiddenRange_readsRequiredFields() {
        val value = mapOf(
            "userId" to "user-1",
            "startAt" to 100,
            "endAt" to 200L
        )

        assertEquals(
            MessageHiddenRange(
                userId = "user-1",
                startAt = 100L,
                endAt = 200L
            ),
            SocialMessageHiddenRangeResolver.readMessageHiddenRange(value)
        )
    }

    @Test
    fun readMessageHiddenRange_allowsOpenEndedRange() {
        val value = mapOf(
            "userId" to "user-1",
            "startAt" to 100L
        )

        assertEquals(
            MessageHiddenRange(
                userId = "user-1",
                startAt = 100L,
                endAt = null
            ),
            SocialMessageHiddenRangeResolver.readMessageHiddenRange(value)
        )
    }

    @Test
    fun readMessageHiddenRange_returnsNullWhenRequiredFieldsAreMissing() {
        assertNull(SocialMessageHiddenRangeResolver.readMessageHiddenRange(emptyMap<String, Any>()))
        assertNull(
            SocialMessageHiddenRangeResolver.readMessageHiddenRange(
                mapOf("userId" to "user-1")
            )
        )
    }

    @Test
    fun isMessageInHiddenRanges_matchesClosedAndOpenRanges() {
        val ranges = listOf(
            MessageHiddenRange(
                userId = "user-1",
                startAt = 100L,
                endAt = 200L
            ),
            MessageHiddenRange(
                userId = "user-2",
                startAt = 300L,
                endAt = null
            )
        )

        assertTrue(
            SocialMessageHiddenRangeResolver.isMessageInHiddenRanges(
                senderId = "user-1",
                createdAt = 150L,
                hiddenRanges = ranges
            )
        )
        assertTrue(
            SocialMessageHiddenRangeResolver.isMessageInHiddenRanges(
                senderId = "user-2",
                createdAt = 350L,
                hiddenRanges = ranges
            )
        )
        assertFalse(
            SocialMessageHiddenRangeResolver.isMessageInHiddenRanges(
                senderId = "user-1",
                createdAt = 250L,
                hiddenRanges = ranges
            )
        )
        assertFalse(
            SocialMessageHiddenRangeResolver.isMessageInHiddenRanges(
                senderId = "user-3",
                createdAt = 150L,
                hiddenRanges = ranges
            )
        )
    }
}
