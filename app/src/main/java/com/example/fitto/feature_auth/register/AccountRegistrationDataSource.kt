package com.example.fitto.feature_auth.register

sealed interface AccountRegistrationStartResult {
    data class VerificationSent(
        val uid: String,
        val email: String,
        val wasResent: Boolean
    ) : AccountRegistrationStartResult

    data object EmailAlreadyRegistered : AccountRegistrationStartResult
    data object VerificationSendFailed : AccountRegistrationStartResult
    data object VerificationResendFailed : AccountRegistrationStartResult
    data class Failure(val message: String?) : AccountRegistrationStartResult
}

interface AccountRegistrationDataSource {
    suspend fun startRegistration(
        email: String,
        password: String
    ): AccountRegistrationStartResult

    suspend fun isCurrentUserEmailVerified(): Boolean
    suspend fun resendCurrentUserVerificationEmail(): Boolean
    suspend fun cancelUnverifiedRegistration()
    suspend fun createInitialProfile(uid: String, email: String): Boolean
}
