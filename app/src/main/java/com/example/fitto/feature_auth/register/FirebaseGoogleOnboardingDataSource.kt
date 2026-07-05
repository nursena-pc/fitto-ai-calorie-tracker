package com.example.fitto.feature_auth.register

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class FirebaseGoogleOnboardingDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) : GoogleOnboardingDataSource {

    override suspend fun signIn(idToken: String): GoogleOnboardingIdentity? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user ?: return null
        return GoogleOnboardingIdentity(
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = user.displayName.orEmpty(),
            photoUrl = user.photoUrl?.toString()
        )
    }

    override suspend fun profileStatus(uid: String): GoogleOnboardingProfileStatus {
        val document = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .await()
        return GoogleOnboardingProfileStatus(
            exists = document.exists(),
            isCompleted = document.getBoolean(PROFILE_COMPLETED_FIELD) == true
        )
    }

    override suspend fun createInitialProfile(identity: GoogleOnboardingIdentity) {
        val cleanEmailName = identity.email.substringBefore("@")
            .lowercase()
            .replace(Regex("[^a-z0-9._]"), "")
            .ifBlank { DEFAULT_USERNAME }
        val profileName = identity.displayName.ifBlank { cleanEmailName }
        val now = clock()

        val userData = hashMapOf(
            "uid" to identity.uid,
            "displayName" to cleanEmailName,
            "displayNameLowercase" to cleanEmailName.lowercase(),
            "realName" to profileName,
            "name" to profileName,
            "userName" to cleanEmailName,
            "username" to cleanEmailName,
            "usernameLowercase" to cleanEmailName.lowercase(),
            "email" to identity.email,
            "photoUrl" to identity.photoUrl,
            "bio" to "",
            "followersCount" to 0,
            "followingCount" to 0,
            PROFILE_COMPLETED_FIELD to false,
            "createdAt" to now,
            "updatedAt" to now
        )

        firestore.collection(USERS_COLLECTION)
            .document(identity.uid)
            .set(userData)
            .await()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val PROFILE_COMPLETED_FIELD = "profileCompleted"
        const val DEFAULT_USERNAME = "fitto_user"
    }
}
