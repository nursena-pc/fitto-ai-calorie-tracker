package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialProfileQuerySource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SocialProfileQuerySource {

    override suspend fun searchUsers(query: String): List<SearchUserUiModel> {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val normalizedQuery = SocialUserLookupResolver.normalizeSearchQuery(query)

        if (!SocialUserLookupResolver.shouldSearch(normalizedQuery)) return emptyList()

        val snapshot = firestore.collection(USERS_COLLECTION)
            .orderBy("usernameLowercase")
            .whereGreaterThanOrEqualTo("usernameLowercase", normalizedQuery)
            .whereLessThanOrEqualTo("usernameLowercase", normalizedQuery + "\uf8ff")
            .limit(25)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            SocialUserLookupResolver.resolveSearchUser(
                id = document.id,
                currentUserId = currentUserId,
                isDeleted = document.getBoolean("isDeleted") == true,
                userName = document.getString("userName"),
                usernameLowercase = document.getString("usernameLowercase"),
                email = document.getString("email"),
                bio = document.getString("bio"),
                photoUrl = document.getString("photoUrl")
            )
        }
    }

    override suspend fun createCurrentUserProfileIfNeeded() {
        val user = auth.currentUser ?: return

        val userRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = userRef.get().await()

        if (!snapshot.exists()) {
            userRef.set(
                SocialProfileMaintenanceResolver.createCurrentUserProfilePayload(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl?.toString(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ).await()
        }
    }

    override suspend fun getCurrentUserProfile(): SocialUser? {
        val user = auth.currentUser ?: return null

        createCurrentUserProfileIfNeeded()

        val profile = getUserProfile(user.uid) ?: return null
        return profile.copy(
            isFollowedByMe = false,
            photoUrl = profile.photoUrl ?: user.photoUrl?.toString()
        )
    }

    override suspend fun getUserProfile(userId: String): SocialUser? {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        val userDoc = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        if (!userDoc.exists()) return null
        if (userDoc.getBoolean("isDeleted") == true) return null

        val followDoc = if (currentUserId.isNotBlank() && currentUserId != userId) {
            firestore.collection(FOLLOWS_COLLECTION)
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

    override suspend fun isUserDeletedOrMissing(userId: String): Boolean {
        if (userId.isBlank()) return true

        val snapshot = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        return !snapshot.exists() || snapshot.getBoolean("isDeleted") == true
    }

    override suspend fun getUsersByIds(userIds: List<String>): List<SocialUser> {
        val lookupChunks = SocialUserListResolver.lookupChunks(userIds)
        if (lookupChunks.isEmpty()) return emptyList()

        val users = mutableMapOf<String, SocialUser>()

        lookupChunks.forEach { chunk ->
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.forEach { document ->
                val user = SocialUserListResolver.resolveVisibleProfile(
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
                ) ?: return@forEach

                users[user.id] = user
            }
        }

        return SocialUserListResolver.orderUsersByRequestedIds(
            requestedIds = userIds,
            usersById = users
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FOLLOWS_COLLECTION = "follows"
    }
}
