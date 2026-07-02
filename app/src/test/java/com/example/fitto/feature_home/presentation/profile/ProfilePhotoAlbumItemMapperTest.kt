package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.SocialPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class ProfilePhotoAlbumItemMapperTest {

    private val formatter = ProfilePhotoAlbumDateFormatter(
        locale = Locale.US,
        localTimeZone = TimeZone.getTimeZone("UTC"),
        utcTimeZone = TimeZone.getTimeZone("UTC")
    )
    private val mapper = ProfilePhotoAlbumItemMapper(formatter)
    private val labels = ProfilePhotoAlbumLabels(
        profilePhotoSource = "Profile photo",
        postPhotoSource = "Post photo",
        undated = "Undated"
    )

    @Test
    fun `profile photo and post photos are sorted newest first`() {
        val older = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()
        val newer = Instant.parse("2026-01-03T00:00:00Z").toEpochMilli()

        val items = mapper.map(
            profilePhotoUrl = "https://example.com/profile.jpg",
            posts = listOf(
                SocialPost(id = "older", imageUrl = "https://example.com/older.jpg", createdAt = older),
                SocialPost(id = "newer", imageUrl = "https://example.com/newer.jpg", createdAt = newer)
            ),
            labels = labels
        )

        assertEquals(listOf("newer", "older", "profile_photo"), items.map { item -> item.id })
    }

    @Test
    fun `posts without an image are ignored`() {
        val items = mapper.map(
            profilePhotoUrl = null,
            posts = listOf(
                SocialPost(id = "null-image", imageUrl = null),
                SocialPost(id = "blank-image", imageUrl = "  "),
                SocialPost(id = "photo", imageUrl = "https://example.com/photo.jpg")
            ),
            labels = labels
        )

        assertEquals(listOf("photo"), items.map { item -> item.id })
    }

    @Test
    fun `meal title is preferred and blank meal title uses post fallback`() {
        val items = mapper.map(
            profilePhotoUrl = null,
            posts = listOf(
                SocialPost(id = "meal", imageUrl = "meal.jpg", mealInfo = "Breakfast"),
                SocialPost(id = "fallback", imageUrl = "fallback.jpg", mealInfo = " ")
            ),
            labels = labels
        )

        assertEquals("Breakfast", items.first { item -> item.id == "meal" }.sourceTitle)
        assertEquals("Post photo", items.first { item -> item.id == "fallback" }.sourceTitle)
    }

    @Test
    fun `non positive post timestamp maps to undated item`() {
        val item = mapper.map(
            profilePhotoUrl = null,
            posts = listOf(SocialPost(id = "post", imageUrl = "photo.jpg", createdAt = -5L)),
            labels = labels
        ).single()

        assertEquals(0L, item.createdAt)
        assertEquals(ProfilePhotoAlbumFilter.UNDATED, item.dateKey)
        assertEquals("Undated", item.dateText)
        assertFalse(item.imageUrl.isBlank())
    }
}
