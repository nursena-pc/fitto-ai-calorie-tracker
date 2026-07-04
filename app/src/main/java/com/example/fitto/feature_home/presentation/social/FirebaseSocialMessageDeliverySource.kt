package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.util.UUID
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialMessageDeliverySource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val ensureCurrentUserProfile: suspend () -> Unit,
    private val loadCurrentUserProfile: suspend () -> SocialUser?,
    private val setTyping: (threadId: String, isTyping: Boolean) -> Unit
) : SocialMessageDeliverySource {

    override suspend fun sendMessage(threadId: String, text: String) {
        val currentUser = auth.currentUser ?: return
        val trimmedText = text.trim()

        if (SocialMessageDeliveryResolver.shouldSkipTextMessage(threadId, trimmedText)) return

        ensureCurrentUserProfile()

        val profile = loadCurrentUserProfile()
        val sender = SocialMessageSenderResolver.resolve(
            profileUserName = profile?.userName,
            profilePhotoUrl = profile?.photoUrl,
            currentUserEmail = currentUser.email,
            currentUserPhotoUrl = currentUser.photoUrl?.toString()
        )

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val threadSnapshot = threadRef.get().await()

        if (!threadSnapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUser.uid)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()
        val messageRef = threadRef.collection(MESSAGES_SUBCOLLECTION).document()

        val blockedRecipientIds = getRecipientsWhoBlockedSender(
            participantIds = participantIds,
            senderId = currentUser.uid
        )

        val threadUpdates = SocialMessageThreadUpdateResolver.createLastMessageUpdates(
            lastMessageText = trimmedText,
            lastMessageSenderId = currentUser.uid,
            lastMessageCreatedAt = now,
            lastMessageType = "text",
            participantIds = participantIds,
            senderId = currentUser.uid,
            blockedRecipientIds = blockedRecipientIds,
            updatedAt = now
        )

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                SocialMessageTextPayloadResolver.createTextMessagePayload(
                    messageId = messageRef.id,
                    threadId = threadId,
                    senderId = currentUser.uid,
                    sender = sender,
                    text = trimmedText,
                    deletedFor = blockedRecipientIds,
                    readAt = now
                )
            )

            batch.update(threadRef, threadUpdates)
        }.await()

        setTyping(threadId, false)
    }

    override suspend fun sendFileMessage(
        context: Context,
        threadId: String,
        fileUri: Uri
    ) {
        val currentUser = auth.currentUser ?: return
        if (SocialMessageDeliveryResolver.shouldSkipAttachmentMessage(threadId)) return

        ensureCurrentUserProfile()

        val profile = loadCurrentUserProfile()
        val sender = SocialMessageSenderResolver.resolve(
            profileUserName = profile?.userName,
            profilePhotoUrl = profile?.photoUrl,
            currentUserEmail = currentUser.email,
            currentUserPhotoUrl = currentUser.photoUrl?.toString()
        )

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val threadSnapshot = threadRef.get().await()

        if (!threadSnapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUser.uid)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val fileInfo = readPickedFileInfo(context, fileUri)
        val now = System.currentTimeMillis()
        val messageRef = threadRef.collection(MESSAGES_SUBCOLLECTION).document()

        val safeFileName = SocialMessageAttachmentPayloadResolver.safeStorageFileName(
            fileName = fileInfo.name,
            fallbackFileName = "file_${System.currentTimeMillis()}"
        )

        val storageRef = storage.reference.child(
            SocialMessageDeliveryResolver.attachmentStoragePath(
                threadId = threadId,
                messageId = messageRef.id,
                fileId = UUID.randomUUID().toString(),
                safeFileName = safeFileName
            )
        )

        val metadata = StorageMetadata.Builder()
            .setContentType(fileInfo.mimeType)
            .build()

        storageRef.putFile(fileUri, metadata).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val lastMessageText = SocialMessageAttachmentPayloadResolver.fileLastMessageText(
            fileName = fileInfo.name,
            mimeType = fileInfo.mimeType
        )
        val blockedRecipientIds = getRecipientsWhoBlockedSender(
            participantIds = participantIds,
            senderId = currentUser.uid
        )

        val threadUpdates = SocialMessageThreadUpdateResolver.createLastMessageUpdates(
            lastMessageText = lastMessageText,
            lastMessageSenderId = currentUser.uid,
            lastMessageCreatedAt = now,
            lastMessageType = "file",
            participantIds = participantIds,
            senderId = currentUser.uid,
            blockedRecipientIds = blockedRecipientIds,
            updatedAt = now
        )

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                SocialMessageAttachmentPayloadResolver.createFileMessagePayload(
                    messageId = messageRef.id,
                    threadId = threadId,
                    senderId = currentUser.uid,
                    sender = sender,
                    fileUrl = downloadUrl,
                    fileName = fileInfo.name,
                    fileMimeType = fileInfo.mimeType,
                    fileSizeBytes = fileInfo.sizeBytes,
                    deletedFor = blockedRecipientIds,
                    readAt = now
                )
            )

            batch.update(threadRef, threadUpdates)
        }.await()

        setTyping(threadId, false)
    }

    override suspend fun sendAudioMessage(
        context: Context,
        threadId: String,
        audioFile: File,
        durationMillis: Long
    ) {
        val currentUser = auth.currentUser ?: return
        if (
            SocialMessageDeliveryResolver.shouldSkipAudioMessage(
                threadId = threadId,
                fileExists = audioFile.exists(),
                fileSizeBytes = audioFile.length()
            )
        ) return

        ensureCurrentUserProfile()

        val profile = loadCurrentUserProfile()
        val sender = SocialMessageSenderResolver.resolve(
            profileUserName = profile?.userName,
            profilePhotoUrl = profile?.photoUrl,
            currentUserEmail = currentUser.email,
            currentUserPhotoUrl = currentUser.photoUrl?.toString()
        )

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val threadSnapshot = threadRef.get().await()

        if (!threadSnapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = threadSnapshot.getStringListCompat("participantIds")

        if (!participantIds.contains(currentUser.uid)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()
        val messageRef = threadRef.collection(MESSAGES_SUBCOLLECTION).document()

        val storageRef = storage.reference.child(
            SocialMessageDeliveryResolver.audioStoragePath(
                threadId = threadId,
                messageId = messageRef.id,
                fileId = UUID.randomUUID().toString()
            )
        )

        val metadata = StorageMetadata.Builder()
            .setContentType(SocialMessageDeliveryResolver.audioContentType())
            .build()

        storageRef.putFile(Uri.fromFile(audioFile), metadata).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val lastMessageText = SocialMessageDeliveryResolver.audioLastMessageText()
        val blockedRecipientIds = getRecipientsWhoBlockedSender(
            participantIds = participantIds,
            senderId = currentUser.uid
        )

        val threadUpdates = SocialMessageThreadUpdateResolver.createLastMessageUpdates(
            lastMessageText = lastMessageText,
            lastMessageSenderId = currentUser.uid,
            lastMessageCreatedAt = now,
            lastMessageType = "audio",
            participantIds = participantIds,
            senderId = currentUser.uid,
            blockedRecipientIds = blockedRecipientIds,
            updatedAt = now
        )

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                SocialMessageAttachmentPayloadResolver.createAudioMessagePayload(
                    messageId = messageRef.id,
                    threadId = threadId,
                    senderId = currentUser.uid,
                    sender = sender,
                    fileUrl = downloadUrl,
                    fileSizeBytes = audioFile.length(),
                    audioDurationMillis = durationMillis,
                    deletedFor = blockedRecipientIds,
                    readAt = now
                )
            )

            batch.update(threadRef, threadUpdates)
        }.await()

        runCatching {
            audioFile.delete()
        }

        setTyping(threadId, false)
    }

    private suspend fun getRecipientsWhoBlockedSender(
        participantIds: List<String>,
        senderId: String
    ): List<String> {
        val blockedRecipientIds = mutableListOf<String>()

        SocialMessageDeliveryResolver.recipientLookupChunks(
            participantIds = participantIds,
            senderId = senderId
        ).forEach { chunk ->
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            val records = snapshot.documents.map { userDocument ->
                SocialMessageRecipientBlockRecord(
                    userId = userDocument.id,
                    blockedUserIds = userDocument.getStringListCompat("blockedUserIds")
                )
            }

            blockedRecipientIds += SocialMessageDeliveryResolver.blockedRecipientIds(
                records = records,
                senderId = senderId
            )
        }

        return blockedRecipientIds.distinct()
    }

    private fun readPickedFileInfo(
        context: Context,
        uri: Uri
    ): PickedFileInfo {
        var fileName = "Dosya"
        var fileSize = 0L

        context.contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex).orEmpty().ifBlank { "Dosya" }
                }

                if (sizeIndex >= 0) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        return PickedFileInfo(
            name = fileName,
            mimeType = context.contentResolver.getType(uri),
            sizeBytes = fileSize
        )
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private data class PickedFileInfo(
        val name: String,
        val mimeType: String?,
        val sizeBytes: Long
    )

    private companion object {
        const val USERS_COLLECTION = "users"
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"
    }
}
