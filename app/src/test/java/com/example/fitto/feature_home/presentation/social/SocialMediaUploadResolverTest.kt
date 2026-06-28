package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialMediaUploadResolverTest {

    @Test
    fun groupPhotoStoragePath_buildsStablePath() {
        assertEquals(
            "group_photos/thread-1/file-1.jpg",
            SocialMediaUploadResolver.groupPhotoStoragePath(
                threadId = "thread-1",
                fileId = "file-1"
            )
        )
    }

    @Test
    fun profilePhotoStoragePath_includesUserTimestampAndFileId() {
        assertEquals(
            "profile_photos/user-1/123_file-1.jpg",
            SocialMediaUploadResolver.profilePhotoStoragePath(
                userId = "user-1",
                nowMillis = 123L,
                fileId = "file-1"
            )
        )
    }

    @Test
    fun postImageStoragePath_buildsStablePath() {
        assertEquals(
            "post_images/file-1.jpg",
            SocialMediaUploadResolver.postImageStoragePath(fileId = "file-1")
        )
    }

    @Test
    fun imageContentType_fallsBackToJpegForBlankValues() {
        assertEquals("image/jpeg", SocialMediaUploadResolver.imageContentType(null))
        assertEquals("image/jpeg", SocialMediaUploadResolver.imageContentType("   "))
        assertEquals("image/png", SocialMediaUploadResolver.imageContentType("image/png"))
    }

    @Test
    fun groupPhotoUpdatePayload_containsPhotoAndUpdatedAt() {
        assertEquals(
            mapOf(
                "photoUrl" to "https://example.com/photo.jpg",
                "updatedAt" to 456L
            ),
            SocialMediaUploadResolver.groupPhotoUpdatePayload(
                photoUrl = "https://example.com/photo.jpg",
                updatedAt = 456L
            )
        )
    }
}
