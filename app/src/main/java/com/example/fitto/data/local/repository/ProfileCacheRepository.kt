package com.example.fitto.data.local.repository

import android.content.Context
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.profile.FirebaseUserProfileMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ProfileCacheRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val database = FittoDatabaseProvider.getDatabase(context.applicationContext)
    private val profileDao = database.profileDao()

    fun observeCurrentUserProfile(): Flow<UserProfileEntity?> {
        val userId = auth.currentUser?.uid.orEmpty()
        return profileDao.observeProfile(userId)
    }

    suspend fun getCurrentUserProfile(): UserProfileEntity? {
        val userId = auth.currentUser?.uid ?: return null
        return profileDao.getProfile(userId)
    }

    suspend fun syncCurrentUserProfileFromFirebase(): UserProfileEntity? {
        val currentUser = auth.currentUser ?: return null
        val userId = currentUser.uid

        val snapshot = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        val data = snapshot.data ?: return null

        val profile = FirebaseUserProfileMapper.fromFirebaseMap(
            userId = userId,
            emailFallback = currentUser.email.orEmpty(),
            data = data
        )

        profileDao.upsertProfile(profile)
        return profile
    }

    suspend fun cacheProfileFromFirebaseMap(
        userId: String,
        emailFallback: String,
        data: Map<String, Any?>
    ): UserProfileEntity {
        val profile = FirebaseUserProfileMapper.fromFirebaseMap(
            userId = userId,
            emailFallback = emailFallback,
            data = data
        )

        profileDao.upsertProfile(profile)
        return profile
    }

    suspend fun clearCurrentUserProfileCache() {
        val userId = auth.currentUser?.uid ?: return
        profileDao.deleteProfile(userId)
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}