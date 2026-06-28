package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialGroupMetadataUpdateResolverTest {

    @Test
    fun isGroupParticipant_returnsTrueForGroupParticipant() {
        assertTrue(
            SocialGroupMetadataUpdateResolver.isGroupParticipant(
                threadType = "group",
                participantIds = listOf("user-1", "user-2"),
                currentUserId = "user-1"
            )
        )
    }

    @Test
    fun isGroupParticipant_returnsFalseForDirectThreadOrMissingUser() {
        assertFalse(
            SocialGroupMetadataUpdateResolver.isGroupParticipant(
                threadType = "direct",
                participantIds = listOf("user-1"),
                currentUserId = "user-1"
            )
        )

        assertFalse(
            SocialGroupMetadataUpdateResolver.isGroupParticipant(
                threadType = "group",
                participantIds = listOf("user-1"),
                currentUserId = "user-2"
            )
        )

        assertFalse(
            SocialGroupMetadataUpdateResolver.isGroupParticipant(
                threadType = "group",
                participantIds = listOf("user-1"),
                currentUserId = ""
            )
        )
    }

    @Test
    fun createTitleUpdatePayload_trimsAndUsesTurkishLowercase() {
        assertEquals(
            mapOf(
                "title" to "İftar Grubu",
                "titleLowercase" to "iftar grubu",
                "updatedAt" to 123L
            ),
            SocialGroupMetadataUpdateResolver.createTitleUpdatePayload(
                newTitle = "  İftar Grubu  ",
                updatedAt = 123L
            )
        )
    }

    @Test
    fun createTitleUpdatePayload_returnsNullForBlankTitle() {
        assertNull(
            SocialGroupMetadataUpdateResolver.createTitleUpdatePayload(
                newTitle = "   ",
                updatedAt = 123L
            )
        )
    }

    @Test
    fun createDescriptionUpdatePayload_allowsBlankDescription() {
        assertEquals(
            mapOf(
                "description" to "",
                "descriptionLowercase" to "",
                "updatedAt" to 456L
            ),
            SocialGroupMetadataUpdateResolver.createDescriptionUpdatePayload(
                newDescription = "   ",
                updatedAt = 456L
            )
        )
    }

    @Test
    fun createLegacyNameUpdatePayload_preservesDefaultLowercaseBehavior() {
        assertEquals(
            mapOf(
                "title" to "Group Name",
                "titleLowercase" to "group name",
                "updatedAt" to 789L
            ),
            SocialGroupMetadataUpdateResolver.createLegacyNameUpdatePayload(
                newName = "  Group Name  ",
                updatedAt = 789L
            )
        )
    }
}
