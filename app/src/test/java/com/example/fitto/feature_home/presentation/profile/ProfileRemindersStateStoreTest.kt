package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileRemindersStateStoreTest {

    @Test
    fun selectCategory_updatesCurrentFilter() {
        val store = ProfileRemindersStateStore()

        val state = store.selectCategory(ReminderCategory.WATER)

        assertEquals(ReminderCategory.WATER, state.selectedCategory)
        assertEquals(ReminderCategory.WATER, store.state.selectedCategory)
    }

    @Test
    fun initialCategory_isPreserved() {
        val store = ProfileRemindersStateStore(
            ProfileRemindersState(ReminderCategory.EXERCISE)
        )

        assertEquals(ReminderCategory.EXERCISE, store.state.selectedCategory)
    }
}
