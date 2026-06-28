package com.example.fitto.feature_home.presentation.social

internal object SocialThreadSharedContentSummaryResolver {

    private val webUrlRegex = Regex(
        pattern = """(?i)\b((?:https?://|www\.)[^\s]+)"""
    )

    data class Message(
        val text: String,
        val content: String,
        val attachmentUrl: String?,
        val attachmentType: String,
        val mimeType: String,
        val imageUrl: String?,
        val videoUrl: String?,
        val linkUrl: String?
    )

    fun summarize(messages: List<Message>): ThreadSharedContentSummary {
        var mediaCount = 0
        var linkCount = 0
        var fileCount = 0

        messages.forEach { message ->
            if (message.hasMedia()) mediaCount++
            if (message.hasFile()) fileCount++
            linkCount += message.linkCount()
        }

        return ThreadSharedContentSummary(
            mediaCount = mediaCount,
            linkCount = linkCount,
            fileCount = fileCount
        )
    }

    private fun Message.hasMedia(): Boolean {
        val normalizedAttachmentType = attachmentType.lowercase()
        val normalizedMimeType = mimeType.lowercase()

        return !imageUrl.isNullOrBlank() ||
                !videoUrl.isNullOrBlank() ||
                normalizedAttachmentType == "image" ||
                normalizedAttachmentType == "video" ||
                normalizedAttachmentType == "media" ||
                normalizedMimeType.startsWith("image/") ||
                normalizedMimeType.startsWith("video/")
    }

    private fun Message.hasFile(): Boolean {
        val normalizedAttachmentType = attachmentType.lowercase()
        val normalizedMimeType = mimeType.lowercase()

        return !hasMedia() && (
                normalizedAttachmentType == "file" ||
                        !attachmentUrl.isNullOrBlank() ||
                        (
                                normalizedMimeType.isNotBlank() &&
                                        !normalizedMimeType.startsWith("image/") &&
                                        !normalizedMimeType.startsWith("video/")
                                )
                )
    }

    private fun Message.linkCount(): Int {
        val body = if (text.isNotBlank()) text else content
        val linksInMessage = mutableSetOf<String>()

        if (body.isNotBlank()) {
            webUrlRegex.findAll(body).forEach { match ->
                val url = match.value
                if (url.isNotBlank()) {
                    linksInMessage.add(url)
                }
            }
        }

        if (!linkUrl.isNullOrBlank()) {
            linksInMessage.add(linkUrl)
        }

        return linksInMessage.size
    }
}


