package com.example.fitto.feature_home.presentation.social

internal object SocialAuthorHydrationResolver {

    fun postAuthorIds(posts: List<SocialPost>): List<String> {
        return posts.map { post -> post.userId }
            .filter { userId -> userId.isNotBlank() }
            .distinct()
    }

    fun commentAuthorIds(comments: List<SocialComment>): List<String> {
        return comments.map { comment -> comment.userId }
            .filter { userId -> userId.isNotBlank() }
            .distinct()
    }

    fun messageThreadParticipantIds(threads: List<MessageThreadUiModel>): List<String> {
        return threads.mapNotNull { thread -> thread.otherParticipantId }
            .filter { userId -> userId.isNotBlank() }
            .distinct()
    }

    fun hydratePosts(
        posts: List<SocialPost>,
        users: List<SocialUser>
    ): List<SocialPost> {
        if (posts.isEmpty() || users.isEmpty()) return posts

        val usersById = users.associateBy { user -> user.id }

        return posts.map { post ->
            val user = usersById[post.userId]
            if (user == null) {
                post
            } else {
                post.copy(
                    userName = user.userName.ifBlank { post.userName },
                    userPhotoUrl = user.photoUrl
                )
            }
        }
    }

    fun hydrateComments(
        comments: List<SocialComment>,
        users: List<SocialUser>
    ): List<SocialComment> {
        if (comments.isEmpty() || users.isEmpty()) return comments

        val usersById = users.associateBy { user -> user.id }

        return comments.map { comment ->
            val user = usersById[comment.userId]
            if (user == null) {
                comment
            } else {
                comment.copy(
                    userName = user.userName.ifBlank { comment.userName },
                    userPhotoUrl = user.photoUrl
                )
            }
        }
    }

    fun hydrateMessageThreads(
        threads: List<MessageThreadUiModel>,
        users: List<SocialUser>
    ): List<MessageThreadUiModel> {
        if (threads.isEmpty() || users.isEmpty()) return threads

        val usersById = users.associateBy { user -> user.id }

        return threads.map { thread ->
            if (thread.isGroup) {
                thread
            } else {
                val user = usersById[thread.otherParticipantId]
                if (user == null) {
                    thread
                } else {
                    val title = user.userName.ifBlank { thread.title }
                    thread.copy(
                        title = title,
                        initials = SocialTextResolver.createInitials(title),
                        photoUrl = user.photoUrl
                    )
                }
            }
        }
    }
}
