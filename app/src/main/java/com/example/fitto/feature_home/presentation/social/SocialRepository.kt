package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import java.io.File
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.firestore.FieldPath
data class MessageHiddenRange(
    val userId: String,
    val startAt: Long,
    val endAt: Long?
)

data class DirectThreadBlockState(
    val isBlocked: Boolean,
    val hiddenRanges: List<MessageHiddenRange>
)


class SocialRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDatabase = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val profileCoordinator by lazy {
        val querySource = FirebaseSocialProfileQuerySource(
            auth = auth,
            firestore = firestore
        )
        SocialProfileCoordinator(
            querySource = querySource,
            commandSource = FirebaseSocialProfileCommandSource(
                auth = auth,
                firestore = firestore,
                realtimeDatabase = realtimeDatabase,
                storage = storage
            ),
            relationshipSource = FirebaseSocialRelationshipSource(
                auth = auth,
                firestore = firestore,
                ensureCurrentUserProfile = querySource::createCurrentUserProfileIfNeeded,
                loadUsersByIds = querySource::getUsersByIds
            )
        )
    }
    private val conversationObservationCoordinator by lazy {
        SocialConversationObservationCoordinator(
            realtimeSource = FirebaseSocialRealtimeObservationSource(
                auth = auth,
                firestore = firestore,
                realtimeDatabase = realtimeDatabase
            ),
            threadSource = FirebaseSocialThreadObservationSource(
                auth = auth,
                firestore = firestore,
                loadUsersByIds = profileCoordinator::getUsersByIds
            )
        )
    }
    private val postObservationCoordinator by lazy {
        SocialPostObservationCoordinator(
            feedSource = FirebaseSocialFeedObservationSource(
                auth = auth,
                firestore = firestore,
                loadUsersByIds = profileCoordinator::getUsersByIds
            ),
            contentSource = FirebaseSocialPostContentObservationSource(
                auth = auth,
                firestore = firestore,
                loadUsersByIds = profileCoordinator::getUsersByIds
            )
        )
    }
    private val postMutationCoordinator by lazy {
        SocialPostMutationCoordinator(
            commandSource = FirebaseSocialPostCommandSource(
                auth = auth,
                firestore = firestore,
                storage = storage,
                ensureCurrentUserProfile = profileCoordinator::createCurrentUserProfileIfNeeded,
                loadCurrentUserProfile = profileCoordinator::getCurrentUserProfile
            ),
            interactionSource = FirebaseSocialPostInteractionSource(
                auth = auth,
                firestore = firestore,
                ensureCurrentUserProfile = profileCoordinator::createCurrentUserProfileIfNeeded,
                loadCurrentUserProfile = profileCoordinator::getCurrentUserProfile
            )
        )
    }

    fun currentUserId(): String {
        return auth.currentUser?.uid.orEmpty()
    }

    fun observeNotifications(context: Context): Flow<List<FittoNotification>> = callbackFlow {
        // Firestore rules in the current project do not allow a separate socialNotifications collection.
        // Notification history is therefore kept locally by FittoNotificationStore, while message
        // unread counts continue to come from the existing messageThreads model.
        trySend(emptyList())
        awaitClose { }
    }

    fun observeUnreadSocialNotificationCount(): Flow<Int> {
        return observeUnreadMessageCount()
    }

    suspend fun markSocialNotificationRead(notificationId: String) {
        // Local notification read state is handled by FittoNotificationStore in the UI layer.
    }


    fun observeThreadMessagesForSearch(
        threadId: String
    ): Flow<List<GroupMessageSearchResultUiModel>> {
        return conversationObservationCoordinator.observeThreadMessagesForSearch(threadId)
    }

    fun observeUserPresence(userId: String): Flow<PresenceStatusUiModel> {
        return conversationObservationCoordinator.observeUserPresence(userId)
    }

    fun observeThreadSharedContentSummary(
        threadId: String
    ): Flow<ThreadSharedContentSummary> {
        return conversationObservationCoordinator.observeThreadSharedContentSummary(threadId)
    }

    fun observeTypingUsers(threadId: String): Flow<List<TypingUserUiModel>> {
        return conversationObservationCoordinator.observeTypingUsers(threadId)
    }

    fun setTyping(threadId: String, isTyping: Boolean) {
        conversationObservationCoordinator.setTyping(threadId, isTyping)
    }

    fun observeFeed(): Flow<List<SocialPost>> {
        return postObservationCoordinator.observeFeed()
    }

    fun observeComments(postId: String): Flow<List<SocialComment>> {
        return postObservationCoordinator.observeComments(postId)
    }

    fun observeMessageThreads(): Flow<List<MessageThreadUiModel>> {
        return conversationObservationCoordinator.observeMessageThreads()
    }

    fun observeMessages(threadId: String): Flow<List<ChatMessageUiModel>> {
        return conversationObservationCoordinator.observeMessages(threadId)
    }

    suspend fun clearChatForMe(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val messagesSnapshot = firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .get()
            .await()

        if (messagesSnapshot.isEmpty) return

        val batch = firestore.batch()
        val now = System.currentTimeMillis()

        messagesSnapshot.documents.forEach { document ->
            batch.update(
                document.reference,
                SocialMessageDeletionPayloadResolver.createDeleteForMePayload(
                    currentUserId = currentUserId,
                    updatedAt = now
                )
            )
        }

        batch.update(
            firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId),
            mapOf("updatedAt" to now)
        )

        batch.commit().await()
    }
    suspend fun refreshFeedFromServer() {
        postObservationCoordinator.refreshFeedFromServer()
    }

    suspend fun getGroupSettings(threadId: String): GroupSettingsUiModel {
        if (threadId.isBlank()) return GroupSettingsUiModel()

        val snapshot = firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        return SocialGroupSettingsResolver.fromRawMap(
            snapshot.get("settings") as? Map<*, *>
        )
    }

    suspend fun updateGroupSettings(
        threadId: String,
        settings: GroupSettingsUiModel
    ) {
        if (threadId.isBlank()) return

        firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .update(
                mapOf(
                    "settings" to SocialGroupSettingsResolver.toRawMap(settings),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }
    suspend fun getGroupMembers(threadId: String): List<GroupMemberUiModel> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        if (threadId.isBlank()) return emptyList()

        val threadSnapshot = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        if (!threadSnapshot.exists()) return emptyList()

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        return SocialGroupMemberResolver.resolveMembers(
            participantIds = participantIds,
            participants = threadSnapshot.get("participants") as? Map<*, *>,
            currentUserId = currentUserId,
            creatorId = threadSnapshot.getString("creatorId").orEmpty()
        )
    }

    suspend fun updateGroupPhoto(
        threadId: String,
        imageUri: Uri
    ): String {
        val currentUserId = auth.currentUser?.uid ?: return ""
        if (threadId.isBlank()) return ""

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = snapshot.getStringListCompat("participantIds")
        val isGroup = snapshot.getString("type") == THREAD_TYPE_GROUP

        if (!isGroup || !participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val storageRef = storage.reference.child(
            SocialMediaUploadResolver.groupPhotoStoragePath(
                threadId = threadId,
                fileId = UUID.randomUUID().toString()
            )
        )

        storageRef.putFile(imageUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        threadRef.update(
            SocialMediaUploadResolver.groupPhotoUpdatePayload(
                photoUrl = downloadUrl,
                updatedAt = System.currentTimeMillis()
            )
        ).await()

        return downloadUrl
    }

    suspend fun getThreadSharedContent(threadId: String): List<SharedContentUiModel> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        if (threadId.isBlank()) return emptyList()

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val threadSnapshot = threadRef.get().await()

        if (!threadSnapshot.exists()) return emptyList()

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val messagesSnapshot = threadRef
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return messagesSnapshot.documents.flatMap { document ->
            SocialThreadSharedContentResolver.resolve(
                messageId = document.id,
                text = document.getString("text").orEmpty(),
                createdAt = document.getLong("createdAt") ?: 0L,
                senderName = document.getString("senderName").orEmpty(),
                imageUrl = document.getString("imageUrl")
                    ?: document.getString("mediaUrl")
                    ?: document.getString("photoUrl"),
                fileUrl = document.getString("fileUrl"),
                fileName = document.getString("fileName").orEmpty(),
                linkExtractor = SocialLinkResolver::extractLinks
            )
        }
    }

    suspend fun createOrGetDirectThread(targetUserId: String): MessageThreadUiModel? {
        val currentUser = auth.currentUser ?: return null
        if (
            SocialDirectThreadPayloadResolver.shouldSkipTarget(
                currentUserId = currentUser.uid,
                targetUserId = targetUserId
            )
        ) return null

        createCurrentUserProfileIfNeeded()

        val currentProfile = getCurrentUserProfile()
        val targetProfile = getUserProfile(targetUserId) ?: return null

        val currentParticipant = SocialDirectThreadPayloadResolver.currentParticipant(
            uid = currentUser.uid,
            profileUserName = currentProfile?.userName,
            profilePhotoUrl = currentProfile?.photoUrl,
            email = currentUser.email,
            authPhotoUrl = currentUser.photoUrl?.toString()
        )

        val targetParticipant = SocialDirectThreadPayloadResolver.targetParticipant(
            uid = targetUserId,
            userName = targetProfile.userName,
            photoUrl = targetProfile.photoUrl
        )

        val threadId = SocialThreadParticipantResolver.directThreadId(currentUser.uid, targetUserId)
        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val now = System.currentTimeMillis()

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(threadRef)

            if (!snapshot.exists()) {
                transaction.set(
                    threadRef,
                    SocialDirectThreadPayloadResolver.createPayload(
                        threadId = threadId,
                        threadType = THREAD_TYPE_DIRECT,
                        currentParticipant = currentParticipant,
                        targetParticipant = targetParticipant,
                        now = now
                    )
                )
            }

            null
        }.await()

        val threadSnapshot = threadRef.get().await()
        return SocialMessageThreadSnapshotMapper.map(
            document = threadSnapshot,
            currentUserId = currentUser.uid,
            includeClearedThread = true
        )
    }

    suspend fun createGroupThread(
        title: String,
        description: String = "",
        selectedUsers: List<SearchUserUiModel>
    ): MessageThreadUiModel? {
        val currentUser = auth.currentUser ?: return null
        val resolvedInput = SocialGroupThreadPayloadResolver.resolveInput(
            title = title,
            description = description,
            selectedUsers = selectedUsers,
            currentUserId = currentUser.uid
        ) ?: return null

        createCurrentUserProfileIfNeeded()

        val currentProfile = getCurrentUserProfile()
        val currentSender = SocialMessageSenderResolver.resolve(
            profileUserName = currentProfile?.userName,
            profilePhotoUrl = currentProfile?.photoUrl,
            currentUserEmail = currentUser.email,
            currentUserPhotoUrl = currentUser.photoUrl?.toString()
        )

        val threadId = "group_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)

        threadRef.set(
            SocialGroupThreadPayloadResolver.createPayload(
                threadId = threadId,
                currentUserId = currentUser.uid,
                currentUserName = currentSender.name,
                currentPhotoUrl = currentSender.photoUrl,
                input = resolvedInput,
                now = now
            )
        ).await()

        return SocialMessageThreadSnapshotMapper.map(threadRef.get().await(), currentUser.uid)
    }

    suspend fun updateGroupTitle(threadId: String, newTitle: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val updatePayload = SocialGroupMetadataUpdateResolver.createTitleUpdatePayload(
            newTitle = newTitle,
            updatedAt = System.currentTimeMillis()
        ) ?: return
        if (threadId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()
        val participantIds = snapshot.getStringListCompat("participantIds")

        if (!SocialGroupMetadataUpdateResolver.isGroupParticipant(
                threadType = snapshot.getString("type"),
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        threadRef.update(updatePayload).await()
    }
    suspend fun updateGroupDescription(threadId: String, newDescription: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()
        val participantIds = snapshot.getStringListCompat("participantIds")

        if (!SocialGroupMetadataUpdateResolver.isGroupParticipant(
                threadType = snapshot.getString("type"),
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        threadRef.update(
            SocialGroupMetadataUpdateResolver.createDescriptionUpdatePayload(
                newDescription = newDescription,
                updatedAt = System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun updateGroupName(
        threadId: String,
        newName: String
    ) {
        if (threadId.isBlank()) return

        val updatePayload = SocialGroupMetadataUpdateResolver.createLegacyNameUpdatePayload(
            newName = newName,
            updatedAt = System.currentTimeMillis()
        ) ?: return

        firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .update(updatePayload)
            .await()
    }
    suspend fun clearThreadForCurrentUser(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = snapshot.getStringListCompat("participantIds")
        if (!SocialThreadVisibilityPayloadResolver.isParticipant(
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()

        threadRef.update(
            SocialThreadVisibilityPayloadResolver.createClearPayload(
                currentUserId = currentUserId,
                clearedAt = now
            )
        ).await()
    }

    suspend fun deleteThreadForCurrentUser(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = snapshot.getStringListCompat("participantIds")
        if (!SocialThreadVisibilityPayloadResolver.isParticipant(
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()

        threadRef.update(
            SocialThreadVisibilityPayloadResolver.createClearPayload(
                currentUserId = currentUserId,
                clearedAt = now
            )
        ).await()
    }

    suspend fun leaveGroup(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = snapshot.getStringListCompat("participantIds")
        val isGroup = snapshot.getString("type") == THREAD_TYPE_GROUP

        if (!SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = isGroup,
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()

        threadRef.update(
            SocialGroupMembershipPayloadResolver.createRemoveMemberPayload(
                memberId = currentUserId,
                participantIds = participantIds,
                updatedAt = now
            )
        ).await()
    }
    suspend fun removeMemberFromGroup(
        threadId: String,
        memberId: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || memberId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val isGroup = snapshot.getString("type") == THREAD_TYPE_GROUP
        val participantIds = snapshot.getStringListCompat("participantIds")
        val creatorId = snapshot.getString("creatorId").orEmpty()

        if (!SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = isGroup,
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        if (currentUserId != creatorId) {
            throw IllegalStateException("ONLY_CREATOR_CAN_REMOVE_MEMBER")
        }

        if (memberId == creatorId) {
            throw IllegalStateException("CREATOR_CAN_NOT_BE_REMOVED")
        }

        if (!participantIds.contains(memberId)) {
            return
        }

        val now = System.currentTimeMillis()

        threadRef.update(
            SocialGroupMembershipPayloadResolver.createRemoveMemberPayload(
                memberId = memberId,
                participantIds = participantIds,
                updatedAt = now
            )
        ).await()
    }


    suspend fun addMembersToGroup(
        threadId: String,
        selectedUsers: List<SearchUserUiModel>
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || selectedUsers.isEmpty()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val isGroup = snapshot.getString("type") == THREAD_TYPE_GROUP
        val currentParticipantIds = snapshot.getStringListCompat("participantIds")

        if (!SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = isGroup,
                participantIds = currentParticipantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val newUsers = SocialGroupMembershipPayloadResolver.filterNewMembers(
            selectedUsers = selectedUsers,
            currentParticipantIds = currentParticipantIds
        )

        if (newUsers.isEmpty()) return

        val now = System.currentTimeMillis()
        val updates = SocialGroupMembershipPayloadResolver.createAddMembersPayload(
            newUsers = newUsers,
            currentParticipantIds = currentParticipantIds,
            updatedAt = now
        )

        threadRef.update(updates).await()
    }
    suspend fun searchUsersForGroupInvite(
        threadId: String,
        query: String
    ): List<SearchUserUiModel> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        if (threadId.isBlank()) return emptyList()

        val threadSnapshot = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        val participantIds = threadSnapshot.getStringListCompat("participantIds")

        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        return searchUsers(query)
            .filterNot { user -> participantIds.contains(user.id) }
    }

    suspend fun getMessageThread(threadId: String): MessageThreadUiModel? {
        val currentUserId = auth.currentUser?.uid ?: return null
        if (threadId.isBlank()) return null

        return SocialMessageThreadSnapshotMapper.map(
            document = firestore.collection(MESSAGE_THREADS_COLLECTION)
                .document(threadId)
                .get()
                .await(),
            currentUserId = currentUserId
        )
    }

    suspend fun getMessageThreadRawData(threadId: String): Map<String, Any?> {
        val currentUserId = auth.currentUser?.uid ?: return emptyMap()
        if (threadId.isBlank()) return emptyMap()

        val snapshot = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        if (!snapshot.exists()) return emptyMap()

        val participantIds = snapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        return snapshot.data.orEmpty()
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
            val snapshot = firestore.collection("users")
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

    suspend fun sendMessage(
        threadId: String,
        text: String
    ) {
        val currentUser = auth.currentUser ?: return
        val trimmedText = text.trim()

        if (SocialMessageDeliveryResolver.shouldSkipTextMessage(threadId, trimmedText)) return

        createCurrentUserProfileIfNeeded()

        val profile = getCurrentUserProfile()
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
    suspend fun sendFileMessage(
        context: Context,
        threadId: String,
        fileUri: Uri
    ) {
        val currentUser = auth.currentUser ?: return
        if (SocialMessageDeliveryResolver.shouldSkipAttachmentMessage(threadId)) return

        createCurrentUserProfileIfNeeded()

        val profile = getCurrentUserProfile()
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

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child(
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
    suspend fun sendAudioMessage(
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

        createCurrentUserProfileIfNeeded()

        val profile = getCurrentUserProfile()
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

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child(
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
    private data class PickedFileInfo(
        val name: String,
        val mimeType: String?,
        val sizeBytes: Long
    )

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
    suspend fun markThreadRead(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (!SocialMessageReadStateResolver.canMarkThreadRead(currentUserId, threadId)) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val now = System.currentTimeMillis()

        val threadSnapshot = threadRef.get().await()
        if (!threadSnapshot.exists()) return

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        val otherUserId = SocialMessageReadStateResolver.directPeerId(
            threadType = threadSnapshot.getString("type"),
            participantIds = participantIds,
            currentUserId = currentUserId
        )

        var isOtherUserCurrentlyBlocked = false
        var hiddenRanges: List<MessageHiddenRange> = emptyList()

        if (!otherUserId.isNullOrBlank()) {
            val userSnapshot = firestore.collection("users")
                .document(currentUserId)
                .get()
                .await()

            val blockedUserIds = SocialMessageReadStateResolver.blockedUserIds(
                userSnapshot.get("blockedUserIds")
            )

            isOtherUserCurrentlyBlocked = SocialMessageReadStateResolver.isPeerBlocked(
                blockedUserIds = blockedUserIds,
                peerId = otherUserId
            )

            hiddenRanges = SocialMessageReadStateResolver.hiddenRanges(
                completedRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(userSnapshot, threadId),
                activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(userSnapshot, threadId)
            )
        }

        val unreadMessages = threadRef
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .get()
            .await()
            .documents
            .filter { document ->
                SocialThreadReadResolver.shouldMarkMessageAsRead(
                    senderId = document.getString("senderId").orEmpty(),
                    createdAt = document.getLong("createdAt") ?: 0L,
                    readBy = document.get("readBy"),
                    currentUserId = currentUserId,
                    otherUserId = otherUserId,
                    isOtherUserCurrentlyBlocked = isOtherUserCurrentlyBlocked,
                    hiddenRanges = hiddenRanges
                )
            }

        firestore.runBatch { batch ->
            batch.update(
                threadRef,
                SocialThreadReadResolver.createThreadReadPayload(currentUserId)
            )

            unreadMessages.forEach { document ->
                batch.set(
                    document.reference,
                    SocialThreadReadResolver.createMessageReadPayload(
                        readBy = document.get("readBy"),
                        currentUserId = currentUserId,
                        readAt = now
                    ),
                    SetOptions.merge()
                )
            }
        }.await()
    }


    suspend fun deleteMessageForMe(
        threadId: String,
        messageId: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || messageId.isBlank()) return

        val messageRef = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(messageId)

        messageRef.set(
            SocialMessageDeletionPayloadResolver.createDeleteForMePayload(
                currentUserId = currentUserId,
                updatedAt = System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun deleteMessageForEveryone(
        threadId: String,
        messageId: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || messageId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val messageRef = threadRef.collection(MESSAGES_SUBCOLLECTION).document(messageId)
        val messageSnapshot = messageRef.get().await()

        if (!messageSnapshot.exists()) return
        if (messageSnapshot.getString("senderId") != currentUserId) {
            throw IllegalStateException("ONLY_SENDER_CAN_DELETE_FOR_EVERYONE")
        }

        val now = System.currentTimeMillis()
        val messageCreatedAt = messageSnapshot.getLong("createdAt") ?: 0L
        val threadSnapshot = threadRef.get().await()
        val isLastMessage = threadSnapshot.getLong("lastMessageCreatedAt") == messageCreatedAt

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                SocialMessageDeletionPayloadResolver.createDeleteForEveryonePayload(
                    currentUserId = currentUserId,
                    deletedAt = now
                ),
                SetOptions.merge()
            )

            if (isLastMessage) {
                batch.update(
                    threadRef,
                    SocialMessageDeletionPayloadResolver.createLastMessageDeletedPayload(updatedAt = now)
                )
            }
        }.await()
    }

    suspend fun searchUsers(query: String): List<SearchUserUiModel> {
        return profileCoordinator.searchUsers(query)
    }

    suspend fun createCurrentUserProfileIfNeeded() {
        profileCoordinator.createCurrentUserProfileIfNeeded()
    }

    suspend fun getCurrentUserProfile(): SocialUser? {
        return profileCoordinator.getCurrentUserProfile()
    }

    suspend fun getUserProfile(userId: String): SocialUser? {
        return profileCoordinator.getUserProfile(userId)
    }

    suspend fun toggleFollow(targetUser: SocialUser) {
        profileCoordinator.toggleFollow(targetUser)
    }

    suspend fun isUserDeletedOrMissing(userId: String): Boolean {
        return profileCoordinator.isUserDeletedOrMissing(userId)
    }

    suspend fun createPost(
        content: String,
        imageUri: Uri?,
        mealInfo: String?,
        calories: Int?,
        allowComments: Boolean,
        visibility: String,
        tags: List<String>,
        isDraft: Boolean
    ) {
        postMutationCoordinator.createPost(
            SocialPostCreateRequest(
                content = content,
                imageUri = imageUri,
                mealInfo = mealInfo,
                calories = calories,
                allowComments = allowComments,
                visibility = visibility,
                tags = tags,
                isDraft = isDraft
            )
        )
    }

    suspend fun createRecipePost(
        content: String,
        recipeId: String,
        recipeTitle: String,
        recipeImageResId: Int,
        recipeDuration: String,
        recipeServing: String,
        recipeCalories: Int?,
        recipeCategory: String,
        tags: List<String>,
        recipeSourceTag: String,
        recipeDescription: String,
        recipeIngredients: List<String>,
        recipeSteps: List<String>
    ): String? {
        return postMutationCoordinator.createRecipePost(
            SocialRecipePostCreateRequest(
                content = content,
                recipeId = recipeId,
                recipeTitle = recipeTitle,
                recipeImageResId = recipeImageResId,
                recipeDuration = recipeDuration,
                recipeServing = recipeServing,
                recipeCalories = recipeCalories,
                recipeCategory = recipeCategory,
                tags = tags,
                recipeSourceTag = recipeSourceTag,
                recipeDescription = recipeDescription,
                recipeIngredients = recipeIngredients,
                recipeSteps = recipeSteps
            )
        )
    }

    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ) {
        postMutationCoordinator.addComment(
            SocialCommentCreateRequest(
                postId = postId,
                text = text,
                parentCommentId = parentCommentId
            )
        )
    }

    suspend fun toggleCommentLike(
        postId: String,
        comment: SocialComment
    ) {
        postMutationCoordinator.toggleCommentLike(
            postId = postId,
            commentId = comment.id
        )
    }

    suspend fun toggleSave(post: SocialPost) {
        postMutationCoordinator.toggleSave(post.id)
    }

    suspend fun toggleLike(post: SocialPost) {
        postMutationCoordinator.toggleLike(post.id)
    }

    suspend fun getBlockedUsers(): List<SocialUser> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()

        val currentUserSnapshot = firestore.collection("users")
            .document(currentUserId)
            .get()
            .await()

        val blockedUserIds = SocialBlockedUserListResolver.blockedUserIds(
            ids = currentUserSnapshot.getStringListCompat("blockedUserIds"),
            currentUserId = currentUserId
        )

        if (blockedUserIds.isEmpty()) return emptyList()

        val usersById = mutableMapOf<String, SocialUser>()

        SocialBlockedUserListResolver.lookupChunks(blockedUserIds).forEach { chunk ->
            val snapshot = firestore.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.forEach { document ->
                usersById[document.id] = SocialBlockedUserListResolver.resolveBlockedUser(
                    id = document.id,
                    isDeleted = document.getBoolean("isDeleted"),
                    userName = document.getString("userName"),
                    username = document.getString("username"),
                    displayName = document.getString("displayName"),
                    name = document.getString("name"),
                    realName = document.getString("realName"),
                    email = document.getString("email"),
                    photoUrl = document.getString("photoUrl"),
                    bio = document.getString("bio"),
                    followersCount = document.getLong("followersCount"),
                    followingCount = document.getLong("followingCount")
                )
            }
        }

        return SocialBlockedUserListResolver.orderBlockedUsers(
            blockedUserIds = blockedUserIds,
            usersById = usersById
        )
    }

    suspend fun reportPost(
        postId: String,
        reason: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("reports")
            .add(
                SocialModerationPayloadResolver.createPostReportPayload(
                    postId = postId,
                    reporterId = userId,
                    reason = reason,
                    createdAt = System.currentTimeMillis()
                )
            )
            .await()
    }
    suspend fun blockUser(
        targetUserId: String,
        threadId: String? = null,
        previewText: String? = null,
        previewCreatedAt: Long? = null
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (SocialModerationPayloadResolver.shouldSkipTargetAction(currentUserId, targetUserId)) return

        createCurrentUserProfileIfNeeded()

        val userRef = firestore.collection("users").document(currentUserId)
        val now = System.currentTimeMillis()

        userRef.update(
            SocialModerationPayloadResolver.createBlockUserPayload(
                targetUserId = targetUserId,
                threadId = threadId,
                previewText = previewText,
                previewCreatedAt = previewCreatedAt,
                updatedAt = now
            )
        ).await()
    }

    suspend fun unblockUser(
        targetUserId: String,
        threadId: String? = null
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (SocialModerationPayloadResolver.shouldSkipTargetAction(currentUserId, targetUserId)) return

        val userRef = firestore.collection("users").document(currentUserId)
        val snapshot = userRef.get().await()
        val now = System.currentTimeMillis()

        val updates = SocialModerationPayloadResolver.createUnblockBasePayload(
            targetUserId = targetUserId,
            updatedAt = now
        ).toMutableMap()

        fun closeHiddenRangeForThread(targetThreadId: String) {
            updates.putAll(
                SocialModerationPayloadResolver.createCloseBlockedThreadPayload(
                    threadId = targetThreadId,
                    activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(snapshot, targetThreadId),
                    targetUserId = targetUserId,
                    updatedAt = now
                )
            )
        }

        if (!threadId.isNullOrBlank()) {
            closeHiddenRangeForThread(threadId)
        } else {
            val activeRanges = snapshot.get("activeBlockedMessageRanges") as? Map<*, *>

            activeRanges.orEmpty().forEach { entry ->
                val targetThreadId = entry.key as? String ?: return@forEach
                val activeRange = SocialMessageHiddenRangeResolver.readMessageHiddenRange(entry.value) ?: return@forEach

                if (activeRange.userId == targetUserId) {
                    closeHiddenRangeForThread(targetThreadId)
                }
            }

        }

        userRef.update(updates).await()
    }

    suspend fun isUserBlocked(targetUserId: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false

        if (targetUserId.isBlank()) return false

        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .get()
            .await()

        return SocialDirectThreadBlockStateResolver.isBlocked(
            blockedUserIds = snapshot.get("blockedUserIds"),
            targetUserId = targetUserId
        )
    }
    suspend fun getDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): DirectThreadBlockState {
        val currentUserId = auth.currentUser?.uid ?: return DirectThreadBlockState(
            isBlocked = false,
            hiddenRanges = emptyList()
        )

        if (threadId.isBlank() || targetUserId.isBlank()) {
            return DirectThreadBlockState(
                isBlocked = false,
                hiddenRanges = emptyList()
            )
        }

        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .get()
            .await()

        val completedRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(snapshot, threadId)
        val activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(snapshot, threadId)

        return SocialDirectThreadBlockStateResolver.resolve(
            blockedUserIds = snapshot.get("blockedUserIds"),
            targetUserId = targetUserId,
            completedRanges = completedRanges,
            activeRange = activeRange
        )
    }

    fun observeDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): Flow<DirectThreadBlockState> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank() || threadId.isBlank() || targetUserId.isBlank()) {
            trySend(
                DirectThreadBlockState(
                    isBlocked = false,
                    hiddenRanges = emptyList()
                )
            )
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(
                        DirectThreadBlockState(
                            isBlocked = false,
                            hiddenRanges = emptyList()
                        )
                    )
                    return@addSnapshotListener
                }

                val completedRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(snapshot, threadId)
                val activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(snapshot, threadId)

                trySend(
                    SocialDirectThreadBlockStateResolver.resolve(
                        blockedUserIds = snapshot.get("blockedUserIds"),
                        targetUserId = targetUserId,
                        completedRanges = completedRanges,
                        activeRange = activeRange
                    )
                )
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun reportUser(
        targetUserId: String,
        threadId: String,
        reason: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (SocialModerationPayloadResolver.shouldSkipTargetAction(currentUserId, targetUserId)) return

        firestore.collection("reports")
            .add(
                SocialModerationPayloadResolver.createUserReportPayload(
                    targetUserId = targetUserId,
                    reporterId = currentUserId,
                    threadId = threadId,
                    reason = reason,
                    createdAt = System.currentTimeMillis()
                )
            )
            .await()
    }
    fun observeUnreadMessageCount(): Flow<Int> {
        return conversationObservationCoordinator.observeUnreadMessageCount()
    }

    suspend fun startDirectMessage(targetUserId: String): MessageThreadUiModel {
        return createOrGetDirectThread(targetUserId)
            ?: throw IllegalStateException("DIRECT_THREAD_COULD_NOT_BE_CREATED")
    }


    fun observeMyProfilePosts(): Flow<List<SocialPost>> {
        return postObservationCoordinator.observeMyProfilePosts()
    }

    fun observeMyDraftPosts(): Flow<List<SocialPost>> {
        return postObservationCoordinator.observeMyDraftPosts()
    }

    fun observeSavedPosts(): Flow<List<SocialPost>> {
        return postObservationCoordinator.observeSavedPosts()
    }

    fun observePost(postId: String): Flow<SocialPost?> {
        return postObservationCoordinator.observePost(postId)
    }

    suspend fun getPostById(postId: String): SocialPost? {
        return postObservationCoordinator.getPostById(postId)
    }

    suspend fun updateDraftPost(
        draftId: String,
        content: String,
        imageUri: Uri?,
        existingImageUrl: String?,
        mealInfo: String?,
        calories: Int?,
        allowComments: Boolean,
        visibility: String,
        tags: List<String>,
        publish: Boolean
    ) {
        postMutationCoordinator.updateDraftPost(
            SocialDraftPostUpdateRequest(
                draftId = draftId,
                content = content,
                imageUri = imageUri,
                existingImageUrl = existingImageUrl,
                mealInfo = mealInfo,
                calories = calories,
                allowComments = allowComments,
                visibility = visibility,
                tags = tags,
                publish = publish
            )
        )
    }


    suspend fun updateCurrentUserProfile(
        displayName: String,
        bio: String
    ) {
        profileCoordinator.updateCurrentUserProfile(displayName, bio)
    }

    suspend fun updateCurrentUserPhotoUrl(photoUrl: String?) {
        profileCoordinator.updateCurrentUserPhotoUrl(photoUrl)
    }

    suspend fun removeFollower(followerId: String) {
        profileCoordinator.removeFollower(followerId)
    }

    suspend fun stopFollowing(followingId: String) {
        profileCoordinator.stopFollowing(followingId)
    }

    suspend fun uploadCurrentUserProfilePhoto(
        context: Context,
        imageUri: Uri
    ): String {
        return profileCoordinator.uploadCurrentUserProfilePhoto(context, imageUri)
    }

    suspend fun getFollowers(userId: String): List<SocialUser> {
        return profileCoordinator.getFollowers(userId)
    }

    suspend fun getFollowing(userId: String): List<SocialUser> {
        return profileCoordinator.getFollowing(userId)
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    companion object {
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"
        const val THREAD_TYPE_DIRECT = "direct"
        const val THREAD_TYPE_GROUP = "group"

    }
}
