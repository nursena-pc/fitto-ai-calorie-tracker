package com.example.fitto.feature_home.presentation.social

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialThreadCommandSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val ensureCurrentUserProfile: suspend () -> Unit,
    private val loadCurrentUserProfile: suspend () -> SocialUser?,
    private val loadUserProfile: suspend (String) -> SocialUser?
) : SocialThreadCommandSource {

    override suspend fun createOrGetDirectThread(targetUserId: String): MessageThreadUiModel? {
        val currentUser = auth.currentUser ?: return null
        if (
            SocialDirectThreadPayloadResolver.shouldSkipTarget(
                currentUserId = currentUser.uid,
                targetUserId = targetUserId
            )
        ) return null

        ensureCurrentUserProfile()

        val currentProfile = loadCurrentUserProfile()
        val targetProfile = loadUserProfile(targetUserId) ?: return null

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

    override suspend fun createGroupThread(
        title: String,
        description: String,
        selectedUsers: List<SearchUserUiModel>
    ): MessageThreadUiModel? {
        val currentUser = auth.currentUser ?: return null
        val resolvedInput = SocialGroupThreadPayloadResolver.resolveInput(
            title = title,
            description = description,
            selectedUsers = selectedUsers,
            currentUserId = currentUser.uid
        ) ?: return null

        ensureCurrentUserProfile()

        val currentProfile = loadCurrentUserProfile()
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

    override suspend fun updateGroupSettings(
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

    override suspend fun updateGroupPhoto(
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

    override suspend fun updateGroupTitle(threadId: String, newTitle: String) {
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

    override suspend fun updateGroupDescription(threadId: String, newDescription: String) {
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

    override suspend fun updateGroupName(threadId: String, newName: String) {
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

    override suspend fun clearThreadForCurrentUser(threadId: String) {
        updateThreadVisibilityForCurrentUser(threadId)
    }

    override suspend fun deleteThreadForCurrentUser(threadId: String) {
        updateThreadVisibilityForCurrentUser(threadId)
    }

    private suspend fun updateThreadVisibilityForCurrentUser(threadId: String) {
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

        threadRef.update(
            SocialThreadVisibilityPayloadResolver.createClearPayload(
                currentUserId = currentUserId,
                clearedAt = System.currentTimeMillis()
            )
        ).await()
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private companion object {
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val THREAD_TYPE_DIRECT = "direct"
        const val THREAD_TYPE_GROUP = "group"
    }
}
