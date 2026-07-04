package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import java.io.File

internal class SocialMessageMutationCoordinator(
    private val deliverySource: SocialMessageDeliverySource,
    private val lifecycleSource: SocialMessageLifecycleSource
) {
    suspend fun sendMessage(threadId: String, text: String) {
        deliverySource.sendMessage(threadId, text)
    }

    suspend fun sendFileMessage(
        context: Context,
        threadId: String,
        fileUri: Uri
    ) {
        deliverySource.sendFileMessage(context, threadId, fileUri)
    }

    suspend fun sendAudioMessage(
        context: Context,
        threadId: String,
        audioFile: File,
        durationMillis: Long
    ) {
        deliverySource.sendAudioMessage(
            context = context,
            threadId = threadId,
            audioFile = audioFile,
            durationMillis = durationMillis
        )
    }

    suspend fun clearChatForMe(threadId: String) {
        lifecycleSource.clearChatForMe(threadId)
    }

    suspend fun markThreadRead(threadId: String) {
        lifecycleSource.markThreadRead(threadId)
    }

    suspend fun deleteMessageForMe(threadId: String, messageId: String) {
        lifecycleSource.deleteMessageForMe(threadId, messageId)
    }

    suspend fun deleteMessageForEveryone(threadId: String, messageId: String) {
        lifecycleSource.deleteMessageForEveryone(threadId, messageId)
    }
}
