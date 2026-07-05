package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.core.text.AppLocales
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Locale

class AccountDeletionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val realtimeDatabase: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun deleteCurrentAccount(
        deletedUserName: String,
        deletedMessageText: String
    ) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("AUTH_USER_NOT_FOUND")
        val uid = currentUser.uid
        val now = System.currentTimeMillis()

        val userRef = firestore.collection(USERS_COLLECTION).document(uid)
        val userSnapshot = userRef.get().await()
        val usernameCandidates = usernameCandidates(
            userSnapshot = userSnapshot,
            authDisplayName = currentUser.displayName,
            authEmail = currentUser.email
        )

        cleanupCommentsWrittenByUser(uid)
        cleanupPostLikesAndSaves(uid)
        cleanupCommentLikes(uid)
        cleanupFollows(uid)
        cleanupMessageThreads(
            uid = uid,
            deletedUserName = deletedUserName,
            deletedMessageText = deletedMessageText,
            now = now
        )
        cleanupPostsOwnedByUser(uid)

        deleteUsernameReservations(
            usernameCandidates = usernameCandidates,
            uid = uid
        )
        anonymizeUserDocument(
            userRef = userRef,
            exists = userSnapshot.exists(),
            uid = uid,
            deletedUserName = deletedUserName,
            now = now
        )

        cleanupRealtimeData(uid)

        currentUser.delete().await()
        auth.signOut()
    }

    private suspend fun cleanupPostsOwnedByUser(uid: String) {
        val posts = firestore.collection(POSTS_COLLECTION)
            .whereEqualTo("userId", uid)
            .get()
            .await()
            .documents

        if (posts.isEmpty()) return

        val writer = BatchWriter(firestore)

        posts.forEach { post ->
            listOfNotNull(
                post.getString("imageUrl"),
                post.getString("photoUrl"),
                post.getString("mediaUrl")
            ).forEach { url ->
                deleteStorageUrlBestEffort(url)
            }

            writer.delete(post.reference)
        }

        writer.commit()
    }

    private suspend fun cleanupCommentsWrittenByUser(uid: String) {
        val comments = firestore.collectionGroup(COMMENTS_SUBCOLLECTION)
            .whereEqualTo("userId", uid)
            .get()
            .await()
            .documents

        if (comments.isEmpty()) return

        val writer = BatchWriter(firestore)
        val postCommentDecrements = linkedMapOf<DocumentReference, Int>()
        val parentReplyDecrements = linkedMapOf<DocumentReference, Int>()
        val deletedCommentPaths = comments.map { comment -> comment.reference.path }.toSet()

        comments.forEach { comment ->
            val postRef = comment.reference.parent.parent
            val parentCommentId = comment.getString("parentCommentId")

            writer.delete(comment.reference)

            if (postRef != null) {
                postCommentDecrements[postRef] = (postCommentDecrements[postRef] ?: 0) + 1

                if (!parentCommentId.isNullOrBlank()) {
                    val parentCommentRef = postRef
                        .collection(COMMENTS_SUBCOLLECTION)
                        .document(parentCommentId)

                    if (parentCommentRef.path !in deletedCommentPaths) {
                        parentReplyDecrements[parentCommentRef] =
                            (parentReplyDecrements[parentCommentRef] ?: 0) + 1
                    }
                }
            }
        }

        postCommentDecrements.forEach { (postRef, decrement) ->
            writer.update(
                postRef,
                mapOf("commentsCount" to FieldValue.increment(-decrement.toLong()))
            )
        }

        parentReplyDecrements.forEach { (commentRef, decrement) ->
            writer.update(
                commentRef,
                mapOf("replyCount" to FieldValue.increment(-decrement.toLong()))
            )
        }

        writer.commit()
    }

    private suspend fun cleanupPostLikesAndSaves(uid: String) {
        val likedPosts = firestore.collection(POSTS_COLLECTION)
            .whereArrayContains("likedBy", uid)
            .get()
            .await()
            .documents

        val savedPosts = firestore.collection(POSTS_COLLECTION)
            .whereArrayContains("savedBy", uid)
            .get()
            .await()
            .documents

        if (likedPosts.isEmpty() && savedPosts.isEmpty()) return

        val writer = BatchWriter(firestore)
        val likedByPostId = likedPosts.associateBy { it.id }
        val savedByPostId = savedPosts.associateBy { it.id }
        val allPostIds = likedByPostId.keys + savedByPostId.keys

        allPostIds.forEach { postId ->
            val likedPost = likedByPostId[postId]
            val savedPost = savedByPostId[postId]
            val post = likedPost ?: savedPost ?: return@forEach
            val updates = mutableMapOf<String, Any>()

            if (likedPost != null) {
                val currentLikes = likedPost.getLong("likesCount")?.toInt() ?: 0
                updates["likedBy"] = FieldValue.arrayRemove(uid)
                updates["likesCount"] = (currentLikes - 1).coerceAtLeast(0)
            }

            if (savedPost != null) {
                updates["savedBy"] = FieldValue.arrayRemove(uid)
            }

            if (updates.isNotEmpty()) {
                writer.update(post.reference, updates)
            }
        }

        writer.commit()
    }

    private suspend fun cleanupCommentLikes(uid: String) {
        val likedComments = firestore.collectionGroup(COMMENTS_SUBCOLLECTION)
            .whereArrayContains("likedBy", uid)
            .get()
            .await()
            .documents

        if (likedComments.isEmpty()) return

        val writer = BatchWriter(firestore)

        likedComments.forEach { comment ->
            val currentLikes = comment.getLong("likesCount")?.toInt() ?: 0
            writer.update(
                comment.reference,
                mapOf(
                    "likedBy" to FieldValue.arrayRemove(uid),
                    "likesCount" to (currentLikes - 1).coerceAtLeast(0)
                )
            )
        }

        writer.commit()
    }

    private suspend fun cleanupFollows(uid: String) {
        val outgoingFollows = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followerId", uid)
            .get()
            .await()
            .documents

        val incomingFollows = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followingId", uid)
            .get()
            .await()
            .documents

        if (outgoingFollows.isEmpty() && incomingFollows.isEmpty()) return

        val writer = BatchWriter(firestore)
        val followerCountDecrements = linkedMapOf<String, Int>()
        val followingCountDecrements = linkedMapOf<String, Int>()

        outgoingFollows.forEach { follow ->
            val followingId = follow.getString("followingId")
            writer.delete(follow.reference)

            if (!followingId.isNullOrBlank()) {
                followerCountDecrements[followingId] =
                    (followerCountDecrements[followingId] ?: 0) + 1
            }
        }

        incomingFollows.forEach { follow ->
            val followerId = follow.getString("followerId")
            writer.delete(follow.reference)

            if (!followerId.isNullOrBlank()) {
                followingCountDecrements[followerId] =
                    (followingCountDecrements[followerId] ?: 0) + 1
            }
        }

        followerCountDecrements.forEach { (userId, decrement) ->
            writer.update(
                firestore.collection(USERS_COLLECTION).document(userId),
                mapOf("followersCount" to FieldValue.increment(-decrement.toLong()))
            )
        }

        followingCountDecrements.forEach { (userId, decrement) ->
            writer.update(
                firestore.collection(USERS_COLLECTION).document(userId),
                mapOf("followingCount" to FieldValue.increment(-decrement.toLong()))
            )
        }

        writer.commit()
    }

    private suspend fun cleanupMessageThreads(
        uid: String,
        deletedUserName: String,
        deletedMessageText: String,
        now: Long
    ) {
        val threads = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .whereArrayContains("participantIds", uid)
            .get()
            .await()
            .documents

        if (threads.isEmpty()) return

        threads.forEach { thread ->
            cleanupMessagesInThread(
                thread = thread,
                uid = uid,
                deletedUserName = deletedUserName,
                deletedMessageText = deletedMessageText,
                now = now
            )

            cleanupThreadMembership(
                thread = thread,
                uid = uid,
                deletedUserName = deletedUserName,
                deletedMessageText = deletedMessageText,
                now = now
            )

            cleanupTypingForThreadBestEffort(thread.id, uid)
        }
    }

    private suspend fun cleanupMessagesInThread(
        thread: DocumentSnapshot,
        uid: String,
        deletedUserName: String,
        deletedMessageText: String,
        now: Long
    ) {
        val messages = thread.reference
            .collection(MESSAGES_SUBCOLLECTION)
            .whereEqualTo("senderId", uid)
            .get()
            .await()
            .documents

        if (messages.isEmpty()) return

        val writer = BatchWriter(firestore)

        messages.forEach { message ->
            listOfNotNull(
                message.getString("fileUrl"),
                message.getString("imageUrl"),
                message.getString("mediaUrl"),
                message.getString("photoUrl"),
                message.getString("attachmentUrl")
            ).forEach { url ->
                deleteStorageUrlBestEffort(url)
            }

            writer.update(
                message.reference,
                mapOf(
                    "text" to deletedMessageText,
                    "type" to MESSAGE_TYPE_TEXT,
                    "isDeleted" to true,
                    "deletedBy" to uid,
                    "deletedAt" to now,
                    "updatedAt" to now,
                    "senderName" to deletedUserName,
                    "senderPhotoUrl" to FieldValue.delete(),
                    "fileUrl" to FieldValue.delete(),
                    "fileName" to FieldValue.delete(),
                    "fileMimeType" to FieldValue.delete(),
                    "fileSizeBytes" to FieldValue.delete(),
                    "audioDurationMillis" to FieldValue.delete(),
                    "imageUrl" to FieldValue.delete(),
                    "mediaUrl" to FieldValue.delete(),
                    "photoUrl" to FieldValue.delete(),
                    "attachmentUrl" to FieldValue.delete(),
                    "attachmentType" to FieldValue.delete(),
                    "mimeType" to FieldValue.delete(),
                    "linkUrl" to FieldValue.delete()
                )
            )
        }

        writer.commit()
    }

    private suspend fun cleanupThreadMembership(
        thread: DocumentSnapshot,
        uid: String,
        deletedUserName: String,
        deletedMessageText: String,
        now: Long
    ) {
        val isGroup = thread.getString("type") == THREAD_TYPE_GROUP
        val participantIds = thread.getStringListCompat("participantIds")
        val remainingParticipantIds = participantIds.filterNot { it == uid }
        val creatorId = thread.getString("creatorId").orEmpty()
        val lastMessageSenderId = thread.getString("lastMessageSenderId").orEmpty()

        val updates = mutableMapOf<String, Any>(
            "updatedAt" to now
        )

        if (lastMessageSenderId == uid) {
            updates["lastMessageText"] = deletedMessageText
            updates["lastMessageType"] = MESSAGE_TYPE_TEXT
        }

        if (isGroup) {
            updates["participantIds"] = remainingParticipantIds
            updates["participants.$uid"] = FieldValue.delete()
            updates["unreadCounts.$uid"] = FieldValue.delete()
            updates["typing.$uid"] = FieldValue.delete()
            updates["memberCount"] = remainingParticipantIds.size

            if (creatorId == uid && remainingParticipantIds.isNotEmpty()) {
                updates["creatorId"] = remainingParticipantIds.first()
            }
        } else {
            updates["participants.$uid"] = deletedParticipantMap(
                uid = uid,
                deletedUserName = deletedUserName
            )
            updates["unreadCounts.$uid"] = FieldValue.delete()
            updates["typing.$uid"] = FieldValue.delete()
        }

        thread.reference.update(updates).await()
    }

    private suspend fun deleteUsernameReservations(
        usernameCandidates: Set<String>,
        uid: String
    ) {
        if (usernameCandidates.isEmpty()) return

        val writer = BatchWriter(firestore)

        val usernameDocumentIds = usernameCandidates
            .flatMap { candidate ->
                listOf(candidate, candidate.normalizeUsernameForDocumentId())
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        usernameDocumentIds.forEach { usernameDocumentId ->
            val usernameRef = firestore.collection(USERNAMES_COLLECTION)
                .document(usernameDocumentId)
            val usernameSnapshot = usernameRef.get().await()

            if (usernameSnapshot.exists() && usernameSnapshot.getString("uid") == uid) {
                writer.delete(usernameRef)
            }
        }

        writer.commit()
    }

    private suspend fun anonymizeUserDocument(
        userRef: DocumentReference,
        exists: Boolean,
        uid: String,
        deletedUserName: String,
        now: Long
    ) {
        val deletedUserNameLowercase = deletedUserName.lowercase(AppLocales.TURKISH)

        val tombstone = mapOf(
            "uid" to uid,
            "id" to uid,

            "isDeleted" to true,
            "deletedAt" to now,
            "updatedAt" to now,

            "userName" to deletedUserName,
            "username" to deletedUserName,
            "displayName" to deletedUserName,
            "name" to deletedUserName,

            "userNameLowercase" to deletedUserNameLowercase,
            "usernameLowercase" to deletedUserNameLowercase,
            "displayNameLowercase" to deletedUserNameLowercase,

            "followersCount" to 0,
            "followingCount" to 0,
            "postsCount" to 0,

            "profileCompleted" to false
        )

        userRef.set(tombstone).await()
    }

    private fun cleanupRealtimeData(uid: String) {
        runCatching {
            realtimeDatabase.reference
                .child(REALTIME_STATUS_NODE)
                .child(uid)
                .removeValue()
        }.onFailure { exception ->
            Log.w(TAG, "Could not remove realtime status for deleted account", exception)
        }
    }

    private fun cleanupTypingForThreadBestEffort(threadId: String, uid: String) {
        runCatching {
            realtimeDatabase.reference
                .child(REALTIME_TYPING_NODE)
                .child(threadId)
                .child(uid)
                .removeValue()
        }.onFailure { exception ->
            Log.w(TAG, "Could not remove realtime typing state for deleted account", exception)
        }
    }

    private suspend fun deleteStorageUrlBestEffort(url: String) {
        if (url.isBlank()) return

        runCatching {
            storage.getReferenceFromUrl(url).delete().await()
        }.onFailure { exception ->
            Log.w(TAG, "Could not delete storage file for deleted account", exception)
        }
    }

    private fun usernameCandidates(
        userSnapshot: DocumentSnapshot,
        authDisplayName: String?,
        authEmail: String?
    ): Set<String> {
        return buildSet {
            add(userSnapshot.getString("userName"))
            add(userSnapshot.getString("username"))
            add(userSnapshot.getString("displayName"))
            add(authDisplayName)
            add(authEmail?.substringBefore("@"))
        }.filterNotNull()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun deletedParticipantMap(
        uid: String,
        deletedUserName: String
    ): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "userName" to deletedUserName,
            "photoUrl" to null,
            "isDeleted" to true
        )
    }

    private fun String.normalizeUsernameForDocumentId(): String {
        return lowercase(Locale.ROOT)
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("\\s+"), ".")
            .replace(Regex("[^a-z0-9._]"), "")
            .replace(Regex("[.]{2,}"), ".")
            .replace(Regex("[_]{2,}"), "_")
            .trim('.', '_')
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private class BatchWriter(
        private val firestore: FirebaseFirestore
    ) {
        private var batch: WriteBatch = firestore.batch()
        private var operationCount: Int = 0

        suspend fun delete(reference: DocumentReference) {
            batch.delete(reference)
            operationCount++
            commitIfNeeded()
        }

        suspend fun update(reference: DocumentReference, updates: Map<String, Any>) {
            batch.update(reference, updates)
            operationCount++
            commitIfNeeded()
        }

        private suspend fun commitIfNeeded() {
            if (operationCount >= MAX_BATCH_OPERATIONS) {
                commit()
            }
        }

        suspend fun commit() {
            if (operationCount <= 0) return

            batch.commit().await()
            batch = firestore.batch()
            operationCount = 0
        }

        private companion object {
            const val MAX_BATCH_OPERATIONS = 450
        }
    }

    private companion object {
        const val TAG = "AccountDeletionRepository"

        const val USERS_COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
        const val POSTS_COLLECTION = "posts"
        const val COMMENTS_SUBCOLLECTION = "comments"
        const val FOLLOWS_COLLECTION = "follows"
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"

        const val THREAD_TYPE_GROUP = "group"
        const val MESSAGE_TYPE_TEXT = "text"

        const val REALTIME_STATUS_NODE = "status"
        const val REALTIME_TYPING_NODE = "typing"
    }
}
