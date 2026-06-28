package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialMessageAttachmentPayloadResolverTest {

    @Test
    fun safeStorageFileName_replacesUnsafeCharacters() {
        assertEquals(
            "my_file_1_.pdf",
            SocialMessageAttachmentPayloadResolver.safeStorageFileName(
                fileName = "my file(1).pdf",
                fallbackFileName = "fallback"
            )
        )
    }

    @Test
    fun safeStorageFileName_usesFallbackWhenBlankAfterSanitizing() {
        assertEquals(
            "fallback-file",
            SocialMessageAttachmentPayloadResolver.safeStorageFileName(
                fileName = "",
                fallbackFileName = "fallback-file"
            )
        )
    }

    @Test
    fun fileLastMessageText_formatsImageAndFileMessages() {
        assertEquals(
            "🖼️ Fotoğraf",
            SocialMessageAttachmentPayloadResolver.fileLastMessageText(
                fileName = "photo.jpg",
                mimeType = "image/jpeg"
            )
        )

        assertEquals(
            "📎 report.pdf",
            SocialMessageAttachmentPayloadResolver.fileLastMessageText(
                fileName = "report.pdf",
                mimeType = "application/pdf"
            )
        )
    }

    @Test
    fun createFileMessagePayload_preservesFileFields() {
        val payload = SocialMessageAttachmentPayloadResolver.createFileMessagePayload(
            messageId = "message-1",
            threadId = "thread-1",
            senderId = "sender-1",
            sender = SocialMessageSender("nursena", "photo"),
            fileUrl = "file-url",
            fileName = "report.pdf",
            fileMimeType = "application/pdf",
            fileSizeBytes = 1200L,
            deletedFor = listOf("blocked-1"),
            readAt = 100L
        )

        assertEquals("message-1", payload["id"])
        assertEquals("thread-1", payload["threadId"])
        assertEquals("sender-1", payload["senderId"])
        assertEquals("nursena", payload["senderName"])
        assertEquals("photo", payload["senderPhotoUrl"])
        assertEquals("photo", payload["photoUrl"])
        assertEquals("report.pdf", payload["text"])
        assertEquals("file", payload["type"])
        assertEquals("file-url", payload["fileUrl"])
        assertEquals(listOf("blocked-1"), payload["deletedFor"])
        assertEquals("report.pdf", payload["fileName"])
        assertEquals("application/pdf", payload["fileMimeType"])
        assertEquals(1200L, payload["fileSizeBytes"])
        assertEquals(mapOf("sender-1" to 100L), payload["readBy"])
        assertEquals(100L, payload["createdAt"])
        assertEquals(100L, payload["deliveredAt"])
    }

    @Test
    fun createAudioMessagePayload_preservesAudioFields() {
        val payload = SocialMessageAttachmentPayloadResolver.createAudioMessagePayload(
            messageId = "message-1",
            threadId = "thread-1",
            senderId = "sender-1",
            sender = SocialMessageSender("nursena", null),
            fileUrl = "audio-url",
            fileSizeBytes = 3400L,
            audioDurationMillis = 5000L,
            deletedFor = emptyList(),
            readAt = 200L
        )

        assertEquals("Ses mesajı", payload["text"])
        assertEquals("audio", payload["type"])
        assertEquals("audio-url", payload["fileUrl"])
        assertEquals("voice_message.m4a", payload["fileName"])
        assertEquals("audio/mp4", payload["fileMimeType"])
        assertEquals(3400L, payload["fileSizeBytes"])
        assertEquals(5000L, payload["audioDurationMillis"])
        assertEquals(mapOf("sender-1" to 200L), payload["readBy"])
        assertEquals(200L, payload["createdAt"])
        assertEquals(200L, payload["deliveredAt"])
    }
}
