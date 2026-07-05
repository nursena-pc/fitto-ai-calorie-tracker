package com.example.fitto.feature_auth.register

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAccountRegistrationDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) : AccountRegistrationDataSource {

    override suspend fun startRegistration(
        email: String,
        password: String
    ): AccountRegistrationStartResult {
        return try {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: return AccountRegistrationStartResult.Failure(message = null)

            try {
                user.sendEmailVerification().await()
                AccountRegistrationStartResult.VerificationSent(
                    uid = user.uid,
                    email = email,
                    wasResent = false
                )
            } catch (_: Exception) {
                AccountRegistrationStartResult.VerificationSendFailed
            }
        } catch (exception: FirebaseAuthUserCollisionException) {
            handleExistingEmail(email, password)
        } catch (exception: Exception) {
            AccountRegistrationStartResult.Failure(exception.localizedMessage)
        }
    }

    override suspend fun isCurrentUserEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return runCatching {
            user.reload().await()
            auth.currentUser?.isEmailVerified == true
        }.getOrDefault(false)
    }

    override suspend fun resendCurrentUserVerificationEmail(): Boolean {
        val user = auth.currentUser ?: return false
        return runCatching {
            user.sendEmailVerification().await()
        }.isSuccess
    }

    override suspend fun cancelUnverifiedRegistration() {
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) {
            runCatching { user.delete().await() }
        }
        auth.signOut()
    }

    override suspend fun createInitialProfile(uid: String, email: String): Boolean {
        val username = email.substringBefore("@")
            .lowercase()
            .replace(Regex("[^a-z0-9._]"), "")
            .ifBlank { "fitto_user" }
        val now = clock()
        val userData = hashMapOf<String, Any?>(
            "uid" to uid,
            "realName" to username,
            "userName" to username,
            "email" to email,
            "photoUrl" to null,
            "bio" to "",
            "followersCount" to 0,
            "followingCount" to 0,
            "profileCompleted" to false,
            "createdAt" to now,
            "updatedAt" to now
        )

        return runCatching {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(userData)
                .await()
        }.isSuccess
    }

    private suspend fun handleExistingEmail(
        email: String,
        password: String
    ): AccountRegistrationStartResult {
        val user = runCatching {
            auth.signInWithEmailAndPassword(email, password).await().user
        }.getOrNull() ?: return AccountRegistrationStartResult.EmailAlreadyRegistered

        val refreshed = runCatching {
            user.reload().await()
            auth.currentUser
        }.getOrNull() ?: return AccountRegistrationStartResult.EmailAlreadyRegistered

        if (refreshed.isEmailVerified) {
            auth.signOut()
            return AccountRegistrationStartResult.EmailAlreadyRegistered
        }

        return try {
            refreshed.sendEmailVerification().await()
            AccountRegistrationStartResult.VerificationSent(
                uid = refreshed.uid,
                email = email,
                wasResent = true
            )
        } catch (_: Exception) {
            AccountRegistrationStartResult.VerificationResendFailed
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
