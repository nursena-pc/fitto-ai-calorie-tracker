package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.UserProfileEntity

internal class DailyProfileLoadAction(
    private val getCachedProfile: suspend () -> UserProfileEntity?,
    private val syncProfile: suspend () -> UserProfileEntity?
) {

    suspend fun load(): UserProfileEntity? {
        return getCachedProfile() ?: syncProfile()
    }
}
