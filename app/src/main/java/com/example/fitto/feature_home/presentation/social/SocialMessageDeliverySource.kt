package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import java.io.File

internal interface SocialMessageDeliverySource {
    suspend fun sendMessage(threadId: String, text: String)

    suspend fun sendFileMessage(
        context: Context,
        threadId: String,
        fileUri: Uri
    )

    suspend fun sendAudioMessage(
        context: Context,
        threadId: String,
        audioFile: File,
        durationMillis: Long
    )
}
