package com.example.fitto.feature_home.presentation.social

internal object SocialThreadSharedContentResolver {

    fun resolve(
        messageId: String,
        text: String,
        createdAt: Long,
        senderName: String,
        imageUrl: String?,
        fileUrl: String?,
        fileName: String,
        linkExtractor: (String) -> List<String>
    ): List<SharedContentUiModel> {
        val subtitleName = senderName
        val sharedItems = mutableListOf<SharedContentUiModel>()

        linkExtractor(text).forEachIndexed { index, link ->
            sharedItems.add(
                SharedContentUiModel(
                    id = "${messageId}_link_$index",
                    type = SharedContentType.LINK,
                    title = link,
                    subtitle = subtitleName.ifBlank { "Mesaj içinden bağlantı" },
                    url = link,
                    createdAt = createdAt
                )
            )
        }

        if (!imageUrl.isNullOrBlank()) {
            sharedItems.add(
                SharedContentUiModel(
                    id = "${messageId}_media",
                    type = SharedContentType.MEDIA,
                    title = "Medya",
                    subtitle = subtitleName.ifBlank { "Paylaşılan medya" },
                    url = imageUrl,
                    createdAt = createdAt
                )
            )
        }

        if (!fileUrl.isNullOrBlank()) {
            sharedItems.add(
                SharedContentUiModel(
                    id = "${messageId}_file",
                    type = SharedContentType.FILE,
                    title = fileName.ifBlank { "Dosya" },
                    subtitle = subtitleName.ifBlank { "Paylaşılan dosya" },
                    url = fileUrl,
                    createdAt = createdAt
                )
            )
        }

        return sharedItems
    }
}
