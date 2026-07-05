package com.example.fitto.feature_auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthHomeReadinessPolicyTest {

    @Test
    fun missingSession_keepsWelcomeScreen() {
        assertFalse(
            AuthHomeReadinessPolicy.shouldOpenHome(
                AuthSessionSnapshot(
                    hasUser = false,
                    email = null,
                    isEmailVerified = false
                )
            )
        )
    }

    @Test
    fun providerAccountWithoutEmail_opensHome() {
        assertTrue(
            AuthHomeReadinessPolicy.shouldOpenHome(
                AuthSessionSnapshot(
                    hasUser = true,
                    email = null,
                    isEmailVerified = false
                )
            )
        )
    }

    @Test
    fun verifiedEmailAccount_opensHome() {
        assertTrue(
            AuthHomeReadinessPolicy.shouldOpenHome(
                AuthSessionSnapshot(
                    hasUser = true,
                    email = "user@example.com",
                    isEmailVerified = true
                )
            )
        )
    }

    @Test
    fun unverifiedEmailAccount_keepsWelcomeScreen() {
        assertFalse(
            AuthHomeReadinessPolicy.shouldOpenHome(
                AuthSessionSnapshot(
                    hasUser = true,
                    email = "user@example.com",
                    isEmailVerified = false
                )
            )
        )
    }
}
