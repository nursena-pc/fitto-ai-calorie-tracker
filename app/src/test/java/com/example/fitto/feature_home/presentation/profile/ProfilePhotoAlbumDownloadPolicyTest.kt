package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePhotoAlbumDownloadPolicyTest {

    private val policy = ProfilePhotoAlbumDownloadPolicy(currentTimeMillis = { 1234L })

    @Test
    fun `modern android uses media store`() {
        val destination = policy.destinationFor(item("post"), sdkInt = 29, mediaStoreMinSdk = 29)

        assertTrue(destination is ProfilePhotoAlbumDownloadDestination.MediaStore)
    }

    @Test
    fun `legacy android requests a named document`() {
        val destination = policy.destinationFor(item("meal post"), sdkInt = 28, mediaStoreMinSdk = 29)
            as ProfilePhotoAlbumDownloadDestination.Document

        assertEquals("fitto_1234_meal_post.jpg", destination.fileName)
    }

    @Test
    fun `file name sanitizes and limits item identifier`() {
        val fileName = policy.fileNameFor(item("***abcdefghijklmnopqrstuvwxyz0123456789"))

        assertEquals("fitto_1234____abcdefghijklmnopqrstu.jpg", fileName)
    }

    private fun item(id: String) = ProfilePhotoAlbumItem(
        id = id,
        imageUrl = "photo.jpg",
        sourceTitle = "Photo",
        dateKey = ProfilePhotoAlbumFilter.UNDATED,
        dateText = "Undated",
        createdAt = 0L
    )
}
