package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialThreadParticipantResolverTest {

    @Test
    fun directThreadId_sortsUserIdsForStableId() {
        assertEquals(
            "direct_user-a_user-b",
            SocialThreadParticipantResolver.directThreadId(
                firstUserId = "user-b",
                secondUserId = "user-a"
            )
        )
    }

    @Test
    fun participantMap_preservesUserData() {
        assertEquals(
            mapOf(
                "uid" to "user-1",
                "userName" to "Nursena",
                "photoUrl" to "https://example.com/photo.jpg"
            ),
            SocialThreadParticipantResolver.participantMap(
                uid = "user-1",
                userName = "Nursena",
                photoUrl = "https://example.com/photo.jpg"
            )
        )
    }

    @Test
    fun participantMap_usesFallbackNameWhenBlank() {
        assertEquals(
            mapOf(
                "uid" to "user-1",
                "userName" to "fitto_user",
                "photoUrl" to null
            ),
            SocialThreadParticipantResolver.participantMap(
                uid = "user-1",
                userName = "   ",
                photoUrl = null
            )
        )
    }
}
