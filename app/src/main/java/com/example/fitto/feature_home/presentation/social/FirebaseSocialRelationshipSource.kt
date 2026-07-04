package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialRelationshipSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val ensureCurrentUserProfile: suspend () -> Unit,
    private val loadUsersByIds: suspend (List<String>) -> List<SocialUser>
) : SocialRelationshipSource {

    override suspend fun toggleFollow(targetUser: SocialUser) {
        val currentUser = auth.currentUser ?: return
        if (SocialFollowPayloadResolver.shouldSkipFollowAction(currentUser.uid, targetUser.id)) return

        ensureCurrentUserProfile()

        val followId = SocialFollowPayloadResolver.followDocumentId(
            followerId = currentUser.uid,
            followingId = targetUser.id
        )
        val followRef = firestore.collection(FOLLOWS_COLLECTION).document(followId)
        val currentUserRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
        val targetUserRef = firestore.collection(USERS_COLLECTION).document(targetUser.id)

        firestore.runTransaction { transaction ->
            val followSnapshot = transaction.get(followRef)
            val targetSnapshot = transaction.get(targetUserRef)

            val targetDeleted = SocialFollowPayloadResolver.isDeletedOrMissing(
                exists = targetSnapshot.exists(),
                isDeleted = targetSnapshot.getBoolean("isDeleted")
            )

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
                        SocialFollowPayloadResolver.deletedUserCountResetPayload()
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
                    SocialFollowPayloadResolver.createFollowPayload(
                        followerId = currentUser.uid,
                        followingId = targetUser.id,
                        createdAt = System.currentTimeMillis()
                    )
                )

                transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
            }
        }.await()
    }

    override suspend fun removeFollower(followerId: String) {
        val currentUser = auth.currentUser ?: return
        if (SocialFollowPayloadResolver.shouldSkipFollowAction(currentUser.uid, followerId)) return

        val followRef = firestore.collection(FOLLOWS_COLLECTION)
            .document(
                SocialFollowPayloadResolver.followDocumentId(
                    followerId = followerId,
                    followingId = currentUser.uid
                )
            )
        val followerRef = firestore.collection(USERS_COLLECTION).document(followerId)
        val currentUserRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)

        firestore.runTransaction { transaction ->
            val followSnapshot = transaction.get(followRef)
            if (!followSnapshot.exists()) return@runTransaction null

            val followerSnapshot = transaction.get(followerRef)
            val currentSnapshot = transaction.get(currentUserRef)

            transaction.delete(followRef)
            val updatedAt = System.currentTimeMillis()
            transaction.set(
                followerRef,
                SocialFollowPayloadResolver.decrementCountPayload(
                    field = "followingCount",
                    currentCount = followerSnapshot.getLong("followingCount"),
                    updatedAt = updatedAt
                ),
                SetOptions.merge()
            )
            transaction.set(
                currentUserRef,
                SocialFollowPayloadResolver.decrementCountPayload(
                    field = "followersCount",
                    currentCount = currentSnapshot.getLong("followersCount"),
                    updatedAt = updatedAt
                ),
                SetOptions.merge()
            )
            null
        }.await()
    }

    override suspend fun stopFollowing(followingId: String) {
        val currentUser = auth.currentUser ?: return
        if (SocialFollowPayloadResolver.shouldSkipFollowAction(currentUser.uid, followingId)) return

        val followRef = firestore.collection(FOLLOWS_COLLECTION)
            .document(
                SocialFollowPayloadResolver.followDocumentId(
                    followerId = currentUser.uid,
                    followingId = followingId
                )
            )
        val currentUserRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
        val followingUserRef = firestore.collection(USERS_COLLECTION).document(followingId)

        firestore.runTransaction { transaction ->
            val followSnapshot = transaction.get(followRef)
            if (!followSnapshot.exists()) return@runTransaction null

            val currentSnapshot = transaction.get(currentUserRef)
            val followingSnapshot = transaction.get(followingUserRef)

            transaction.delete(followRef)
            val updatedAt = System.currentTimeMillis()
            transaction.set(
                currentUserRef,
                SocialFollowPayloadResolver.decrementCountPayload(
                    field = "followingCount",
                    currentCount = currentSnapshot.getLong("followingCount"),
                    updatedAt = updatedAt
                ),
                SetOptions.merge()
            )
            transaction.set(
                followingUserRef,
                SocialFollowPayloadResolver.decrementCountPayload(
                    field = "followersCount",
                    currentCount = followingSnapshot.getLong("followersCount"),
                    updatedAt = updatedAt
                ),
                SetOptions.merge()
            )
            null
        }.await()
    }

    override suspend fun getFollowers(userId: String): List<SocialUser> {
        if (SocialUserListResolver.shouldSkipRelationshipLookup(userId)) return emptyList()

        val followerIds = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followingId", userId)
            .get()
            .await()
            .documents
            .map { document -> document.getString("followerId") }
            .let(SocialUserListResolver::followerIds)

        return loadUsersByIds(followerIds)
    }

    override suspend fun getFollowing(userId: String): List<SocialUser> {
        if (SocialUserListResolver.shouldSkipRelationshipLookup(userId)) return emptyList()

        val followingIds = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followerId", userId)
            .get()
            .await()
            .documents
            .map { document -> document.getString("followingId") }
            .let(SocialUserListResolver::followingIds)

        return loadUsersByIds(followingIds)
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FOLLOWS_COLLECTION = "follows"
    }
}
