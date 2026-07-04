package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialPostContentObservationSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val loadUsersByIds: suspend (List<String>) -> List<SocialUser>
) : SocialPostContentObservationSource {

    override fun observeComments(postId: String): Flow<List<SocialComment>> = callbackFlow {
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

                val comments = snapshot?.documents?.map { document ->
                    SocialCommentDocumentResolver.resolve(
                        id = document.id,
                        postId = postId,
                        currentUserId = currentUserId,
                        userId = document.getString("userId"),
                        userName = document.getString("userName"),
                        userPhotoUrl = document.getString("userPhotoUrl"),
                        fallbackPhotoUrl = document.getString("photoUrl"),
                        text = document.getString("text"),
                        parentCommentId = document.getString("parentCommentId"),
                        likesCount = document.getLong("likesCount"),
                        replyCount = document.getLong("replyCount"),
                        likedBy = document.get("likedBy"),
                        createdAt = document.getLong("createdAt")
                    )
                }.orEmpty()

                launch {
                    trySend(hydrateCommentAuthors(comments))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeMyProfilePosts(): Flow<List<SocialPost>> = callbackFlow {
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

                val posts = SocialPostListResolver.profilePosts(
                    snapshot?.documents.orEmpty()
                        .mapNotNull { document ->
                            FirebaseSocialPostSnapshotMapper.map(
                                document = document,
                                currentUserId = currentUserId
                            )
                        }
                )

                launch {
                    trySend(hydratePostAuthors(posts))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeMyDraftPosts(): Flow<List<SocialPost>> = callbackFlow {
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

                val drafts = SocialPostListResolver.draftPosts(
                    snapshot?.documents.orEmpty()
                        .mapNotNull { document ->
                            FirebaseSocialPostSnapshotMapper.map(
                                document = document,
                                currentUserId = currentUserId
                            )
                        }
                )

                launch {
                    trySend(hydratePostAuthors(drafts))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeSavedPosts(): Flow<List<SocialPost>> = callbackFlow {
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

                val posts = SocialPostListResolver.savedPosts(
                    snapshot?.documents.orEmpty()
                        .mapNotNull { document ->
                            FirebaseSocialPostSnapshotMapper.map(
                                document = document,
                                currentUserId = currentUserId
                            )
                        }
                )

                launch {
                    trySend(hydratePostAuthors(posts))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observePost(postId: String): Flow<SocialPost?> = callbackFlow {
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

                val post = SocialPostListResolver.visiblePost(
                    post = snapshot
                        ?.takeIf { document -> document.exists() }
                        ?.let { document ->
                            FirebaseSocialPostSnapshotMapper.map(
                                document = document,
                                currentUserId = currentUserId
                            )
                        },
                    currentUserId = currentUserId
                )

                launch {
                    trySend(post?.let { hydratePostAuthors(listOf(it)).firstOrNull() })
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getPostById(postId: String): SocialPost? {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (postId.isBlank() || currentUserId.isBlank()) return null

        val snapshot = firestore.collection("posts")
            .document(postId)
            .get()
            .await()

        if (!snapshot.exists()) return null

        val post = FirebaseSocialPostSnapshotMapper.map(
            document = snapshot,
            currentUserId = currentUserId
        ) ?: return null

        return hydratePostAuthors(listOf(post)).firstOrNull()
    }

    private suspend fun hydratePostAuthors(posts: List<SocialPost>): List<SocialPost> {
        val users = loadUsersByIds(SocialAuthorHydrationResolver.postAuthorIds(posts))
        return SocialAuthorHydrationResolver.hydratePosts(posts, users)
    }

    private suspend fun hydrateCommentAuthors(comments: List<SocialComment>): List<SocialComment> {
        val users = loadUsersByIds(SocialAuthorHydrationResolver.commentAuthorIds(comments))
        return SocialAuthorHydrationResolver.hydrateComments(comments, users)
    }
}
