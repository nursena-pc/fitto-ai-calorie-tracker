package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

internal class FirebaseSocialProfileCommandSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val realtimeDatabase: FirebaseDatabase,
    private val storage: FirebaseStorage
) : SocialProfileCommandSource {

    override suspend fun updateCurrentUserProfile(
        displayName: String,
        bio: String
    ) {
        val user = auth.currentUser ?: return
        val newVisibleName = SocialTextResolver.normalizeUsername(displayName)
        SocialTextResolver.validateVisibleName(newVisibleName)

        val now = System.currentTimeMillis()
        val userRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        val newUsernameRef = firestore.collection(USERNAMES_COLLECTION).document(newVisibleName)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val oldUsernameCandidates = SocialProfileMaintenanceResolver.oldUsernameCandidates(
                userName = userSnapshot.getString("userName"),
                username = userSnapshot.getString("username"),
                displayName = userSnapshot.getString("displayName")
            )

            val newUsernameSnapshot = transaction.get(newUsernameRef)
            val ownerUid = newUsernameSnapshot.getString("uid")

            if (newUsernameSnapshot.exists() && ownerUid != user.uid) {
                throw IllegalStateException("VISIBLE_NAME_TAKEN")
            }

            val oldUsernameSnapshots = oldUsernameCandidates
                .filterNot { it == newVisibleName }
                .map { oldUsername ->
                    val oldUsernameRef = firestore.collection(USERNAMES_COLLECTION).document(oldUsername)
                    oldUsernameRef to transaction.get(oldUsernameRef)
                }

            oldUsernameSnapshots.forEach { (oldUsernameRef, oldUsernameSnapshot) ->
                if (oldUsernameSnapshot.exists() && oldUsernameSnapshot.getString("uid") == user.uid) {
                    transaction.delete(oldUsernameRef)
                }
            }

            transaction.set(
                newUsernameRef,
                SocialProfileMaintenanceResolver.createUsernameClaimPayload(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    username = newVisibleName,
                    updatedAt = now
                ),
                SetOptions.merge()
            )

            transaction.set(
                userRef,
                SocialProfileMaintenanceResolver.createProfileUpdatePayload(
                    username = newVisibleName,
                    bio = bio,
                    updatedAt = now
                ),
                SetOptions.merge()
            )

            null
        }.await()

        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(newVisibleName)
                    .build()
            ).await()
        }

        updateOwnedPublicUsernameCopies(
            uid = user.uid,
            userName = newVisibleName,
            now = now
        )
    }

    override suspend fun updateCurrentUserPhotoUrl(photoUrl: String?) {
        val user = auth.currentUser ?: return
        val now = System.currentTimeMillis()
        val userRef = firestore.collection(USERS_COLLECTION).document(user.uid)

        userRef.set(
            SocialProfileMaintenanceResolver.createPhotoDocumentUpdatePayload(
                photoUrl = photoUrl,
                updatedAt = now
            ),
            SetOptions.merge()
        ).await()

        runCatching {
            realtimeDatabase.reference
                .child(REALTIME_STATUS_NODE)
                .child(user.uid)
                .updateChildren(
                    SocialProfileMaintenanceResolver.createRealtimeStatusPhotoUpdatePayload(
                        photoUrl = photoUrl,
                        updatedAt = now
                    )
                )
                .await()
        }.onFailure { error ->
            error.printStackTrace()
        }

        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setPhotoUri(photoUrl?.let { Uri.parse(it) })
                    .build()
            ).await()
        }

        runCatching {
            updateOwnedProfilePhotoCopies(
                uid = user.uid,
                photoUrl = photoUrl,
                now = now
            )
        }.onFailure { error ->
            error.printStackTrace()
        }
    }

    override suspend fun uploadCurrentUserProfilePhoto(
        context: Context,
        imageUri: Uri
    ): String {
        val user = auth.currentUser ?: return ""
        val now = System.currentTimeMillis()
        val storageRef = storage.reference.child(
            SocialMediaUploadResolver.profilePhotoStoragePath(
                userId = user.uid,
                nowMillis = now,
                fileId = UUID.randomUUID().toString()
            )
        )

        val metadata = StorageMetadata.Builder()
            .setContentType(
                SocialMediaUploadResolver.imageContentType(
                    context.contentResolver.getType(imageUri)
                )
            )
            .build()

        storageRef.putFile(imageUri, metadata).await()
        return storageRef.downloadUrl.await().toString()
    }

    private suspend fun updateOwnedPublicUsernameCopies(
        uid: String,
        userName: String,
        now: Long
    ) {
        SocialProfileCopyPropagationResolver.publicUsernameTargets(
            uid = uid,
            userName = userName,
            now = now
        ).forEach { target ->
            propagateProfileCopyTarget(target)
        }
    }

    private suspend fun updateOwnedProfilePhotoCopies(
        uid: String,
        photoUrl: String?,
        now: Long
    ) {
        SocialProfileCopyPropagationResolver.profilePhotoTargets(
            uid = uid,
            photoUrl = photoUrl,
            now = now
        ).forEach { target ->
            propagateProfileCopyTarget(target)
        }
    }

    private suspend fun propagateProfileCopyTarget(target: SocialProfileCopyPropagationTarget) {
        runCatching {
            val query = when (target.scope) {
                SocialProfileCopyQueryScope.COLLECTION -> firestore.collection(target.sourceName)
                SocialProfileCopyQueryScope.COLLECTION_GROUP -> firestore.collectionGroup(target.sourceName)
            }
            val filteredQuery = when (target.lookupOperator) {
                SocialProfileCopyLookupOperator.EQUAL_TO -> query.whereEqualTo(
                    target.lookupField,
                    target.lookupValue
                )
                SocialProfileCopyLookupOperator.ARRAY_CONTAINS -> query.whereArrayContains(
                    target.lookupField,
                    target.lookupValue
                )
            }
            val documents = filteredQuery
                .get()
                .await()
                .documents

            updateDocumentsInBatches(
                documents = documents,
                updates = target.updates,
                useSetMerge = target.useSetMerge
            )
        }.onFailure { error ->
            error.printStackTrace()
        }
    }

    private suspend fun updateDocumentsInBatches(
        documents: List<DocumentSnapshot>,
        updates: Map<String, Any>,
        useSetMerge: Boolean = true
    ) {
        val documentBatches = SocialProfileCopyPropagationResolver.documentBatches(documents)
        if (documentBatches.isEmpty()) return

        documentBatches.forEach { chunk ->
            val batch = firestore.batch()

            chunk.forEach { document ->
                if (useSetMerge) {
                    batch.set(document.reference, updates, SetOptions.merge())
                } else {
                    batch.update(document.reference, updates)
                }
            }

            batch.commit().await()
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
        const val REALTIME_STATUS_NODE = "status"
    }
}
