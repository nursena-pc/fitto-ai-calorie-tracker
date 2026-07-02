package com.example.fitto.feature_home.presentation.profile

internal sealed interface ProfilePhotoAlbumDownloadDestination {
    data object MediaStore : ProfilePhotoAlbumDownloadDestination
    data class Document(val fileName: String) : ProfilePhotoAlbumDownloadDestination
}

internal class ProfilePhotoAlbumDownloadPolicy(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    fun destinationFor(
        item: ProfilePhotoAlbumItem,
        sdkInt: Int,
        mediaStoreMinSdk: Int
    ): ProfilePhotoAlbumDownloadDestination {
        return if (sdkInt >= mediaStoreMinSdk) {
            ProfilePhotoAlbumDownloadDestination.MediaStore
        } else {
            ProfilePhotoAlbumDownloadDestination.Document(fileNameFor(item))
        }
    }

    fun fileNameFor(item: ProfilePhotoAlbumItem): String {
        val safeSuffix = item.id
            .replace(INVALID_FILE_NAME_CHARACTERS, "_")
            .take(MAX_ID_SUFFIX_LENGTH)
            .ifBlank { DEFAULT_ID_SUFFIX }

        return "fitto_${currentTimeMillis()}_$safeSuffix.jpg"
    }

    private companion object {
        val INVALID_FILE_NAME_CHARACTERS = Regex("[^A-Za-z0-9_-]")
        const val MAX_ID_SUFFIX_LENGTH = 24
        const val DEFAULT_ID_SUFFIX = "photo"
    }
}
