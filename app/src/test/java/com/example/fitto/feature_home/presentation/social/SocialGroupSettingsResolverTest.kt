package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialGroupSettingsResolverTest {

    @Test
    fun fromRawMap_returnsDefaultsWhenSettingsAreMissing() {
        assertEquals(
            GroupSettingsUiModel(),
            SocialGroupSettingsResolver.fromRawMap(null)
        )
    }

    @Test
    fun fromRawMap_readsBooleanValuesAndIgnoresInvalidTypes() {
        val settings = mapOf(
            "joinApprovalRequired" to true,
            "membersCanSendMessages" to false,
            "membersCanAddMembers" to "invalid",
            "membersCanEditGroupInfo" to true
        )

        assertEquals(
            GroupSettingsUiModel(
                joinApprovalRequired = true,
                membersCanSendMessages = false,
                membersCanAddMembers = true,
                membersCanEditGroupInfo = true
            ),
            SocialGroupSettingsResolver.fromRawMap(settings)
        )
    }

    @Test
    fun toRawMap_writesExpectedFirestorePayload() {
        val settings = GroupSettingsUiModel(
            joinApprovalRequired = true,
            membersCanSendMessages = false,
            membersCanAddMembers = false,
            membersCanEditGroupInfo = true
        )

        assertEquals(
            mapOf(
                "joinApprovalRequired" to true,
                "membersCanSendMessages" to false,
                "membersCanAddMembers" to false,
                "membersCanEditGroupInfo" to true
            ),
            SocialGroupSettingsResolver.toRawMap(settings)
        )
    }
}
