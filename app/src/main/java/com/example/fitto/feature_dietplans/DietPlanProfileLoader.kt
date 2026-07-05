package com.example.fitto.feature_dietplans

import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.repository.ProfileCacheRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DietPlanProfileLoader(
    private val source: ProfileSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    constructor(
        repository: ProfileCacheRepository,
        ioDispatcher: CoroutineDispatcher
    ) : this(
        source = object : ProfileSource {
            override suspend fun getCachedProfile(): UserProfileEntity? {
                return repository.getCurrentUserProfile()
            }

            override suspend fun syncProfile(): UserProfileEntity? {
                return repository.syncCurrentUserProfileFromFirebase()
            }
        },
        ioDispatcher = ioDispatcher
    )

    suspend fun loadCurrentProfile(): UserProfileEntity? {
        return withContext(ioDispatcher) {
            source.getCachedProfile() ?: source.syncProfile()
        }
    }

    interface ProfileSource {
        suspend fun getCachedProfile(): UserProfileEntity?
        suspend fun syncProfile(): UserProfileEntity?
    }
}
