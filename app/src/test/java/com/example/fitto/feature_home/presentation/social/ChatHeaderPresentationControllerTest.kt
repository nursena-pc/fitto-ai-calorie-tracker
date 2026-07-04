package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHeaderPresentationControllerTest {

    @Test
    fun currentPresentation_resolvesDirectChatIdentityAndOfflineState() {
        val controller = directController()

        val presentation = controller.currentPresentation

        assertEquals("Ada", presentation.title)
        assertEquals("A", presentation.initials)
        assertNull(presentation.photoUrl)
        assertEquals(ChatHeaderAvatarMode.INITIALS, presentation.avatarMode)
        assertEquals(ChatHeaderSubtitle.Offline, presentation.subtitle)
        assertNull(presentation.typingIndicator)
        assertFalse(presentation.shouldAnimateTyping)
    }

    @Test
    fun currentPresentation_resolvesGroupIdentityAndMemberSubtitle() {
        val controller = ChatHeaderPresentationController(
            isGroup = true,
            memberCount = 4,
            initialTitle = "Fit Grup",
            initialInitials = "FG",
            initialPhotoUrl = null
        )

        val presentation = controller.currentPresentation

        assertEquals(ChatHeaderAvatarMode.GROUP_ICON, presentation.avatarMode)
        assertEquals(ChatHeaderSubtitle.GroupMembers(4), presentation.subtitle)
    }

    @Test
    fun onPresenceChanged_resolvesOnlineAndLastSeenStates() {
        val controller = directController()

        val online = controller.onPresenceChanged(
            PresenceStatusUiModel(
                userId = "user-1",
                isOnline = true,
                lastChanged = 100L
            )
        )
        val lastSeen = controller.onPresenceChanged(
            PresenceStatusUiModel(
                userId = "user-1",
                isOnline = false,
                lastChanged = 250L
            )
        )

        assertEquals(ChatHeaderSubtitle.Online, online.subtitle)
        assertEquals(ChatHeaderSubtitle.LastSeen(250L), lastSeen.subtitle)
    }

    @Test
    fun onTypingUsersChanged_overridesPresenceAndCyclesTypingDots() {
        val controller = directController()
        controller.onPresenceChanged(
            PresenceStatusUiModel(
                userId = "user-1",
                isOnline = true,
                lastChanged = 100L
            )
        )

        val initialTyping = controller.onTypingUsersChanged(
            listOf(typingUser(name = "Ayşe", photoUrl = "photo"))
        )
        val firstTick = controller.onTypingAnimationTick()
        val secondTick = controller.onTypingAnimationTick()
        val thirdTick = controller.onTypingAnimationTick()
        val fourthTick = controller.onTypingAnimationTick()

        assertEquals(ChatHeaderSubtitle.DirectTyping(""), initialTyping.subtitle)
        assertEquals(ChatHeaderSubtitle.DirectTyping("."), firstTick.subtitle)
        assertEquals(ChatHeaderSubtitle.DirectTyping(".."), secondTick.subtitle)
        assertEquals(ChatHeaderSubtitle.DirectTyping("..."), thirdTick.subtitle)
        assertEquals(ChatHeaderSubtitle.DirectTyping(""), fourthTick.subtitle)
        assertEquals(
            ChatTypingIndicatorPresentation(
                userName = "Ayşe",
                photoUrl = "photo",
                initials = "A"
            ),
            initialTyping.typingIndicator
        )
        assertTrue(initialTyping.shouldAnimateTyping)
    }

    @Test
    fun onTypingUsersChanged_restoresLatestPresenceWhenTypingStops() {
        val controller = directController()
        controller.onPresenceChanged(
            PresenceStatusUiModel(
                userId = "user-1",
                isOnline = false,
                lastChanged = 500L
            )
        )
        controller.onTypingUsersChanged(listOf(typingUser()))

        val presentation = controller.onTypingUsersChanged(emptyList())

        assertEquals(ChatHeaderSubtitle.LastSeen(500L), presentation.subtitle)
        assertNull(presentation.typingIndicator)
        assertFalse(presentation.shouldAnimateTyping)
    }

    @Test
    fun onTypingUsersChanged_resolvesSingleAndMultipleGroupTypingStates() {
        val controller = ChatHeaderPresentationController(
            isGroup = true,
            memberCount = 3,
            initialTitle = "Fit Grup",
            initialInitials = "FG",
            initialPhotoUrl = null
        )

        val oneUser = controller.onTypingUsersChanged(
            listOf(typingUser(name = "Deniz"))
        )
        val manyUsers = controller.onTypingUsersChanged(
            listOf(
                typingUser(uid = "user-1", name = "Deniz"),
                typingUser(uid = "user-2", name = "Ece")
            )
        )

        assertEquals(
            ChatHeaderSubtitle.GroupTypingOne(
                userName = "Deniz",
                dots = ""
            ),
            oneUser.subtitle
        )
        assertEquals(
            ChatHeaderSubtitle.GroupTypingMany(
                userCount = 2,
                dots = ""
            ),
            manyUsers.subtitle
        )
    }

    @Test
    fun updateTitle_ignoresBlankValuesAndPreservesNonBlankText() {
        val controller = directController()

        val updated = controller.updateTitle("  Yeni Ad  ")
        val ignored = controller.updateTitle("   ")

        assertEquals("  Yeni Ad  ", updated.title)
        assertEquals("  Yeni Ad  ", ignored.title)
    }

    @Test
    fun updatePhoto_usesNewPhotoAndFallsBackToInitialPhotoForNullUpdate() {
        val controller = ChatHeaderPresentationController(
            isGroup = true,
            memberCount = 2,
            initialTitle = "Fit Grup",
            initialInitials = "FG",
            initialPhotoUrl = "initial-photo"
        )

        val updated = controller.updatePhoto("new-photo")
        val restored = controller.updatePhoto(null)
        val removed = controller.updatePhoto("")

        assertEquals("new-photo", updated.photoUrl)
        assertEquals(ChatHeaderAvatarMode.PHOTO, updated.avatarMode)
        assertEquals("initial-photo", restored.photoUrl)
        assertEquals(ChatHeaderAvatarMode.PHOTO, restored.avatarMode)
        assertNull(removed.photoUrl)
        assertEquals(ChatHeaderAvatarMode.GROUP_ICON, removed.avatarMode)
    }

    private fun directController(): ChatHeaderPresentationController {
        return ChatHeaderPresentationController(
            isGroup = false,
            memberCount = 0,
            initialTitle = "Ada",
            initialInitials = "A",
            initialPhotoUrl = null
        )
    }

    private fun typingUser(
        uid: String = "user-1",
        name: String = "Fitto",
        photoUrl: String? = null
    ): TypingUserUiModel {
        return TypingUserUiModel(
            uid = uid,
            userName = name,
            photoUrl = photoUrl,
            updatedAt = 100L
        )
    }
}
