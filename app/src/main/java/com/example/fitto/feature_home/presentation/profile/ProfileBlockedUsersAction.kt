package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.SocialUser

class ProfileBlockedUsersAction(
    private val getBlockedUsers: suspend () -> List<SocialUser>,
    private val unblockUser: suspend (userId: String) -> Unit
) {
    suspend fun load(): List<SocialUser> = getBlockedUsers()

    suspend fun unblock(
        currentUsers: List<SocialUser>,
        userId: String
    ): ProfileBlockedUsersUnblockResult {
        unblockUser(userId)
        return ProfileBlockedUsersUnblockResult(
            userId = userId,
            remainingUsers = currentUsers.filterNot { user -> user.id == userId }
        )
    }
}

data class ProfileBlockedUsersUnblockResult(
    val userId: String,
    val remainingUsers: List<SocialUser>
)
