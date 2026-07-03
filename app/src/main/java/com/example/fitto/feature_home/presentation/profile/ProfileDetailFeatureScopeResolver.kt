package com.example.fitto.feature_home.presentation.profile

internal enum class ProfileDetailResumeEffect {
    NONE,
    REFRESH_ACCOUNT_EMAIL,
    REFRESH_ROWS
}

internal data class ProfileDetailFeatureScope(
    val usesAccountEditing: Boolean = false,
    val usesAccountLifecycle: Boolean = false,
    val usesNotificationSettings: Boolean = false,
    val usesPhotoAlbum: Boolean = false,
    val loadsBlockedUsers: Boolean = false,
    val resumeEffect: ProfileDetailResumeEffect = ProfileDetailResumeEffect.NONE
)

internal object ProfileDetailFeatureScopeResolver {
    fun resolve(menuId: ProfileMenuId): ProfileDetailFeatureScope {
        return when (menuId) {
            ProfileMenuId.ACCOUNT_MANAGEMENT -> ProfileDetailFeatureScope(
                usesAccountEditing = true,
                resumeEffect = ProfileDetailResumeEffect.REFRESH_ACCOUNT_EMAIL
            )

            ProfileMenuId.SETTINGS -> ProfileDetailFeatureScope(
                usesAccountLifecycle = true
            )

            ProfileMenuId.NOTIFICATION_PREFERENCES -> ProfileDetailFeatureScope(
                usesNotificationSettings = true,
                resumeEffect = ProfileDetailResumeEffect.REFRESH_ROWS
            )

            ProfileMenuId.NUTRITION_PREFERENCES -> ProfileDetailFeatureScope(
                resumeEffect = ProfileDetailResumeEffect.REFRESH_ROWS
            )

            ProfileMenuId.PHOTO_ALBUM -> ProfileDetailFeatureScope(
                usesPhotoAlbum = true
            )

            ProfileMenuId.BLOCKED_USERS -> ProfileDetailFeatureScope(
                loadsBlockedUsers = true
            )

            else -> ProfileDetailFeatureScope()
        }
    }
}
