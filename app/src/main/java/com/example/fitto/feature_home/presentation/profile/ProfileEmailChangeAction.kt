package com.example.fitto.feature_home.presentation.profile

class ProfileEmailChangeAction(
    private val checkAndSyncPendingEmailChange: suspend (String) -> EmailChangeSyncResult,
    private val signInAfterVerifiedEmailChange: suspend (email: String, password: String) -> EmailChangeSyncResult,
    private val resendPendingEmailChangeVerification: suspend (email: String, languageCode: String) -> Unit
) {
    suspend fun check(email: String): EmailChangeSyncResult {
        return checkAndSyncPendingEmailChange(email.trim())
    }

    suspend fun restoreSession(
        email: String,
        password: String
    ): EmailChangeSyncResult {
        return signInAfterVerifiedEmailChange(email.trim(), password)
    }

    suspend fun resend(
        email: String,
        languageCode: String
    ) {
        resendPendingEmailChangeVerification(email.trim(), languageCode)
    }
}
