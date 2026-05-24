package com.example.fitto.feature_home.presentation.social

import android.content.Context
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPermission
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPublisher
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object SocialNotificationObserver {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var messageRegistration: ListenerRegistration? = null
    private var postRegistration: ListenerRegistration? = null
    private var followRegistration: ListenerRegistration? = null
    private var followingRegistration: ListenerRegistration? = null
    private val followingPostRegistrations = mutableListOf<ListenerRegistration>()
    private val commentRegistrations = mutableMapOf<String, ListenerRegistration>()
    private var currentUserId: String? = null
    private var listenerStartedAt: Long = 0L
    private val deliveredNotificationKeys = mutableSetOf<String>()
    private val knownPostLikes = mutableMapOf<String, Set<String>>()
    private val knownCommentIds = mutableMapOf<String, MutableSet<String>>()
    private var knownFollowerIds: Set<String>? = null
    private var knownFollowingIds: Set<String>? = null
    private var knownFollowingPostIds: MutableSet<String>? = null
    private var hasPersistedFollowingPostHistory = false
    private val initializedFollowingPostSources = mutableSetOf<String>()
    private val newlyFollowedUserIds = mutableSetOf<String>()

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun stateKey(userId: String, name: String): String = "$userId:$name"

    private fun readKnownSet(context: Context, userId: String, name: String): Set<String>? {
        return preferences(context)
            .getStringSet(stateKey(userId, name), null)
            ?.toSet()
    }

    private fun saveKnownSet(context: Context, userId: String, name: String, values: Set<String>) {
        preferences(context)
            .edit()
            .putStringSet(stateKey(userId, name), values)
            .apply()
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (authListener != null) return

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            attachForUser(
                context = appContext,
                userId = firebaseAuth.currentUser?.uid.orEmpty()
            )
        }.also { listener ->
            auth.addAuthStateListener(listener)
        }

        attachForUser(
            context = appContext,
            userId = auth.currentUser?.uid.orEmpty()
        )
    }

    fun stop() {
        clearRegistrations()
        authListener?.let { auth.removeAuthStateListener(it) }
        authListener = null
        currentUserId = null
    }

    private fun attachForUser(context: Context, userId: String) {
        if (userId == currentUserId) return

        clearRegistrations()
        currentUserId = userId.takeIf { it.isNotBlank() }
        deliveredNotificationKeys.clear()
        knownPostLikes.clear()
        knownCommentIds.clear()
        knownFollowerIds = null
        knownFollowingIds = null
        knownFollowingPostIds = null
        hasPersistedFollowingPostHistory = false
        initializedFollowingPostSources.clear()
        newlyFollowedUserIds.clear()

        if (userId.isBlank()) return

        listenerStartedAt = System.currentTimeMillis()
        observeMessageThreads(context, userId)
        observeOwnedPosts(context, userId)
        observeFollowers(context, userId)
        observeFollowingPosts(context, userId)
    }

    private fun clearRegistrations() {
        messageRegistration?.remove()
        messageRegistration = null
        postRegistration?.remove()
        postRegistration = null
        followRegistration?.remove()
        followRegistration = null
        followingRegistration?.remove()
        followingRegistration = null
        followingPostRegistrations.forEach { it.remove() }
        followingPostRegistrations.clear()
        initializedFollowingPostSources.clear()
        newlyFollowedUserIds.clear()
        commentRegistrations.values.forEach { it.remove() }
        commentRegistrations.clear()
    }

    private fun observeMessageThreads(context: Context, userId: String) {
        messageRegistration = firestore.collection(SocialRepository.MESSAGE_THREADS_COLLECTION)
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.documents.orEmpty().forEach { document ->
                    val unreadCounts = document.get("unreadCounts") as? Map<*, *>
                    val unreadCount = (unreadCounts?.get(userId) as? Number)?.toInt() ?: 0

                    val lastMessageCreatedAt = document.getLong("lastMessageCreatedAt") ?: 0L
                    val clearedAtMap = document.get("clearedAt") as? Map<*, *>
                    val currentUserClearedAt = (clearedAtMap?.get(userId) as? Number)?.toLong() ?: 0L

                    if (unreadCount <= 0 || lastMessageCreatedAt <= currentUserClearedAt) {
                        FittoNotificationStore.removeMessageNotificationsForThread(context, document.id)
                        return@forEach
                    }
                    val lastMessageSenderId = document.getString("lastMessageSenderId").orEmpty()
                    val lastMessageHiddenFor = document.getStringListCompat("lastMessageHiddenFor")

                    val shouldNotify =
                        lastMessageCreatedAt > 0L &&
                                lastMessageCreatedAt >= listenerStartedAt - INITIAL_GRACE_MILLIS &&
                                lastMessageSenderId.isNotBlank() &&
                                lastMessageSenderId != userId &&
                                !lastMessageHiddenFor.contains(userId)

                    if (!shouldNotify) return@forEach

                    val notificationKey = "message_${document.id}"
                    val deliveryKey = "${notificationKey}_$lastMessageCreatedAt"
                    if (!deliveredNotificationKeys.add(deliveryKey)) return@forEach

                    val isGroup = document.getString("type") == THREAD_TYPE_GROUP
                    val participants = document.get("participants") as? Map<*, *>
                    val sender = participants?.get(lastMessageSenderId) as? Map<*, *>
                    val senderName = (sender?.get("userName") as? String)
                        ?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.social_notification_someone)

                    val title = if (isGroup) {
                        val groupTitle = document.getString("title").orEmpty()
                        if (groupTitle.isBlank()) {
                            context.getString(R.string.social_notification_group_message, senderName)
                        } else {
                            "$groupTitle · $senderName"
                        }
                    } else {
                        context.getString(R.string.social_notification_message, senderName)
                    }

                    FittoNotificationPublisher.showSocialNotification(
                        context = context,
                        notification = FittoNotification(
                            id = notificationKey,
                            title = title,
                            description = context.getString(
                                if (isGroup) R.string.social_notification_new_group_messages_count else R.string.social_notification_new_messages_count,
                                unreadCount
                            ),
                            iconRes = if (isGroup) R.drawable.ic_group else R.drawable.ic_messages,
                            type = if (isGroup) FittoNotification.TYPE_GROUP_MESSAGE else FittoNotification.TYPE_MESSAGE,
                            targetId = document.id,
                            isRead = false,
                            createdAt = lastMessageCreatedAt
                        )
                    )
                }
            }
    }


    private fun observeFollowers(context: Context, userId: String) {
        followRegistration = firestore.collection("follows")
            .whereEqualTo("followingId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val documents = snapshot?.documents.orEmpty()
                val currentFollowers = documents
                    .mapNotNull { it.getString("followerId") }
                    .filter { it.isNotBlank() && it != userId }
                    .toSet()

                val previousFollowers = knownFollowerIds
                    ?: readKnownSet(context, userId, KEY_FOLLOWERS).also { knownFollowerIds = it }

                if (previousFollowers == null) {
                    knownFollowerIds = currentFollowers
                    saveKnownSet(context, userId, KEY_FOLLOWERS, currentFollowers)
                    return@addSnapshotListener
                }

                val addedFollowers = currentFollowers - previousFollowers
                val removedFollowers = previousFollowers - currentFollowers
                knownFollowerIds = currentFollowers
                saveKnownSet(context, userId, KEY_FOLLOWERS, currentFollowers)

                addedFollowers.forEach { followerId ->
                    val createdAt = documents
                        .firstOrNull { it.getString("followerId") == followerId }
                        ?.getLong("createdAt") ?: System.currentTimeMillis()

                    val notificationId = "follow_${followerId}_$createdAt"
                    if (!deliveredNotificationKeys.add(notificationId)) return@forEach

                    FittoNotificationPublisher.showInAppSocialNotification(
                        context = context,
                        notification = FittoNotification(
                            id = notificationId,
                            title = context.getString(
                                R.string.social_notification_follow,
                                context.getString(R.string.social_notification_someone)
                            ),
                            description = context.getString(R.string.social_notification_follow_detail),
                            iconRes = R.drawable.ic_person_add,
                            type = FittoNotification.TYPE_FOLLOW,
                            targetId = followerId,
                            isRead = false,
                            createdAt = createdAt
                        )
                    )
                }

                removedFollowers.forEach { followerId ->
                    val now = System.currentTimeMillis()
                    val notificationId = "unfollow_${followerId}_$now"
                    if (!deliveredNotificationKeys.add(notificationId)) return@forEach

                    FittoNotificationPublisher.showInAppSocialNotification(
                        context = context,
                        notification = FittoNotification(
                            id = notificationId,
                            title = context.getString(
                                R.string.social_notification_unfollow,
                                context.getString(R.string.social_notification_someone)
                            ),
                            description = context.getString(R.string.social_notification_unfollow_detail),
                            iconRes = R.drawable.ic_person_add,
                            type = FittoNotification.TYPE_UNFOLLOW,
                            targetId = followerId,
                            isRead = false,
                            createdAt = now
                        )
                    )
                }
            }
    }

    private fun observeOwnedPosts(context: Context, userId: String) {
        postRegistration = firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val activePostIds = snapshot?.documents.orEmpty().map { it.id }.toSet()
                val removedPostIds = commentRegistrations.keys - activePostIds
                removedPostIds.forEach { postId ->
                    commentRegistrations.remove(postId)?.remove()
                    knownCommentIds.remove(postId)
                    knownPostLikes.remove(postId)
                }

                snapshot?.documents.orEmpty().forEach { document ->
                    handlePostLikeChanges(context, userId, document)
                    observeCommentsForPost(context, userId, document.id)
                }
            }
    }

    private fun handlePostLikeChanges(context: Context, userId: String, document: DocumentSnapshot) {
        val postId = document.id
        val likedBy = document.getStringListCompat("likedBy").toSet()
        val stateName = "$KEY_POST_LIKES_PREFIX$postId"
        val previous = knownPostLikes[postId]
            ?: readKnownSet(context, userId, stateName).also { if (it != null) knownPostLikes[postId] = it }

        if (previous == null) {
            knownPostLikes[postId] = likedBy
            saveKnownSet(context, userId, stateName, likedBy)
            return
        }

        val newLikers = likedBy - previous - userId
        knownPostLikes[postId] = likedBy
        saveKnownSet(context, userId, stateName, likedBy)

        newLikers.forEach { likerId ->
            val deliveryKey = "post_like_${postId}_${likerId}_${likedBy.size}"
            if (!deliveredNotificationKeys.add(deliveryKey)) return@forEach

            val actorName = context.getString(R.string.social_notification_someone)
            FittoNotificationPublisher.showInAppSocialNotification(
                context = context,
                notification = FittoNotification(
                    id = deliveryKey,
                    title = context.getString(R.string.social_notification_post_like, actorName),
                    description = context.getString(R.string.social_notification_open_detail),
                    iconRes = R.drawable.ic_notifications,
                    type = FittoNotification.TYPE_POST_LIKE,
                    targetId = postId,
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun observeCommentsForPost(context: Context, userId: String, postId: String) {
        if (commentRegistrations.containsKey(postId)) return

        commentRegistrations[postId] = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val currentIds = snapshot?.documents.orEmpty().map { it.id }.toMutableSet()
                val stateName = "$KEY_POST_COMMENTS_PREFIX$postId"
                val knownIds = knownCommentIds[postId]
                    ?: readKnownSet(context, userId, stateName)?.toMutableSet()?.also { knownCommentIds[postId] = it }

                if (knownIds == null) {
                    knownCommentIds[postId] = currentIds
                    saveKnownSet(context, userId, stateName, currentIds)
                    return@addSnapshotListener
                }

                snapshot?.documents.orEmpty().forEach { comment ->
                    if (knownIds.contains(comment.id)) return@forEach

                    val authorId = comment.getString("userId").orEmpty()
                    val createdAt = comment.getLong("createdAt") ?: 0L
                    if (authorId == userId) {
                        knownIds.add(comment.id)
                        return@forEach
                    }

                    val actorName = comment.getString("userName")
                        ?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.social_notification_someone)
                    val isReply = !comment.getString("parentCommentId").isNullOrBlank()
                    val type = if (isReply) FittoNotification.TYPE_COMMENT_REPLY else FittoNotification.TYPE_POST_COMMENT
                    val notificationId = "${type}_${postId}_${comment.id}"
                    if (deliveredNotificationKeys.add(notificationId)) {
                        FittoNotificationPublisher.showInAppSocialNotification(
                            context = context,
                            notification = FittoNotification(
                                id = notificationId,
                                title = context.getString(
                                    if (isReply) R.string.social_notification_comment_reply else R.string.social_notification_post_comment,
                                    actorName
                                ),
                                description = context.getString(R.string.social_notification_open_detail),
                                iconRes = if (isReply) R.drawable.ic_reply else R.drawable.ic_comment,
                                type = type,
                                targetId = postId,
                                secondaryTargetId = comment.id,
                                isRead = false,
                                createdAt = createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                            )
                        )
                    }

                    knownIds.add(comment.id)
                }

                knownIds.addAll(currentIds)
                saveKnownSet(context, userId, stateName, knownIds)
            }
    }


    private fun observeFollowingPosts(context: Context, userId: String) {
        followingRegistration = firestore.collection("follows")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val currentFollowing = snapshot?.documents.orEmpty()
                    .mapNotNull { it.getString("followingId") }
                    .filter { it.isNotBlank() && it != userId }
                    .toSet()

                val previousFollowing = knownFollowingIds
                    ?: readKnownSet(context, userId, KEY_FOLLOWING_USERS).also { knownFollowingIds = it }

                val addedFollowing = previousFollowing
                    ?.let { currentFollowing - it }
                    .orEmpty()

                knownFollowingIds = currentFollowing
                saveKnownSet(context, userId, KEY_FOLLOWING_USERS, currentFollowing)

                if (previousFollowing == currentFollowing && followingPostRegistrations.isNotEmpty()) {
                    return@addSnapshotListener
                }

                newlyFollowedUserIds.clear()
                newlyFollowedUserIds.addAll(addedFollowing)
                attachFollowingPostListeners(context, userId, currentFollowing)
            }
    }

    private fun attachFollowingPostListeners(
        context: Context,
        userId: String,
        followingIds: Set<String>
    ) {
        followingPostRegistrations.forEach { it.remove() }
        followingPostRegistrations.clear()
        initializedFollowingPostSources.clear()

        val persistedKnownIds = readKnownSet(context, userId, KEY_FOLLOWING_POSTS)
        hasPersistedFollowingPostHistory = persistedKnownIds != null
        knownFollowingPostIds = persistedKnownIds?.toMutableSet() ?: mutableSetOf()

        if (followingIds.isEmpty()) {
            saveKnownSet(context, userId, KEY_FOLLOWING_POSTS, emptySet())
            return
        }

        followingIds.chunked(FIRESTORE_IN_QUERY_LIMIT).forEachIndexed { index, chunk ->
            val sourceKey = "following_posts_$index"
            val registration = firestore.collection("posts")
                .whereIn("userId", chunk)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    handleFollowingPostSnapshot(
                        context = context,
                        userId = userId,
                        sourceKey = sourceKey,
                        sourceUserIds = chunk.toSet(),
                        documents = snapshot?.documents.orEmpty()
                    )
                }

            followingPostRegistrations.add(registration)
        }
    }

    private fun handleFollowingPostSnapshot(
        context: Context,
        userId: String,
        sourceKey: String,
        sourceUserIds: Set<String>,
        documents: List<DocumentSnapshot>
    ) {
        val visibleDocuments = documents.filter { document ->
            val authorId = document.getString("userId").orEmpty()
            val visibility = document.getString("visibility").orEmpty().ifBlank { POST_VISIBILITY_COMMUNITY }
            val isDraft = document.getBoolean("isDraft") ?: false

            authorId.isNotBlank() &&
                    authorId != userId &&
                    !isDraft &&
                    (visibility == POST_VISIBILITY_COMMUNITY || visibility == POST_VISIBILITY_FOLLOWERS)
        }

        val currentPostIds = visibleDocuments.map { it.id }.toSet()
        val knownIds = knownFollowingPostIds ?: mutableSetOf<String>().also { freshKnownIds ->
            knownFollowingPostIds = freshKnownIds
        }
        val isFirstSnapshotForSource = initializedFollowingPostSources.add(sourceKey)

        if (isFirstSnapshotForSource && !hasPersistedFollowingPostHistory) {
            knownIds.addAll(currentPostIds)
            saveKnownSet(context, userId, KEY_FOLLOWING_POSTS, knownIds)
            newlyFollowedUserIds.removeAll(sourceUserIds)
            return
        }

        visibleDocuments.forEach { document ->
            if (document.id in knownIds) return@forEach

            val authorId = document.getString("userId").orEmpty()
            if (isFirstSnapshotForSource && authorId in newlyFollowedUserIds) {
                knownIds.add(document.id)
                return@forEach
            }

            val createdAt = document.getLong("createdAt") ?: 0L

            val authorName = document.getString("userName")
                ?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.social_notification_someone)
            val contentPreview = document.getString("content")
                .orEmpty()
                .trim()
                .ellipsize(MAX_POST_NOTIFICATION_PREVIEW)
                .ifBlank { context.getString(R.string.social_notification_following_post_detail) }
            val notificationId = "following_post_${document.id}"

            if (deliveredNotificationKeys.add(notificationId)) {
                val notification = FittoNotification(
                    id = notificationId,
                    title = context.getString(R.string.social_notification_following_post, authorName),
                    description = contentPreview,
                    iconRes = R.drawable.ic_notifications,
                    type = FittoNotification.TYPE_FOLLOWING_POST,
                    targetId = document.id,
                    isRead = false,
                    createdAt = createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                )

                if (FittoNotificationPermission.hasPermission(context)) {
                    FittoNotificationPublisher.showSocialNotification(
                        context = context,
                        notification = notification
                    )
                } else {
                    FittoNotificationPublisher.showInAppSocialNotification(
                        context = context,
                        notification = notification
                    )
                }
            }

            knownIds.add(document.id)
        }

        knownIds.addAll(currentPostIds)
        if (isFirstSnapshotForSource) {
            newlyFollowedUserIds.removeAll(sourceUserIds)
        }
        saveKnownSet(context, userId, KEY_FOLLOWING_POSTS, knownIds)
    }

    private fun String.ellipsize(maxLength: Int): String {
        return if (length <= maxLength) {
            this
        } else {
            take(maxLength - 1).trimEnd() + "…"
        }
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private const val THREAD_TYPE_GROUP = "group"
    private const val INITIAL_GRACE_MILLIS = 10_000L
    private const val PREF_NAME = "fitto_social_observer_state"
    private const val KEY_FOLLOWERS = "followers"
    private const val KEY_FOLLOWING_USERS = "following_users"
    private const val KEY_FOLLOWING_POSTS = "following_posts"
    private const val KEY_POST_LIKES_PREFIX = "post_likes:"
    private const val KEY_POST_COMMENTS_PREFIX = "post_comments:"
    private const val FIRESTORE_IN_QUERY_LIMIT = 10
    private const val MAX_POST_NOTIFICATION_PREVIEW = 90
}
