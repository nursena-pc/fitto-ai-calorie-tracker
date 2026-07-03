package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDetailFeatureScopeResolverTest {

    @Test
    fun `account management enables account editing and email refresh`() {
        val scope = ProfileDetailFeatureScopeResolver.resolve(
            ProfileMenuId.ACCOUNT_MANAGEMENT
        )

        assertTrue(scope.usesAccountEditing)
        assertFalse(scope.usesAccountLifecycle)
        assertEquals(
            ProfileDetailResumeEffect.REFRESH_ACCOUNT_EMAIL,
            scope.resumeEffect
        )
    }

    @Test
    fun `settings enables account lifecycle only`() {
        val scope = ProfileDetailFeatureScopeResolver.resolve(ProfileMenuId.SETTINGS)

        assertTrue(scope.usesAccountLifecycle)
        assertFalse(scope.usesAccountEditing)
        assertFalse(scope.usesNotificationSettings)
        assertEquals(ProfileDetailResumeEffect.NONE, scope.resumeEffect)
    }

    @Test
    fun `notification preferences enables notification coordinator and row refresh`() {
        val scope = ProfileDetailFeatureScopeResolver.resolve(
            ProfileMenuId.NOTIFICATION_PREFERENCES
        )

        assertTrue(scope.usesNotificationSettings)
        assertEquals(ProfileDetailResumeEffect.REFRESH_ROWS, scope.resumeEffect)
    }

    @Test
    fun `nutrition preferences refreshes rows without notification coordinator`() {
        val scope = ProfileDetailFeatureScopeResolver.resolve(
            ProfileMenuId.NUTRITION_PREFERENCES
        )

        assertFalse(scope.usesNotificationSettings)
        assertEquals(ProfileDetailResumeEffect.REFRESH_ROWS, scope.resumeEffect)
    }

    @Test
    fun `photo album and blocked users expose their dedicated startup effects`() {
        val photoScope = ProfileDetailFeatureScopeResolver.resolve(ProfileMenuId.PHOTO_ALBUM)
        val blockedScope = ProfileDetailFeatureScopeResolver.resolve(ProfileMenuId.BLOCKED_USERS)

        assertTrue(photoScope.usesPhotoAlbum)
        assertFalse(photoScope.loadsBlockedUsers)
        assertTrue(blockedScope.loadsBlockedUsers)
        assertFalse(blockedScope.usesPhotoAlbum)
    }

    @Test
    fun `unrelated profile screens use the empty feature scope`() {
        assertEquals(
            ProfileDetailFeatureScope(),
            ProfileDetailFeatureScopeResolver.resolve(ProfileMenuId.WEIGHT)
        )
    }
}
