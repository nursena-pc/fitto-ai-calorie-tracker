package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialMessageMutationCoordinatorTest {

    @Test
    fun sendMessage_routesThreadAndTextToDeliverySource() = runBlocking {
        val deliverySource = FakeDeliverySource()
        val coordinator = createCoordinator(deliverySource = deliverySource)

        coordinator.sendMessage(
            threadId = "thread-1",
            text = "Merhaba"
        )

        assertEquals("thread-1", deliverySource.sentThreadId)
        assertEquals("Merhaba", deliverySource.sentText)
    }

    @Test
    fun clearChatForMe_routesThreadToLifecycleSource() = runBlocking {
        val lifecycleSource = FakeLifecycleSource()
        val coordinator = createCoordinator(lifecycleSource = lifecycleSource)

        coordinator.clearChatForMe("thread-clear")

        assertEquals("thread-clear", lifecycleSource.clearedThreadId)
    }

    @Test
    fun markThreadRead_routesThreadToLifecycleSource() = runBlocking {
        val lifecycleSource = FakeLifecycleSource()
        val coordinator = createCoordinator(lifecycleSource = lifecycleSource)

        coordinator.markThreadRead("thread-read")

        assertEquals("thread-read", lifecycleSource.readThreadId)
    }

    @Test
    fun deleteMessageForMe_routesIdentifiersToLifecycleSource() = runBlocking {
        val lifecycleSource = FakeLifecycleSource()
        val coordinator = createCoordinator(lifecycleSource = lifecycleSource)

        coordinator.deleteMessageForMe(
            threadId = "thread-1",
            messageId = "message-me"
        )

        assertEquals("thread-1", lifecycleSource.deletedForMeThreadId)
        assertEquals("message-me", lifecycleSource.deletedForMeMessageId)
    }

    @Test
    fun deleteMessageForEveryone_routesIdentifiersToLifecycleSource() = runBlocking {
        val lifecycleSource = FakeLifecycleSource()
        val coordinator = createCoordinator(lifecycleSource = lifecycleSource)

        coordinator.deleteMessageForEveryone(
            threadId = "thread-2",
            messageId = "message-all"
        )

        assertEquals("thread-2", lifecycleSource.deletedForEveryoneThreadId)
        assertEquals("message-all", lifecycleSource.deletedForEveryoneMessageId)
    }

    private fun createCoordinator(
        deliverySource: FakeDeliverySource = FakeDeliverySource(),
        lifecycleSource: FakeLifecycleSource = FakeLifecycleSource()
    ): SocialMessageMutationCoordinator {
        return SocialMessageMutationCoordinator(
            deliverySource = deliverySource,
            lifecycleSource = lifecycleSource
        )
    }

    private class FakeDeliverySource : SocialMessageDeliverySource {
        var sentThreadId: String? = null
        var sentText: String? = null

        override suspend fun sendMessage(threadId: String, text: String) {
            sentThreadId = threadId
            sentText = text
        }

        override suspend fun sendFileMessage(
            context: Context,
            threadId: String,
            fileUri: Uri
        ) = Unit

        override suspend fun sendAudioMessage(
            context: Context,
            threadId: String,
            audioFile: File,
            durationMillis: Long
        ) = Unit
    }

    private class FakeLifecycleSource : SocialMessageLifecycleSource {
        var clearedThreadId: String? = null
        var readThreadId: String? = null
        var deletedForMeThreadId: String? = null
        var deletedForMeMessageId: String? = null
        var deletedForEveryoneThreadId: String? = null
        var deletedForEveryoneMessageId: String? = null

        override suspend fun clearChatForMe(threadId: String) {
            clearedThreadId = threadId
        }

        override suspend fun markThreadRead(threadId: String) {
            readThreadId = threadId
        }

        override suspend fun deleteMessageForMe(threadId: String, messageId: String) {
            deletedForMeThreadId = threadId
            deletedForMeMessageId = messageId
        }

        override suspend fun deleteMessageForEveryone(threadId: String, messageId: String) {
            deletedForEveryoneThreadId = threadId
            deletedForEveryoneMessageId = messageId
        }
    }
}
