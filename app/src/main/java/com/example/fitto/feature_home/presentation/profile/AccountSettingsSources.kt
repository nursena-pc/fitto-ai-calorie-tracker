package com.example.fitto.feature_home.presentation.profile

internal interface AccountIdentitySettingsSource {
    suspend fun updateDisplayName(displayName: String)
    suspend fun updateUsername(rawUsername: String)
}

internal interface AccountEmailChangeSource {
    suspend fun updateEmailAfterReauth(newEmail: String, languageCode: String)
    suspend fun resendPendingEmailChangeVerification(pendingEmail: String, languageCode: String)
    fun currentAuthEmail(): String
    fun hasCurrentUser(): Boolean
    suspend fun reloadCurrentUserBestEffort()
    suspend fun checkAndSyncPendingEmailChange(expectedEmail: String): EmailChangeSyncResult
    suspend fun signInAfterVerifiedEmailChange(email: String, password: String): EmailChangeSyncResult
    suspend fun syncCurrentAuthEmailToProfileIfNeeded(skipReload: Boolean): EmailChangeSyncResult
}

internal interface AccountSecuritySettingsSource {
    suspend fun reauthenticateWithPassword(password: String)
    suspend fun updatePasswordAfterReauth(newPassword: String, confirmPassword: String)
    fun isPasswordProvider(): Boolean
    fun isGoogleProvider(): Boolean
    suspend fun clearCurrentUserHistory(): String
}
