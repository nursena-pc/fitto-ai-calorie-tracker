package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue
import java.util.Locale

internal object SocialProfileMaintenanceResolver {

    fun createCurrentUserProfilePayload(
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        createdAt: Long,
        updatedAt: Long
    ): Map<String, Any?> {
        val publicUserName = SocialTextResolver.normalizeUsername(
            email?.substringBefore("@")
                ?: displayName
                ?: "fitto_user"
        ).ifBlank { "fitto_user" }

        val personalName = displayName
            ?: email?.substringBefore("@")
            ?: "Fitto"

        return mapOf(
            "uid" to uid,
            "displayName" to publicUserName,
            "displayNameLowercase" to publicUserName.lowercase(Locale.ROOT),
            "realName" to personalName,
            "name" to personalName,
            "userName" to publicUserName,
            "username" to publicUserName,
            "usernameLowercase" to publicUserName.lowercase(Locale.ROOT),
            "email" to email,
            "photoUrl" to photoUrl,
            "bio" to "",
            "followersCount" to 0,
            "followingCount" to 0,
            "profileCompleted" to false,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    fun oldUsernameCandidates(
        userName: String?,
        username: String?,
        displayName: String?
    ): Set<String> {
        return buildSet {
            add(userName)
            add(username)
            add(displayName)
        }.filterNotNull()
            .map { SocialTextResolver.normalizeUsername(it) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun createUsernameClaimPayload(
        uid: String,
        email: String,
        username: String,
        updatedAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "username" to username,
            "usernameLowercase" to username.lowercase(Locale.ROOT),
            "updatedAt" to updatedAt
        )
    }

    fun createProfileUpdatePayload(
        username: String,
        bio: String,
        updatedAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "displayName" to username,
            "displayNameLowercase" to username.lowercase(Locale.ROOT),
            "userName" to username,
            "username" to username,
            "usernameLowercase" to username.lowercase(Locale.ROOT),
            "bio" to bio.trim().take(160),
            "updatedAt" to updatedAt
        )
    }

    fun createPhotoDocumentUpdatePayload(
        photoUrl: String?,
        updatedAt: Long
    ): Map<String, Any> {
        return if (photoUrl.isNullOrBlank()) {
            mapOf(
                "photoUrl" to FieldValue.delete(),
                "updatedAt" to updatedAt
            )
        } else {
            mapOf(
                "photoUrl" to photoUrl,
                "updatedAt" to updatedAt
            )
        }
    }

    fun createRealtimeStatusPhotoUpdatePayload(
        photoUrl: String?,
        updatedAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "photoUrl" to photoUrl?.takeIf { it.isNotBlank() },
            "updatedAt" to updatedAt
        )
    }
}
