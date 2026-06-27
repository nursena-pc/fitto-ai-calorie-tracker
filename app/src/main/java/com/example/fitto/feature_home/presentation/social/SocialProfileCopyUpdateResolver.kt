package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue

internal object SocialProfileCopyUpdateResolver {

    fun publicUsernameUpdate(
        userName: String,
        now: Long
    ): Map<String, Any> {
        return mapOf(
            "userName" to userName,
            "updatedAt" to now
        )
    }

    fun participantUsernameUpdate(
        uid: String,
        userName: String,
        now: Long
    ): Map<String, Any> {
        return mapOf(
            "participants.$uid.userName" to userName,
            "updatedAt" to now
        )
    }

    fun senderUsernameUpdate(
        userName: String,
        now: Long
    ): Map<String, Any> {
        return mapOf(
            "senderName" to userName,
            "updatedAt" to now
        )
    }

    fun profilePhotoUpdate(
        fieldNames: List<String>,
        photoUrl: String?,
        now: Long
    ): Map<String, Any> {
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to now
        )

        fieldNames.forEach { fieldName ->
            updates[fieldName] = photoValue(photoUrl)
        }

        return updates
    }

    fun participantPhotoUpdate(
        uid: String,
        photoUrl: String?,
        now: Long
    ): Map<String, Any> {
        return mapOf(
            "participants.$uid.photoUrl" to photoValue(photoUrl),
            "updatedAt" to now
        )
    }

    private fun photoValue(photoUrl: String?): Any {
        return if (photoUrl.isNullOrBlank()) {
            FieldValue.delete()
        } else {
            photoUrl
        }
    }
}
