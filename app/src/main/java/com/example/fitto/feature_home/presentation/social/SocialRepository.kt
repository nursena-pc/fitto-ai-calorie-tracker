package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
    private val messageMutationCoordinator by lazy {
        SocialMessageMutationCoordinator(
            deliverySource = FirebaseSocialMessageDeliverySource(
                auth = auth,
                firestore = firestore,
                storage = storage,
                ensureCurrentUserProfile = profileCoordinator::createCurrentUserProfileIfNeeded,
                loadCurrentUserProfile = profileCoordinator::getCurrentUserProfile,
                setTyping = conversationObservationCoordinator::setTyping
            ),
            lifecycleSource = FirebaseSocialMessageLifecycleSource(
                auth = auth,
                firestore = firestore
            )
        )
    }
    private val threadManagementCoordinator by lazy {
        SocialThreadManagementCoordinator(
            querySource = FirebaseSocialThreadQuerySource(
                auth = auth,
                firestore = firestore,
                searchUsers = profileCoordinator::searchUsers
            ),
            commandSource = FirebaseSocialThreadCommandSource(
                auth = auth,
                firestore = firestore,
                storage = storage,
                ensureCurrentUserProfile = profileCoordinator::createCurrentUserProfileIfNeeded,
                loadCurrentUserProfile = profileCoordinator::getCurrentUserProfile,
                loadUserProfile = profileCoordinator::getUserProfile
            ),
            membershipSource = FirebaseSocialGroupMembershipSource(
                auth = auth,
                firestore = firestore
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
        messageMutationCoordinator.clearChatForMe(threadId)
    }

    suspend fun refreshFeedFromServer() {
        postObservationCoordinator.refreshFeedFromServer()
    }

    suspend fun getGroupSettings(threadId: String): GroupSettingsUiModel {
        return threadManagementCoordinator.getGroupSettings(threadId)
    }

    suspend fun updateGroupSettings(
        threadId: String,
        settings: GroupSettingsUiModel
    ) {
        threadManagementCoordinator.updateGroupSettings(threadId, settings)
    }

    suspend fun getGroupMembers(threadId: String): List<GroupMemberUiModel> {
        return threadManagementCoordinator.getGroupMembers(threadId)
    }

    suspend fun updateGroupPhoto(
        threadId: String,
        imageUri: Uri
    ): String {
        return threadManagementCoordinator.updateGroupPhoto(threadId, imageUri)
    }

    suspend fun getThreadSharedContent(threadId: String): List<SharedContentUiModel> {
        return threadManagementCoordinator.getThreadSharedContent(threadId)
    }

    suspend fun createOrGetDirectThread(targetUserId: String): MessageThreadUiModel? {
        return threadManagementCoordinator.createOrGetDirectThread(targetUserId)
    }

    suspend fun createGroupThread(
        title: String,
        description: String = "",
        selectedUsers: List<SearchUserUiModel>
    ): MessageThreadUiModel? {
        return threadManagementCoordinator.createGroupThread(
            title = title,
            description = description,
            selectedUsers = selectedUsers
        )
    }

    suspend fun updateGroupTitle(threadId: String, newTitle: String) {
        threadManagementCoordinator.updateGroupTitle(threadId, newTitle)
    }

    suspend fun updateGroupDescription(threadId: String, newDescription: String) {
        threadManagementCoordinator.updateGroupDescription(threadId, newDescription)
    }

    suspend fun updateGroupName(
        threadId: String,
        newName: String
    ) {
        threadManagementCoordinator.updateGroupName(threadId, newName)
    }

    suspend fun clearThreadForCurrentUser(threadId: String) {
        threadManagementCoordinator.clearThreadForCurrentUser(threadId)
    }

    suspend fun deleteThreadForCurrentUser(threadId: String) {
        threadManagementCoordinator.deleteThreadForCurrentUser(threadId)
    }

    suspend fun leaveGroup(threadId: String) {
        threadManagementCoordinator.leaveGroup(threadId)
    }

    suspend fun removeMemberFromGroup(
        threadId: String,
        memberId: String
    ) {
        threadManagementCoordinator.removeMemberFromGroup(threadId, memberId)
    }

    suspend fun addMembersToGroup(
        threadId: String,
        selectedUsers: List<SearchUserUiModel>
    ) {
        threadManagementCoordinator.addMembersToGroup(threadId, selectedUsers)
    }

    suspend fun searchUsersForGroupInvite(
        threadId: String,
        query: String
    ): List<SearchUserUiModel> {
        return threadManagementCoordinator.searchUsersForGroupInvite(threadId, query)
    }

    suspend fun getMessageThread(threadId: String): MessageThreadUiModel? {
        return threadManagementCoordinator.getMessageThread(threadId)
    }

    suspend fun getMessageThreadRawData(threadId: String): Map<String, Any?> {
        return threadManagementCoordinator.getMessageThreadRawData(threadId)
    }

    suspend fun sendMessage(
        threadId: String,
        text: String
    ) {
        messageMutationCoordinator.sendMessage(threadId, text)
    }

    suspend fun sendFileMessage(
        context: Context,
        threadId: String,
        fileUri: Uri
    ) {
        messageMutationCoordinator.sendFileMessage(
            context = context,
            threadId = threadId,
            fileUri = fileUri
        )
    }

    suspend fun sendAudioMessage(
        context: Context,
        threadId: String,
        audioFile: File,
        durationMillis: Long
    ) {
        messageMutationCoordinator.sendAudioMessage(
            context = context,
            threadId = threadId,
            audioFile = audioFile,
            durationMillis = durationMillis
        )
    }

    suspend fun markThreadRead(threadId: String) {
        messageMutationCoordinator.markThreadRead(threadId)
    }

    suspend fun deleteMessageForMe(
        threadId: String,
        messageId: String
    ) {
        messageMutationCoordinator.deleteMessageForMe(threadId, messageId)
    }

    suspend fun deleteMessageForEveryone(
        threadId: String,
        messageId: String
    ) {
        messageMutationCoordinator.deleteMessageForEveryone(threadId, messageId)
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
