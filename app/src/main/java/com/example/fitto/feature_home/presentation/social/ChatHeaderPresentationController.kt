package com.example.fitto.feature_home.presentation.social

internal enum class ChatHeaderAvatarMode {
    PHOTO,
    GROUP_ICON,
    INITIALS
}

internal sealed interface ChatHeaderSubtitle {
    data class GroupMembers(
        val memberCount: Int
    ) : ChatHeaderSubtitle

    data object Online : ChatHeaderSubtitle

    data object Offline : ChatHeaderSubtitle

    data class LastSeen(
        val lastChanged: Long
    ) : ChatHeaderSubtitle

    data class DirectTyping(
        val dots: String
    ) : ChatHeaderSubtitle

    data class GroupTypingOne(
        val userName: String,
        val dots: String
    ) : ChatHeaderSubtitle

    data class GroupTypingMany(
        val userCount: Int,
        val dots: String
    ) : ChatHeaderSubtitle
}

internal data class ChatTypingIndicatorPresentation(
    val userName: String,
    val photoUrl: String?,
    val initials: String
)

internal data class ChatHeaderPresentation(
    val title: String,
    val initials: String,
    val photoUrl: String?,
    val avatarMode: ChatHeaderAvatarMode,
    val subtitle: ChatHeaderSubtitle,
    val typingIndicator: ChatTypingIndicatorPresentation?,
    val shouldAnimateTyping: Boolean
)

internal class ChatHeaderPresentationController(
    private val isGroup: Boolean,
    private val memberCount: Int,
    initialTitle: String,
    private val initialInitials: String,
    private val initialPhotoUrl: String?
) {
    private var currentTitle = initialTitle
    private var currentPhotoUrl = initialPhotoUrl
    private var currentPresence: PresenceStatusUiModel? = null
    private var currentTypingUsers: List<TypingUserUiModel> = emptyList()
    private var typingDotIndex = 0

    val currentPresentation: ChatHeaderPresentation
        get() = resolvePresentation()

    fun updateTitle(title: String): ChatHeaderPresentation {
        title.takeIf(String::isNotBlank)
            ?.let { currentTitle = it }

        return resolvePresentation()
    }

    fun updatePhoto(photoUrl: String?): ChatHeaderPresentation {
        currentPhotoUrl = photoUrl ?: initialPhotoUrl
        return resolvePresentation()
    }

    fun onPresenceChanged(presence: PresenceStatusUiModel): ChatHeaderPresentation {
        currentPresence = presence
        return resolvePresentation()
    }

    fun onTypingUsersChanged(users: List<TypingUserUiModel>): ChatHeaderPresentation {
        currentTypingUsers = users
        typingDotIndex = 0
        return resolvePresentation()
    }

    fun onTypingAnimationTick(): ChatHeaderPresentation {
        if (currentTypingUsers.isNotEmpty()) {
            typingDotIndex = (typingDotIndex + 1) % TYPING_DOT_STATE_COUNT
        }

        return resolvePresentation()
    }

    private fun resolvePresentation(): ChatHeaderPresentation {
        val resolvedPhotoUrl = currentPhotoUrl
            ?.takeIf(String::isNotBlank)
        val typingIndicator = currentTypingUsers.firstOrNull()?.let { user ->
            ChatTypingIndicatorPresentation(
                userName = user.userName,
                photoUrl = user.photoUrl,
                initials = user.userName.firstOrNull()?.uppercase() ?: DEFAULT_TYPING_INITIAL
            )
        }

        return ChatHeaderPresentation(
            title = currentTitle,
            initials = initialInitials,
            photoUrl = resolvedPhotoUrl,
            avatarMode = resolveAvatarMode(resolvedPhotoUrl),
            subtitle = resolveSubtitle(),
            typingIndicator = typingIndicator,
            shouldAnimateTyping = currentTypingUsers.isNotEmpty()
        )
    }

    private fun resolveAvatarMode(photoUrl: String?): ChatHeaderAvatarMode {
        return when {
            !photoUrl.isNullOrBlank() -> ChatHeaderAvatarMode.PHOTO
            isGroup -> ChatHeaderAvatarMode.GROUP_ICON
            else -> ChatHeaderAvatarMode.INITIALS
        }
    }

    private fun resolveSubtitle(): ChatHeaderSubtitle {
        val typingUsers = currentTypingUsers
        if (typingUsers.isNotEmpty()) {
            val dots = ".".repeat(typingDotIndex)

            return when {
                !isGroup -> ChatHeaderSubtitle.DirectTyping(dots)
                typingUsers.size == 1 -> ChatHeaderSubtitle.GroupTypingOne(
                    userName = typingUsers.first().userName,
                    dots = dots
                )
                else -> ChatHeaderSubtitle.GroupTypingMany(
                    userCount = typingUsers.size,
                    dots = dots
                )
            }
        }

        if (isGroup) {
            return ChatHeaderSubtitle.GroupMembers(memberCount)
        }

        val presence = currentPresence
        return when {
            presence?.isOnline == true -> ChatHeaderSubtitle.Online
            presence == null || presence.lastChanged <= 0L -> ChatHeaderSubtitle.Offline
            else -> ChatHeaderSubtitle.LastSeen(presence.lastChanged)
        }
    }

    private companion object {
        const val TYPING_DOT_STATE_COUNT = 4
        const val DEFAULT_TYPING_INITIAL = "F"
    }
}
