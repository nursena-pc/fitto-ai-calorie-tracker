package com.example.fitto.feature_home.presentation.profile

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

internal class FirebaseAccountIdentitySettingsSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AccountIdentitySettingsSource {

    override suspend fun updateDisplayName(displayName: String) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val cleanName = AccountSettingsInputPolicy.normalizeDisplayName(displayName)
        val now = System.currentTimeMillis()

        firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
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

    override suspend fun updateUsername(rawUsername: String) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val uid = currentUser.uid
        val newUsername = AccountSettingsInputPolicy.normalizeUsername(rawUsername)
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
                .map(AccountSettingsInputPolicy::normalizeStoredUsername)
                .filter { it.isNotBlank() }
                .toSet()

            val newUsernameSnapshot = transaction.get(newUsernameRef)
            val ownerUid = newUsernameSnapshot.getString("uid")

            if (newUsernameSnapshot.exists() && ownerUid != uid) {
                throw AccountSettingsException(AccountSettingsError.USERNAME_TAKEN)
            }

            oldUsernameCandidates
                .filterNot { it == newUsername }
                .map { oldUsername ->
                    val oldUsernameRef = firestore.collection(USERNAMES_COLLECTION).document(oldUsername)
                    oldUsernameRef to transaction.get(oldUsernameRef)
                }
                .forEach { (oldUsernameRef, oldUsernameSnapshot) ->
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

    private suspend fun cleanupOwnedUsernameDocuments(
        uid: String,
        keepUsername: String
    ) {
        val usernameDocuments = firestore.collection(USERNAMES_COLLECTION)
            .whereEqualTo("uid", uid)
            .get()
            .await()
            .documents

        val batchWriter = FirestoreBatchWriter(firestore)

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

        val batchWriter = FirestoreBatchWriter(firestore)

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

        val batchWriter = FirestoreBatchWriter(firestore)

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

    private companion object {
        const val USERS_COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
        const val POSTS_COLLECTION = "posts"
        const val COMMENTS_SUBCOLLECTION = "comments"
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"
        const val TAG = "AccountIdentitySettings"
    }
}
