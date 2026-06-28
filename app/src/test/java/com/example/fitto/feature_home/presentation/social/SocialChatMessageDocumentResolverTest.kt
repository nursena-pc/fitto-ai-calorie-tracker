package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialChatMessageDocumentResolverTest {

    @Test
    fun resolve_returnsNullWhenMessageWasClearedForCurrentUser() {
        val message = resolveMessage(
            createdAt = 100L,
            currentUserClearedAt = 100L
        )

        assertNull(message)
    }

    @Test
    fun resolve_returnsNullWhenMessageWasDeletedForCurrentUser() {
        val message = resolveMessage(
            deletedFor = listOf("me")
        )

        assertNull(message)
    }

    @Test
    fun resolve_mapsTextMessageWithParticipantFallbacks() {
        val message = resolveMessage(
            senderId = "other",
            senderName = "",
            senderPhotoUrl = null,
            fallbackPhotoUrl = null,
            participantNames = mapOf("other" to "Ayşe"),
            participantPhotoUrls = mapOf("other" to "photo-url"),
            text = "Merhaba"
        )

        requireNotNull(message)
        assertEquals("Merhaba", message.text)
        assertEquals("Ayşe", message.senderName)
        assertEquals("photo-url", message.senderPhotoUrl)
        assertEquals(false, message.isFromMe)
        assertEquals("text", message.type)
    }

    @Test
    fun resolve_mapsOwnMessageReadReceipt() {
        val message = resolveMessage(
            senderId = "me",
            readBy = mapOf(
                "me" to 100L,
                "user-1" to 110L,
                "user-2" to 120L
            ),
            totalRecipientCount = 3
        )

        requireNotNull(message)
        assertEquals(true, message.isFromMe)
        assertEquals(2, message.readByCount)
        assertEquals("2/3 görüldü", message.readReceiptText)
    }

    @Test
    fun resolve_hidesImageFileTextAndUsesFileFallbackForOtherFiles() {
        val imageMessage = resolveMessage(
            messageType = "file",
            fileName = "foto.png",
            fileMimeType = "image/png"
        )
        val documentMessage = resolveMessage(
            messageType = "file",
            fileName = null,
            fileMimeType = "application/pdf"
        )

        requireNotNull(imageMessage)
        requireNotNull(documentMessage)
        assertEquals("", imageMessage.text)
        assertEquals("Dosya", documentMessage.text)
    }

    @Test
    fun resolve_mapsAudioAndDeletedMessages() {
        val audioMessage = resolveMessage(messageType = "audio")
        val deletedMessage = resolveMessage(
            isDeleted = true,
            text = "gizli"
        )

        requireNotNull(audioMessage)
        requireNotNull(deletedMessage)
        assertEquals("Ses mesajı", audioMessage.text)
        assertEquals("Bu mesaj silindi", deletedMessage.text)
    }

    private fun resolveMessage(
        id: String = "message-1",
        createdAt: Long = 200L,
        currentUserId: String = "me",
        currentUserClearedAt: Long = 0L,
        deletedFor: List<String> = emptyList(),
        senderId: String = "other",
        senderName: String? = "Nursena",
        senderPhotoUrl: String? = "sender-photo",
        fallbackPhotoUrl: String? = "fallback-photo",
        participantNames: Map<String, String> = mapOf("other" to "Other User"),
        participantPhotoUrls: Map<String, String?> = mapOf("other" to "participant-photo"),
        readBy: Map<String, Long> = emptyMap(),
        totalRecipientCount: Int = 1,
        isDeleted: Boolean = false,
        messageType: String? = null,
        text: String? = "Mesaj",
        fileName: String? = null,
        fileUrl: String? = null,
        fileMimeType: String? = null,
        fileSizeBytes: Long = 0L,
        audioDurationMillis: Long = 0L,
        deliveredAt: Long? = null,
        deletedAt: Long = 0L,
        deletedBy: String? = null
    ): ChatMessageUiModel? {
        return SocialChatMessageDocumentResolver.resolve(
            id = id,
            createdAt = createdAt,
            currentUserId = currentUserId,
            currentUserClearedAt = currentUserClearedAt,
            deletedFor = deletedFor,
            senderId = senderId,
            senderName = senderName,
            senderPhotoUrl = senderPhotoUrl,
            fallbackPhotoUrl = fallbackPhotoUrl,
            participantNames = participantNames,
            participantPhotoUrls = participantPhotoUrls,
            readBy = readBy,
            totalRecipientCount = totalRecipientCount,
            isDeleted = isDeleted,
            messageType = messageType,
            text = text,
            fileName = fileName,
            fileUrl = fileUrl,
            fileMimeType = fileMimeType,
            fileSizeBytes = fileSizeBytes,
            audioDurationMillis = audioDurationMillis,
            deliveredAt = deliveredAt,
            deletedAt = deletedAt,
            deletedBy = deletedBy
        )
    }
}
