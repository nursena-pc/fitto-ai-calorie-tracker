package com.example.fitto.feature_home.presentation.social

import kotlin.math.abs

internal object SocialTypingUserResolver {

    data class TypingRecord(
        val key: String?,
        val uid: String?,
        val isTyping: Boolean,
        val updatedAt: Long,
        val userName: String?,
        val photoUrl: String?
    )

    fun resolve(
        records: List<TypingRecord>,
        currentUserId: String,
        nowMillis: Long,
        staleMillis: Long
    ): List<TypingUserUiModel> {
        if (currentUserId.isBlank()) return emptyList()

        return records.mapNotNull { record ->
            val uid = record.uid ?: record.key ?: return@mapNotNull null
            if (uid == currentUserId) return@mapNotNull null
            if (!record.isTyping) return@mapNotNull null
            if (record.updatedAt > 0L && abs(nowMillis - record.updatedAt) > staleMillis) {
                return@mapNotNull null
            }

            TypingUserUiModel(
                uid = uid,
                userName = record.userName ?: "fitto_user",
                photoUrl = record.photoUrl,
                updatedAt = record.updatedAt
            )
        }
    }
}
