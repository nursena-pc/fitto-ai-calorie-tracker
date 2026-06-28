package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialThreadSharedContentSummaryResolverTest {

    @Test
    fun summarize_countsImageAndVideoMessagesAsMedia() {
        val summary = SocialThreadSharedContentSummaryResolver.summarize(
            listOf(
                message(imageUrl = "https://example.com/photo.jpg"),
                message(attachmentType = "video"),
                message(mimeType = "image/png")
            )
        )

        assertEquals(3, summary.mediaCount)
        assertEquals(0, summary.fileCount)
        assertEquals(0, summary.linkCount)
    }

    @Test
    fun summarize_countsFileOnlyWhenMessageIsNotMedia() {
        val summary = SocialThreadSharedContentSummaryResolver.summarize(
            listOf(
                message(attachmentUrl = "https://example.com/report.pdf"),
                message(attachmentType = "file"),
                message(mimeType = "application/pdf"),
                message(attachmentUrl = "https://example.com/photo.jpg", mimeType = "image/jpeg")
            )
        )

        assertEquals(1, summary.mediaCount)
        assertEquals(3, summary.fileCount)
        assertEquals(0, summary.linkCount)
    }

    @Test
    fun summarize_countsUniqueLinksFromBodyAndExplicitLinkUrl() {
        val summary = SocialThreadSharedContentSummaryResolver.summarize(
            listOf(
                message(
                    text = "https://fitto.app tarif https://fitto.app",
                    linkUrl = "https://example.com"
                ),
                message(
                    content = "www.example.org"
                )
            )
        )

        assertEquals(0, summary.mediaCount)
        assertEquals(0, summary.fileCount)
        assertEquals(3, summary.linkCount)
    }

    @Test
    fun summarize_prefersTextOverContentForLinkCounting() {
        val summary = SocialThreadSharedContentSummaryResolver.summarize(
            listOf(
                message(
                    text = "https://text.example",
                    content = "https://content.example"
                )
            )
        )

        assertEquals(1, summary.linkCount)
    }

    private fun message(
        text: String = "",
        content: String = "",
        attachmentUrl: String? = null,
        attachmentType: String = "",
        mimeType: String = "",
        imageUrl: String? = null,
        videoUrl: String? = null,
        linkUrl: String? = null
    ): SocialThreadSharedContentSummaryResolver.Message {
        return SocialThreadSharedContentSummaryResolver.Message(
            text = text,
            content = content,
            attachmentUrl = attachmentUrl,
            attachmentType = attachmentType,
            mimeType = mimeType,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            linkUrl = linkUrl
        )
    }
}
