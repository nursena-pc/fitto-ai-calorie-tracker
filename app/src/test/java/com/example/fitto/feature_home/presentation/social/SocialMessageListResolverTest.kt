package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMessageListResolverTest {

    @Test
    fun shouldSkipMessageListRequiresCurrentUserAndThread() {
        assertTrue(SocialMessageListResolver.shouldSkipMessageList(null, "thread-1"))
        assertTrue(SocialMessageListResolver.shouldSkipMessageList("", "thread-1"))
        assertTrue(SocialMessageListResolver.shouldSkipMessageList("user-1", ""))
        assertTrue(SocialMessageListResolver.shouldSkipMessageList("user-1", "   "))

        assertFalse(SocialMessageListResolver.shouldSkipMessageList("user-1", "thread-1"))
    }

    @Test
    fun participantNamesUseStoredUsernameAndFallbackForBlankValues() {
        val names = SocialMessageListResolver.participantNames(
            participantIds = listOf("user-1", "user-2", "user-3"),
            participants = mapOf(
                "user-1" to mapOf("userName" to "nursena"),
                "user-2" to mapOf("userName" to ""),
                "user-3" to emptyMap<String, Any>()
            )
        )

        assertEquals("nursena", names["user-1"])
        assertEquals("fitto_user", names["user-2"])
        assertEquals("fitto_user", names["user-3"])
    }

    @Test
    fun participantPhotoUrlsPreferLatestUserProfileThenStoredPhoto() {
        val photoUrls = SocialMessageListResolver.participantPhotoUrls(
            participantIds = listOf("user-1", "user-2", "user-3"),
            participants = mapOf(
                "user-1" to mapOf("photoUrl" to "old-1"),
                "user-2" to mapOf("photoUrl" to "stored-2")
            ),
            latestParticipants = mapOf(
                "user-1" to SocialUser(id = "user-1", photoUrl = "latest-1"),
                "user-3" to SocialUser(id = "user-3", photoUrl = null)
            )
        )

        assertEquals("latest-1", photoUrls["user-1"])
        assertEquals("stored-2", photoUrls["user-2"])
        assertNull(photoUrls["user-3"])
    }

    @Test
    fun totalRecipientCountExcludesSenderButKeepsOneAsMinimum() {
        assertEquals(1, SocialMessageListResolver.totalRecipientCount(emptyList()))
        assertEquals(1, SocialMessageListResolver.totalRecipientCount(listOf("sender")))
        assertEquals(2, SocialMessageListResolver.totalRecipientCount(listOf("sender", "u2", "u3")))
    }

    @Test
    fun currentUserClearedAtReadsCurrentUserValueFromMap() {
        assertEquals(
            25L,
            SocialMessageListResolver.currentUserClearedAt(
                clearedAt = mapOf("user-1" to 25),
                currentUserId = "user-1"
            )
        )
        assertEquals(
            0L,
            SocialMessageListResolver.currentUserClearedAt(
                clearedAt = mapOf("user-2" to 25),
                currentUserId = "user-1"
            )
        )
        assertEquals(
            0L,
            SocialMessageListResolver.currentUserClearedAt(
                clearedAt = null,
                currentUserId = "user-1"
            )
        )
    }
}
