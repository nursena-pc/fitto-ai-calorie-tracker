package com.example.fitto.feature_auth.register

import android.content.Context
import android.util.Log
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseUsernameSetupDataSource(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val profileCacheRepository: ProfileCacheRepository = ProfileCacheRepository(
        context.applicationContext
    )
) : UsernameSetupDataSource {

    override fun currentIdentity(): UsernameSetupIdentity? {
        val user = auth.currentUser ?: return null
        return UsernameSetupIdentity(
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = user.displayName
        )
    }

    override suspend fun checkAvailability(username: String): UsernameAvailability {
        val document = firestore.collection(USERNAMES_COLLECTION)
            .document(username)
            .get()
            .await()
        val ownerUid = document.getString("uid")
        val currentUid = auth.currentUser?.uid
        return if (!document.exists() || ownerUid == currentUid) {
            UsernameAvailability.AVAILABLE
        } else {
            UsernameAvailability.TAKEN
        }
    }

    override suspend fun saveProfile(
        request: UsernameProfileSaveRequest
    ): UsernameProfileSaveResult {
        val usernameRef = firestore.collection(USERNAMES_COLLECTION)
            .document(request.username)
        val userRef = firestore.collection(USERS_COLLECTION)
            .document(request.identity.uid)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(usernameRef)
                val ownerUid = snapshot.getString("uid")
                if (snapshot.exists() && ownerUid != request.identity.uid) {
                    throw UsernameTakenException()
                }

                transaction.set(
                    usernameRef,
                    request.payload.usernameDocument,
                    SetOptions.merge()
                )
                transaction.set(
                    userRef,
                    request.payload.userDocument,
                    SetOptions.merge()
                )
                null
            }.await()

            runCatching {
                withContext(Dispatchers.IO) {
                    profileCacheRepository.cacheProfileFromFirebaseMap(
                        userId = request.identity.uid,
                        emailFallback = request.identity.email,
                        data = request.payload.userDocument
                    )
                }
            }.onFailure { exception ->
                Log.e(TAG, "Profile cached to Room failed", exception)
            }
            UsernameProfileSaveResult.Success(request.payload.userDocument)
        } catch (exception: Exception) {
            if (exception.isUsernameTaken()) {
                UsernameProfileSaveResult.UsernameTaken
            } else {
                UsernameProfileSaveResult.Failure(exception)
            }
        }
    }

    private fun Throwable.isUsernameTaken(): Boolean {
        return generateSequence(this) { throwable -> throwable.cause }
            .any { throwable -> throwable is UsernameTakenException }
    }

    private class UsernameTakenException : Exception()

    private companion object {
        const val TAG = "FirebaseUsernameSetup"
        const val USERS_COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
    }
}
