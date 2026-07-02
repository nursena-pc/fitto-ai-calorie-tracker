package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class ProfileDataExportShareCoordinator(
    context: Context,
    private val subject: String,
    private val chooserTitle: String,
    private val launchIntent: (Intent) -> Unit
) {
    private val appContext = context.applicationContext
    private val fileProviderAuthority = "${context.packageName}.fileprovider"

    fun share(file: File) {
        val uri = FileProvider.getUriForFile(
            appContext,
            fileProviderAuthority,
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = JSON_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchIntent(Intent.createChooser(shareIntent, chooserTitle))
    }

    private companion object {
        const val JSON_MIME_TYPE = "application/json"
    }
}
