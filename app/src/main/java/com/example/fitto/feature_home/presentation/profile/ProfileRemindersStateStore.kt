package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory

internal data class ProfileRemindersState(
    val selectedCategory: ReminderCategory = ReminderCategory.ALL
)

internal class ProfileRemindersStateStore(
    initialState: ProfileRemindersState = ProfileRemindersState()
) {
    var state: ProfileRemindersState = initialState
        private set

    fun selectCategory(category: ReminderCategory): ProfileRemindersState {
        state = state.copy(selectedCategory = category)
        return state
    }
}
