package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SocialGroupThreadPayloadResolverTest {

    @Test
    fun resolveInput_trimsTitleAndDescriptionAndFiltersSelectedUsers() {
        val input = SocialGroupThreadPayloadResolver.resolveInput(
            title = "  Kahvaltı Ekibi  ",
            description = "  Sağlıklı tarifler  ",
            selectedUsers = listOf(
                searchUser("current"),
                searchUser(""),
                searchUser("user-1"),
                searchUser("user-1", userName = "duplicate"),
                searchUser("user-2")
            ),
            currentUserId = "current"
        )

        assertNotNull(input)
        assertEquals("Kahvaltı Ekibi", input?.title)
        assertEquals("Sağlıklı tarifler", input?.description)
        assertEquals(listOf("user-1", "user-2"), input?.selectedUsers?.map { it.id })
    }

    @Test
    fun resolveInput_returnsNullWhenTitleBlankOrNotEnoughUsers() {
        assertNull(
            SocialGroupThreadPayloadResolver.resolveInput(
                title = "   ",
                description = "desc",
                selectedUsers = listOf(searchUser("user-1"), searchUser("user-2")),
                currentUserId = "current"
            )
        )

        assertNull(
            SocialGroupThreadPayloadResolver.resolveInput(
                title = "Group",
                description = "desc",
                selectedUsers = listOf(searchUser("user-1"), searchUser("user-1")),
                currentUserId = "current"
            )
        )
    }

    @Test
    fun createPayload_buildsGroupThreadPayload() {
        val input = SocialGroupThreadInput(
            title = "İyi Yaşam",
            description = "Sağlıklı Öğünler",
            selectedUsers = listOf(
                searchUser("user-1", userName = "ayse", photoUrl = "photo-1"),
                searchUser("user-2", userName = "mehmet", photoUrl = null)
            )
        )

        val payload = SocialGroupThreadPayloadResolver.createPayload(
            threadId = "group-1",
            currentUserId = "current",
            currentUserName = "nursena",
            currentPhotoUrl = "current-photo",
            input = input,
            now = 123L
        )

        assertEquals("group-1", payload["id"])
        assertEquals("group", payload["type"])
        assertEquals("İyi Yaşam", payload["title"])
        assertEquals("iyi yaşam", payload["titleLowercase"])
        assertEquals("current", payload["creatorId"])
        assertEquals(listOf("current", "user-1", "user-2"), payload["participantIds"])
        assertEquals(3, payload["memberCount"])
        assertEquals(mapOf("current" to 0, "user-1" to 0, "user-2" to 0), payload["unreadCounts"])
        assertEquals("", payload["lastMessageText"])
        assertNull(payload["lastMessageSenderId"])
        assertNull(payload["lastMessageCreatedAt"])
        assertEquals(123L, payload["createdAt"])
        assertEquals(123L, payload["deliveredAt"])
        assertEquals(123L, payload["updatedAt"])
        assertEquals("Sağlıklı Öğünler", payload["description"])
        assertEquals("sağlıklı öğünler", payload["descriptionLowercase"])

        @Suppress("UNCHECKED_CAST")
        val participants = payload["participants"] as Map<String, Map<String, Any?>>
        assertEquals("nursena", participants.getValue("current")["userName"])
        assertEquals("current-photo", participants.getValue("current")["photoUrl"])
        assertEquals("ayse", participants.getValue("user-1")["userName"])
        assertEquals("photo-1", participants.getValue("user-1")["photoUrl"])
        assertEquals("mehmet", participants.getValue("user-2")["userName"])
    }

    private fun searchUser(
        id: String,
        userName: String = id,
        photoUrl: String? = null
    ): SearchUserUiModel {
        return SearchUserUiModel(
            id = id,
            userName = userName,
            bio = null,
            photoUrl = photoUrl
        )
    }
}
