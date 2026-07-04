package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialPostInteractionSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val ensureCurrentUserProfile: suspend () -> Unit,
    private val loadCurrentUserProfile: suspend () -> SocialUser?
) : SocialPostInteractionSource {

    override suspend fun addComment(request: SocialCommentCreateRequest) {
        val user = auth.currentUser ?: return
        val postRef = firestore.collection(POSTS_COLLECTION).document(request.postId)
        val postSnapshot = postRef.get().await()
        val allowComments = postSnapshot.getBoolean("allowComments") ?: true

        if (!allowComments) {
            throw IllegalStateException("COMMENTS_DISABLED")
        }

        ensureCurrentUserProfile()
        val profile = loadCurrentUserProfile()
        val author = SocialPostPayloadResolver.resolveAuthor(
            profileUserName = profile?.userName,
            profilePhotoUrl = profile?.photoUrl,
            email = user.email,
            authPhotoUrl = user.photoUrl?.toString()
        )

        firestore.runBatch { batch ->
            val commentRef = postRef.collection(COMMENTS_SUBCOLLECTION).document()

            batch.set(
                commentRef,
                SocialPostPayloadResolver.createCommentPayload(
                    userId = user.uid,
                    author = author,
                    postId = request.postId,
                    postOwnerId = postSnapshot.getString("userId").orEmpty(),
                    text = request.text,
                    parentCommentId = request.parentCommentId,
                    createdAt = System.currentTimeMillis()
                )
            )

            batch.update(
                postRef,
                "commentsCount",
                FieldValue.increment(1)
            )

            request.parentCommentId?.let { parentCommentId ->
                val parentCommentRef = postRef
                    .collection(COMMENTS_SUBCOLLECTION)
                    .document(parentCommentId)

                batch.update(
                    parentCommentRef,
                    "replyCount",
                    FieldValue.increment(1)
                )
            }
        }.await()
    }

    override suspend fun toggleCommentLike(postId: String, commentId: String) {
        val userId = auth.currentUser?.uid ?: return
        val commentRef = firestore.collection(POSTS_COLLECTION)
            .document(postId)
            .collection(COMMENTS_SUBCOLLECTION)
            .document(commentId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)

            transaction.update(
                commentRef,
                SocialPostInteractionResolver.toggleLikePayload(
                    likedByValue = snapshot.get("likedBy"),
                    currentLikes = snapshot.getLong("likesCount") ?: 0L,
                    userId = userId
                )
            )
        }.await()
    }

    override suspend fun toggleSave(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(POSTS_COLLECTION).document(postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)

            transaction.update(
                postRef,
                SocialPostInteractionResolver.toggleSavePayload(
                    savedByValue = snapshot.get("savedBy"),
                    userId = userId
                )
            )
        }.await()
    }

    override suspend fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(POSTS_COLLECTION).document(postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)

            transaction.update(
                postRef,
                SocialPostInteractionResolver.toggleLikePayload(
                    likedByValue = snapshot.get("likedBy"),
                    currentLikes = snapshot.getLong("likesCount") ?: 0L,
                    userId = userId
                )
            )
        }.await()
    }

    private companion object {
        const val POSTS_COLLECTION = "posts"
        const val COMMENTS_SUBCOLLECTION = "comments"
    }
}
