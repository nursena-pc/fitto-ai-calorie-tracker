package com.example.fitto.feature_home.presentation.social

import java.util.Locale

internal object SocialGroupMemberResolver {

    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    fun resolveMembers(
        participantIds: List<String>,
        participants: Map<*, *>?,
        currentUserId: String,
        creatorId: String
    ): List<GroupMemberUiModel> {
        return participantIds.map { uid ->
            val participant = participants?.get(uid) as? Map<*, *>

            GroupMemberUiModel(
                uid = uid,
                userName = (participant?.get("userName") as? String)
                    ?.ifBlank { null }
                    ?: "fitto_user",
                photoUrl = participant?.get("photoUrl") as? String,
                isMe = uid == currentUserId,
                isCreator = uid == creatorId
            )
        }.sortedWith(
            compareByDescending<GroupMemberUiModel> { it.isMe }
                .thenByDescending { it.isCreator }
                .thenBy { it.userName.lowercase(turkishLocale) }
        )
    }
}
