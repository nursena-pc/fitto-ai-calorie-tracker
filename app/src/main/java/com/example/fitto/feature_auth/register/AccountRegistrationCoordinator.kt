package com.example.fitto.feature_auth.register

class AccountRegistrationCoordinator(
    private val dataSource: AccountRegistrationDataSource
) {
    suspend fun startRegistration(
        email: String,
        password: String
    ): AccountRegistrationStartResult {
        return dataSource.startRegistration(email, password)
    }

    suspend fun isEmailVerified(): Boolean {
        return dataSource.isCurrentUserEmailVerified()
    }

    suspend fun resendVerificationEmail(): Boolean {
        return dataSource.resendCurrentUserVerificationEmail()
    }

    suspend fun cancelRegistration() {
        dataSource.cancelUnverifiedRegistration()
    }

    suspend fun createInitialProfile(uid: String, email: String): Boolean {
        return dataSource.createInitialProfile(uid, email)
    }
}
