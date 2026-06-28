package com.example.fitto.feature_home.presentation.social

internal object SocialPostInteractionResolver {

    fun toggleSavePayload(
        savedByValue: Any?,
        userId: String
    ): Map<String, Any> {
        val savedBy = readStringList(savedByValue).toMutableList()

        if (savedBy.contains(userId)) {
            savedBy.remove(userId)
        } else {
            savedBy.add(userId)
        }

        return mapOf("savedBy" to savedBy)
    }

    fun toggleLikePayload(
        likedByValue: Any?,
        currentLikes: Long,
        userId: String
    ): Map<String, Any> {
        val likedBy = readStringList(likedByValue).toMutableList()
        val likesCount = if (likedBy.contains(userId)) {
            likedBy.remove(userId)
            (currentLikes - 1).coerceAtLeast(0)
        } else {
            likedBy.add(userId)
            currentLikes + 1
        }

        return mapOf(
            "likedBy" to likedBy,
            "likesCount" to likesCount
        )
    }

    private fun readStringList(value: Any?): List<String> {
        return (value as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }
}
