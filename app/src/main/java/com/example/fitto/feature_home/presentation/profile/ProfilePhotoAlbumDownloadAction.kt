package com.example.fitto.feature_home.presentation.profile

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URL

internal class ProfilePhotoAlbumDownloadAction(
    context: Context,
    private val downloadPolicy: ProfilePhotoAlbumDownloadPolicy = ProfilePhotoAlbumDownloadPolicy()
) {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver

    fun fileNameFor(item: ProfilePhotoAlbumItem): String = downloadPolicy.fileNameFor(item)

    suspend fun saveToGallery(item: ProfilePhotoAlbumItem): Uri = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileNameFor(item))
            put(MediaStore.Images.Media.MIME_TYPE, JPEG_MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + FITTO_DIRECTORY)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed")

        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                openPhotoInputStream(item.imageUrl).use { input ->
                    input.copyTo(output)
                }
            } ?: error("Output stream could not be opened")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            uri
        } catch (exception: Exception) {
            contentResolver.delete(uri, null, null)
            throw exception
        }
    }

    suspend fun copyToDocument(
        item: ProfilePhotoAlbumItem,
        targetUri: Uri
    ) = withContext(Dispatchers.IO) {
        contentResolver.openOutputStream(targetUri)?.use { output ->
            openPhotoInputStream(item.imageUrl).use { input ->
                input.copyTo(output)
            }
        } ?: error("Output stream could not be opened")
    }

    private fun openPhotoInputStream(imageUrl: String): InputStream {
        val uri = Uri.parse(imageUrl)
        return when (uri.scheme.orEmpty()) {
            CONTENT_SCHEME -> contentResolver.openInputStream(uri)
                ?: error("Content uri could not be opened")
            FILE_SCHEME -> File(uri.path.orEmpty()).inputStream()
            HTTP_SCHEME,
            HTTPS_SCHEME -> URL(imageUrl).openStream()
            else -> URL(imageUrl).openStream()
        }
    }

    private companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val FITTO_DIRECTORY = "/Fitto"
        const val CONTENT_SCHEME = "content"
        const val FILE_SCHEME = "file"
        const val HTTP_SCHEME = "http"
        const val HTTPS_SCHEME = "https"
    }
}
