package com.example.fitto.feature_home.presentation.profile

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

enum class EmailChangeSyncResult {
    WAITING,
    VERIFIED,
    SIGNED_OUT
}

class AccountSettingsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun updateDisplayName(displayName: String) {
        val currentUser = auth.currentUser ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val uid = currentUser.uid
        val cleanName = displayName.trim().replace(Regex("\\s+"), " ")

        if (cleanName.length < MIN_DISPLAY_NAME_LENGTH) {
            throw AccountSettingsException(AccountSettingsError.DISPLAY_NAME_TOO_SHORT)
        }

        if (cleanName.length > MAX_DISPLAY_NAME_LENGTH) {
            throw AccountSettingsException(AccountSettingsError.DISPLAY_NAME_TOO_LONG)
        }

        val now = System.currentTimeMillis()

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(
                mapOf(
                    // Kişisel hitap adı. Sosyalde/girişte kullanılan görünen ad değildir.
                    "realName" to cleanName,
                    "name" to cleanName,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            ).await()
    }

    suspend fun updateUsername(rawUsername: String) {
        val currentUser = auth.currentUser ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val uid = currentUser.uid
        val newUsername = normalizeUsername(rawUsername)

        validateUsername(newUsername)

        val now = System.currentTimeMillis()
        val userRef = firestore.collection(USERS_COLLECTION).document(uid)
        val newUsernameRef = firestore.collection(USERNAMES_COLLECTION).document(newUsername)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val oldUsernameCandidates = buildSet {
                add(userSnapshot.getString("userName"))
                add(userSnapshot.getString("username"))
                add(userSnapshot.getString("displayName"))
            }.filterNotNull()
                .map { normalizeUsername(it) }
                .filter { it.isNotBlank() }
                .toSet()

            val newUsernameSnapshot = transaction.get(newUsernameRef)
            val ownerUid = newUsernameSnapshot.getString("uid")

            if (newUsernameSnapshot.exists() && ownerUid != uid) {
                throw AccountSettingsException(AccountSettingsError.USERNAME_TAKEN)
            }

            val oldUsernameSnapshots = oldUsernameCandidates
                .filterNot { it == newUsername }
                .map { oldUsername ->
                    val oldUsernameRef = firestore.collection(USERNAMES_COLLECTION).document(oldUsername)
                    oldUsernameRef to transaction.get(oldUsernameRef)
                }

            oldUsernameSnapshots.forEach { (oldUsernameRef, oldUsernameSnapshot) ->
                if (oldUsernameSnapshot.exists() && oldUsernameSnapshot.getString("uid") == uid) {
                    transaction.delete(oldUsernameRef)
                }
            }

            transaction.set(
                newUsernameRef,
                mapOf(
                    "uid" to uid,
                    "email" to currentUser.email.orEmpty(),
                    "username" to newUsername,
                    "usernameLowercase" to newUsername.lowercase(Locale.ROOT),
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )

            transaction.set(
                userRef,
                mapOf(
                    "userName" to newUsername,
                    "username" to newUsername,
                    "usernameLowercase" to newUsername.lowercase(Locale.ROOT),
                    "displayName" to newUsername,
                    "displayNameLowercase" to newUsername.lowercase(Locale.ROOT),
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )

            null
        }.await()

        cleanupOwnedUsernameDocuments(
            uid = uid,
            keepUsername = newUsername
        )

        updateOwnedPublicUsernameCopies(
            uid = uid,
            userName = newUsername,
            now = now
        )

        runCatching {
            currentUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build()
            ).await()
        }
    }

    suspend fun reauthenticateWithPassword(password: String) {
        val currentUser = auth.currentUser ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val email = currentUser.email?.takeIf { it.isNotBlank() }
            ?: throw AccountSettingsException(AccountSettingsError.EMAIL_MISSING)

        if (password.isBlank()) {
            throw AccountSettingsException(AccountSettingsError.CURRENT_PASSWORD_EMPTY)
        }

        try {
            currentUser.reauthenticate(
                EmailAuthProvider.getCredential(email, password)
            ).await()
        } catch (exception: FirebaseAuthInvalidCredentialsException) {
            throw AccountSettingsException(AccountSettingsError.WRONG_PASSWORD, exception)
        }
    }

    suspend fun updatePasswordAfterReauth(newPassword: String, confirmPassword: String) {
        val currentUser = auth.currentUser ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)

        validateNewPassword(
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        currentUser.updatePassword(newPassword).await()
    }

    suspend fun updateEmailAfterReauth(
        newEmail: String,
        languageCode: String = "tr"
    ) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)

        val uid = currentUser.uid
        val cleanEmail = newEmail.trim()

        validateEmail(cleanEmail)

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
            ?.let { normalizeUsername(it) }
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

    suspend fun resendPendingEmailChangeVerification(
        pendingEmail: String,
        languageCode: String = "tr"
    ) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val cleanEmail = pendingEmail.trim()

        validateEmail(cleanEmail)

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
            ?.let { normalizeUsername(it) }
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

    fun currentAuthEmail(): String {
        return auth.currentUser?.email.orEmpty()
    }

    fun hasCurrentUser(): Boolean {
        return auth.currentUser != null
    }

    suspend fun reloadCurrentUserBestEffort() {
        runCatching {
            auth.currentUser?.reload()?.await()
        }.onFailure { exception ->
            Log.w(TAG, "Current user reload skipped", exception)
        }
    }

    suspend fun checkAndSyncPendingEmailChange(expectedEmail: String): EmailChangeSyncResult {
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

    suspend fun signInAfterVerifiedEmailChange(
        email: String,
        password: String
    ): EmailChangeSyncResult {
        val cleanEmail = email.trim()
        validateEmail(cleanEmail)

        if (password.isBlank()) {
            throw AccountSettingsException(AccountSettingsError.CURRENT_PASSWORD_EMPTY)
        }

        return try {
            auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val signedInEmail = auth.currentUser?.email
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (signedInEmail.equals(cleanEmail, ignoreCase = true)) {
                syncCurrentAuthEmailToProfileIfNeeded()
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

    suspend fun syncCurrentAuthEmailToProfileIfNeeded(
        skipReload: Boolean = false
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
            ?.let { normalizeUsername(it) }
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

        val batchWriter = BatchWriter(firestore)
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

    fun isPasswordProvider(): Boolean {
        return auth.currentUser?.providerData
            ?.any { providerInfo -> providerInfo.providerId == EmailAuthProvider.PROVIDER_ID } == true
    }

    fun isGoogleProvider(): Boolean {
        return auth.currentUser?.providerData
            ?.any { providerInfo -> providerInfo.providerId == GOOGLE_PROVIDER_ID } == true
    }

    suspend fun clearCurrentUserHistory(): String {
        val currentUser = auth.currentUser ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val uid = currentUser.uid
        val now = System.currentTimeMillis()

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(
                mapOf(
                    BodyMeasurementsData.FIELD_NAME to FieldValue.delete(),
                    BodyMeasurementsData.HISTORY_FIELD_NAME to FieldValue.delete(),
                    WEIGHT_HISTORY_FIELD to FieldValue.delete(),
                    MEAL_HISTORY_FIELD to FieldValue.delete(),
                    NUTRITION_HISTORY_FIELD to FieldValue.delete(),
                    "weightKg" to FieldValue.delete(),
                    "currentWeightKg" to FieldValue.delete(),
                    "waterHistory" to FieldValue.delete(),
                    "exerciseHistory" to FieldValue.delete(),
                    "sleepHistory" to FieldValue.delete(),
                    "fastingHistory" to FieldValue.delete(),
                    "dailyCheckins" to FieldValue.delete(),
                    "weeklyReports" to FieldValue.delete(),
                    "activeDietPlanId" to FieldValue.delete(),
                    "activePlanId" to FieldValue.delete(),
                    "lastSelectedDietPlanId" to FieldValue.delete(),
                    "historyClearedAt" to now,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )
            .await()

        return uid
    }

    private suspend fun cleanupOwnedUsernameDocuments(
        uid: String,
        keepUsername: String
    ) {
        val usernameDocuments = firestore.collection(USERNAMES_COLLECTION)
            .whereEqualTo("uid", uid)
            .get()
            .await()
            .documents

        val batchWriter = BatchWriter(firestore)

        usernameDocuments
            .filterNot { document -> document.id == keepUsername }
            .forEach { document ->
                batchWriter.delete(document.reference)
            }

        batchWriter.commit()
    }

    private suspend fun updateOwnedPublicUsernameCopies(
        uid: String,
        userName: String,
        now: Long
    ) {
        val posts = firestore.collection(POSTS_COLLECTION)
            .whereEqualTo("userId", uid)
            .get()
            .await()
            .documents

        val comments = firestore.collectionGroup(COMMENTS_SUBCOLLECTION)
            .whereEqualTo("userId", uid)
            .get()
            .await()
            .documents

        val batchWriter = BatchWriter(firestore)

        posts.forEach { post ->
            batchWriter.setMerge(
                post.reference,
                mapOf(
                    "userName" to userName,
                    "updatedAt" to now
                )
            )
        }

        comments.forEach { comment ->
            batchWriter.setMerge(
                comment.reference,
                mapOf(
                    "userName" to userName,
                    "updatedAt" to now
                )
            )
        }

        batchWriter.commit()

        // Eski sohbet kopyaları yardımcı veridir. İzin/index kaynaklı hata olursa
        // kullanıcı adı değişimini bozmadan sadece loglarız.
        runCatching {
            updateOwnedPublicUsernameChatCopies(
                uid = uid,
                userName = userName,
                now = now
            )
        }.onFailure { exception ->
            Log.w(TAG, "Public username chat copy update skipped", exception)
        }
    }

    private suspend fun updateOwnedPublicUsernameChatCopies(
        uid: String,
        userName: String,
        now: Long
    ) {
        val messageThreads = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .whereArrayContains("participantIds", uid)
            .get()
            .await()
            .documents

        val messages = firestore.collectionGroup(MESSAGES_SUBCOLLECTION)
            .whereEqualTo("senderId", uid)
            .get()
            .await()
            .documents

        val batchWriter = BatchWriter(firestore)

        messageThreads.forEach { thread ->
            batchWriter.update(
                thread.reference,
                mapOf(
                    "participants.$uid.userName" to userName,
                    "updatedAt" to now
                )
            )
        }

        messages.forEach { message ->
            batchWriter.setMerge(
                message.reference,
                mapOf(
                    "senderName" to userName,
                    "updatedAt" to now
                )
            )
        }

        batchWriter.commit()
    }

    private fun validateUsername(username: String) {
        if (username.length < MIN_USERNAME_LENGTH) {
            throw AccountSettingsException(AccountSettingsError.USERNAME_TOO_SHORT)
        }

        if (username.length > MAX_USERNAME_LENGTH) {
            throw AccountSettingsException(AccountSettingsError.USERNAME_TOO_LONG)
        }

        if (!USERNAME_REGEX.matches(username)) {
            throw AccountSettingsException(AccountSettingsError.USERNAME_INVALID)
        }

        if (username.startsWith('.') || username.startsWith('_') ||
            username.endsWith('.') || username.endsWith('_')
        ) {
            throw AccountSettingsException(AccountSettingsError.USERNAME_INVALID)
        }
    }

    private fun validateEmail(email: String) {
        if (email.isBlank() || !EMAIL_REGEX.matches(email)) {
            throw AccountSettingsException(AccountSettingsError.EMAIL_INVALID)
        }
    }

    private fun validateNewPassword(
        newPassword: String,
        confirmPassword: String
    ) {
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            throw AccountSettingsException(AccountSettingsError.NEW_PASSWORD_TOO_SHORT)
        }

        if (newPassword != confirmPassword) {
            throw AccountSettingsException(AccountSettingsError.PASSWORD_CONFIRM_MISMATCH)
        }
    }

    private fun normalizeUsername(value: String): String {
        return value
            .removePrefix("@")
            .trim()
            .lowercase(Locale.ROOT)
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("\\s+"), ".")
            .replace(Regex("[^a-z0-9._]"), "")
            .replace(Regex("[.]{2,}"), ".")
            .replace(Regex("[_]{2,}"), "_")
            .trim()
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

    private class BatchWriter(
        private val firestore: FirebaseFirestore
    ) {
        private var batch = firestore.batch()
        private var operationCount = 0

        suspend fun update(
            reference: com.google.firebase.firestore.DocumentReference,
            updates: Map<String, Any>
        ) {
            batch.update(reference, updates)
            operationCount++
            commitIfNeeded()
        }

        suspend fun setMerge(
            reference: com.google.firebase.firestore.DocumentReference,
            updates: Map<String, Any?>
        ) {
            batch.set(reference, updates, SetOptions.merge())
            operationCount++
            commitIfNeeded()
        }

        suspend fun delete(reference: com.google.firebase.firestore.DocumentReference) {
            batch.delete(reference)
            operationCount++
            commitIfNeeded()
        }

        private suspend fun commitIfNeeded() {
            if (operationCount >= MAX_BATCH_OPERATIONS) {
                commit()
            }
        }

        suspend fun commit() {
            if (operationCount <= 0) return

            batch.commit().await()
            batch = firestore.batch()
            operationCount = 0
        }

        private companion object {
            const val MAX_BATCH_OPERATIONS = 450
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
        const val POSTS_COLLECTION = "posts"
        const val COMMENTS_SUBCOLLECTION = "comments"
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"
        const val GOOGLE_PROVIDER_ID = "google.com"
        const val WEIGHT_HISTORY_FIELD = "weightHistory"
        const val MEAL_HISTORY_FIELD = "mealHistory"
        const val NUTRITION_HISTORY_FIELD = "nutritionHistory"

        const val MIN_DISPLAY_NAME_LENGTH = 2
        const val MAX_DISPLAY_NAME_LENGTH = 40
        const val MIN_USERNAME_LENGTH = 3
        const val MAX_USERNAME_LENGTH = 20
        const val MIN_PASSWORD_LENGTH = 6

        val USERNAME_REGEX = Regex("^[a-z0-9._]+$")
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        const val TAG = "AccountSettingsRepository"
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
