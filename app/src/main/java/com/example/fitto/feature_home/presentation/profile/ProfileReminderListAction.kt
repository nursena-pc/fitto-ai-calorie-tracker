package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import java.util.Locale

class ProfileReminderListAction(
    private val loadReminders: () -> List<ProfileReminderItem>,
    private val persistEnabled: (id: String, enabled: Boolean) -> Unit,
    private val deleteReminder: (id: String) -> Unit,
    private val scheduleReminder: (ProfileReminderItem) -> Unit,
    private val cancelReminder: (id: String) -> Unit,
    private val titleSortKey: (String) -> String = { title ->
        title.lowercase(Locale.getDefault())
    }
) {

    fun load(category: ReminderCategory): ProfileReminderListState {
        val items = loadReminders()
            .asSequence()
            .filter { category == ReminderCategory.ALL || it.category == category }
            .sortedWith(
                compareByDescending<ProfileReminderItem> { it.isEnabled }
                    .thenBy(::sortMinute)
                    .thenBy { titleSortKey(it.title) }
            )
            .toList()

        return ProfileReminderListState(
            items = items,
            activeItems = items.filter(ProfileReminderItem::isEnabled)
        )
    }

    fun updateEnabled(
        item: ProfileReminderItem,
        enabled: Boolean
    ) {
        persistEnabled(item.id, enabled)

        if (enabled) {
            scheduleReminder(item.copy(isEnabled = true))
        } else {
            cancelReminder(item.id)
        }
    }

    fun delete(item: ProfileReminderItem) {
        deleteReminder(item.id)
        cancelReminder(item.id)
    }

    private fun sortMinute(item: ProfileReminderItem): Int {
        return if (item.isWaterInterval) item.waterStartMinute else item.minuteOfDay
    }
}

data class ProfileReminderListState(
    val items: List<ProfileReminderItem>,
    val activeItems: List<ProfileReminderItem>
)
