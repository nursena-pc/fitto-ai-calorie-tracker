package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SocialProfileMaintenanceResolverTest {

    @Test
    fun createCurrentUserProfilePayload_usesEmailFallbacks() {
        val payload = SocialProfileMaintenanceResolver.createCurrentUserProfilePayload(
            uid = "user-1",
            email = "Nur Sena@example.com",
            displayName = null,
            photoUrl = "photo",
            createdAt = 100L,
            updatedAt = 200L
        )

        assertEquals("user-1", payload["uid"])
        assertEquals("nur.sena", payload["displayName"])
        assertEquals("nur.sena", payload["userName"])
        assertEquals("nur.sena", payload["username"])
        assertEquals("Nur Sena", payload["realName"])
        assertEquals("photo", payload["photoUrl"])
        assertEquals(0, payload["followersCount"])
        assertEquals(false, payload["profileCompleted"])
    }

    @Test
    fun oldUsernameCandidates_normalizesAndDropsBlankValues() {
        val result = SocialProfileMaintenanceResolver.oldUsernameCandidates(
            userName = " Nur Sena ",
            username = "nur.sena",
            displayName = "   "
        )

        assertEquals(setOf("nur.sena"), result)
    }

    @Test
    fun createProfileUpdatePayload_trimsBioAndLowercasesUsername() {
        val payload = SocialProfileMaintenanceResolver.createProfileUpdatePayload(
            username = "nursena",
            bio = "  hello  ",
            updatedAt = 300L
        )

        assertEquals("nursena", payload["displayName"])
        assertEquals("nursena", payload["usernameLowercase"])
        assertEquals("hello", payload["bio"])
        assertEquals(300L, payload["updatedAt"])
    }

    @Test
    fun createPhotoDocumentUpdatePayload_deletesBlankPhoto() {
        val payload = SocialProfileMaintenanceResolver.createPhotoDocumentUpdatePayload(
            photoUrl = "   ",
            updatedAt = 400L
        )

        assertNotNull(payload["photoUrl"])
        assertEquals(400L, payload["updatedAt"])
    }

    @Test
    fun createRealtimeStatusPhotoUpdatePayload_usesNullForBlankPhoto() {
        val payload = SocialProfileMaintenanceResolver.createRealtimeStatusPhotoUpdatePayload(
            photoUrl = "   ",
            updatedAt = 500L
        )

        assertEquals(null, payload["photoUrl"])
        assertEquals(500L, payload["updatedAt"])
    }
}
