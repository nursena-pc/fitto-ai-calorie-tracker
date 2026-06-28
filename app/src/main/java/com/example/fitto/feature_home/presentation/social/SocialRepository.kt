package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.Source
import android.content.Context
import android.net.Uri
import java.io.File
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
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

    fun currentUserId(): String {
        return auth.currentUser?.uid.orEmpty()
    }

    private var connectedRef: DatabaseReference? = null
    private var connectedListener: ValueEventListener? = null
    private var currentStatusRef: DatabaseReference? = null

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


    fun observeThreadMessagesForSearch(threadId: String): Flow<List<GroupMessageSearchResultUiModel>> = callbackFlow {
        if (threadId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val text = document.getString("text").orEmpty().trim()
                    if (text.isBlank()) return@mapNotNull null

                    GroupMessageSearchResultUiModel(
                        messageId = document.id,
                        threadId = threadId,
                        senderName = document.getString("senderName")
                            ?: document.getString("senderDisplayName")
                            ?: "Kullanıcı",
                        messageText = text,
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                }

                trySend(items)
            }

        awaitClose { listener.remove() }
    }
    fun observeUserPresence(userId: String): Flow<PresenceStatusUiModel> = callbackFlow {
        if (userId.isBlank()) {
            trySend(PresenceStatusUiModel(userId = userId, isOnline = false, lastChanged = 0L))
            close()
            return@callbackFlow
        }

        val statusRef = realtimeDatabase.reference
            .child(REALTIME_STATUS_NODE)
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.child("state").getValue(String::class.java).orEmpty()
                val lastChanged = snapshot.child("lastChanged").getValue(Long::class.java) ?: 0L

                trySend(
                    PresenceStatusUiModel(
                        userId = userId,
                        isOnline = state == PRESENCE_ONLINE,
                        lastChanged = lastChanged
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(PresenceStatusUiModel(userId = userId, isOnline = false, lastChanged = 0L))
            }
        }

        statusRef.addValueEventListener(listener)
        awaitClose { statusRef.removeEventListener(listener) }
    }
    fun observeThreadSharedContentSummary(threadId: String): Flow<ThreadSharedContentSummary> = callbackFlow {
        val registration = firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().map { doc ->
                    SocialThreadSharedContentSummaryResolver.Message(
                        text = doc.getString("text").orEmpty(),
                        content = doc.getString("content").orEmpty(),
                        attachmentUrl = doc.getString("attachmentUrl"),
                        attachmentType = doc.getString("attachmentType").orEmpty(),
                        mimeType = doc.getString("mimeType").orEmpty(),
                        imageUrl = doc.getString("imageUrl"),
                        videoUrl = doc.getString("videoUrl"),
                        linkUrl = doc.getString("linkUrl")
                    )
                }

                trySend(SocialThreadSharedContentSummaryResolver.summarize(messages))
            }

        awaitClose { registration.remove() }
    }

    fun observeTypingUsers(threadId: String): Flow<List<TypingUserUiModel>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (threadId.isBlank() || currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val typingRef = realtimeDatabase.reference
            .child(REALTIME_TYPING_NODE)
            .child(threadId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val records = snapshot.children.map { child ->
                    SocialTypingUserResolver.TypingRecord(
                        key = child.key,
                        uid = child.child("uid").getValue(String::class.java),
                        isTyping = child.child("isTyping").getValue(Boolean::class.java) == true,
                        updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L,
                        userName = child.child("userName").getValue(String::class.java),
                        photoUrl = child.child("photoUrl").getValue(String::class.java)
                    )
                }

                trySend(
                    SocialTypingUserResolver.resolve(
                        records = records,
                        currentUserId = currentUserId,
                        nowMillis = System.currentTimeMillis(),
                        staleMillis = TYPING_STALE_MILLIS
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        typingRef.addValueEventListener(listener)
        awaitClose { typingRef.removeEventListener(listener) }
    }

    fun setTyping(threadId: String, isTyping: Boolean) {
        val user = auth.currentUser ?: return
        if (threadId.isBlank()) return

        val typingRef = realtimeDatabase.reference
            .child(REALTIME_TYPING_NODE)
            .child(threadId)
            .child(user.uid)

        if (!isTyping) {
            typingRef.removeValue()
            return
        }

        typingRef.onDisconnect().removeValue()

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val publicUserName = document.getString("userName")
                    ?: document.getString("username")
                    ?: user.email?.substringBefore("@")
                    ?: "fitto_user"

                val publicPhotoUrl = document.getString("photoUrl")
                    ?: user.photoUrl?.toString()

                typingRef.setValue(
                    mapOf(
                        "uid" to user.uid,
                        "userName" to publicUserName,
                        "photoUrl" to publicPhotoUrl,
                        "isTyping" to true,
                        "updatedAt" to ServerValue.TIMESTAMP
                    )
                )
            }
            .addOnFailureListener {
                typingRef.setValue(
                    mapOf(
                        "uid" to user.uid,
                        "userName" to (user.email?.substringBefore("@") ?: "fitto_user"),
                        "photoUrl" to user.photoUrl?.toString(),
                        "isTyping" to true,
                        "updatedAt" to ServerValue.TIMESTAMP
                    )
                )
            }
    }

    fun observeFeed(): Flow<List<SocialPost>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val sourcePostMaps = mutableMapOf<String, Map<String, SocialPost>>()
        val registrations = mutableListOf<ListenerRegistration>()

        fun emitMergedPosts() {
            val mergedPosts = sourcePostMaps
                .values
                .flatMap { it.values }
                .associateBy { post -> post.id }
                .values
                .sortedByDescending { post -> post.createdAt }

            trySend(mergedPosts)
        }

        fun attachSource(
            key: String,
            query: Query,
            includePost: (SocialPost) -> Boolean
        ) {
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    sourcePostMaps[key] = emptyMap()
                    emitMergedPosts()
                    return@addSnapshotListener
                }

                val rawPosts = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { document ->
                        mapSocialPostDocument(
                            document = document,
                            currentUserId = currentUserId
                        )
                    }
                    .filter(includePost)

                launch {
                    val posts = hydratePostAuthors(rawPosts)
                        .associateBy { post -> post.id }

                    sourcePostMaps[key] = posts
                    emitMergedPosts()
                }
            }

            registrations += registration
        }

        val followingIds = firestore.collection("follows")
            .whereEqualTo("followerId", currentUserId)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.getString("followingId")
            }
            .toSet()

        // Herkesin görebileceği topluluk gönderileri
        attachSource(
            key = "community",
            query = firestore.collection("posts")
                .whereEqualTo("visibility", POST_VISIBILITY_COMMUNITY)
                .whereEqualTo("isDraft", false),
            includePost = { true }
        )

        // Benim taslak olmayan bütün gönderilerim: private/followers/community olabilir
        attachSource(
            key = "my_posts",
            query = firestore.collection("posts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isDraft", false),
            includePost = { true }
        )

        // Takip ettiğim kişilerin followers gönderileri
        followingIds
            .chunked(10)
            .forEachIndexed { index, chunk ->
                if (chunk.isNotEmpty()) {
                    attachSource(
                        key = "following_$index",
                        query = firestore.collection("posts")
                            .whereEqualTo("visibility", POST_VISIBILITY_FOLLOWERS)
                            .whereEqualTo("isDraft", false)
                            .whereIn("userId", chunk),
                        includePost = { true }
                    )
                }
            }

        awaitClose {
            registrations.forEach { registration ->
                registration.remove()
            }
        }
    }
    private fun mapSocialPostDocument(
        document: DocumentSnapshot,
        currentUserId: String
    ): SocialPost? {
        return SocialPostDocumentResolver.resolve(
            id = document.id,
            currentUserId = currentUserId,
            userId = document.getString("userId"),
            userName = document.getString("userName"),
            userPhotoUrl = document.getString("userPhotoUrl"),
            fallbackPhotoUrl = document.getString("photoUrl"),
            content = document.getString("content"),
            imageUrl = document.getString("imageUrl"),
            mealInfo = document.getString("mealInfo"),
            calories = document.getLong("calories"),
            likesCount = document.getLong("likesCount"),
            commentsCount = document.getLong("commentsCount"),
            createdAt = document.getLong("createdAt"),
            likedBy = document.get("likedBy"),
            savedBy = document.get("savedBy"),
            allowComments = document.getBoolean("allowComments"),
            visibility = document.getString("visibility"),
            tags = document.get("tags"),
            isDraft = document.getBoolean("isDraft"),
            postType = document.getString("postType"),
            recipeId = document.getString("recipeId"),
            recipeTitle = document.getString("recipeTitle"),
            recipeImageResId = document.getLong("recipeImageResId"),
            recipeDuration = document.getString("recipeDuration"),
            recipeServing = document.getString("recipeServing"),
            recipeCalories = document.getLong("recipeCalories"),
            recipeCategory = document.getString("recipeCategory"),
            recipeSourceTag = document.getString("recipeSourceTag"),
            recipeDescription = document.getString("recipeDescription"),
            recipeIngredients = document.get("recipeIngredients"),
            recipeSteps = document.get("recipeSteps")
        )
    }

    private suspend fun hydratePostAuthors(posts: List<SocialPost>): List<SocialPost> {
        if (posts.isEmpty()) return posts

        val usersById = getUsersByIds(
            posts.map { post -> post.userId }
                .filter { userId -> userId.isNotBlank() }
                .distinct()
        ).associateBy { user -> user.id }

        if (usersById.isEmpty()) return posts

        return posts.map { post ->
            val user = usersById[post.userId]
            if (user == null) {
                post
            } else {
                post.copy(
                    userName = user.userName.ifBlank { post.userName },
                    userPhotoUrl = user.photoUrl
                )
            }
        }
    }

    private suspend fun hydrateCommentAuthors(comments: List<SocialComment>): List<SocialComment> {
        if (comments.isEmpty()) return comments

        val usersById = getUsersByIds(
            comments.map { comment -> comment.userId }
                .filter { userId -> userId.isNotBlank() }
                .distinct()
        ).associateBy { user -> user.id }

        if (usersById.isEmpty()) return comments

        return comments.map { comment ->
            val user = usersById[comment.userId]
            if (user == null) {
                comment
            } else {
                comment.copy(
                    userName = user.userName.ifBlank { comment.userName },
                    userPhotoUrl = user.photoUrl
                )
            }
        }
    }

    fun observeComments(postId: String): Flow<List<SocialComment>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        val listener = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.map { doc ->
                    SocialCommentDocumentResolver.resolve(
                        id = doc.id,
                        postId = postId,
                        currentUserId = currentUserId,
                        userId = doc.getString("userId"),
                        userName = doc.getString("userName"),
                        userPhotoUrl = doc.getString("userPhotoUrl"),
                        fallbackPhotoUrl = doc.getString("photoUrl"),
                        text = doc.getString("text"),
                        parentCommentId = doc.getString("parentCommentId"),
                        likesCount = doc.getLong("likesCount"),
                        replyCount = doc.getLong("replyCount"),
                        likedBy = doc.get("likedBy"),
                        createdAt = doc.getLong("createdAt")
                    )
                }.orEmpty()

                launch {
                    trySend(hydrateCommentAuthors(comments))
                }
            }

        awaitClose { listener.remove() }
    }

    private suspend fun hydrateMessageThreads(threads: List<MessageThreadUiModel>): List<MessageThreadUiModel> {
        if (threads.isEmpty()) return threads

        val usersById = getUsersByIds(
            threads.mapNotNull { thread -> thread.otherParticipantId }
                .filter { userId -> userId.isNotBlank() }
                .distinct()
        ).associateBy { user -> user.id }

        if (usersById.isEmpty()) return threads

        return threads.map { thread ->
            if (thread.isGroup) {
                thread
            } else {
                val user = usersById[thread.otherParticipantId]
                if (user == null) {
                    thread
                } else {
                    val title = user.userName.ifBlank { thread.title }
                    thread.copy(
                        title = title,
                        initials = SocialTextResolver.createInitials(title),
                        photoUrl = user.photoUrl
                    )
                }
            }
        }
    }

    fun observeMessageThreads(): Flow<List<MessageThreadUiModel>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var threadDocuments: List<DocumentSnapshot> = emptyList()
        var blockedUserIds: Set<String> = emptySet()
        var blockedThreadPreviews: Map<String, BlockedThreadPreview> = emptyMap()

        fun emitThreads() {
            val threads = threadDocuments
                .mapNotNull { document ->
                    val thread = mapMessageThreadDocument(
                        document = document,
                        currentUserId = currentUserId
                    ) ?: return@mapNotNull null

                    val isBlockedDirectThread =
                        !thread.isGroup &&
                                !thread.otherParticipantId.isNullOrBlank() &&
                                blockedUserIds.contains(thread.otherParticipantId)

                    if (isBlockedDirectThread) {
                        val frozenPreview = blockedThreadPreviews[thread.id]

                        thread.copy(
                            lastMessage = frozenPreview?.text ?: "Bu kullanıcı engellendi",
                            timeText = frozenPreview
                                ?.createdAt
                                ?.takeIf { it > 0L }
                                ?.let { SocialTextResolver.formatRelativeTime(it) }
                                .orEmpty(),
                            unreadCount = 0,
                            highlightText = null
                        )
                    } else {
                        thread
                    }
                }
                .sortedByDescending { thread ->
                    threadDocuments
                        .firstOrNull { document -> document.id == thread.id }
                        ?.getLong("updatedAt") ?: 0L
                }

            launch {
                trySend(hydrateMessageThreads(threads))
            }
        }

        val threadsListener = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                threadDocuments = snapshot?.documents.orEmpty()
                emitThreads()
            }

        val userListener = firestore.collection("users")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    blockedUserIds = emptySet()
                    blockedThreadPreviews = emptyMap()
                    emitThreads()
                    return@addSnapshotListener
                }

                blockedUserIds = (snapshot?.get("blockedUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()
                    ?: emptySet()

                blockedThreadPreviews = SocialMessageHiddenRangeResolver.readBlockedThreadPreviews(
                    snapshot?.get("blockedThreadPreviews")
                )

                emitThreads()
            }

        awaitClose {
            threadsListener.remove()
            userListener.remove()
        }
    }

    fun observeMessages(threadId: String): Flow<List<ChatMessageUiModel>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (threadId.isBlank() || currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val threadSnapshot = threadRef.get().await()
        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        val participants = threadSnapshot.get("participants") as? Map<*, *>
        val participantNames = participantIds.associateWith { uid ->
            val participant = participants?.get(uid) as? Map<*, *>
            (participant?.get("userName") as? String).orEmpty().ifBlank { "fitto_user" }
        }
        val latestParticipants = getUsersByIds(participantIds)
            .associateBy { user -> user.id }
        val participantPhotoUrls = participantIds.associateWith { uid ->
            val participant = participants?.get(uid) as? Map<*, *>
            val latestParticipant = latestParticipants[uid]
            if (latestParticipant != null) {
                latestParticipant.photoUrl
            } else {
                participant?.get("photoUrl") as? String
            }
        }

        val totalRecipientCount = (participantIds.size - 1).coerceAtLeast(1)
        val clearedAtMap = threadSnapshot.get("clearedAt") as? Map<*, *>
        val currentUserClearedAt = (clearedAtMap?.get(currentUserId) as? Number)?.toLong() ?: 0L
        val listener = threadRef
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { document ->
                    SocialChatMessageDocumentResolver.resolve(
                        id = document.id,
                        createdAt = document.getLong("createdAt") ?: 0L,
                        currentUserId = currentUserId,
                        currentUserClearedAt = currentUserClearedAt,
                        deletedFor = document.getStringListCompat("deletedFor"),
                        senderId = document.getString("senderId").orEmpty(),
                        senderName = document.getString("senderName"),
                        senderPhotoUrl = document.getString("senderPhotoUrl"),
                        fallbackPhotoUrl = document.getString("photoUrl"),
                        participantNames = participantNames,
                        participantPhotoUrls = participantPhotoUrls,
                        readBy = SocialReadReceiptResolver.readByMap(document.get("readBy")),
                        totalRecipientCount = totalRecipientCount,
                        isDeleted = document.getBoolean("isDeleted") == true,
                        messageType = document.getString("type"),
                        text = document.getString("text"),
                        fileName = document.getString("fileName"),
                        fileUrl = document.getString("fileUrl"),
                        fileMimeType = document.getString("fileMimeType"),
                        fileSizeBytes = document.getLong("fileSizeBytes") ?: 0L,
                        audioDurationMillis = document.getLong("audioDurationMillis") ?: 0L,
                        deliveredAt = document.getLong("deliveredAt"),
                        deletedAt = document.getLong("deletedAt") ?: 0L,
                        deletedBy = document.getString("deletedBy")
                    )
                }.orEmpty()

                trySend(messages)
            }

        awaitClose { listener.remove() }
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
                mapOf(
                    "deletedFor" to FieldValue.arrayUnion(currentUserId),
                    "updatedAt" to now
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
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .await()
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

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("group_photos")
            .child(threadId)
            .child("${UUID.randomUUID()}.jpg")

        storageRef.putFile(imageUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        threadRef.update(
            mapOf(
                "photoUrl" to downloadUrl,
                "updatedAt" to System.currentTimeMillis()
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
        if (targetUserId.isBlank() || targetUserId == currentUser.uid) return null

        createCurrentUserProfileIfNeeded()

        val currentProfile = getCurrentUserProfile()
        val targetProfile = getUserProfile(targetUserId) ?: return null

        val threadId = SocialThreadParticipantResolver.directThreadId(currentUser.uid, targetUserId)
        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val now = System.currentTimeMillis()

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(threadRef)

            if (!snapshot.exists()) {
                transaction.set(
                    threadRef,
                    mapOf(
                        "id" to threadId,
                        "type" to THREAD_TYPE_DIRECT,
                        "title" to null,
                        "participantIds" to listOf(currentUser.uid, targetUserId).sorted(),
                        "memberCount" to 2,
                        "participants" to mapOf(
                            currentUser.uid to SocialThreadParticipantResolver.participantMap(
                                uid = currentUser.uid,
                                userName = currentProfile?.userName?.takeIf { it.isNotBlank() }
                                    ?: currentUser.email?.substringBefore("@")
                                    ?: "fitto_user",
                                photoUrl = currentProfile?.photoUrl ?: currentUser.photoUrl?.toString()
                            ),
                            targetUserId to SocialThreadParticipantResolver.participantMap(
                                uid = targetUserId,
                                userName = targetProfile.userName.ifBlank { "fitto_user" },
                                photoUrl = targetProfile.photoUrl
                            )
                        ),
                        "unreadCounts" to mapOf(
                            currentUser.uid to 0,
                            targetUserId to 0
                        ),
                        "lastMessageText" to "",
                        "lastMessageSenderId" to null,
                        "lastMessageCreatedAt" to null,
                        "createdAt" to now,
                        "deliveredAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            null
        }.await()

        val threadSnapshot = threadRef.get().await()
        return mapMessageThreadDocument(
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
        val trimmedTitle = title.trim()
        val trimmedDescription = description.trim()
        val uniqueSelectedUsers = selectedUsers
            .filter { user -> user.id.isNotBlank() && user.id != currentUser.uid }
            .distinctBy { user -> user.id }

        if (trimmedTitle.isBlank() || uniqueSelectedUsers.size < 2) return null

        createCurrentUserProfileIfNeeded()

        val currentProfile = getCurrentUserProfile()
        val currentUserName = currentProfile?.userName?.takeIf { it.isNotBlank() }
            ?: currentUser.email?.substringBefore("@")
            ?: "fitto_user"
        val currentPhotoUrl = currentProfile?.photoUrl ?: currentUser.photoUrl?.toString()

        val threadId = "group_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val participantIds = (listOf(currentUser.uid) + uniqueSelectedUsers.map { it.id }).distinct()
        val participants = mutableMapOf<String, Map<String, Any?>>()

        participants[currentUser.uid] = SocialThreadParticipantResolver.participantMap(
            uid = currentUser.uid,
            userName = currentUserName,
            photoUrl = currentPhotoUrl
        )

        uniqueSelectedUsers.forEach { user ->
            participants[user.id] = SocialThreadParticipantResolver.participantMap(
                uid = user.id,
                userName = user.userName,
                photoUrl = user.photoUrl
            )
        }

        val unreadCounts = participantIds.associateWith { 0 }
        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)

        threadRef.set(
            mapOf(
                "id" to threadId,
                "type" to THREAD_TYPE_GROUP,
                "title" to trimmedTitle,
                "titleLowercase" to trimmedTitle.lowercase(Locale("tr", "TR")),
                "creatorId" to currentUser.uid,
                "participantIds" to participantIds,
                "participants" to participants,
                "memberCount" to participantIds.size,
                "unreadCounts" to unreadCounts,
                "lastMessageText" to "",
                "lastMessageSenderId" to null,
                "lastMessageCreatedAt" to null,
                "createdAt" to now,
                "deliveredAt" to now,
                "updatedAt" to now,
                "description" to trimmedDescription,
                "descriptionLowercase" to trimmedDescription.lowercase(Locale("tr", "TR"))
            )
        ).await()

        return mapMessageThreadDocument(threadRef.get().await(), currentUser.uid)
    }

    suspend fun updateGroupTitle(threadId: String, newTitle: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val trimmedTitle = newTitle.trim()
        if (threadId.isBlank() || trimmedTitle.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()
        val participantIds = snapshot.getStringListCompat("participantIds")

        if (snapshot.getString("type") != THREAD_TYPE_GROUP || !participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        threadRef.update(
            mapOf(
                "title" to trimmedTitle,
                "titleLowercase" to trimmedTitle.lowercase(Locale("tr", "TR")),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }
    suspend fun updateGroupDescription(threadId: String, newDescription: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val trimmedDescription = newDescription.trim()

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()
        val participantIds = snapshot.getStringListCompat("participantIds")

        if (snapshot.getString("type") != THREAD_TYPE_GROUP || !participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        threadRef.update(
            mapOf(
                "description" to trimmedDescription,
                "descriptionLowercase" to trimmedDescription.lowercase(Locale("tr", "TR")),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun updateGroupName(
        threadId: String,
        newName: String
    ) {
        if (threadId.isBlank()) return

        val trimmedName = newName.trim()

        firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .update(
                mapOf(
                    "title" to trimmedName,
                    "titleLowercase" to trimmedName.lowercase(),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
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
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()

        threadRef.update(
            mapOf(
                "clearedAt.$currentUserId" to now,
                "unreadCounts.$currentUserId" to 0,
                "updatedAt" to now
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
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()

        threadRef.update(
            mapOf(
                "clearedAt.$currentUserId" to now,
                "unreadCounts.$currentUserId" to 0,
                "updatedAt" to now
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

        if (!isGroup || !participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val now = System.currentTimeMillis()
        val newMemberCount = (participantIds.size - 1).coerceAtLeast(0)

        threadRef.update(
            mapOf(
                "participantIds" to FieldValue.arrayRemove(currentUserId),
                "participants.$currentUserId" to FieldValue.delete(),
                "unreadCounts.$currentUserId" to FieldValue.delete(),
                "typing.$currentUserId" to FieldValue.delete(),
                "memberCount" to newMemberCount,
                "updatedAt" to now
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

        if (!isGroup || !participantIds.contains(currentUserId)) {
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
        val newMemberCount = (participantIds.size - 1).coerceAtLeast(0)

        threadRef.update(
            mapOf(
                "participantIds" to FieldValue.arrayRemove(memberId),
                "participants.$memberId" to FieldValue.delete(),
                "unreadCounts.$memberId" to FieldValue.delete(),
                "typing.$memberId" to FieldValue.delete(),
                "memberCount" to newMemberCount,
                "updatedAt" to now
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

        if (!isGroup || !currentParticipantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val newUsers = selectedUsers
            .filter { user -> user.id.isNotBlank() && !currentParticipantIds.contains(user.id) }
            .distinctBy { user -> user.id }

        if (newUsers.isEmpty()) return

        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>(
            "participantIds" to FieldValue.arrayUnion(*newUsers.map { it.id }.toTypedArray()),
            "memberCount" to currentParticipantIds.size + newUsers.size,
            "updatedAt" to now
        )

        newUsers.forEach { user ->
            updates["participants.${user.id}"] = SocialThreadParticipantResolver.participantMap(
                uid = user.id,
                userName = user.userName,
                photoUrl = user.photoUrl
            )

            updates["unreadCounts.${user.id}"] = 0
        }

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

        return mapMessageThreadDocument(
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
        val recipientIds = participantIds
            .filter { userId -> userId != senderId }
            .distinct()

        if (recipientIds.isEmpty()) return emptyList()

        val blockedRecipientIds = mutableSetOf<String>()

        recipientIds.chunked(10).forEach { chunk ->
            val snapshot = firestore.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.forEach { userDocument ->
                val blockedUserIds = userDocument.getStringListCompat("blockedUserIds")

                if (blockedUserIds.contains(senderId)) {
                    blockedRecipientIds.add(userDocument.id)
                }
            }
        }

        return blockedRecipientIds.toList()
    }

    suspend fun sendMessage(
        threadId: String,
        text: String
    ) {
        val currentUser = auth.currentUser ?: return
        val trimmedText = text.trim()

        if (threadId.isBlank() || trimmedText.isBlank()) return

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

        val visibleRecipientIds = participantIds
            .filter { participantId ->
                participantId != currentUser.uid &&
                        !blockedRecipientIds.contains(participantId)
            }
        val threadUpdates = mutableMapOf<String, Any>(
            "lastMessageText" to trimmedText,
            "lastMessageSenderId" to currentUser.uid,
            "lastMessageCreatedAt" to now,
            "lastMessageType" to "text",
            "lastMessageHiddenFor" to blockedRecipientIds,
            "updatedAt" to now,
            "unreadCounts.${currentUser.uid}" to 0
        )

        visibleRecipientIds.forEach { participantId ->
            threadUpdates["unreadCounts.$participantId"] = FieldValue.increment(1)
        }

        blockedRecipientIds.forEach { participantId ->
            threadUpdates["unreadCounts.$participantId"] = 0
        }

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                mapOf(
                    "id" to messageRef.id,
                    "threadId" to threadId,
                    "senderId" to currentUser.uid,
                    "senderName" to sender.name,
                    "senderPhotoUrl" to sender.photoUrl,
                    "photoUrl" to sender.photoUrl,
                    "text" to trimmedText,
                    "type" to "text",
                    "readBy" to mapOf(currentUser.uid to now),
                    "deletedFor" to blockedRecipientIds,
                    "createdAt" to now,
                    "deliveredAt" to now,
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
        if (threadId.isBlank()) return

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

        val safeFileName = fileInfo.name
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "file_${System.currentTimeMillis()}" }

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("chat_attachments")
            .child(threadId)
            .child(messageRef.id)
            .child("${UUID.randomUUID()}_$safeFileName")

        val metadata = StorageMetadata.Builder()
            .setContentType(fileInfo.mimeType)
            .build()

        storageRef.putFile(fileUri, metadata).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val isImage = fileInfo.mimeType?.startsWith("image/") == true
        val lastMessageText = if (isImage) {
            "🖼️ Fotoğraf"
        } else {
            "📎 ${fileInfo.name}"
        }
        val blockedRecipientIds = getRecipientsWhoBlockedSender(
            participantIds = participantIds,
            senderId = currentUser.uid
        )

        val visibleRecipientIds = participantIds
            .filter { participantId ->
                participantId != currentUser.uid &&
                        !blockedRecipientIds.contains(participantId)
            }
        val threadUpdates = mutableMapOf<String, Any>(
            "lastMessageText" to lastMessageText,
            "lastMessageSenderId" to currentUser.uid,
            "lastMessageCreatedAt" to now,
            "lastMessageHiddenFor" to blockedRecipientIds,
            "lastMessageType" to "file",
            "updatedAt" to now,
            "unreadCounts.${currentUser.uid}" to 0
        )

        visibleRecipientIds.forEach { participantId ->
            threadUpdates["unreadCounts.$participantId"] = FieldValue.increment(1)
        }

        blockedRecipientIds.forEach { participantId ->
            threadUpdates["unreadCounts.$participantId"] = 0
        }

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                mapOf(
                    "id" to messageRef.id,
                    "threadId" to threadId,
                    "senderId" to currentUser.uid,
                    "senderName" to sender.name,
                    "senderPhotoUrl" to sender.photoUrl,
                    "photoUrl" to sender.photoUrl,
                    "text" to fileInfo.name,
                    "type" to "file",
                    "fileUrl" to downloadUrl,
                    "deletedFor" to blockedRecipientIds,
                    "fileName" to fileInfo.name,
                    "fileMimeType" to fileInfo.mimeType,
                    "fileSizeBytes" to fileInfo.sizeBytes,
                    "readBy" to mapOf(currentUser.uid to now),
                    "createdAt" to now,
                    "deliveredAt" to now
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
        if (threadId.isBlank()) return
        if (!audioFile.exists() || audioFile.length() <= 0L) return

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
            .child("chat_attachments")
            .child(threadId)
            .child(messageRef.id)
            .child("${UUID.randomUUID()}_voice_message.m4a")

        val metadata = StorageMetadata.Builder()
            .setContentType("audio/mp4")
            .build()

        storageRef.putFile(Uri.fromFile(audioFile), metadata).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val lastMessageText = "🎤 Ses mesajı"
        val blockedRecipientIds = getRecipientsWhoBlockedSender(
            participantIds = participantIds,
            senderId = currentUser.uid
        )

        val visibleRecipientIds = participantIds
            .filter { participantId ->
                participantId != currentUser.uid &&
                        !blockedRecipientIds.contains(participantId)
            }
        val threadUpdates = mutableMapOf<String, Any>(
            "lastMessageText" to lastMessageText,
            "lastMessageSenderId" to currentUser.uid,
            "lastMessageCreatedAt" to now,
            "lastMessageHiddenFor" to blockedRecipientIds,
            "lastMessageType" to "audio",
            "updatedAt" to now,
            "unreadCounts.${currentUser.uid}" to 0
        )

        visibleRecipientIds.forEach { participantId ->
            threadUpdates["unreadCounts.$participantId"] = FieldValue.increment(1)
        }

        blockedRecipientIds.forEach { participantId ->
            threadUpdates["unreadCounts.$participantId"] = 0
        }

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                mapOf(
                    "id" to messageRef.id,
                    "threadId" to threadId,
                    "senderId" to currentUser.uid,
                    "senderName" to sender.name,
                    "senderPhotoUrl" to sender.photoUrl,
                    "photoUrl" to sender.photoUrl,
                    "text" to "Ses mesajı",
                    "type" to "audio",
                    "fileUrl" to downloadUrl,
                    "deletedFor" to blockedRecipientIds,
                    "fileName" to "voice_message.m4a",
                    "fileMimeType" to "audio/mp4",
                    "fileSizeBytes" to audioFile.length(),
                    "audioDurationMillis" to durationMillis,
                    "readBy" to mapOf(currentUser.uid to now),
                    "createdAt" to now,
                    "deliveredAt" to now
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
        if (threadId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val now = System.currentTimeMillis()

        val threadSnapshot = threadRef.get().await()
        if (!threadSnapshot.exists()) return

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        val isDirectThread = threadSnapshot.getString("type") == THREAD_TYPE_DIRECT

        val otherUserId = if (isDirectThread) {
            participantIds.firstOrNull { uid -> uid != currentUserId }
        } else {
            null
        }

        var isOtherUserCurrentlyBlocked = false
        var hiddenRanges: List<MessageHiddenRange> = emptyList()

        if (!otherUserId.isNullOrBlank()) {
            val userSnapshot = firestore.collection("users")
                .document(currentUserId)
                .get()
                .await()

            val blockedUserIds = (userSnapshot.get("blockedUserIds") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()

            isOtherUserCurrentlyBlocked = blockedUserIds.contains(otherUserId)

            hiddenRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(userSnapshot, threadId) +
                    listOfNotNull(SocialMessageHiddenRangeResolver.readActiveHiddenRange(userSnapshot, threadId))
        }

        val unreadMessages = threadRef
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .get()
            .await()
            .documents
            .filter { document ->
                val senderId = document.getString("senderId").orEmpty()
                val createdAt = document.getLong("createdAt") ?: 0L

                val isBlockedSenderMessage =
                    !otherUserId.isNullOrBlank() &&
                            senderId == otherUserId &&
                            (
                                    SocialMessageHiddenRangeResolver.isMessageInHiddenRanges(
                                        senderId = senderId,
                                        createdAt = createdAt,
                                        hiddenRanges = hiddenRanges
                                    )
                                            ||
                                            (
                                                    isOtherUserCurrentlyBlocked &&
                                                            hiddenRanges.isEmpty()
                                                    )
                                    )

                senderId != currentUserId &&
                        !isBlockedSenderMessage &&
                        !SocialReadReceiptResolver.readByIds(document.get("readBy")).contains(currentUserId)
            }

        firestore.runBatch { batch ->
            batch.update(
                threadRef,
                mapOf("unreadCounts.$currentUserId" to 0)
            )

            unreadMessages.forEach { document ->
                val readByMap = SocialReadReceiptResolver.readByMap(document.get("readBy")).toMutableMap()
                readByMap[currentUserId] = now

                batch.set(
                    document.reference,
                    mapOf(
                        "readBy" to readByMap,
                        "updatedAt" to now
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
            mapOf(
                "deletedFor" to FieldValue.arrayUnion(currentUserId),
                "updatedAt" to System.currentTimeMillis()
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
                mapOf(
                    "text" to "",
                    "isDeleted" to true,
                    "deletedBy" to currentUserId,
                    "deletedAt" to now,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )

            if (isLastMessage) {
                batch.update(
                    threadRef,
                    mapOf(
                        "lastMessageText" to "Bu mesaj silindi",
                        "updatedAt" to now
                    )
                )
            }
        }.await()
    }

    suspend fun searchUsers(query: String): List<SearchUserUiModel> {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val normalizedQuery = SocialTextResolver.normalizeUsername(query)

        if (normalizedQuery.length < 2) return emptyList()

        val snapshot = firestore.collection("users")
            .orderBy("usernameLowercase")
            .whereGreaterThanOrEqualTo("usernameLowercase", normalizedQuery)
            .whereLessThanOrEqualTo("usernameLowercase", normalizedQuery + "\uf8ff")
            .limit(25)
            .get()
            .await()

        return snapshot.documents
            .filter { document ->
                document.id != currentUserId && document.getBoolean("isDeleted") != true
            }
            .mapNotNull { document ->
                val userName = document.getString("userName")
                    ?: document.getString("usernameLowercase")
                    ?: document.getString("email")?.substringBefore("@")
                    ?: return@mapNotNull null

                SearchUserUiModel(
                    id = document.id,
                    userName = userName,
                    bio = document.getString("bio"),
                    photoUrl = document.getString("photoUrl")
                )
            }
    }

    suspend fun createCurrentUserProfileIfNeeded() {
        val user = auth.currentUser ?: return

        val userRef = firestore.collection("users").document(user.uid)
        val snapshot = userRef.get().await()

        if (!snapshot.exists()) {
            val publicUserName = SocialTextResolver.normalizeUsername(
                user.email?.substringBefore("@")
                    ?: user.displayName
                    ?: "fitto_user"
            ).ifBlank { "fitto_user" }
            val personalName = user.displayName
                ?: user.email?.substringBefore("@")
                ?: "Fitto"

            userRef.set(
                mapOf(
                    "uid" to user.uid,
                    "displayName" to publicUserName,
                    "displayNameLowercase" to publicUserName.lowercase(Locale.ROOT),
                    "realName" to personalName,
                    "name" to personalName,
                    "userName" to publicUserName,
                    "username" to publicUserName,
                    "usernameLowercase" to publicUserName.lowercase(Locale.ROOT),
                    "email" to user.email,
                    "photoUrl" to user.photoUrl?.toString(),
                    "bio" to "",
                    "followersCount" to 0,
                    "followingCount" to 0,
                    "profileCompleted" to false,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun getCurrentUserProfile(): SocialUser? {
        val user = auth.currentUser ?: return null

        createCurrentUserProfileIfNeeded()

        val profile = getUserProfile(user.uid) ?: return null
        return profile.copy(
            isFollowedByMe = false,
            photoUrl = profile.photoUrl ?: user.photoUrl?.toString()
        )
    }

    suspend fun getUserProfile(userId: String): SocialUser? {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        val userDoc = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        if (!userDoc.exists()) return null
        if (userDoc.getBoolean("isDeleted") == true) return null

        val followDoc = if (currentUserId.isNotBlank() && currentUserId != userId) {
            firestore.collection("follows")
                .document("${currentUserId}_$userId")
                .get()
                .await()
        } else {
            null
        }

        return SocialUserDocumentResolver.resolveProfile(
            id = userDoc.id,
            userName = userDoc.getString("userName"),
            username = userDoc.getString("username"),
            displayName = userDoc.getString("displayName"),
            name = userDoc.getString("name"),
            realName = userDoc.getString("realName"),
            email = userDoc.getString("email"),
            photoUrl = userDoc.getString("photoUrl"),
            bio = userDoc.getString("bio"),
            followersCount = userDoc.getLong("followersCount")?.toInt() ?: 0,
            followingCount = userDoc.getLong("followingCount")?.toInt() ?: 0,
            isFollowedByMe = followDoc?.exists() == true
        )
    }

    suspend fun toggleFollow(targetUser: SocialUser) {
        val currentUser = auth.currentUser ?: return
        if (currentUser.uid == targetUser.id) return

        createCurrentUserProfileIfNeeded()

        val followId = "${currentUser.uid}_${targetUser.id}"
        val followRef = firestore.collection("follows").document(followId)
        val currentUserRef = firestore.collection("users").document(currentUser.uid)
        val targetUserRef = firestore.collection("users").document(targetUser.id)

        firestore.runTransaction { transaction ->
            val followSnapshot = transaction.get(followRef)
            val targetSnapshot = transaction.get(targetUserRef)

            val targetDeleted = !targetSnapshot.exists() ||
                    targetSnapshot.getBoolean("isDeleted") == true

            if (targetDeleted) {
                if (followSnapshot.exists()) {
                    transaction.delete(followRef)
                    transaction.update(
                        currentUserRef,
                        "followingCount",
                        FieldValue.increment(-1)
                    )
                }

                if (targetSnapshot.exists()) {
                    transaction.update(
                        targetUserRef,
                        mapOf(
                            "followersCount" to 0,
                            "followingCount" to 0
                        )
                    )
                }

                return@runTransaction
            }

            if (followSnapshot.exists()) {
                transaction.delete(followRef)
                transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
            } else {
                transaction.set(
                    followRef,
                    mapOf(
                        "followerId" to currentUser.uid,
                        "followingId" to targetUser.id,
                        "createdAt" to System.currentTimeMillis()
                    )
                )

                transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
            }
        }.await()
    }

    suspend fun isUserDeletedOrMissing(userId: String): Boolean {
        if (userId.isBlank()) return true

        val snapshot = firestore
            .collection("users")
            .document(userId)
            .get()
            .await()

        return !snapshot.exists() || snapshot.getBoolean("isDeleted") == true
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
        val user = auth.currentUser ?: return

        createCurrentUserProfileIfNeeded()

        val profile = getCurrentUserProfile()

        val publicUserName = profile?.userName?.ifBlank { null }
            ?: user.email?.substringBefore("@")
            ?: "fitto_user"

        val publicPhotoUrl = profile?.photoUrl ?: user.photoUrl?.toString()

        val imageUrl = imageUri?.let { uploadPostImage(it) }

        firestore.collection("posts")
            .add(
                hashMapOf(
                    "userId" to user.uid,
                    "userName" to publicUserName,
                    "userPhotoUrl" to publicPhotoUrl,
                    "photoUrl" to publicPhotoUrl,
                    "content" to content.trim(),
                    "imageUrl" to imageUrl,
                    "mealInfo" to mealInfo?.trim(),
                    "calories" to calories,
                    "visibility" to visibility,
                    "tags" to tags,
                    "isDraft" to isDraft,
                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "likedBy" to emptyList<String>(),
                    "savedBy" to emptyList<String>(),
                    "allowComments" to allowComments,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            .await()
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
        val user = auth.currentUser ?: return null

        createCurrentUserProfileIfNeeded()

        val profile = getCurrentUserProfile()
        val publicUserName = profile?.userName?.ifBlank { null }
            ?: user.email?.substringBefore("@")
            ?: "fitto_user"
        val publicPhotoUrl = profile?.photoUrl ?: user.photoUrl?.toString()
        val now = System.currentTimeMillis()
        val postRef = firestore.collection("posts").document()

        postRef.set(
            hashMapOf(
                "userId" to user.uid,
                "userName" to publicUserName,
                "userPhotoUrl" to publicPhotoUrl,
                "photoUrl" to publicPhotoUrl,
                "content" to content.trim(),
                "imageUrl" to null,
                "mealInfo" to recipeTitle.trim(),
                "calories" to recipeCalories,
                "visibility" to POST_VISIBILITY_COMMUNITY,
                "tags" to tags.distinct().filter { it.isNotBlank() },
                "isDraft" to false,
                "likesCount" to 0,
                "commentsCount" to 0,
                "likedBy" to emptyList<String>(),
                "savedBy" to emptyList<String>(),
                "allowComments" to true,
                "createdAt" to now,
                "postType" to "recipe",
                "recipeId" to recipeId,
                "recipeTitle" to recipeTitle,
                "recipeImageResId" to recipeImageResId,
                "recipeDuration" to recipeDuration,
                "recipeServing" to recipeServing,
                "recipeCalories" to recipeCalories,
                "recipeCategory" to recipeCategory,
                "recipeSourceTag" to recipeSourceTag,
                "recipeDescription" to recipeDescription.trim(),
                "recipeIngredients" to recipeIngredients.map { it.trim() }.filter { it.isNotBlank() },
                "recipeSteps" to recipeSteps.map { it.trim() }.filter { it.isNotBlank() }
            )
        ).await()

        return postRef.id
    }

    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ) {
        val user = auth.currentUser ?: return
        val postRef = firestore.collection("posts").document(postId)
        val postSnapshot = postRef.get().await()
        val allowComments = postSnapshot.getBoolean("allowComments") ?: true

        if (!allowComments) {
            throw IllegalStateException("COMMENTS_DISABLED")
        }

        createCurrentUserProfileIfNeeded()

        val profile = getCurrentUserProfile()

        val publicUserName = profile?.userName?.ifBlank { null }
            ?: user.email?.substringBefore("@")
            ?: "fitto_user"

        val publicPhotoUrl = profile?.photoUrl ?: user.photoUrl?.toString()

        firestore.runBatch { batch ->
            val commentRef = postRef.collection("comments").document()

            batch.set(
                commentRef,
                mapOf(
                    "userId" to user.uid,
                    "userName" to publicUserName,
                    "userPhotoUrl" to publicPhotoUrl,
                    "photoUrl" to publicPhotoUrl,
                    "postId" to postId,
                    "postOwnerId" to postSnapshot.getString("userId").orEmpty(),
                    "text" to text.trim(),
                    "parentCommentId" to parentCommentId,
                    "likesCount" to 0,
                    "replyCount" to 0,
                    "likedBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
            )

            batch.update(
                postRef,
                "commentsCount",
                FieldValue.increment(1)
            )

            if (parentCommentId != null) {
                val parentCommentRef = postRef
                    .collection("comments")
                    .document(parentCommentId)

                batch.update(
                    parentCommentRef,
                    "replyCount",
                    FieldValue.increment(1)
                )
            }
        }.await()
    }

    suspend fun toggleCommentLike(
        postId: String,
        comment: SocialComment
    ) {
        val userId = auth.currentUser?.uid ?: return

        val commentRef = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .document(comment.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)

            val likedBy = (snapshot.get("likedBy") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.toMutableList()
                ?: mutableListOf()

            val currentLikes = snapshot.getLong("likesCount") ?: 0L

            if (likedBy.contains(userId)) {
                likedBy.remove(userId)

                transaction.update(
                    commentRef,
                    mapOf(
                        "likedBy" to likedBy,
                        "likesCount" to (currentLikes - 1).coerceAtLeast(0)
                    )
                )
            } else {
                likedBy.add(userId)

                transaction.update(
                    commentRef,
                    mapOf(
                        "likedBy" to likedBy,
                        "likesCount" to currentLikes + 1
                    )
                )
            }
        }.await()
    }

    suspend fun toggleSave(post: SocialPost) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(post.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)

            val savedBy = (snapshot.get("savedBy") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.toMutableList()
                ?: mutableListOf()

            if (savedBy.contains(userId)) {
                savedBy.remove(userId)
            } else {
                savedBy.add(userId)
            }

            transaction.update(
                postRef,
                mapOf(
                    "savedBy" to savedBy
                )
            )
        }.await()
    }

    suspend fun toggleLike(post: SocialPost) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(post.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)

            val likedBy = (snapshot.get("likedBy") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.toMutableList()
                ?: mutableListOf()

            val currentLikes = snapshot.getLong("likesCount") ?: 0L

            if (likedBy.contains(userId)) {
                likedBy.remove(userId)

                transaction.update(
                    postRef,
                    mapOf(
                        "likedBy" to likedBy,
                        "likesCount" to (currentLikes - 1).coerceAtLeast(0)
                    )
                )
            } else {
                likedBy.add(userId)

                transaction.update(
                    postRef,
                    mapOf(
                        "likedBy" to likedBy,
                        "likesCount" to currentLikes + 1
                    )
                )
            }
        }.await()
    }

    suspend fun getBlockedUsers(): List<SocialUser> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()

        val currentUserSnapshot = firestore.collection("users")
            .document(currentUserId)
            .get()
            .await()

        val blockedUserIds = currentUserSnapshot
            .getStringListCompat("blockedUserIds")
            .filter { userId -> userId.isNotBlank() && userId != currentUserId }
            .distinct()

        if (blockedUserIds.isEmpty()) return emptyList()

        val usersById = mutableMapOf<String, SocialUser>()

        blockedUserIds.chunked(10).forEach { chunk ->
            val snapshot = firestore.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.forEach { document ->
                val isDeleted = document.getBoolean("isDeleted") == true
                val fallbackName = if (isDeleted) {
                    "Silinen Kullanıcı"
                } else {
                    "fitto_user"
                }

                usersById[document.id] = SocialUserDocumentResolver.resolveBlockedUser(
                    id = document.id,
                    userName = document.getString("userName"),
                    username = document.getString("username"),
                    displayName = document.getString("displayName"),
                    name = document.getString("name"),
                    realName = document.getString("realName"),
                    email = document.getString("email"),
                    photoUrl = document.getString("photoUrl"),
                    bio = document.getString("bio"),
                    followersCount = document.getLong("followersCount")?.toInt() ?: 0,
                    followingCount = document.getLong("followingCount")?.toInt() ?: 0,
                    fallbackName = fallbackName
                )
            }
        }

        return blockedUserIds.map { userId ->
            usersById[userId] ?: SocialUser(
                id = userId,
                userName = "Silinen Kullanıcı",
                displayName = "Silinen Kullanıcı"
            )
        }.sortedBy { user ->
            user.userName.ifBlank { user.displayName.orEmpty() }
        }
    }

    suspend fun reportPost(
        postId: String,
        reason: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("reports")
            .add(
                mapOf(
                    "postId" to postId,
                    "reporterId" to userId,
                    "reason" to reason,
                    "createdAt" to System.currentTimeMillis()
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

        if (targetUserId.isBlank() || targetUserId == currentUserId) return

        createCurrentUserProfileIfNeeded()

        val userRef = firestore.collection("users").document(currentUserId)
        val now = System.currentTimeMillis()

        val updates = mutableMapOf<String, Any>(
            "blockedUserIds" to FieldValue.arrayUnion(targetUserId),
            "updatedAt" to now
        )

        if (!threadId.isNullOrBlank()) {
            updates["activeBlockedMessageRanges.$threadId"] = mapOf(
                "userId" to targetUserId,
                "startAt" to now
            )

            updates["blockedThreadPreviews.$threadId"] = mapOf(
                "text" to previewText.orEmpty().ifBlank { "Henüz mesaj yok." },
                "createdAt" to (previewCreatedAt ?: now)
            )
        }

        userRef.update(updates).await()
    }

    suspend fun unblockUser(
        targetUserId: String,
        threadId: String? = null
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (targetUserId.isBlank() || targetUserId == currentUserId) return

        val userRef = firestore.collection("users").document(currentUserId)
        val snapshot = userRef.get().await()
        val now = System.currentTimeMillis()

        val updates = mutableMapOf<String, Any>(
            "blockedUserIds" to FieldValue.arrayRemove(targetUserId),
            "updatedAt" to now
        )

        fun closeHiddenRangeForThread(targetThreadId: String) {
            val activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(snapshot, targetThreadId)

            if (activeRange != null && activeRange.userId == targetUserId) {
                updates["blockedMessageRanges.$targetThreadId"] = FieldValue.arrayUnion(
                    mapOf(
                        "userId" to activeRange.userId,
                        "startAt" to activeRange.startAt,
                        "endAt" to now
                    )
                )
            }

            updates["activeBlockedMessageRanges.$targetThreadId"] = FieldValue.delete()
            updates["blockedThreadPreviews.$targetThreadId"] = FieldValue.delete()
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

        val blockedUserIds = (snapshot.get("blockedUserIds") as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()

        return blockedUserIds.contains(targetUserId)
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

        val blockedUserIds = (snapshot.get("blockedUserIds") as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()

        val completedRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(snapshot, threadId)
        val activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(snapshot, threadId)

        return DirectThreadBlockState(
            isBlocked = blockedUserIds.contains(targetUserId),
            hiddenRanges = completedRanges + listOfNotNull(activeRange)
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

                val blockedUserIds = (snapshot.get("blockedUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()

                val completedRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(snapshot, threadId)
                val activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(snapshot, threadId)

                trySend(
                    DirectThreadBlockState(
                        isBlocked = blockedUserIds.contains(targetUserId),
                        hiddenRanges = completedRanges + listOfNotNull(activeRange)
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

        if (targetUserId.isBlank() || targetUserId == currentUserId) return

        firestore.collection("reports")
            .add(
                mapOf(
                    "type" to "user",
                    "reportedUserId" to targetUserId,
                    "reporterId" to currentUserId,
                    "threadId" to threadId,
                    "reason" to reason,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            .await()
    }
    fun observeUnreadMessageCount(): Flow<Int> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null

        fun attachForUser(userId: String) {
            listenerRegistration?.remove()
            listenerRegistration = null

            if (userId.isBlank()) {
                trySend(0)
                return
            }

            listenerRegistration = firestore.collection(MESSAGE_THREADS_COLLECTION)
                .whereArrayContains("participantIds", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(0)
                        return@addSnapshotListener
                    }

                    val totalUnread = snapshot?.documents.orEmpty().sumOf { document ->
                        val unreadCounts = document.get("unreadCounts") as? Map<*, *>
                        val rawUnreadCount = (unreadCounts?.get(userId) as? Number)?.toInt() ?: 0

                        val lastMessageHiddenFor = document.getStringListCompat("lastMessageHiddenFor")
                        val lastMessageCreatedAt = document.getLong("lastMessageCreatedAt") ?: 0L
                        val clearedAtMap = document.get("clearedAt") as? Map<*, *>
                        val currentUserClearedAt = (clearedAtMap?.get(userId) as? Number)?.toLong() ?: 0L

                        if (lastMessageHiddenFor.contains(userId) || lastMessageCreatedAt <= currentUserClearedAt) {
                            0
                        } else {
                            rawUnreadCount
                        }
                    }

                    trySend(totalUnread)
                }
        }
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            attachForUser(firebaseAuth.currentUser?.uid.orEmpty())
        }

        auth.addAuthStateListener(authListener)
        attachForUser(auth.currentUser?.uid.orEmpty())

        awaitClose {
            listenerRegistration?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    suspend fun startDirectMessage(targetUserId: String): MessageThreadUiModel {
        return createOrGetDirectThread(targetUserId)
            ?: throw IllegalStateException("DIRECT_THREAD_COULD_NOT_BE_CREATED")
    }


    fun observeMyProfilePosts(): Flow<List<SocialPost>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("posts")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isDraft", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        mapSocialPostDocument(
                            document = document,
                            currentUserId = currentUserId
                        )
                    }
                    .filterNot { post -> post.isDraft }
                    .sortedByDescending { post -> post.createdAt }

                launch {
                    trySend(hydratePostAuthors(posts))
                }
            }

        awaitClose { listener.remove() }
    }

    fun observeMyDraftPosts(): Flow<List<SocialPost>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("posts")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isDraft", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val drafts = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        mapSocialPostDocument(
                            document = document,
                            currentUserId = currentUserId
                        )
                    }
                    .filter { post -> post.isDraft }
                    .sortedByDescending { post -> post.createdAt }

                launch {
                    trySend(hydratePostAuthors(drafts))
                }
            }

        awaitClose { listener.remove() }
    }

    fun observeSavedPosts(): Flow<List<SocialPost>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("posts")
            .whereArrayContains("savedBy", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        mapSocialPostDocument(
                            document = document,
                            currentUserId = currentUserId
                        )
                    }
                    .filterNot { post -> post.isDraft }
                    .sortedByDescending { post -> post.createdAt }

                launch {
                    trySend(hydratePostAuthors(posts))
                }
            }

        awaitClose { listener.remove() }
    }


    fun observePost(postId: String): Flow<SocialPost?> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (postId.isBlank() || currentUserId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("posts")
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val post = snapshot
                    ?.takeIf { document -> document.exists() }
                    ?.let { document ->
                        mapSocialPostDocument(
                            document = document,
                            currentUserId = currentUserId
                        )
                    }
                    ?.takeIf { post -> !post.isDraft || post.userId == currentUserId }

                launch {
                    trySend(post?.let { hydratePostAuthors(listOf(it)).firstOrNull() })
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun getPostById(postId: String): SocialPost? {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (postId.isBlank() || currentUserId.isBlank()) return null

        val snapshot = firestore.collection("posts")
            .document(postId)
            .get()
            .await()

        if (!snapshot.exists()) return null

        val post = mapSocialPostDocument(
            document = snapshot,
            currentUserId = currentUserId
        ) ?: return null

        return hydratePostAuthors(listOf(post)).firstOrNull()
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
        val currentUserId = auth.currentUser?.uid ?: return
        if (draftId.isBlank()) return

        val postRef = firestore.collection("posts").document(draftId)
        val snapshot = postRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("DRAFT_NOT_FOUND")
        }

        if (snapshot.getString("userId") != currentUserId) {
            throw IllegalStateException("DRAFT_OWNER_MISMATCH")
        }

        val finalImageUrl = imageUri?.let { uri ->
            uploadPostImage(uri)
        } ?: existingImageUrl ?: snapshot.getString("imageUrl")

        val finalVisibility = if (publish) {
            visibility.ifBlank { POST_VISIBILITY_COMMUNITY }
        } else {
            POST_VISIBILITY_PRIVATE
        }

        val now = System.currentTimeMillis()

        val updates = mutableMapOf<String, Any?>(
            "content" to content.trim(),
            "imageUrl" to finalImageUrl,
            "mealInfo" to mealInfo?.trim(),
            "calories" to calories,
            "allowComments" to allowComments,
            "visibility" to finalVisibility,
            "tags" to tags,
            "isDraft" to !publish,
            "updatedAt" to now
        )

        if (publish) {
            updates["publishedAt"] = now
        }

        postRef.set(updates, SetOptions.merge()).await()
    }


    suspend fun updateCurrentUserProfile(
        displayName: String,
        bio: String
    ) {
        val user = auth.currentUser ?: return
        val newVisibleName = SocialTextResolver.normalizeUsername(displayName)
        SocialTextResolver.validateVisibleName(newVisibleName)

        val cleanBio = bio.trim().take(160)
        val now = System.currentTimeMillis()
        val userRef = firestore.collection("users").document(user.uid)
        val newUsernameRef = firestore.collection("usernames").document(newVisibleName)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val oldUsernameCandidates = buildSet {
                add(userSnapshot.getString("userName"))
                add(userSnapshot.getString("username"))
                add(userSnapshot.getString("displayName"))
            }.filterNotNull()
                .map { SocialTextResolver.normalizeUsername(it) }
                .filter { it.isNotBlank() }
                .toSet()

            val newUsernameSnapshot = transaction.get(newUsernameRef)
            val ownerUid = newUsernameSnapshot.getString("uid")

            if (newUsernameSnapshot.exists() && ownerUid != user.uid) {
                throw IllegalStateException("VISIBLE_NAME_TAKEN")
            }

            val oldUsernameSnapshots = oldUsernameCandidates
                .filterNot { it == newVisibleName }
                .map { oldUsername ->
                    val oldUsernameRef = firestore.collection("usernames").document(oldUsername)
                    oldUsernameRef to transaction.get(oldUsernameRef)
                }

            oldUsernameSnapshots.forEach { (oldUsernameRef, oldUsernameSnapshot) ->
                if (oldUsernameSnapshot.exists() && oldUsernameSnapshot.getString("uid") == user.uid) {
                    transaction.delete(oldUsernameRef)
                }
            }

            transaction.set(
                newUsernameRef,
                mapOf(
                    "uid" to user.uid,
                    "email" to user.email.orEmpty(),
                    "username" to newVisibleName,
                    "usernameLowercase" to newVisibleName.lowercase(Locale.ROOT),
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to newVisibleName,
                    "displayNameLowercase" to newVisibleName.lowercase(Locale.ROOT),
                    "userName" to newVisibleName,
                    "username" to newVisibleName,
                    "usernameLowercase" to newVisibleName.lowercase(Locale.ROOT),
                    "bio" to cleanBio,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )

            null
        }.await()

        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(newVisibleName)
                    .build()
            ).await()
        }

        updateOwnedPublicUsernameCopies(
            uid = user.uid,
            userName = newVisibleName,
            now = now
        )
    }

    suspend fun updateCurrentUserPhotoUrl(photoUrl: String?) {
        val user = auth.currentUser ?: return
        val now = System.currentTimeMillis()
        val userRef = firestore.collection("users").document(user.uid)

        val updates = if (photoUrl.isNullOrBlank()) {
            mapOf(
                "photoUrl" to FieldValue.delete(),
                "updatedAt" to now
            )
        } else {
            mapOf(
                "photoUrl" to photoUrl,
                "updatedAt" to now
            )
        }

        userRef.set(updates, SetOptions.merge()).await()

        runCatching {
            val statusUpdates = if (photoUrl.isNullOrBlank()) {
                mapOf<String, Any?>(
                    "photoUrl" to null,
                    "updatedAt" to now
                )
            } else {
                mapOf<String, Any?>(
                    "photoUrl" to photoUrl,
                    "updatedAt" to now
                )
            }

            realtimeDatabase.reference
                .child(REALTIME_STATUS_NODE)
                .child(user.uid)
                .updateChildren(statusUpdates)
                .await()
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setPhotoUri(photoUrl?.let { Uri.parse(it) })
                    .build()
            ).await()
        }

        // Profil belgesi başarıyla güncellendiyse işlem kullanıcı açısından başarılıdır.
        // Post/yorum/mesaj kopyaları denormalize alanlar olduğu için ayrı ayrı en iyi
        // çabayla güncellenir; bu kopyalardan biri izin/index sebebiyle güncellenemezse
        // kullanıcıya yanlışlıkla "hata oldu" göstermeyelim.
        runCatching {
            updateOwnedProfilePhotoCopies(
                uid = user.uid,
                photoUrl = photoUrl,
                now = now
            )
        }.onFailure { error ->
            error.printStackTrace()
        }
    }

    suspend fun removeFollower(followerId: String) {
        val currentUser = auth.currentUser ?: return
        if (followerId.isBlank() || followerId == currentUser.uid) return

        val followRef = firestore.collection("follows")
            .document("${followerId}_${currentUser.uid}")
        val followerRef = firestore.collection("users").document(followerId)
        val currentUserRef = firestore.collection("users").document(currentUser.uid)

        firestore.runTransaction { transaction ->
            val followSnapshot = transaction.get(followRef)
            if (!followSnapshot.exists()) return@runTransaction null

            val followerSnapshot = transaction.get(followerRef)
            val currentSnapshot = transaction.get(currentUserRef)

            transaction.delete(followRef)
            transaction.set(
                followerRef,
                mapOf(
                    "followingCount" to ((followerSnapshot.getLong("followingCount") ?: 0L) - 1L).coerceAtLeast(0L),
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            transaction.set(
                currentUserRef,
                mapOf(
                    "followersCount" to ((currentSnapshot.getLong("followersCount") ?: 0L) - 1L).coerceAtLeast(0L),
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            null
        }.await()
    }

    suspend fun stopFollowing(followingId: String) {
        val currentUser = auth.currentUser ?: return
        if (followingId.isBlank() || followingId == currentUser.uid) return

        val followRef = firestore.collection("follows")
            .document("${currentUser.uid}_${followingId}")
        val currentUserRef = firestore.collection("users").document(currentUser.uid)
        val followingUserRef = firestore.collection("users").document(followingId)

        firestore.runTransaction { transaction ->
            val followSnapshot = transaction.get(followRef)
            if (!followSnapshot.exists()) return@runTransaction null

            val currentSnapshot = transaction.get(currentUserRef)
            val followingSnapshot = transaction.get(followingUserRef)

            transaction.delete(followRef)
            transaction.set(
                currentUserRef,
                mapOf(
                    "followingCount" to ((currentSnapshot.getLong("followingCount") ?: 0L) - 1L).coerceAtLeast(0L),
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            transaction.set(
                followingUserRef,
                mapOf(
                    "followersCount" to ((followingSnapshot.getLong("followersCount") ?: 0L) - 1L).coerceAtLeast(0L),
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            null
        }.await()
    }

    suspend fun uploadCurrentUserProfilePhoto(
        context: Context,
        imageUri: Uri
    ): String {
        val user = auth.currentUser ?: return ""
        val storageRef = storage.reference
            .child("profile_photos")
            .child(user.uid)
            .child("${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")

        val metadata = StorageMetadata.Builder()
            .setContentType(context.contentResolver.getType(imageUri) ?: "image/jpeg")
            .build()

        storageRef.putFile(imageUri, metadata).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun getFollowers(userId: String): List<SocialUser> {
        if (userId.isBlank()) return emptyList()

        val followerIds = firestore.collection("follows")
            .whereEqualTo("followingId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { document -> document.getString("followerId") }
            .distinct()

        return getUsersByIds(followerIds)
    }

    suspend fun getFollowing(userId: String): List<SocialUser> {
        if (userId.isBlank()) return emptyList()

        val followingIds = firestore.collection("follows")
            .whereEqualTo("followerId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { document -> document.getString("followingId") }
            .distinct()

        return getUsersByIds(followingIds)
    }

    private suspend fun getUsersByIds(userIds: List<String>): List<SocialUser> {
        if (userIds.isEmpty()) return emptyList()

        val users = mutableMapOf<String, SocialUser>()

        userIds.filter { it.isNotBlank() }
            .distinct()
            .chunked(10)
            .forEach { chunk ->
                val snapshot = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                snapshot.documents.forEach { document ->
                    if (document.getBoolean("isDeleted") == true) return@forEach

                    users[document.id] = SocialUserDocumentResolver.resolveProfile(
                        id = document.id,
                        userName = document.getString("userName"),
                        username = document.getString("username"),
                        displayName = document.getString("displayName"),
                        name = document.getString("name"),
                        realName = document.getString("realName"),
                        email = document.getString("email"),
                        photoUrl = document.getString("photoUrl"),
                        bio = document.getString("bio"),
                        followersCount = document.getLong("followersCount")?.toInt() ?: 0,
                        followingCount = document.getLong("followingCount")?.toInt() ?: 0
                    )
                }
            }

        return userIds.mapNotNull { users[it] }
    }

    private suspend fun updateOwnedPublicUsernameCopies(
        uid: String,
        userName: String,
        now: Long
    ) {
        runCatching {
            val posts = firestore.collection("posts")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = posts,
                updates = SocialProfileCopyUpdateResolver.publicUsernameUpdate(
                    userName = userName,
                    now = now
                )
            )
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            val comments = firestore.collectionGroup("comments")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = comments,
                updates = SocialProfileCopyUpdateResolver.publicUsernameUpdate(
                    userName = userName,
                    now = now
                )
            )
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            val messageThreads = firestore.collection(MESSAGE_THREADS_COLLECTION)
                .whereArrayContains("participantIds", uid)
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = messageThreads,
                updates = SocialProfileCopyUpdateResolver.participantUsernameUpdate(
                    uid = uid,
                    userName = userName,
                    now = now
                ),
                useSetMerge = false
            )
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            val messages = firestore.collectionGroup(MESSAGES_SUBCOLLECTION)
                .whereEqualTo("senderId", uid)
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = messages,
                updates = SocialProfileCopyUpdateResolver.senderUsernameUpdate(
                    userName = userName,
                    now = now
                )
            )
        }.onFailure { error ->
            error.printStackTrace()
        }
    }

    private suspend fun updateOwnedProfilePhotoCopies(
        uid: String,
        photoUrl: String?,
        now: Long
    ) {
        // Her alanı ayrı güncelliyoruz. Böylece örneğin collectionGroup yorum
        // sorgusu izin/index yüzünden başarısız olsa bile postlar ve mesaj
        // thread'leri güncellenmeye devam eder.
        runCatching {
            val posts = firestore.collection("posts")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = posts,
                updates = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
                    fieldNames = listOf("userPhotoUrl", "photoUrl"),
                    photoUrl = photoUrl,
                    now = now
                )
            )
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            val comments = firestore.collectionGroup("comments")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = comments,
                updates = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
                    fieldNames = listOf("userPhotoUrl", "photoUrl"),
                    photoUrl = photoUrl,
                    now = now
                )
            )
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            val messageThreads = firestore.collection(MESSAGE_THREADS_COLLECTION)
                .whereArrayContains("participantIds", uid)
                .get()
                .await()
                .documents

            val updates = SocialProfileCopyUpdateResolver.participantPhotoUpdate(
                uid = uid,
                photoUrl = photoUrl,
                now = now
            )

            updateDocumentsInBatches(
                documents = messageThreads,
                updates = updates,
                useSetMerge = false
            )
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            val messages = firestore.collectionGroup(MESSAGES_SUBCOLLECTION)
                .whereEqualTo("senderId", uid)
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = messages,
                updates = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
                    fieldNames = listOf("senderPhotoUrl", "photoUrl"),
                    photoUrl = photoUrl,
                    now = now
                )
            )
        }.onFailure { error ->
            error.printStackTrace()
        }
    }


    private suspend fun updateDocumentsInBatches(
        documents: List<DocumentSnapshot>,
        updates: Map<String, Any>,
        useSetMerge: Boolean = true
    ) {
        if (documents.isEmpty()) return

        documents.chunked(450).forEach { chunk ->
            val batch = firestore.batch()

            chunk.forEach { document ->
                if (useSetMerge) {
                    batch.set(document.reference, updates, SetOptions.merge())
                } else {
                    batch.update(document.reference, updates)
                }
            }

            batch.commit().await()
        }
    }

    private suspend fun uploadPostImage(uri: Uri): String? {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child("post_images/$fileName")

            imageRef.putFile(uri).await()

            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapMessageThreadDocument(
        document: DocumentSnapshot,
        currentUserId: String,
        includeClearedThread: Boolean = false
    ): MessageThreadUiModel? {
        if (!document.exists()) return null

        return SocialMessageThreadDocumentResolver.resolve(
            id = document.id,
            type = document.getString("type"),
            currentUserId = currentUserId,
            participantIds = document.getStringListCompat("participantIds"),
            participants = document.get("participants") as? Map<*, *>,
            groupTitle = document.getString("title"),
            groupPhotoUrl = document.getString("photoUrl"),
            lastMessageText = document.getString("lastMessageText"),
            lastMessageCreatedAt = document.getLong("lastMessageCreatedAt"),
            updatedAt = document.getLong("updatedAt"),
            clearedAt = document.get("clearedAt") as? Map<*, *>,
            unreadCounts = document.get("unreadCounts") as? Map<*, *>,
            memberCount = document.getLong("memberCount")?.toInt(),
            includeClearedThread = includeClearedThread
        )
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
        const val REALTIME_STATUS_NODE = "status"
        const val REALTIME_TYPING_NODE = "typing"
        const val REALTIME_CONNECTED_NODE = ".info/connected"
        const val PRESENCE_ONLINE = "online"
        const val PRESENCE_OFFLINE = "offline"
        const val TYPING_STALE_MILLIS = 8_000L

    }
}
