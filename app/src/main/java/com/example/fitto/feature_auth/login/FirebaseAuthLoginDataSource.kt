package com.example.fitto.feature_auth.login

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthLoginDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthLoginDataSource {

    override suspend fun lookupEmailForUsername(
        normalizedUsername: String
    ): UsernameEmailLookupResult {
        return try {
            val document = firestore.collection(USERNAMES_COLLECTION)
                .document(normalizedUsername)
                .get()
                .await()

            if (!document.exists()) {
                UsernameEmailLookupResult.NotFound
            } else {
                document.getString("email")
                    ?.takeIf(String::isNotBlank)
                    ?.let(UsernameEmailLookupResult::Found)
                    ?: UsernameEmailLookupResult.MissingEmail
            }
        } catch (exception: Exception) {
            UsernameEmailLookupResult.Failure(
                if (exception is FirebaseNetworkException) {
                    AuthLoginFailure.NETWORK
                } else {
                    AuthLoginFailure.USERNAME_LOOKUP_UNKNOWN
                }
            )
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthLoginResult {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            AuthLoginResult.Success
        } catch (exception: Exception) {
            AuthLoginResult.Failure(
                when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        AuthLoginFailure.INVALID_CREDENTIALS
                    }

                    is FirebaseAuthInvalidUserException -> {
                        AuthLoginFailure.ACCOUNT_NOT_FOUND
                    }

                    is FirebaseNetworkException -> {
                        AuthLoginFailure.NETWORK
                    }

                    else -> AuthLoginFailure.UNKNOWN
                }
            )
        }
    }

    override suspend fun sendPasswordReset(email: String): PasswordResetResult {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            PasswordResetResult.Success
        } catch (exception: Exception) {
            PasswordResetResult.Failure(
                when (exception) {
                    is FirebaseAuthInvalidUserException -> {
                        PasswordResetFailure.ACCOUNT_NOT_FOUND
                    }

                    is FirebaseAuthInvalidCredentialsException -> {
                        PasswordResetFailure.INVALID_EMAIL
                    }

                    is FirebaseNetworkException -> PasswordResetFailure.NETWORK
                    else -> PasswordResetFailure.UNKNOWN
                }
            )
        }
    }

    override fun currentSession(): AuthSessionSnapshot? {
        val user = auth.currentUser ?: return null
        return AuthSessionSnapshot(
            uid = user.uid,
            isPasswordUser = user.providerData.any { provider ->
                provider.providerId == EmailAuthProvider.PROVIDER_ID
            },
            isEmailVerified = user.isEmailVerified
        )
    }

    override fun sendCurrentUserVerificationEmail() {
        auth.currentUser?.sendEmailVerification()
    }

    override fun signOut() {
        auth.signOut()
    }

    private companion object {
        const val USERNAMES_COLLECTION = "usernames"
    }
}
