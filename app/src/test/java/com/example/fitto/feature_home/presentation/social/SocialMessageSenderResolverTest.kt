package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialMessageSenderResolverTest {

    @Test
    fun resolve_prefersProfileUserNameAndPhoto() {
        val sender = SocialMessageSenderResolver.resolve(
            profileUserName = "nursena",
            profilePhotoUrl = "profile-photo",
            currentUserEmail = "mail@example.com",
            currentUserPhotoUrl = "auth-photo"
        )

        assertEquals("nursena", sender.name)
        assertEquals("profile-photo", sender.photoUrl)
    }

    @Test
    fun resolve_fallsBackToEmailPrefixWhenProfileUserNameIsBlank() {
        val sender = SocialMessageSenderResolver.resolve(
            profileUserName = "   ",
            profilePhotoUrl = null,
            currentUserEmail = "nursena@example.com",
            currentUserPhotoUrl = "auth-photo"
        )

        assertEquals("nursena", sender.name)
        assertEquals("auth-photo", sender.photoUrl)
    }

    @Test
    fun resolve_fallsBackToDefaultNameWhenProfileAndEmailAreMissing() {
        val sender = SocialMessageSenderResolver.resolve(
            profileUserName = null,
            profilePhotoUrl = null,
            currentUserEmail = null,
            currentUserPhotoUrl = null
        )

        assertEquals("fitto_user", sender.name)
        assertNull(sender.photoUrl)
    }
}
