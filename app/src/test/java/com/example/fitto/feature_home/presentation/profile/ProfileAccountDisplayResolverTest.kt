package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.SocialUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ProfileAccountDisplayResolverTest {

    @Test
    fun `resolve replaces stale profile email with auth email`() {
        val user = SocialUser(id = "user", email = "old@example.com", userName = "fitto")

        val result = ProfileAccountDisplayResolver.resolve(user, " new@example.com ")

        assertEquals("new@example.com", result?.email)
        assertEquals("fitto", result?.userName)
    }

    @Test
    fun `resolve keeps user instance when email already matches`() {
        val user = SocialUser(id = "user", email = "user@example.com")

        val result = ProfileAccountDisplayResolver.resolve(user, "user@example.com")

        assertSame(user, result)
    }

    @Test
    fun `resolve creates display user when only auth email exists`() {
        val result = ProfileAccountDisplayResolver.resolve(null, "user@example.com")

        assertEquals(SocialUser(email = "user@example.com"), result)
    }

    @Test
    fun `resolve returns null when both profile and auth email are absent`() {
        assertNull(ProfileAccountDisplayResolver.resolve(null, "   "))
    }
}
