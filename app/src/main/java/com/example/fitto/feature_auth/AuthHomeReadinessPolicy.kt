package com.example.fitto.feature_auth

internal data class AuthSessionSnapshot(
    val hasUser: Boolean,
    val email: String?,
    val isEmailVerified: Boolean
)

internal object AuthHomeReadinessPolicy {
    fun shouldOpenHome(session: AuthSessionSnapshot): Boolean {
        if (!session.hasUser) return false
        return session.email.isNullOrBlank() || session.isEmailVerified
    }
}
