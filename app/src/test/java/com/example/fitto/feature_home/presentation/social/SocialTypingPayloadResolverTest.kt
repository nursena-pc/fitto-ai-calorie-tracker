package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialTypingPayloadResolverTest {

    @Test
    fun createPayload_prefersPublicProfileNameAndPhoto() {
        val payload = SocialTypingPayloadResolver.createPayload(
            uid = "user-1",
            profileUserName = "nursena",
            profileUsername = "fallback-name",
            profilePhotoUrl = "profile-photo",
            email = "mail@example.com",
            authPhotoUrl = "auth-photo",
            updatedAt = 100L
        )

        assertEquals("user-1", payload["uid"])
        assertEquals("nursena", payload["userName"])
        assertEquals("profile-photo", payload["photoUrl"])
        assertEquals(true, payload["isTyping"])
        assertEquals(100L, payload["updatedAt"])
    }

    @Test
    fun createPayload_usesUsernameWhenPublicUserNameIsBlank() {
        val payload = SocialTypingPayloadResolver.createPayload(
            uid = "user-1",
            profileUserName = "   ",
            profileUsername = "username",
            profilePhotoUrl = "   ",
            email = "mail@example.com",
            authPhotoUrl = "auth-photo",
            updatedAt = 200L
        )

        assertEquals("username", payload["userName"])
        assertEquals("auth-photo", payload["photoUrl"])
    }

    @Test
    fun createPayload_fallsBackToEmailPrefix() {
        val payload = SocialTypingPayloadResolver.createPayload(
            uid = "user-1",
            profileUserName = null,
            profileUsername = null,
            profilePhotoUrl = null,
            email = "nursena@example.com",
            authPhotoUrl = null,
            updatedAt = 300L
        )

        assertEquals("nursena", payload["userName"])
        assertNull(payload["photoUrl"])
    }

    @Test
    fun createPayload_usesDefaultNameWhenNamesAreMissing() {
        val payload = SocialTypingPayloadResolver.createPayload(
            uid = "user-1",
            profileUserName = null,
            profileUsername = null,
            profilePhotoUrl = null,
            email = null,
            authPhotoUrl = null,
            updatedAt = 400L
        )

        assertEquals("fitto_user", payload["userName"])
        assertNull(payload["photoUrl"])
    }
}
