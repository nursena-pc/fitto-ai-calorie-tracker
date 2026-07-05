package com.example.fitto.feature_auth.register

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernameSetupControllerTest {

    @Test
    fun onInputChanged_normalizesTurkishCharactersAndSeparators() {
        val controller = UsernameSetupController()

        val update = controller.onInputChanged("  @Çağrı  Fit__ ")

        assertEquals("cagri.fit", update.state.input)
        assertTrue(update.shouldCheckAvailability)
        assertFalse(update.showSuggestions)
    }

    @Test
    fun onInputChanged_blankInput_showsSuggestions() {
        val controller = UsernameSetupController()

        val update = controller.onInputChanged("")

        assertTrue(update.showSuggestions)
        assertFalse(update.shouldCheckAvailability)
    }

    @Test
    fun applyAvailability_enablesContinueForCurrentInput() {
        val controller = UsernameSetupController()
        controller.onInputChanged("fitto.user")

        val state = controller.applyAvailability("fitto.user", available = true)

        assertTrue(state.canContinue)
    }

    @Test
    fun staleAvailabilityResult_isIgnored() {
        val controller = UsernameSetupController()
        controller.onInputChanged("first.user")
        controller.onInputChanged("second.user")

        val state = controller.applyAvailability("first.user", available = true)

        assertFalse(state.canContinue)
        assertEquals("second.user", state.input)
    }

    @Test
    fun prepareSubmission_requiresMatchingAvailabilityCheck() {
        val controller = UsernameSetupController()
        controller.onInputChanged("fitto.user")

        assertEquals(
            UsernameSubmissionDecision.NeedsAvailabilityCheck,
            controller.prepareSubmission()
        )
    }

    @Test
    fun prepareSubmission_marksControllerAsSubmitting() {
        val controller = UsernameSetupController()
        controller.onInputChanged("fitto.user")
        controller.applyAvailability("fitto.user", available = true)

        val decision = controller.prepareSubmission()

        assertEquals(UsernameSubmissionDecision.Ready("fitto.user"), decision)
        assertTrue(controller.state.isSubmitting)
    }

    @Test
    fun suggestions_areNormalizedDistinctAndLimited() {
        val controller = UsernameSetupController()

        val suggestions = controller.suggestions("Çağrı", listOf(12, 13, 14))

        assertEquals(listOf("cagri", "cagri.fit", "cagri_fit"), suggestions)
    }

    @Test
    fun displayName_prefersOnboardingNameAndUsesTurkishTitlecase() {
        val controller = UsernameSetupController()

        val result = controller.resolveDisplayName(
            onboardingName = "çağrı",
            authDisplayName = "Auth",
            email = "mail@example.com",
            appName = "Fitto"
        )

        assertEquals("Çağrı", result)
    }
}
