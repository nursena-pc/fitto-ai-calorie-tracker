package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialThreadSharedContentResolverTest {

    @Test
    fun resolve_mapsLinksWithStableIds() {
        val items = SocialThreadSharedContentResolver.resolve(
            messageId = "message-1",
            text = "hello links",
            createdAt = 123L,
            senderName = "Nursena",
            imageUrl = null,
            fileUrl = null,
            fileName = "",
            linkExtractor = { listOf("https://fitto.app", "https://example.com") }
        )

        assertEquals(2, items.size)
        assertEquals("message-1_link_0", items[0].id)
        assertEquals(SharedContentType.LINK, items[0].type)
        assertEquals("https://fitto.app", items[0].title)
        assertEquals("Nursena", items[0].subtitle)
        assertEquals(123L, items[0].createdAt)
        assertEquals("message-1_link_1", items[1].id)
    }

    @Test
    fun resolve_mapsMediaAndFileWithFallbackSubtitlesAndTitle() {
        val items = SocialThreadSharedContentResolver.resolve(
            messageId = "message-2",
            text = "",
            createdAt = 456L,
            senderName = "   ",
            imageUrl = "https://cdn.example/image.jpg",
            fileUrl = "https://cdn.example/file.pdf",
            fileName = "",
            linkExtractor = { emptyList() }
        )

        assertEquals(2, items.size)
        assertEquals(
            SharedContentUiModel(
                id = "message-2_media",
                type = SharedContentType.MEDIA,
                title = "Medya",
                subtitle = "Paylaşılan medya",
                url = "https://cdn.example/image.jpg",
                createdAt = 456L
            ),
            items[0]
        )
        assertEquals(
            SharedContentUiModel(
                id = "message-2_file",
                type = SharedContentType.FILE,
                title = "Dosya",
                subtitle = "Paylaşılan dosya",
                url = "https://cdn.example/file.pdf",
                createdAt = 456L
            ),
            items[1]
        )
    }

    @Test
    fun resolve_ignoresBlankMediaAndFileUrls() {
        val items = SocialThreadSharedContentResolver.resolve(
            messageId = "message-3",
            text = "",
            createdAt = 0L,
            senderName = "Sender",
            imageUrl = "   ",
            fileUrl = "",
            fileName = "report.pdf",
            linkExtractor = { emptyList() }
        )

        assertEquals(emptyList<SharedContentUiModel>(), items)
    }
}
