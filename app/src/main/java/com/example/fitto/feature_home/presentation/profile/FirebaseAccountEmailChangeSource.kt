package com.example.fitto.feature_home.presentation.profile

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

internal class FirebaseAccountEmailChangeSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AccountEmailChangeSource {

    override suspend fun updateEmailAfterReauth(
        newEmail: String,
        languageCode: String
    ) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val uid = currentUser.uid
        val cleanEmail = AccountSettingsInputPolicy.normalizeEmail(newEmail)

        if (cleanEmail.equals(currentUser.email.orEmpty(), ignoreCase = true)) {
            throw AccountSettingsException(AccountSettingsError.EMAIL_SAME_AS_CURRENT)
        }

        val now = System.currentTimeMillis()
        val userRef = firestore.collection(USERS_COLLECTION).document(uid)
        val userSnapshot = userRef.get().await()
        val currentUsername = userSnapshot.getString("userName")
            ?: userSnapshot.getString("username")

        try {
            auth.setLanguageCode(languageCode)
            currentUser.verifyBeforeUpdateEmail(cleanEmail).await()
        } catch (exception: FirebaseAuthRecentLoginRequiredException) {
            throw AccountSettingsException(AccountSettingsError.REQUIRES_RECENT_LOGIN, exception)
        }

        userRef.set(
            mapOf(
                "pendingEmail" to cleanEmail,
                "emailChangeRequestedAt" to now,
                "updatedAt" to now
            ),
            SetOptions.merge()
        ).await()

        currentUsername
            ?.let(AccountSettingsInputPolicy::normalizeStoredUsername)
            ?.takeIf { it.isNotBlank() }
            ?.let { username ->
                firestore.collection(USERNAMES_COLLECTION)
                    .document(username)
                    .set(
                        mapOf(
                            "pendingEmail" to cleanEmail,
                            "emailChangeRequestedAt" to now,
                            "updatedAt" to now
                        ),
                        SetOptions.merge()
                    ).await()
            }
    }

    override suspend fun resendPendingEmailChangeVerification(
        pendingEmail: String,
        languageCode: String
    ) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val cleanEmail = AccountSettingsInputPolicy.normalizeEmail(pendingEmail)

        if (cleanEmail.equals(currentUser.email.orEmpty(), ignoreCase = true)) {
            throw AccountSettingsException(AccountSettingsError.EMAIL_SAME_AS_CURRENT)
        }

        try {
            auth.setLanguageCode(languageCode)
            currentUser.verifyBeforeUpdateEmail(cleanEmail).await()
        } catch (exception: FirebaseAuthRecentLoginRequiredException) {
            throw AccountSettingsException(AccountSettingsError.REQUIRES_RECENT_LOGIN, exception)
        }

        val now = System.currentTimeMillis()
        val uid = currentUser.uid
        val userRef = firestore.collection(USERS_COLLECTION).document(uid)
        val userSnapshot = userRef.get().await()
        val currentUsername = userSnapshot.getString("userName")
            ?: userSnapshot.getString("username")

        userRef.set(
            mapOf(
                "pendingEmail" to cleanEmail,
                "emailChangeRequestedAt" to now,
                "updatedAt" to now
            ),
            SetOptions.merge()
        ).await()

        currentUsername
            ?.let(AccountSettingsInputPolicy::normalizeStoredUsername)
            ?.takeIf { it.isNotBlank() }
            ?.let { username ->
                firestore.collection(USERNAMES_COLLECTION)
                    .document(username)
                    .set(
                        mapOf(
                            "pendingEmail" to cleanEmail,
                            "emailChangeRequestedAt" to now,
                            "updatedAt" to now
                        ),
                        SetOptions.merge()
                    ).await()
            }
    }

    override fun currentAuthEmail(): String {
        return auth.currentUser?.email.orEmpty()
    }

    override fun hasCurrentUser(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun reloadCurrentUserBestEffort() {
        runCatching {
            auth.currentUser?.reload()?.await()
        }.onFailure { exception ->
            Log.w(TAG, "Current user reload skipped", exception)
        }
    }

    override suspend fun checkAndSyncPendingEmailChange(
        expectedEmail: String
    ): EmailChangeSyncResult {
        val cleanExpectedEmail = expectedEmail.trim()
        val currentUser = auth.currentUser ?: return EmailChangeSyncResult.SIGNED_OUT

        val reloadResult = runCatching {
            currentUser.reload().await()
        }

        val reloadException = reloadResult.exceptionOrNull()
        if (auth.currentUser == null || reloadException?.isRecoverableSessionLoss() == true) {
            Log.w(TAG, "Pending email change requires session recovery", reloadException)
            return EmailChangeSyncResult.SIGNED_OUT
        }

        reloadException?.let { exception ->
            Log.w(TAG, "Pending email change check failed", exception)
            return EmailChangeSyncResult.WAITING
        }

        val refreshedUser = auth.currentUser ?: return EmailChangeSyncResult.SIGNED_OUT
        val authEmail = refreshedUser.email
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return EmailChangeSyncResult.WAITING

        return if (authEmail.equals(cleanExpectedEmail, ignoreCase = true)) {
            syncCurrentAuthEmailToProfileIfNeeded(skipReload = true)
            EmailChangeSyncResult.VERIFIED
        } else {
            EmailChangeSyncResult.WAITING
        }
    }

    override suspend fun signInAfterVerifiedEmailChange(
        email: String,
        password: String
    ): EmailChangeSyncResult {
        val cleanEmail = AccountSettingsInputPolicy.normalizeEmail(email)
        AccountSettingsInputPolicy.requireCurrentPassword(password)

        return try {
            auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val signedInEmail = auth.currentUser?.email
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (signedInEmail.equals(cleanEmail, ignoreCase = true)) {
                syncCurrentAuthEmailToProfileIfNeeded(skipReload = false)
            } else {
                EmailChangeSyncResult.WAITING
            }
        } catch (exception: FirebaseAuthInvalidCredentialsException) {
            // Doğrulama linki henüz uygulanmadıysa yeni e-posta ile giriş başarısız olur.
            // Şifre reauth aşamasında doğrulandığı için burada beklemeye devam etmek daha doğru.
            Log.w(TAG, "Verified email sign-in is not ready yet", exception)
            EmailChangeSyncResult.WAITING
        }
    }

    override suspend fun syncCurrentAuthEmailToProfileIfNeeded(
        skipReload: Boolean
    ): EmailChangeSyncResult {
        if (!skipReload) {
            reloadCurrentUserBestEffort()
        }

        val currentUser = auth.currentUser ?: return EmailChangeSyncResult.SIGNED_OUT
        val uid = currentUser.uid
        val authEmail = currentUser.email
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return EmailChangeSyncResult.WAITING

        val now = System.currentTimeMillis()
        val userRef = firestore.collection(USERS_COLLECTION).document(uid)
        val userSnapshot = userRef.get().await()
        val savedEmail = userSnapshot.getString("email").orEmpty()
        val pendingEmail = userSnapshot.getString("pendingEmail")
            ?.trim()
            .orEmpty()
        val isPendingVerified = pendingEmail.isNotBlank() &&
            authEmail.equals(pendingEmail, ignoreCase = true)
        val hasPendingDifferentEmail = pendingEmail.isNotBlank() &&
            !authEmail.equals(pendingEmail, ignoreCase = true)
        val currentUsername = (userSnapshot.getString("userName")
            ?: userSnapshot.getString("username"))
            ?.let(AccountSettingsInputPolicy::normalizeStoredUsername)
            .orEmpty()

        when {
            isPendingVerified -> {
                userRef.set(
                    mapOf(
                        "email" to authEmail,
                        "pendingEmail" to FieldValue.delete(),
                        "emailChangeRequestedAt" to FieldValue.delete(),
                        "updatedAt" to now
                    ),
                    SetOptions.merge()
                ).await()
            }

            hasPendingDifferentEmail -> {
                // Doğrulama linki henüz onaylanmadı. pendingEmail saklanır;
                // aksi halde kullanıcı hesap ekranında bekleme durumunu kaybeder.
                if (savedEmail.isBlank()) {
                    userRef.set(
                        mapOf(
                            "email" to authEmail,
                            "updatedAt" to now
                        ),
                        SetOptions.merge()
                    ).await()
                }
                return EmailChangeSyncResult.WAITING
            }

            !savedEmail.equals(authEmail, ignoreCase = true) -> {
                userRef.set(
                    mapOf(
                        "email" to authEmail,
                        "updatedAt" to now
                    ),
                    SetOptions.merge()
                ).await()
            }

            else -> return EmailChangeSyncResult.VERIFIED
        }

        val usernameDocs = firestore.collection(USERNAMES_COLLECTION)
            .whereEqualTo("uid", uid)
            .get()
            .await()
            .documents

        val batchWriter = FirestoreBatchWriter(firestore)
        usernameDocs.forEach { document ->
            if (currentUsername.isNotBlank() && document.id != currentUsername) {
                batchWriter.delete(document.reference)
            } else {
                batchWriter.setMerge(
                    document.reference,
                    mapOf(
                        "email" to authEmail,
                        "pendingEmail" to FieldValue.delete(),
                        "emailChangeRequestedAt" to FieldValue.delete(),
                        "updatedAt" to now
                    )
                )
            }
        }
        batchWriter.commit()

        return EmailChangeSyncResult.VERIFIED
    }

    private fun Throwable.isRecoverableSessionLoss(): Boolean {
        val authException = this as? FirebaseAuthException ?: return false
        return authException.errorCode in setOf(
            "ERROR_USER_TOKEN_EXPIRED",
            "ERROR_INVALID_USER_TOKEN",
            "ERROR_USER_NOT_FOUND",
            "ERROR_USER_DISABLED",
            "ERROR_CREDENTIAL_ALREADY_IN_USE"
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
        const val TAG = "AccountEmailChangeSource"
    }
}
