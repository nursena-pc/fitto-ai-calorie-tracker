package com.example.fitto.feature_home.presentation.social

import java.util.Locale

internal data class SocialGroupThreadInput(
    val title: String,
    val description: String,
    val selectedUsers: List<SearchUserUiModel>
)

internal object SocialGroupThreadPayloadResolver {

    private val turkishLocale: Locale = Locale.forLanguageTag("tr-TR")

    fun resolveInput(
        title: String,
        description: String,
        selectedUsers: List<SearchUserUiModel>,
        currentUserId: String
    ): SocialGroupThreadInput? {
        val trimmedTitle = title.trim()
        val uniqueSelectedUsers = selectedUsers
            .filter { user -> user.id.isNotBlank() && user.id != currentUserId }
            .distinctBy { user -> user.id }

        if (trimmedTitle.isBlank() || uniqueSelectedUsers.size < 2) return null

        return SocialGroupThreadInput(
            title = trimmedTitle,
            description = description.trim(),
            selectedUsers = uniqueSelectedUsers
        )
    }

    fun createPayload(
        threadId: String,
        currentUserId: String,
        currentUserName: String,
        currentPhotoUrl: String?,
        input: SocialGroupThreadInput,
        now: Long
    ): Map<String, Any?> {
        val participantIds = (listOf(currentUserId) + input.selectedUsers.map { user -> user.id }).distinct()
        val participants = buildMap<String, Map<String, Any?>> {
            put(
                currentUserId,
                SocialThreadParticipantResolver.participantMap(
                    uid = currentUserId,
                    userName = currentUserName,
                    photoUrl = currentPhotoUrl
                )
            )

            input.selectedUsers.forEach { user ->
                put(
                    user.id,
                    SocialThreadParticipantResolver.participantMap(
                        uid = user.id,
                        userName = user.userName,
                        photoUrl = user.photoUrl
                    )
                )
            }
        }

        return mapOf(
            "id" to threadId,
            "type" to "group",
            "title" to input.title,
            "titleLowercase" to input.title.lowercase(turkishLocale),
            "creatorId" to currentUserId,
            "participantIds" to participantIds,
            "participants" to participants,
            "memberCount" to participantIds.size,
            "unreadCounts" to participantIds.associateWith { 0 },
            "lastMessageText" to "",
            "lastMessageSenderId" to null,
            "lastMessageCreatedAt" to null,
            "createdAt" to now,
            "deliveredAt" to now,
            "updatedAt" to now,
            "description" to input.description,
            "descriptionLowercase" to input.description.lowercase(turkishLocale)
        )
    }
}
