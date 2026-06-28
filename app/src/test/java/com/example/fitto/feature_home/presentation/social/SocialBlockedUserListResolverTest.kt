package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialBlockedUserListResolverTest {

    @Test
    fun blockedUserIds_keepsUniqueNonBlankNonCurrentIdsInOrder() {
        assertEquals(
            listOf("target-1", "target-2", "target-3"),
            SocialBlockedUserListResolver.blockedUserIds(
                ids = listOf("target-1", "", "current", "target-2", "target-1", "   ", "target-3"),
                currentUserId = "current"
            )
        )
    }

    @Test
    fun lookupChunks_usesFirestoreWhereInLimit() {
        val ids = (1..12).map { index -> "user-$index" }

        assertEquals(
            listOf(
                (1..10).map { index -> "user-$index" },
                listOf("user-11", "user-12")
            ),
            SocialBlockedUserListResolver.lookupChunks(ids)
        )
    }

    @Test
    fun resolveBlockedUser_usesDeletedFallbackWhenDeletedUserHasNoNames() {
        assertEquals(
            SocialUser(
                id = "deleted-user",
                userName = "Silinen Kullanıcı",
                displayName = "Silinen Kullanıcı",
                followersCount = 3,
                followingCount = 1
            ),
            SocialBlockedUserListResolver.resolveBlockedUser(
                id = "deleted-user",
                isDeleted = true,
                userName = null,
                username = null,
                displayName = null,
                name = null,
                realName = null,
                email = null,
                photoUrl = null,
                bio = null,
                followersCount = 3L,
                followingCount = 1L
            )
        )
    }

    @Test
    fun resolveBlockedUser_usesFittoFallbackWhenActiveUserHasNoNames() {
        assertEquals(
            SocialUser(
                id = "target-user",
                userName = "fitto_user",
                displayName = "fitto_user"
            ),
            SocialBlockedUserListResolver.resolveBlockedUser(
                id = "target-user",
                isDeleted = false,
                userName = null,
                username = null,
                displayName = null,
                name = null,
                realName = null,
                email = null,
                photoUrl = null,
                bio = null,
                followersCount = null,
                followingCount = null
            )
        )
    }

    @Test
    fun orderBlockedUsers_addsDeletedFallbackForMissingDocumentsAndSortsByVisibleName() {
        val ali = SocialUser(id = "target-2", userName = "ali")
        val zeynep = SocialUser(id = "target-1", userName = "zeynep")

        assertEquals(
            listOf(
                SocialUser(
                    id = "missing-target",
                    userName = "Silinen Kullanıcı",
                    displayName = "Silinen Kullanıcı"
                ),
                ali,
                zeynep
            ),
            SocialBlockedUserListResolver.orderBlockedUsers(
                blockedUserIds = listOf("target-1", "missing-target", "target-2"),
                usersById = mapOf(
                    zeynep.id to zeynep,
                    ali.id to ali
                )
            )
        )
    }
}
