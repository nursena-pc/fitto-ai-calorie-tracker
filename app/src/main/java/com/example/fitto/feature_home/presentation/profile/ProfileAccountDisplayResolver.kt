package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.SocialUser

object ProfileAccountDisplayResolver {
    fun resolve(
        user: SocialUser?,
        authEmail: String
    ): SocialUser? {
        val normalizedAuthEmail = authEmail.trim().takeIf { it.isNotBlank() }

        return when {
            user != null && normalizedAuthEmail != null &&
                user.email != normalizedAuthEmail -> {
                user.copy(email = normalizedAuthEmail)
            }

            user == null && normalizedAuthEmail != null -> {
                SocialUser(email = normalizedAuthEmail)
            }

            else -> user
        }
    }
}
