package com.example.fitto.feature_home.presentation.social

internal object SocialMediaUploadResolver {

    private const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"

    fun groupPhotoStoragePath(
        threadId: String,
        fileId: String
    ): String {
        return "group_photos/${threadId}/${fileId}.jpg"
    }

    fun profilePhotoStoragePath(
        userId: String,
        nowMillis: Long,
        fileId: String
    ): String {
        return "profile_photos/${userId}/${nowMillis}_${fileId}.jpg"
    }

    fun postImageStoragePath(fileId: String): String {
        return "post_images/${fileId}.jpg"
    }

    fun imageContentType(mimeType: String?): String {
        return mimeType?.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE_MIME_TYPE
    }

    fun groupPhotoUpdatePayload(
        photoUrl: String,
        updatedAt: Long
    ): Map<String, Any> {
        return mapOf(
            "photoUrl" to photoUrl,
            "updatedAt" to updatedAt
        )
    }
}
