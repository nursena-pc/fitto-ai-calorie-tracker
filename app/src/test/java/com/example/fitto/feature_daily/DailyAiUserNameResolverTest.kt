package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiUserNameResolverTest {

    @Test
    fun resolve_prefersDisplayName() {
        assertEquals(
            "Nursena",
            DailyAiUserNameResolver.resolve(
                displayName = " Nursena ",
                userName = "fitto_user",
                fallbackName = "Fitto"
            )
        )
    }

    @Test
    fun resolve_usesUserNameWhenDisplayNameIsBlank() {
        assertEquals(
            "fitto_user",
            DailyAiUserNameResolver.resolve(
                displayName = " ",
                userName = " fitto_user ",
                fallbackName = "Fitto"
            )
        )
    }

    @Test
    fun resolve_usesFallbackWhenProfileNamesAreBlank() {
        assertEquals(
            "Fitto",
            DailyAiUserNameResolver.resolve(
                displayName = null,
                userName = "",
                fallbackName = " Fitto "
            )
        )
    }

    @Test
    fun resolve_returnsEmptyWhenAllCandidatesAreBlank() {
        assertEquals(
            "",
            DailyAiUserNameResolver.resolve(
                displayName = null,
                userName = " ",
                fallbackName = ""
            )
        )
    }
}
