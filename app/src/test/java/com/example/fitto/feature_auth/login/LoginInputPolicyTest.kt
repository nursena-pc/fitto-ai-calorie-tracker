package com.example.fitto.feature_auth.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginInputPolicyTest {

    @Test
    fun validate_reportsRequiredFields() {
        val result = LoginInputPolicy.validate("", "") { true }

        assertEquals(LoginIdentifierIssue.REQUIRED, result.identifierIssue)
        assertEquals(LoginPasswordIssue.REQUIRED, result.passwordIssue)
    }

    @Test
    fun validate_reportsInvalidEmailAndShortPassword() {
        val result = LoginInputPolicy.validate("bad@email", "123") { false }

        assertEquals(LoginIdentifierIssue.INVALID_EMAIL, result.identifierIssue)
        assertEquals(LoginPasswordIssue.TOO_SHORT, result.passwordIssue)
    }

    @Test
    fun validate_acceptsUsernameAndPassword() {
        val result = LoginInputPolicy.validate("fitto_user", "123456") { false }

        assertTrue(result.isValid)
    }
}
