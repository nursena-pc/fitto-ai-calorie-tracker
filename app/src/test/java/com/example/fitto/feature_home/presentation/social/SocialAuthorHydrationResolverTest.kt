package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialAuthorHydrationResolverTest {

    @Test
    fun postAuthorIds_returnsDistinctNonBlankIds() {
        val posts = listOf(
            SocialPost(id = "post-1", userId = "user-1"),
            SocialPost(id = "post-2", userId = ""),
            SocialPost(id = "post-3", userId = "user-1"),
            SocialPost(id = "post-4", userId = "user-2")
        )

        assertEquals(
            listOf("user-1", "user-2"),
            SocialAuthorHydrationResolver.postAuthorIds(posts)
        )
    }

    @Test
    fun hydratePosts_replacesAuthorNameAndPhotoWhenUserExists() {
        val posts = listOf(
            SocialPost(
                id = "post-1",
                userId = "user-1",
                userName = "old_name",
                userPhotoUrl = "old-photo"
            ),
            SocialPost(
                id = "post-2",
                userId = "missing-user",
                userName = "missing",
                userPhotoUrl = "missing-photo"
            )
        )
        val users = listOf(
            SocialUser(id = "user-1", userName = "new_name", photoUrl = "new-photo")
        )

        val result = SocialAuthorHydrationResolver.hydratePosts(posts, users)

        assertEquals("new_name", result[0].userName)
        assertEquals("new-photo", result[0].userPhotoUrl)
        assertEquals(posts[1], result[1])
    }

    @Test
    fun hydratePosts_preservesExistingNameWhenResolvedUserNameIsBlank() {
        val posts = listOf(
            SocialPost(id = "post-1", userId = "user-1", userName = "old_name")
        )
        val users = listOf(
            SocialUser(id = "user-1", userName = "", photoUrl = null)
        )

        val result = SocialAuthorHydrationResolver.hydratePosts(posts, users)

        assertEquals("old_name", result.single().userName)
        assertEquals(null, result.single().userPhotoUrl)
    }

    @Test
    fun commentAuthorIds_returnsDistinctNonBlankIds() {
        val comments = listOf(
            SocialComment(id = "comment-1", userId = "user-1"),
            SocialComment(id = "comment-2", userId = ""),
            SocialComment(id = "comment-3", userId = "user-1"),
            SocialComment(id = "comment-4", userId = "user-2")
        )

        assertEquals(
            listOf("user-1", "user-2"),
            SocialAuthorHydrationResolver.commentAuthorIds(comments)
        )
    }

    @Test
    fun hydrateComments_replacesAuthorNameAndPhotoWhenUserExists() {
        val comments = listOf(
            SocialComment(
                id = "comment-1",
                userId = "user-1",
                userName = "old_name",
                userPhotoUrl = "old-photo"
            )
        )
        val users = listOf(
            SocialUser(id = "user-1", userName = "new_name", photoUrl = "new-photo")
        )

        val result = SocialAuthorHydrationResolver.hydrateComments(comments, users)

        assertEquals("new_name", result.single().userName)
        assertEquals("new-photo", result.single().userPhotoUrl)
    }

    @Test
    fun messageThreadParticipantIds_returnsDistinctNonBlankOtherParticipantIds() {
        val threads = listOf(
            messageThread(id = "thread-1", otherParticipantId = "user-1"),
            messageThread(id = "thread-2", otherParticipantId = null),
            messageThread(id = "thread-3", otherParticipantId = ""),
            messageThread(id = "thread-4", otherParticipantId = "user-1"),
            messageThread(id = "thread-5", otherParticipantId = "user-2")
        )

        assertEquals(
            listOf("user-1", "user-2"),
            SocialAuthorHydrationResolver.messageThreadParticipantIds(threads)
        )
    }

    @Test
    fun hydrateMessageThreads_updatesDirectThreadTitleInitialsAndPhoto() {
        val threads = listOf(
            messageThread(
                id = "thread-1",
                title = "Old Name",
                initials = "ON",
                otherParticipantId = "user-1",
                photoUrl = "old-photo"
            ),
            messageThread(
                id = "thread-2",
                title = "Group",
                initials = "G",
                isGroup = true,
                otherParticipantId = "user-1",
                photoUrl = "group-photo"
            )
        )
        val users = listOf(
            SocialUser(id = "user-1", userName = "Ayşe Yılmaz", photoUrl = "new-photo")
        )

        val result = SocialAuthorHydrationResolver.hydrateMessageThreads(threads, users)

        assertEquals("Ayşe Yılmaz", result[0].title)
        assertEquals("AY", result[0].initials)
        assertEquals("new-photo", result[0].photoUrl)
        assertEquals(threads[1], result[1])
    }

    @Test
    fun hydrateMessageThreads_preservesTitleWhenResolvedUserNameIsBlank() {
        val threads = listOf(
            messageThread(
                id = "thread-1",
                title = "Old Name",
                initials = "ON",
                otherParticipantId = "user-1"
            )
        )
        val users = listOf(
            SocialUser(id = "user-1", userName = "", photoUrl = "new-photo")
        )

        val result = SocialAuthorHydrationResolver.hydrateMessageThreads(threads, users)

        assertEquals("Old Name", result.single().title)
        assertEquals("ON", result.single().initials)
        assertEquals("new-photo", result.single().photoUrl)
    }

    private fun messageThread(
        id: String,
        title: String = "Thread",
        initials: String = "T",
        isGroup: Boolean = false,
        otherParticipantId: String? = null,
        photoUrl: String? = null
    ): MessageThreadUiModel {
        return MessageThreadUiModel(
            id = id,
            title = title,
            lastMessage = "Last message",
            timeText = "şimdi",
            isGroup = isGroup,
            initials = initials,
            otherParticipantId = otherParticipantId,
            photoUrl = photoUrl
        )
    }
}
