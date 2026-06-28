package com.example.fitto.feature_home.presentation.social

import java.util.Locale

internal object SocialGroupMetadataUpdateResolver {

    private val turkishLocale = Locale("tr", "TR")

    fun isGroupParticipant(
        threadType: String?,
        participantIds: List<String>,
        currentUserId: String
    ): Boolean {
        return threadType == "group" && currentUserId.isNotBlank() && participantIds.contains(currentUserId)
    }

    fun createTitleUpdatePayload(
        newTitle: String,
        updatedAt: Long
    ): Map<String, Any>? {
        val trimmedTitle = newTitle.trim()
        if (trimmedTitle.isBlank()) return null

        return mapOf(
            "title" to trimmedTitle,
            "titleLowercase" to trimmedTitle.lowercase(turkishLocale),
            "updatedAt" to updatedAt
        )
    }

    fun createDescriptionUpdatePayload(
        newDescription: String,
        updatedAt: Long
    ): Map<String, Any> {
        val trimmedDescription = newDescription.trim()

        return mapOf(
            "description" to trimmedDescription,
            "descriptionLowercase" to trimmedDescription.lowercase(turkishLocale),
            "updatedAt" to updatedAt
        )
    }

    fun createLegacyNameUpdatePayload(
        newName: String,
        updatedAt: Long
    ): Map<String, Any>? {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return null

        return mapOf(
            "title" to trimmedName,
            "titleLowercase" to trimmedName.lowercase(),
            "updatedAt" to updatedAt
        )
    }
}
