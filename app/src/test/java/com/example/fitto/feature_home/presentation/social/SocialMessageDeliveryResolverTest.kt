package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMessageDeliveryResolverTest {

    @Test
    fun shouldSkipTextMessage_requiresThreadAndNonBlankText() {
        assertTrue(SocialMessageDeliveryResolver.shouldSkipTextMessage("", "hello"))
        assertTrue(SocialMessageDeliveryResolver.shouldSkipTextMessage("thread-1", "   "))
        assertFalse(SocialMessageDeliveryResolver.shouldSkipTextMessage("thread-1", " hello "))
    }

    @Test
    fun shouldSkipAttachmentMessage_requiresThread() {
        assertTrue(SocialMessageDeliveryResolver.shouldSkipAttachmentMessage(""))
        assertTrue(SocialMessageDeliveryResolver.shouldSkipAttachmentMessage("   "))
        assertFalse(SocialMessageDeliveryResolver.shouldSkipAttachmentMessage("thread-1"))
    }

    @Test
    fun shouldSkipAudioMessage_requiresThreadAndExistingNonEmptyFile() {
        assertTrue(
            SocialMessageDeliveryResolver.shouldSkipAudioMessage(
                threadId = "",
                fileExists = true,
                fileSizeBytes = 120L
            )
        )
        assertTrue(
            SocialMessageDeliveryResolver.shouldSkipAudioMessage(
                threadId = "thread-1",
                fileExists = false,
                fileSizeBytes = 120L
            )
        )
        assertTrue(
            SocialMessageDeliveryResolver.shouldSkipAudioMessage(
                threadId = "thread-1",
                fileExists = true,
                fileSizeBytes = 0L
            )
        )
        assertFalse(
            SocialMessageDeliveryResolver.shouldSkipAudioMessage(
                threadId = "thread-1",
                fileExists = true,
                fileSizeBytes = 120L
            )
        )
    }

    @Test
    fun recipientIds_removesSenderBlankAndDuplicateParticipants() {
        val recipients = SocialMessageDeliveryResolver.recipientIds(
            participantIds = listOf("sender-1", " user-1 ", "", "user-2", "user-1", "sender-1"),
            senderId = "sender-1"
        )

        assertEquals(listOf("user-1", "user-2"), recipients)
    }

    @Test
    fun recipientLookupChunks_usesFirestoreWhereInLimit() {
        val participantIds = listOf("sender-1") + (1..21).map { index -> "user-$index" }

        val chunks = SocialMessageDeliveryResolver.recipientLookupChunks(
            participantIds = participantIds,
            senderId = "sender-1"
        )

        assertEquals(listOf(10, 10, 1), chunks.map { it.size })
        assertEquals("user-1", chunks.first().first())
        assertEquals("user-21", chunks.last().last())
    }

    @Test
    fun blockedRecipientIds_returnsRecipientsThatBlockedSender() {
        val blockedRecipientIds = SocialMessageDeliveryResolver.blockedRecipientIds(
            records = listOf(
                SocialMessageRecipientBlockRecord(
                    userId = "user-1",
                    blockedUserIds = listOf("sender-1")
                ),
                SocialMessageRecipientBlockRecord(
                    userId = "user-2",
                    blockedUserIds = listOf("other-user")
                ),
                SocialMessageRecipientBlockRecord(
                    userId = "user-1",
                    blockedUserIds = listOf("sender-1")
                )
            ),
            senderId = "sender-1"
        )

        assertEquals(listOf("user-1"), blockedRecipientIds)
    }

    @Test
    fun attachmentStoragePath_buildsStableChatAttachmentPath() {
        val path = SocialMessageDeliveryResolver.attachmentStoragePath(
            threadId = "thread-1",
            messageId = "message-1",
            fileId = "file-id",
            safeFileName = "report.pdf"
        )

        assertEquals("chat_attachments/thread-1/message-1/file-id_report.pdf", path)
    }

    @Test
    fun audioStoragePath_andMetadataValues_useVoiceMessageDefaults() {
        val path = SocialMessageDeliveryResolver.audioStoragePath(
            threadId = "thread-1",
            messageId = "message-1",
            fileId = "audio-id"
        )

        assertEquals("chat_attachments/thread-1/message-1/audio-id_voice_message.m4a", path)
        assertEquals("audio/mp4", SocialMessageDeliveryResolver.audioContentType())
        assertEquals("🎤 Ses mesajı", SocialMessageDeliveryResolver.audioLastMessageText())
    }
}
