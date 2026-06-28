package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialUserDocumentResolverTest {

    @Test
    fun resolveProfile_prefersExplicitUserNameAndDisplayName() {
        val user = SocialUserDocumentResolver.resolveProfile(
            id = "user-1",
            userName = "nursena",
            username = "fallback_username",
            displayName = "Nursena",
            name = "Name",
            realName = "Real Name",
            email = "nursena@example.com",
            photoUrl = "photo-url",
            bio = "bio",
            followersCount = 12,
            followingCount = 5,
            isFollowedByMe = true
        )

        assertEquals("user-1", user.id)
        assertEquals("nursena", user.userName)
        assertEquals("Nursena", user.displayName)
        assertEquals("Real Name", user.personalName)
        assertEquals("nursena@example.com", user.email)
        assertEquals("photo-url", user.photoUrl)
        assertEquals("bio", user.bio)
        assertEquals(12, user.followersCount)
        assertEquals(5, user.followingCount)
        assertTrue(user.isFollowedByMe)
    }

    @Test
    fun resolveProfile_fallsBackToEmailPrefixForUserName() {
        val user = SocialUserDocumentResolver.resolveProfile(
            id = "user-1",
            userName = null,
            username = "",
            displayName = null,
            name = null,
            realName = null,
            email = "fitto@example.com",
            photoUrl = null,
            bio = null,
            followersCount = 0,
            followingCount = 0
        )

        assertEquals("fitto", user.userName)
    }

    @Test
    fun resolveProfile_preservesLegacyBlankDisplayName() {
        val user = SocialUserDocumentResolver.resolveProfile(
            id = "user-1",
            userName = "fitto_user",
            username = null,
            displayName = "",
            name = "Name",
            realName = "Real Name",
            email = null,
            photoUrl = null,
            bio = null,
            followersCount = 0,
            followingCount = 0
        )

        assertEquals("", user.displayName)
    }

    @Test
    fun resolveBlockedUser_usesDisplayNameBeforeFallbackForUserName() {
        val user = SocialUserDocumentResolver.resolveBlockedUser(
            id = "user-1",
            userName = null,
            username = null,
            displayName = "Blocked User",
            name = null,
            realName = null,
            email = "blocked@example.com",
            photoUrl = null,
            bio = null,
            followersCount = 0,
            followingCount = 0,
            fallbackName = "Silinen Kullanıcı"
        )

        assertEquals("Blocked User", user.userName)
        assertEquals("Blocked User", user.displayName)
    }

    @Test
    fun resolveBlockedUser_usesFallbackNameWhenIdentityFieldsAreNull() {
        val user = SocialUserDocumentResolver.resolveBlockedUser(
            id = "user-1",
            userName = null,
            username = null,
            displayName = null,
            name = null,
            realName = null,
            email = null,
            photoUrl = null,
            bio = null,
            followersCount = 0,
            followingCount = 0,
            fallbackName = "Silinen Kullanıcı"
        )

        assertEquals("Silinen Kullanıcı", user.userName)
        assertEquals("Silinen Kullanıcı", user.displayName)
    }
}
