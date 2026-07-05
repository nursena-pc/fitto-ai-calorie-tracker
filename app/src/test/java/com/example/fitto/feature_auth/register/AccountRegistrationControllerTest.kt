package com.example.fitto.feature_auth.register

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRegistrationControllerTest {

    @Test
    fun prepareSubmission_normalizesEmailAndStartsSubmission() {
        val controller = AccountRegistrationController()

        val result = controller.prepareSubmission(
            email = " User@Example.COM ",
            password = "secret!",
            passwordRepeat = "secret!",
            isValidEmail = { true }
        )

        assertEquals(
            AccountRegistrationSubmission.Ready("user@example.com", "secret!"),
            result
        )
        assertTrue(controller.isSubmitting)
    }

    @Test
    fun prepareSubmission_reportsAllValidationIssues() {
        val controller = AccountRegistrationController()

        val result = controller.prepareSubmission(
            email = "bad",
            password = "123456",
            passwordRepeat = "654321",
            isValidEmail = { false }
        ) as AccountRegistrationSubmission.Invalid

        assertEquals(RegistrationEmailIssue.INVALID, result.validation.emailIssue)
        assertEquals(
            RegistrationPasswordIssue.SPECIAL_CHARACTER_REQUIRED,
            result.validation.passwordIssue
        )
        assertEquals(
            RegistrationPasswordRepeatIssue.DOES_NOT_MATCH,
            result.validation.passwordRepeatIssue
        )
        assertFalse(controller.isSubmitting)
    }

    @Test
    fun prepareSubmission_whileSubmitting_isRejected() {
        val controller = AccountRegistrationController()
        controller.prepareSubmission("a@b.com", "secret!", "secret!") { true }

        val second = controller.prepareSubmission("a@b.com", "secret!", "secret!") { true }

        assertEquals(AccountRegistrationSubmission.AlreadySubmitting, second)
    }

    @Test
    fun completeSubmission_allowsAnotherAttempt() {
        val controller = AccountRegistrationController()
        controller.prepareSubmission("a@b.com", "secret!", "secret!") { true }

        controller.completeSubmission()

        assertFalse(controller.isSubmitting)
    }
}
