package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AccountSettingsInputPolicyTest {

    @Test
    fun `display name is trimmed and repeated whitespace is collapsed`() {
        val normalized = AccountSettingsInputPolicy.normalizeDisplayName("  Ada   Lovelace  ")

        assertEquals("Ada Lovelace", normalized)
    }

    @Test
    fun `display name limits preserve existing account errors`() {
        assertAccountError(AccountSettingsError.DISPLAY_NAME_TOO_SHORT) {
            AccountSettingsInputPolicy.normalizeDisplayName("A")
        }
        assertAccountError(AccountSettingsError.DISPLAY_NAME_TOO_LONG) {
            AccountSettingsInputPolicy.normalizeDisplayName("a".repeat(41))
        }
    }

    @Test
    fun `username normalization preserves Turkish transliteration and separators`() {
        val normalized = AccountSettingsInputPolicy.normalizeUsername(" @Çağrı  ÜŞENMEZ__Test ")

        assertEquals("cagri.usenmez_test", normalized)
    }

    @Test
    fun `username validation preserves length and edge separator errors`() {
        assertAccountError(AccountSettingsError.USERNAME_TOO_SHORT) {
            AccountSettingsInputPolicy.normalizeUsername("ab")
        }
        assertAccountError(AccountSettingsError.USERNAME_TOO_LONG) {
            AccountSettingsInputPolicy.normalizeUsername("a".repeat(21))
        }
        assertAccountError(AccountSettingsError.USERNAME_INVALID) {
            AccountSettingsInputPolicy.normalizeUsername("_username")
        }
        assertAccountError(AccountSettingsError.USERNAME_INVALID) {
            AccountSettingsInputPolicy.normalizeUsername("username.")
        }
    }

    @Test
    fun `stored username normalization does not reject legacy blank or edge values`() {
        assertEquals("", AccountSettingsInputPolicy.normalizeStoredUsername(" @@@ "))
        assertEquals("legacy.name", AccountSettingsInputPolicy.normalizeStoredUsername(" Legacy  Name "))
    }

    @Test
    fun `email is trimmed and invalid email keeps existing error`() {
        assertEquals(
            "ada@example.com",
            AccountSettingsInputPolicy.normalizeEmail("  ada@example.com  ")
        )
        assertAccountError(AccountSettingsError.EMAIL_INVALID) {
            AccountSettingsInputPolicy.normalizeEmail("not-an-email")
        }
    }

    @Test
    fun `password validation preserves empty short and mismatch errors`() {
        assertEquals(
            "current-password",
            AccountSettingsInputPolicy.requireCurrentPassword("current-password")
        )
        assertAccountError(AccountSettingsError.CURRENT_PASSWORD_EMPTY) {
            AccountSettingsInputPolicy.requireCurrentPassword("   ")
        }
        assertAccountError(AccountSettingsError.NEW_PASSWORD_TOO_SHORT) {
            AccountSettingsInputPolicy.validateNewPassword("12345", "12345")
        }
        assertAccountError(AccountSettingsError.PASSWORD_CONFIRM_MISMATCH) {
            AccountSettingsInputPolicy.validateNewPassword("123456", "654321")
        }
    }

    private fun assertAccountError(
        expected: AccountSettingsError,
        operation: () -> Unit
    ) {
        val exception = runCatching(operation).exceptionOrNull() as AccountSettingsException
        assertSame(expected, exception.error)
    }
}
