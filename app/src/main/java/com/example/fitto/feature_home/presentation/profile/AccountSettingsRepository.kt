package com.example.fitto.feature_home.presentation.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

enum class EmailChangeSyncResult {
    WAITING,
    VERIFIED,
    SIGNED_OUT
}

class AccountSettingsRepository internal constructor(
    private val identitySettingsSource: AccountIdentitySettingsSource,
    private val emailChangeSource: AccountEmailChangeSource,
    private val securitySettingsSource: AccountSecuritySettingsSource
) {

    constructor(
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ) : this(
        identitySettingsSource = FirebaseAccountIdentitySettingsSource(auth, firestore),
        emailChangeSource = FirebaseAccountEmailChangeSource(auth, firestore),
        securitySettingsSource = FirebaseAccountSecuritySettingsSource(auth, firestore)
    )

    suspend fun updateDisplayName(displayName: String) {
        identitySettingsSource.updateDisplayName(displayName)
    }

    suspend fun updateUsername(rawUsername: String) {
        identitySettingsSource.updateUsername(rawUsername)
    }

    suspend fun reauthenticateWithPassword(password: String) {
        securitySettingsSource.reauthenticateWithPassword(password)
    }

    suspend fun updatePasswordAfterReauth(
        newPassword: String,
        confirmPassword: String
    ) {
        securitySettingsSource.updatePasswordAfterReauth(newPassword, confirmPassword)
    }

    suspend fun updateEmailAfterReauth(
        newEmail: String,
        languageCode: String = "tr"
    ) {
        emailChangeSource.updateEmailAfterReauth(newEmail, languageCode)
    }

    suspend fun resendPendingEmailChangeVerification(
        pendingEmail: String,
        languageCode: String = "tr"
    ) {
        emailChangeSource.resendPendingEmailChangeVerification(pendingEmail, languageCode)
    }

    fun currentAuthEmail(): String {
        return emailChangeSource.currentAuthEmail()
    }

    fun hasCurrentUser(): Boolean {
        return emailChangeSource.hasCurrentUser()
    }

    suspend fun reloadCurrentUserBestEffort() {
        emailChangeSource.reloadCurrentUserBestEffort()
    }

    suspend fun checkAndSyncPendingEmailChange(
        expectedEmail: String
    ): EmailChangeSyncResult {
        return emailChangeSource.checkAndSyncPendingEmailChange(expectedEmail)
    }

    suspend fun signInAfterVerifiedEmailChange(
        email: String,
        password: String
    ): EmailChangeSyncResult {
        return emailChangeSource.signInAfterVerifiedEmailChange(email, password)
    }

    suspend fun syncCurrentAuthEmailToProfileIfNeeded(
        skipReload: Boolean = false
    ): EmailChangeSyncResult {
        return emailChangeSource.syncCurrentAuthEmailToProfileIfNeeded(skipReload)
    }

    fun isPasswordProvider(): Boolean {
        return securitySettingsSource.isPasswordProvider()
    }

    fun isGoogleProvider(): Boolean {
        return securitySettingsSource.isGoogleProvider()
    }

    suspend fun clearCurrentUserHistory(): String {
        return securitySettingsSource.clearCurrentUserHistory()
    }
}

class AccountSettingsException(
    val error: AccountSettingsError,
    cause: Throwable? = null
) : Exception(error.name, cause)

enum class AccountSettingsError {
    NOT_SIGNED_IN,
    EMAIL_MISSING,
    DISPLAY_NAME_TOO_SHORT,
    DISPLAY_NAME_TOO_LONG,
    USERNAME_TOO_SHORT,
    USERNAME_TOO_LONG,
    USERNAME_INVALID,
    USERNAME_TAKEN,
    EMAIL_INVALID,
    EMAIL_SAME_AS_CURRENT,
    EMAIL_VERIFICATION_SENT,
    CURRENT_PASSWORD_EMPTY,
    WRONG_PASSWORD,
    NEW_PASSWORD_TOO_SHORT,
    PASSWORD_CONFIRM_MISMATCH,
    REQUIRES_RECENT_LOGIN,
    PROVIDER_NOT_SUPPORTED,
    UNKNOWN
}
