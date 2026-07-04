package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialProfileCoordinatorTest {

    @Test
    fun profileQueries_routeArgumentsAndReturnSourceValues() = runBlocking {
        val expectedSearchUsers = listOf(
            SearchUserUiModel(
                id = "user-1",
                userName = "fit_user",
                bio = "Merhaba",
                photoUrl = null
            )
        )
        val expectedProfile = SocialUser(id = "user-2", userName = "profile_user")
        val querySource = FakeProfileQuerySource(
            searchResult = expectedSearchUsers,
            profileResult = expectedProfile
        )
        val coordinator = createCoordinator(querySource = querySource)

        val searchUsers = coordinator.searchUsers("fit")
        val profile = coordinator.getUserProfile("user-2")

        assertEquals("fit", querySource.searchQuery)
        assertEquals(expectedSearchUsers, searchUsers)
        assertEquals("user-2", querySource.profileUserId)
        assertEquals(expectedProfile, profile)
    }

    @Test
    fun currentProfileQueries_routeToQuerySource() = runBlocking {
        val currentProfile = SocialUser(id = "current-user", userName = "current")
        val usersById = listOf(
            SocialUser(id = "user-2"),
            SocialUser(id = "user-1")
        )
        val querySource = FakeProfileQuerySource(
            currentProfileResult = currentProfile,
            deletedOrMissingResult = false,
            usersByIdResult = usersById
        )
        val coordinator = createCoordinator(querySource = querySource)

        coordinator.createCurrentUserProfileIfNeeded()
        val loadedCurrentProfile = coordinator.getCurrentUserProfile()
        val deletedOrMissing = coordinator.isUserDeletedOrMissing("user-3")
        val loadedUsers = coordinator.getUsersByIds(listOf("user-2", "user-1"))

        assertTrue(querySource.profileCreationRequested)
        assertEquals(currentProfile, loadedCurrentProfile)
        assertEquals("user-3", querySource.deletedUserId)
        assertFalse(deletedOrMissing)
        assertEquals(listOf("user-2", "user-1"), querySource.requestedUserIds)
        assertEquals(usersById, loadedUsers)
    }

    @Test
    fun profileUpdates_routeToCommandSource() = runBlocking {
        val commandSource = FakeProfileCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)

        coordinator.updateCurrentUserProfile(
            displayName = "fit_user",
            bio = "Yeni biyografi"
        )
        coordinator.updateCurrentUserPhotoUrl("https://example.com/photo.jpg")

        assertEquals("fit_user", commandSource.updatedDisplayName)
        assertEquals("Yeni biyografi", commandSource.updatedBio)
        assertEquals("https://example.com/photo.jpg", commandSource.updatedPhotoUrl)
    }

    @Test
    fun relationshipCommands_routeToRelationshipSource() = runBlocking {
        val relationshipSource = FakeRelationshipSource()
        val coordinator = createCoordinator(relationshipSource = relationshipSource)
        val targetUser = SocialUser(id = "target-user")

        coordinator.toggleFollow(targetUser)
        coordinator.removeFollower("follower-user")
        coordinator.stopFollowing("following-user")

        assertEquals(targetUser, relationshipSource.followTarget)
        assertEquals("follower-user", relationshipSource.removedFollowerId)
        assertEquals("following-user", relationshipSource.stoppedFollowingId)
    }

    @Test
    fun relationshipQueries_routeArgumentsAndReturnSourceValues() = runBlocking {
        val followers = listOf(SocialUser(id = "follower-1"))
        val following = listOf(SocialUser(id = "following-1"))
        val relationshipSource = FakeRelationshipSource(
            followersResult = followers,
            followingResult = following
        )
        val coordinator = createCoordinator(relationshipSource = relationshipSource)

        val loadedFollowers = coordinator.getFollowers("profile-user")
        val loadedFollowing = coordinator.getFollowing("profile-user")

        assertEquals("profile-user", relationshipSource.followersUserId)
        assertEquals("profile-user", relationshipSource.followingUserId)
        assertEquals(followers, loadedFollowers)
        assertEquals(following, loadedFollowing)
    }

    private fun createCoordinator(
        querySource: FakeProfileQuerySource = FakeProfileQuerySource(),
        commandSource: FakeProfileCommandSource = FakeProfileCommandSource(),
        relationshipSource: FakeRelationshipSource = FakeRelationshipSource()
    ): SocialProfileCoordinator {
        return SocialProfileCoordinator(
            querySource = querySource,
            commandSource = commandSource,
            relationshipSource = relationshipSource
        )
    }

    private class FakeProfileQuerySource(
        private val searchResult: List<SearchUserUiModel> = emptyList(),
        private val currentProfileResult: SocialUser? = null,
        private val profileResult: SocialUser? = null,
        private val deletedOrMissingResult: Boolean = true,
        private val usersByIdResult: List<SocialUser> = emptyList()
    ) : SocialProfileQuerySource {
        var searchQuery: String? = null
        var profileCreationRequested: Boolean = false
        var profileUserId: String? = null
        var deletedUserId: String? = null
        var requestedUserIds: List<String>? = null

        override suspend fun searchUsers(query: String): List<SearchUserUiModel> {
            searchQuery = query
            return searchResult
        }

        override suspend fun createCurrentUserProfileIfNeeded() {
            profileCreationRequested = true
        }

        override suspend fun getCurrentUserProfile(): SocialUser? {
            return currentProfileResult
        }

        override suspend fun getUserProfile(userId: String): SocialUser? {
            profileUserId = userId
            return profileResult
        }

        override suspend fun isUserDeletedOrMissing(userId: String): Boolean {
            deletedUserId = userId
            return deletedOrMissingResult
        }

        override suspend fun getUsersByIds(userIds: List<String>): List<SocialUser> {
            requestedUserIds = userIds
            return usersByIdResult
        }
    }

    private class FakeProfileCommandSource : SocialProfileCommandSource {
        var updatedDisplayName: String? = null
        var updatedBio: String? = null
        var updatedPhotoUrl: String? = null

        override suspend fun updateCurrentUserProfile(displayName: String, bio: String) {
            updatedDisplayName = displayName
            updatedBio = bio
        }

        override suspend fun updateCurrentUserPhotoUrl(photoUrl: String?) {
            updatedPhotoUrl = photoUrl
        }

        override suspend fun uploadCurrentUserProfilePhoto(
            context: Context,
            imageUri: Uri
        ): String {
            return ""
        }
    }

    private class FakeRelationshipSource(
        private val followersResult: List<SocialUser> = emptyList(),
        private val followingResult: List<SocialUser> = emptyList()
    ) : SocialRelationshipSource {
        var followTarget: SocialUser? = null
        var removedFollowerId: String? = null
        var stoppedFollowingId: String? = null
        var followersUserId: String? = null
        var followingUserId: String? = null

        override suspend fun toggleFollow(targetUser: SocialUser) {
            followTarget = targetUser
        }

        override suspend fun removeFollower(followerId: String) {
            removedFollowerId = followerId
        }

        override suspend fun stopFollowing(followingId: String) {
            stoppedFollowingId = followingId
        }

        override suspend fun getFollowers(userId: String): List<SocialUser> {
            followersUserId = userId
            return followersResult
        }

        override suspend fun getFollowing(userId: String): List<SocialUser> {
            followingUserId = userId
            return followingResult
        }
    }
}
