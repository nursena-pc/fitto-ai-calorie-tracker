package com.example.fitto.feature_auth.register

internal data class GoogleOnboardingIdentity(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

internal enum class GoogleOnboardingDestination {
    HOME,
    USERNAME_SETUP
}

internal sealed interface GoogleOnboardingResult {
    data class Success(val destination: GoogleOnboardingDestination) : GoogleOnboardingResult
    data class Failure(val cause: Throwable? = null) : GoogleOnboardingResult
}

internal data class GoogleOnboardingProfileStatus(
    val exists: Boolean,
    val isCompleted: Boolean
)

internal interface GoogleOnboardingDataSource {
    suspend fun signIn(idToken: String): GoogleOnboardingIdentity?
    suspend fun profileStatus(uid: String): GoogleOnboardingProfileStatus
    suspend fun createInitialProfile(identity: GoogleOnboardingIdentity)
}

internal class OnboardingGoogleSignUpCoordinator(
    private val dataSource: GoogleOnboardingDataSource
) {
    suspend fun signIn(idToken: String): GoogleOnboardingResult {
        if (idToken.isBlank()) return GoogleOnboardingResult.Failure()

        return runCatching {
            val identity = dataSource.signIn(idToken)
                ?: return GoogleOnboardingResult.Failure()

            val profileStatus = dataSource.profileStatus(identity.uid)
            val destination = when {
                !profileStatus.exists -> {
                    dataSource.createInitialProfile(identity)
                    GoogleOnboardingDestination.USERNAME_SETUP
                }
                profileStatus.isCompleted -> GoogleOnboardingDestination.HOME
                else -> GoogleOnboardingDestination.USERNAME_SETUP
            }

            GoogleOnboardingResult.Success(destination)
        }.getOrElse { error ->
            GoogleOnboardingResult.Failure(error)
        }
    }
}
