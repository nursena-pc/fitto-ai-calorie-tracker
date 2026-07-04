package com.example.fitto.feature_home.presentation.social

import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SocialThreadManagementCoordinatorTest {

    @Test
    fun getGroupSettings_routesThreadToQuerySource() = runBlocking {
        val expected = GroupSettingsUiModel(joinApprovalRequired = true)
        val querySource = FakeQuerySource(groupSettings = expected)
        val coordinator = createCoordinator(querySource = querySource)

        val result = coordinator.getGroupSettings("thread-settings")

        assertSame(expected, result)
        assertEquals("thread-settings", querySource.groupSettingsThreadId)
    }

    @Test
    fun searchUsersForGroupInvite_routesThreadAndQueryToQuerySource() = runBlocking {
        val expected = listOf(searchUser("candidate-1"))
        val querySource = FakeQuerySource(inviteCandidates = expected)
        val coordinator = createCoordinator(querySource = querySource)

        val result = coordinator.searchUsersForGroupInvite(
            threadId = "thread-invite",
            query = "nur"
        )

        assertSame(expected, result)
        assertEquals("thread-invite", querySource.inviteThreadId)
        assertEquals("nur", querySource.inviteQuery)
    }

    @Test
    fun createGroupThread_routesCompleteInputToCommandSource() = runBlocking {
        val selectedUsers = listOf(searchUser("member-1"), searchUser("member-2"))
        val expected = messageThread("group-1")
        val commandSource = FakeCommandSource(createdGroupThread = expected)
        val coordinator = createCoordinator(commandSource = commandSource)

        val result = coordinator.createGroupThread(
            title = "Takım",
            description = "Açıklama",
            selectedUsers = selectedUsers
        )

        assertSame(expected, result)
        assertEquals("Takım", commandSource.groupTitle)
        assertEquals("Açıklama", commandSource.groupDescription)
        assertSame(selectedUsers, commandSource.groupSelectedUsers)
    }

    @Test
    fun updateGroupTitle_routesThreadAndTitleToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)

        coordinator.updateGroupTitle(
            threadId = "thread-title",
            newTitle = "Yeni ad"
        )

        assertEquals("thread-title", commandSource.updatedTitleThreadId)
        assertEquals("Yeni ad", commandSource.updatedTitle)
    }

    @Test
    fun deleteThreadForCurrentUser_routesThreadToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)

        coordinator.deleteThreadForCurrentUser("thread-delete")

        assertEquals("thread-delete", commandSource.deletedThreadId)
    }

    @Test
    fun removeMemberFromGroup_routesIdentifiersToMembershipSource() = runBlocking {
        val membershipSource = FakeMembershipSource()
        val coordinator = createCoordinator(membershipSource = membershipSource)

        coordinator.removeMemberFromGroup(
            threadId = "thread-members",
            memberId = "member-remove"
        )

        assertEquals("thread-members", membershipSource.removedMemberThreadId)
        assertEquals("member-remove", membershipSource.removedMemberId)
    }

    @Test
    fun addMembersToGroup_routesSelectedUsersToMembershipSource() = runBlocking {
        val selectedUsers = listOf(searchUser("member-add"))
        val membershipSource = FakeMembershipSource()
        val coordinator = createCoordinator(membershipSource = membershipSource)

        coordinator.addMembersToGroup(
            threadId = "thread-add",
            selectedUsers = selectedUsers
        )

        assertEquals("thread-add", membershipSource.addedMembersThreadId)
        assertSame(selectedUsers, membershipSource.addedMembers)
    }

    private fun createCoordinator(
        querySource: FakeQuerySource = FakeQuerySource(),
        commandSource: FakeCommandSource = FakeCommandSource(),
        membershipSource: FakeMembershipSource = FakeMembershipSource()
    ): SocialThreadManagementCoordinator {
        return SocialThreadManagementCoordinator(
            querySource = querySource,
            commandSource = commandSource,
            membershipSource = membershipSource
        )
    }

    private fun searchUser(id: String): SearchUserUiModel {
        return SearchUserUiModel(
            id = id,
            userName = id,
            bio = null,
            photoUrl = null
        )
    }

    private fun messageThread(id: String): MessageThreadUiModel {
        return MessageThreadUiModel(
            id = id,
            title = "Başlık",
            lastMessage = "",
            timeText = "",
            isGroup = true,
            initials = "B"
        )
    }

    private class FakeQuerySource(
        private val groupSettings: GroupSettingsUiModel = GroupSettingsUiModel(),
        private val inviteCandidates: List<SearchUserUiModel> = emptyList()
    ) : SocialThreadQuerySource {
        var groupSettingsThreadId: String? = null
        var inviteThreadId: String? = null
        var inviteQuery: String? = null

        override suspend fun getGroupSettings(threadId: String): GroupSettingsUiModel {
            groupSettingsThreadId = threadId
            return groupSettings
        }

        override suspend fun getGroupMembers(threadId: String): List<GroupMemberUiModel> {
            return emptyList()
        }

        override suspend fun getThreadSharedContent(threadId: String): List<SharedContentUiModel> {
            return emptyList()
        }

        override suspend fun searchUsersForGroupInvite(
            threadId: String,
            query: String
        ): List<SearchUserUiModel> {
            inviteThreadId = threadId
            inviteQuery = query
            return inviteCandidates
        }

        override suspend fun getMessageThread(threadId: String): MessageThreadUiModel? {
            return null
        }

        override suspend fun getMessageThreadRawData(threadId: String): Map<String, Any?> {
            return emptyMap()
        }
    }

    private class FakeCommandSource(
        private val createdGroupThread: MessageThreadUiModel? = null
    ) : SocialThreadCommandSource {
        var groupTitle: String? = null
        var groupDescription: String? = null
        var groupSelectedUsers: List<SearchUserUiModel>? = null
        var updatedTitleThreadId: String? = null
        var updatedTitle: String? = null
        var deletedThreadId: String? = null

        override suspend fun createOrGetDirectThread(targetUserId: String): MessageThreadUiModel? {
            return null
        }

        override suspend fun createGroupThread(
            title: String,
            description: String,
            selectedUsers: List<SearchUserUiModel>
        ): MessageThreadUiModel? {
            groupTitle = title
            groupDescription = description
            groupSelectedUsers = selectedUsers
            return createdGroupThread
        }

        override suspend fun updateGroupSettings(
            threadId: String,
            settings: GroupSettingsUiModel
        ) = Unit

        override suspend fun updateGroupPhoto(
            threadId: String,
            imageUri: Uri
        ): String = ""

        override suspend fun updateGroupTitle(threadId: String, newTitle: String) {
            updatedTitleThreadId = threadId
            updatedTitle = newTitle
        }

        override suspend fun updateGroupDescription(threadId: String, newDescription: String) = Unit

        override suspend fun updateGroupName(threadId: String, newName: String) = Unit

        override suspend fun clearThreadForCurrentUser(threadId: String) = Unit

        override suspend fun deleteThreadForCurrentUser(threadId: String) {
            deletedThreadId = threadId
        }
    }

    private class FakeMembershipSource : SocialGroupMembershipSource {
        var removedMemberThreadId: String? = null
        var removedMemberId: String? = null
        var addedMembersThreadId: String? = null
        var addedMembers: List<SearchUserUiModel>? = null

        override suspend fun leaveGroup(threadId: String) = Unit

        override suspend fun removeMemberFromGroup(
            threadId: String,
            memberId: String
        ) {
            removedMemberThreadId = threadId
            removedMemberId = memberId
        }

        override suspend fun addMembersToGroup(
            threadId: String,
            selectedUsers: List<SearchUserUiModel>
        ) {
            addedMembersThreadId = threadId
            addedMembers = selectedUsers
        }
    }
}
