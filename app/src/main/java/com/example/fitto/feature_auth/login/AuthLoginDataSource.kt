package com.example.fitto.feature_auth.login

interface AuthLoginDataSource {
    suspend fun lookupEmailForUsername(normalizedUsername: String): UsernameEmailLookupResult
    suspend fun signInWithEmail(email: String, password: String): AuthLoginResult
    suspend fun sendPasswordReset(email: String): PasswordResetResult
    fun currentSession(): AuthSessionSnapshot?
    fun sendCurrentUserVerificationEmail()
    fun signOut()
}
