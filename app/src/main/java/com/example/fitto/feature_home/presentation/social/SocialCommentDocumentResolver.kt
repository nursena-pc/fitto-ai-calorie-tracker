package com.example.fitto.feature_home.presentation.social

internal object SocialCommentDocumentResolver {

    fun resolve(
        id: String,
        postId: String,
        currentUserId: String,
        userId: String?,
        userName: String?,
        userPhotoUrl: String?,
        fallbackPhotoUrl: String?,
        text: String?,
        parentCommentId: String?,
        likesCount: Long?,
        replyCount: Long?,
        likedBy: Any?,
        createdAt: Long?
    ): SocialComment {
        val likedByIds = when (likedBy) {
            is List<*> -> likedBy.mapNotNull { it as? String }
            else -> emptyList()
        }

        return SocialComment(
            id = id,
            postId = postId,
            userId = userId.orEmpty(),
            userName = userName.orEmpty().ifBlank { "fitto_user" },
            userPhotoUrl = userPhotoUrl ?: fallbackPhotoUrl,
            text = text.orEmpty(),
            parentCommentId = parentCommentId,
            likesCount = likesCount?.toInt() ?: 0,
            replyCount = replyCount?.toInt() ?: 0,
            likedByMe = likedByIds.contains(currentUserId),
            createdAt = createdAt ?: 0L
        )
    }
}
