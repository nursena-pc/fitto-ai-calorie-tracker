package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialTypingUserResolverTest {

    @Test
    fun resolve_returnsEmptyWhenCurrentUserIsBlank() {
        val records = listOf(
            SocialTypingUserResolver.TypingRecord(
                key = "user-1",
                uid = null,
                isTyping = true,
                updatedAt = 100L,
                userName = "Ayşe",
                photoUrl = "photo"
            )
        )

        assertEquals(
            emptyList<TypingUserUiModel>(),
            SocialTypingUserResolver.resolve(
                records = records,
                currentUserId = " ",
                nowMillis = 200L,
                staleMillis = 8_000L
            )
        )
    }

    @Test
    fun resolve_filtersCurrentUserNonTypingAndStaleRecords() {
        val records = listOf(
            SocialTypingUserResolver.TypingRecord(
                key = "current-user",
                uid = null,
                isTyping = true,
                updatedAt = 1_000L,
                userName = "Current",
                photoUrl = null
            ),
            SocialTypingUserResolver.TypingRecord(
                key = "user-1",
                uid = null,
                isTyping = false,
                updatedAt = 1_000L,
                userName = "Not typing",
                photoUrl = null
            ),
            SocialTypingUserResolver.TypingRecord(
                key = "user-2",
                uid = null,
                isTyping = true,
                updatedAt = 1_000L,
                userName = "Stale",
                photoUrl = null
            )
        )

        assertEquals(
            emptyList<TypingUserUiModel>(),
            SocialTypingUserResolver.resolve(
                records = records,
                currentUserId = "current-user",
                nowMillis = 10_000L,
                staleMillis = 8_000L
            )
        )
    }

    @Test
    fun resolve_keepsFreshTypingUsersAndPrefersExplicitUid() {
        val records = listOf(
            SocialTypingUserResolver.TypingRecord(
                key = "key-user",
                uid = "explicit-user",
                isTyping = true,
                updatedAt = 9_000L,
                userName = "Nursena",
                photoUrl = "photo-url"
            ),
            SocialTypingUserResolver.TypingRecord(
                key = "fallback-user",
                uid = null,
                isTyping = true,
                updatedAt = 0L,
                userName = null,
                photoUrl = null
            )
        )

        assertEquals(
            listOf(
                TypingUserUiModel(
                    uid = "explicit-user",
                    userName = "Nursena",
                    photoUrl = "photo-url",
                    updatedAt = 9_000L
                ),
                TypingUserUiModel(
                    uid = "fallback-user",
                    userName = "fitto_user",
                    photoUrl = null,
                    updatedAt = 0L
                )
            ),
            SocialTypingUserResolver.resolve(
                records = records,
                currentUserId = "current-user",
                nowMillis = 10_000L,
                staleMillis = 8_000L
            )
        )
    }
}
