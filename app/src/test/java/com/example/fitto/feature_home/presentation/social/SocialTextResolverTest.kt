package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SocialTextResolverTest {

    @Test
    fun createInitials_usesFirstTwoNameParts() {
        assertEquals("AY", SocialTextResolver.createInitials("Ayşe Yılmaz"))
    }

    @Test
    fun createInitials_preservesLegacyWhitespaceFallback() {
        assertEquals(" ", SocialTextResolver.createInitials("   "))
    }

    @Test
    fun formatRelativeTime_returnsEmptyForMissingTime() {
        assertEquals("", SocialTextResolver.formatRelativeTime(0L, nowMillis = 60_000L))
    }

    @Test
    fun formatRelativeTime_formatsRecentMinutes() {
        assertEquals("5 dk", SocialTextResolver.formatRelativeTime(5 * 60_000L, nowMillis = 10 * 60_000L))
    }

    @Test
    fun formatRelativeTime_formatsYesterday() {
        val now = 2L * 24L * 60L * 60L * 1000L
        val yesterday = now - 1L * 24L * 60L * 60L * 1000L

        assertEquals("Dün", SocialTextResolver.formatRelativeTime(yesterday, nowMillis = now))
    }

    @Test
    fun formatClockTime_returnsNowForMissingTime() {
        assertEquals("şimdi", SocialTextResolver.formatClockTime(0L))
    }

    @Test
    fun normalizeUsername_removesAtSignAndTurkishCharacters() {
        assertEquals("nur.sena_01", SocialTextResolver.normalizeUsername(" @Nür Şena__01!! "))
    }

    @Test
    fun validateVisibleName_acceptsValidValue() {
        SocialTextResolver.validateVisibleName("nur.sena_01")
    }

    @Test
    fun validateVisibleName_rejectsTooShortValue() {
        assertValidationError("VISIBLE_NAME_LENGTH") {
            SocialTextResolver.validateVisibleName("ab")
        }
    }

    @Test
    fun validateVisibleName_rejectsInvalidCharacters() {
        assertValidationError("VISIBLE_NAME_INVALID") {
            SocialTextResolver.validateVisibleName("nur-sena")
        }
    }

    @Test
    fun validateVisibleName_rejectsEdgeSeparators() {
        assertValidationError("VISIBLE_NAME_INVALID") {
            SocialTextResolver.validateVisibleName("nur.")
        }
    }

    private fun assertValidationError(
        expectedMessage: String,
        block: () -> Unit
    ) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            assertEquals(expectedMessage, error.message)
        }
    }
}
