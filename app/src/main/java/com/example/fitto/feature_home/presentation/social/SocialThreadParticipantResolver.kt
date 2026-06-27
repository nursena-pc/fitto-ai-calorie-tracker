package com.example.fitto.feature_home.presentation.social

internal object SocialThreadParticipantResolver {

    fun directThreadId(
        firstUserId: String,
        secondUserId: String
    ): String {
        val sortedIds = listOf(firstUserId, secondUserId).sorted()
        return "direct_${sortedIds[0]}_${sortedIds[1]}"
    }

    fun participantMap(
        uid: String,
        userName: String,
        photoUrl: String?
    ): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "userName" to userName.ifBlank { "fitto_user" },
            "photoUrl" to photoUrl
        )
    }
}
