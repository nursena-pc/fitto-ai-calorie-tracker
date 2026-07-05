package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.social.SocialUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProfileScreenStateStoreTest {

    @Test
    fun profileUpdate_preservesPreviouslyLoadedUserWhenPayloadHasNoUser() {
        val user = SocialUser(id = "user-1", userName = "fitto")
        val store = ProfileScreenStateStore(
            ProfileScreenState(user = user, postCount = 3)
        )

        val state = store.applyProfile(
            ProfileUiState(
                user = null,
                postCount = 4,
                healthData = ProfileHealthData(currentWeightKg = 65.0)
            )
        )

        assertSame(user, state.user)
        assertEquals(4, state.postCount)
        assertEquals(65.0, state.healthData.currentWeightKg, 0.0)
    }

    @Test
    fun weeklySummaryUpdate_preservesProfileContent() {
        val user = SocialUser(id = "user-1")
        val store = ProfileScreenStateStore(ProfileScreenState(user = user))
        val summary = ProfileWeeklySummary(todayCalories = 1700, recordedDays = 5)

        val state = store.updateWeeklySummary(summary)

        assertSame(user, state.user)
        assertEquals(summary, state.weeklySummary)
    }

    @Test
    fun negativeScrollPosition_isClampedToZero() {
        val store = ProfileScreenStateStore()

        val state = store.updateScrollY(-20)

        assertEquals(0, state.scrollY)
    }
}
