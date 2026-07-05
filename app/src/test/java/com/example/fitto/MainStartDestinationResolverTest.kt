package com.example.fitto

import org.junit.Assert.assertEquals
import org.junit.Test

class MainStartDestinationResolverTest {

    @Test
    fun `resolve opens welcome when there is no authenticated user`() {
        assertEquals(
            MainStartDestination.WELCOME,
            MainStartDestinationResolver.resolve(
                hasAuthenticatedUser = false,
                email = null,
                isEmailVerified = false
            )
        )
    }

    @Test
    fun `resolve opens home for providers without an email`() {
        assertEquals(
            MainStartDestination.HOME,
            MainStartDestinationResolver.resolve(
                hasAuthenticatedUser = true,
                email = null,
                isEmailVerified = false
            )
        )
    }

    @Test
    fun `resolve opens home for verified email account`() {
        assertEquals(
            MainStartDestination.HOME,
            MainStartDestinationResolver.resolve(
                hasAuthenticatedUser = true,
                email = "verified@example.com",
                isEmailVerified = true
            )
        )
    }

    @Test
    fun `resolve keeps unverified email account on welcome`() {
        assertEquals(
            MainStartDestination.WELCOME,
            MainStartDestinationResolver.resolve(
                hasAuthenticatedUser = true,
                email = "pending@example.com",
                isEmailVerified = false
            )
        )
    }
}
