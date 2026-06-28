package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialUserLookupResolverTest {

    @Test
    fun normalizeSearchQuery_reusesSocialUsernameNormalization() {
        assertEquals(
            "nur.sena_01",
            SocialUserLookupResolver.normalizeSearchQuery(" @Nür Şena__01!! ")
        )
    }

    @Test
    fun shouldSearch_requiresAtLeastTwoCharacters() {
        assertFalse(SocialUserLookupResolver.shouldSearch("n"))
        assertTrue(SocialUserLookupResolver.shouldSearch("nu"))
    }

    @Test
    fun uniqueNonBlankIds_removesBlanksAndDuplicatesPreservingOrder() {
        assertEquals(
            listOf("user-1", "user-2", "user-3"),
            SocialUserLookupResolver.uniqueNonBlankIds(
                listOf("", "user-1", "user-2", "user-1", "   ", "user-3")
            )
        )
    }

    @Test
    fun orderUsersByRequestedIds_preservesRequestedOrderAndDuplicates() {
        val user1 = SocialUser(id = "user-1", userName = "nursena")
        val user2 = SocialUser(id = "user-2", userName = "ayse")

        assertEquals(
            listOf(user2, user1, user2),
            SocialUserLookupResolver.orderUsersByRequestedIds(
                requestedIds = listOf("missing", "user-2", "user-1", "user-2"),
                usersById = mapOf("user-1" to user1, "user-2" to user2)
            )
        )
    }

    @Test
    fun resolveSearchUser_skipsCurrentDeletedAndUnnamedUsers() {
        assertNull(
            SocialUserLookupResolver.resolveSearchUser(
                id = "me",
                currentUserId = "me",
                isDeleted = false,
                userName = "me",
                usernameLowercase = null,
                email = null,
                bio = null,
                photoUrl = null
            )
        )

        assertNull(
            SocialUserLookupResolver.resolveSearchUser(
                id = "deleted",
                currentUserId = "me",
                isDeleted = true,
                userName = "deleted",
                usernameLowercase = null,
                email = null,
                bio = null,
                photoUrl = null
            )
        )

        assertNull(
            SocialUserLookupResolver.resolveSearchUser(
                id = "unnamed",
                currentUserId = "me",
                isDeleted = false,
                userName = " ",
                usernameLowercase = "",
                email = null,
                bio = null,
                photoUrl = null
            )
        )
    }

    @Test
    fun resolveSearchUser_usesNameFallbacks() {
        val usernameUser = SocialUserLookupResolver.resolveSearchUser(
            id = "user-1",
            currentUserId = "me",
            isDeleted = false,
            userName = null,
            usernameLowercase = "nursena",
            email = "mail@example.com",
            bio = "bio",
            photoUrl = "photo"
        )

        assertEquals("user-1", usernameUser?.id)
        assertEquals("nursena", usernameUser?.userName)
        assertEquals("bio", usernameUser?.bio)
        assertEquals("photo", usernameUser?.photoUrl)

        val emailUser = SocialUserLookupResolver.resolveSearchUser(
            id = "user-2",
            currentUserId = "me",
            isDeleted = false,
            userName = " ",
            usernameLowercase = null,
            email = "ayse@example.com",
            bio = null,
            photoUrl = null
        )

        assertEquals("ayse", emailUser?.userName)
    }
}
