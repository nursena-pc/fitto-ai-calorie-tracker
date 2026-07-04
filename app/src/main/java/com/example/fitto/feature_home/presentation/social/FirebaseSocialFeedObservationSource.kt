package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialFeedObservationSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val loadUsersByIds: suspend (List<String>) -> List<SocialUser>
) : SocialFeedObservationSource {

    override fun observeFeed(): Flow<List<SocialPost>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (SocialFeedResolver.shouldSkipFeed(currentUserId)) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val sourcePostMaps = mutableMapOf<String, Map<String, SocialPost>>()
        val registrations = mutableListOf<ListenerRegistration>()

        fun emitMergedPosts() {
            trySend(SocialFeedResolver.mergeSourcePosts(sourcePostMaps))
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
                        FirebaseSocialPostSnapshotMapper.map(
                            document = document,
                            currentUserId = currentUserId
                        )
                    }
                    .filter(includePost)

                launch {
                    sourcePostMaps[key] = SocialFeedResolver.sourcePostMap(
                        hydratePostAuthors(rawPosts)
                    )
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
            .map { document -> document.getString("followingId") }
            .let(SocialFeedResolver::followingIds)

        attachSource(
            key = "community",
            query = firestore.collection("posts")
                .whereEqualTo("visibility", POST_VISIBILITY_COMMUNITY)
                .whereEqualTo("isDraft", false),
            includePost = { true }
        )

        attachSource(
            key = "my_posts",
            query = firestore.collection("posts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isDraft", false),
            includePost = { true }
        )

        SocialFeedResolver.followingLookupChunks(followingIds)
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

    override suspend fun refreshFeedFromServer() {
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .await()
    }

    private suspend fun hydratePostAuthors(posts: List<SocialPost>): List<SocialPost> {
        val users = loadUsersByIds(SocialAuthorHydrationResolver.postAuthorIds(posts))
        return SocialAuthorHydrationResolver.hydratePosts(posts, users)
    }
}
