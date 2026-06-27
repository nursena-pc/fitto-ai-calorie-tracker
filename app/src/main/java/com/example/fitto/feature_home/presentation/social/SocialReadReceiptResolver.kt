package com.example.fitto.feature_home.presentation.social

internal object SocialReadReceiptResolver {

    fun readByIds(value: Any?): List<String> {
        return when (value) {
            is Map<*, *> -> value.keys.mapNotNull { it as? String }
            is List<*> -> value.mapNotNull { it as? String }
            else -> emptyList()
        }
    }

    fun readByMap(value: Any?): Map<String, Long> {
        return when (value) {
            is Map<*, *> -> value.mapNotNull { entry ->
                val uid = entry.key as? String ?: return@mapNotNull null
                val time = (entry.value as? Number)?.toLong() ?: 0L
                uid to time
            }.toMap()

            is List<*> -> value.mapNotNull { uid ->
                (uid as? String)?.let { it to 0L }
            }.toMap()

            else -> emptyMap()
        }
    }

    fun createReadReceiptText(
        readByOthersCount: Int,
        totalRecipientCount: Int
    ): String {
        if (totalRecipientCount <= 1) {
            return if (readByOthersCount > 0) "Görüldü" else "Gönderildi"
        }

        return if (readByOthersCount > 0) {
            "$readByOthersCount/$totalRecipientCount görüldü"
        } else {
            "Gönderildi"
        }
    }
}
