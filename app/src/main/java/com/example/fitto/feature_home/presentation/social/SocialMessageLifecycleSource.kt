package com.example.fitto.feature_home.presentation.social

internal interface SocialMessageLifecycleSource {
    suspend fun clearChatForMe(threadId: String)

    suspend fun markThreadRead(threadId: String)

    suspend fun deleteMessageForMe(threadId: String, messageId: String)

    suspend fun deleteMessageForEveryone(threadId: String, messageId: String)
}
