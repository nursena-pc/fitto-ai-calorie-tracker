package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri

internal interface SocialProfileCommandSource {
    suspend fun updateCurrentUserProfile(displayName: String, bio: String)

    suspend fun updateCurrentUserPhotoUrl(photoUrl: String?)

    suspend fun uploadCurrentUserProfilePhoto(context: Context, imageUri: Uri): String
}
