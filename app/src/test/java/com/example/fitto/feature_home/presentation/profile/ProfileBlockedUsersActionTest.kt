package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.SocialUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileBlockedUsersActionTest {

    @Test
    fun `load returns repository users without changing order`() = runBlocking {
        val users = listOf(user("two"), user("one"))
        val action = ProfileBlockedUsersAction(
            getBlockedUsers = { users },
            unblockUser = {}
        )

        assertEquals(users, action.load())
    }

    @Test
    fun `unblock removes every matching user after repository succeeds`() = runBlocking {
        val unblockedIds = mutableListOf<String>()
        val action = ProfileBlockedUsersAction(
            getBlockedUsers = { emptyList() },
            unblockUser = { userId -> unblockedIds += userId }
        )

        val result = action.unblock(
            currentUsers = listOf(user("one"), user("two"), user("one")),
            userId = "one"
        )

        assertEquals(listOf("one"), unblockedIds)
        assertEquals("one", result.userId)
        assertEquals(listOf("two"), result.remainingUsers.map { user -> user.id })
    }

    @Test
    fun `unblock failure keeps optimistic result from being created`() = runBlocking {
        val action = ProfileBlockedUsersAction(
            getBlockedUsers = { emptyList() },
            unblockUser = { error("failed") }
        )

        val result = runCatching {
            action.unblock(listOf(user("one")), "one")
        }

        assertTrue(result.isFailure)
    }

    private fun user(id: String) = SocialUser(id = id, userName = id)
}
