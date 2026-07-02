package com.example.fitto.feature_home.presentation.profile

class ProfileHistoryClearAction(
    private val clearRemoteHistory: suspend () -> String,
    private val clearLocalHistory: suspend (userId: String) -> Unit,
    private val clearNotifications: () -> Unit
) {
    suspend fun clear(): ProfileHistoryClearResult {
        val userId = clearRemoteHistory()
        clearLocalHistory(userId)
        clearNotifications()
        return ProfileHistoryClearResult(userId = userId)
    }
}

data class ProfileHistoryClearResult(
    val userId: String
)
