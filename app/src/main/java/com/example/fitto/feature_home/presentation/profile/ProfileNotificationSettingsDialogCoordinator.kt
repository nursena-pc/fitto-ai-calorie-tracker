package com.example.fitto.feature_home.presentation.profile

import android.app.AlarmManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.example.fitto.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class ProfileNotificationSettingsDialogCoordinator(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val fragmentManager: FragmentManager,
    private val store: ProfileNotificationSettingsStore,
    private val openCustomSoundPicker: () -> Unit,
    private val refreshRows: () -> Unit,
    private val showMessage: (Int) -> Unit,
    private val launchIntent: (Intent) -> Unit,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val canScheduleExactAlarms: () -> Boolean = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)
                ?.canScheduleExactAlarms() == true
        } else {
            false
        }
    }
) {

    private var activeChoiceDialog: Dialog? = null
    private var activeExactAlarmDialog: Dialog? = null
    private var activeTimePicker: MaterialTimePicker? = null

    fun showNotificationSoundDialog() {
        val currentSound = store.notificationSound()

        showChoiceDialog(
            titleResId = R.string.profile_notifications_sound,
            options = listOf(
                ChoiceOption(
                    title = context.getString(R.string.profile_notification_sound_soft),
                    value = ProfileNotificationSound.SOFT,
                    isSelected = currentSound == ProfileNotificationSound.SOFT
                ),
                ChoiceOption(
                    title = context.getString(R.string.profile_notification_sound_classic),
                    value = ProfileNotificationSound.CLASSIC,
                    isSelected = currentSound == ProfileNotificationSound.CLASSIC
                ),
                ChoiceOption(
                    title = context.getString(R.string.profile_notification_sound_nature),
                    value = ProfileNotificationSound.NATURE,
                    isSelected = currentSound == ProfileNotificationSound.NATURE
                ),
                ChoiceOption(
                    title = context.getString(R.string.profile_notification_sound_pick_from_device),
                    value = ProfileNotificationSound.CUSTOM,
                    isSelected = currentSound == ProfileNotificationSound.CUSTOM
                ),
                ChoiceOption(
                    title = context.getString(R.string.profile_notification_sound_silent),
                    value = ProfileNotificationSound.SILENT,
                    isSelected = currentSound == ProfileNotificationSound.SILENT
                )
            ),
            onSelected = { selectedSound ->
                if (selectedSound == ProfileNotificationSound.CUSTOM) {
                    openCustomSoundPicker()
                } else {
                    store.setNotificationSound(selectedSound)
                    refreshRows()
                }
            }
        )
    }

    fun saveCustomNotificationSound(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        store.setCustomNotificationSound(
            uri = uri.toString(),
            displayName = displayNameFor(uri)
        )
        refreshRows()
    }

    fun showSilentHoursDialog() {
        val currentValue = store.silentHours()
        val defaultValue = context.getString(R.string.profile_silent_hours_default)
        val offValue = context.getString(R.string.profile_silent_hours_off)
        val state = ProfileSilentHoursResolver.choiceState(
            currentValue = currentValue,
            defaultValue = defaultValue,
            offValue = offValue
        )

        showChoiceDialog(
            titleResId = R.string.profile_notifications_silent_hours,
            options = listOf(
                ChoiceOption(
                    title = defaultValue,
                    value = defaultValue,
                    isSelected = state.currentValue == defaultValue
                ),
                ChoiceOption(
                    title = context.getString(R.string.profile_silent_hours_choose_range),
                    value = ProfileSilentHoursResolver.CHOOSE_RANGE_VALUE,
                    isSelected = state.customRangeSelected
                ),
                ChoiceOption(
                    title = offValue,
                    value = offValue,
                    isSelected = state.currentValue == offValue
                )
            ),
            onSelected = { selectedValue ->
                if (selectedValue == ProfileSilentHoursResolver.CHOOSE_RANGE_VALUE) {
                    showSilentHoursRangePicker(currentValue)
                } else {
                    store.setSilentHours(selectedValue)
                    refreshRows()
                }
            }
        )
    }

    fun showExactAlarmPermissionDialog() {
        val exactAlarmEnabled = sdkInt < Build.VERSION_CODES.S || canScheduleExactAlarms()

        when (
            ProfileExactAlarmPolicy.resolve(
                sdkInt = sdkInt,
                canScheduleExactAlarms = exactAlarmEnabled
            )
        ) {
            ProfileExactAlarmState.NotRequired -> {
                showMessage(R.string.profile_notifications_exact_alarm_not_required)
            }

            ProfileExactAlarmState.AlreadyEnabled -> {
                showMessage(R.string.profile_notifications_exact_alarm_already_enabled)
            }

            ProfileExactAlarmState.PermissionRequired -> {
                activeExactAlarmDialog?.dismiss()
                activeExactAlarmDialog = MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.reminder_exact_alarm_permission_title)
                    .setMessage(R.string.reminder_exact_alarm_permission_message)
                    .setPositiveButton(R.string.reminder_exact_alarm_permission_action) { _, _ ->
                        store.markExactAlarmPromptSeen()
                        openExactAlarmPermissionSettings()
                    }
                    .setNegativeButton(R.string.reminder_exact_alarm_permission_later) { _, _ ->
                        store.markExactAlarmPromptSeen()
                    }
                    .show()
                    .also { dialog ->
                        dialog.setOnDismissListener {
                            if (activeExactAlarmDialog === dialog) {
                                activeExactAlarmDialog = null
                            }
                        }
                    }
            }
        }
    }

    fun clear() {
        activeChoiceDialog?.dismiss()
        activeChoiceDialog = null
        activeExactAlarmDialog?.dismiss()
        activeExactAlarmDialog = null
        activeTimePicker?.dismissAllowingStateLoss()
        activeTimePicker = null
    }

    private fun showSilentHoursRangePicker(currentValue: String) {
        val range = ProfileSilentHoursResolver.rangeFor(currentValue)

        showTimePicker(
            initialHour = range.startHour,
            initialMinute = range.startMinute,
            title = context.getString(R.string.profile_silent_hours_start_time)
        ) { startHour, startMinute ->
            showTimePicker(
                initialHour = range.endHour,
                initialMinute = range.endMinute,
                title = context.getString(R.string.profile_silent_hours_end_time)
            ) { endHour, endMinute ->
                store.setSilentHours(
                    ProfileSilentHoursResolver.formatRange(
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute
                    )
                )
                refreshRows()
            }
        }
    }

    private fun showTimePicker(
        initialHour: Int,
        initialMinute: Int,
        title: String,
        onTimeSelected: (hour: Int, minute: Int) -> Unit
    ) {
        activeTimePicker?.dismissAllowingStateLoss()

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText(title)
            .setTheme(R.style.ThemeOverlay_Fitto_WhiteTimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }
        picker.addOnDismissListener {
            if (activeTimePicker === picker) {
                activeTimePicker = null
            }
        }

        activeTimePicker = picker
        picker.show(fragmentManager, TIME_PICKER_TAG)
    }

    private fun openExactAlarmPermissionSettings() {
        if (sdkInt < Build.VERSION_CODES.S) return

        val packageUri = Uri.parse("package:${context.packageName}")
        val exactAlarmIntent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            packageUri
        )

        runCatching { launchIntent(exactAlarmIntent) }
            .onFailure {
                runCatching {
                    launchIntent(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            packageUri
                        )
                    )
                }
            }
    }

    private fun displayNameFor(uri: Uri): String {
        val fallback = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.profile_notification_sound_custom)

        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)?.takeIf { it.isNotBlank() } ?: fallback
                } else {
                    fallback
                }
            } ?: fallback
        }.getOrDefault(fallback)
    }

    private fun <T> showChoiceDialog(
        @StringRes titleResId: Int,
        options: List<ChoiceOption<T>>,
        onSelected: (T) -> Unit
    ) {
        activeChoiceDialog?.dismiss()

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogView = inflater.inflate(
            R.layout.dialog_profile_choice,
            null,
            false
        )

        val titleTextView = dialogView.findViewById<TextView>(R.id.textChoiceDialogTitle)
        val optionsLayout = dialogView.findViewById<LinearLayout>(R.id.layoutChoiceOptions)
        val cancelButton = dialogView.findViewById<TextView>(R.id.buttonChoiceDialogCancel)

        titleTextView.setText(titleResId)
        optionsLayout.removeAllViews()

        options.forEach { option ->
            val optionView = inflater.inflate(
                R.layout.item_profile_choice,
                optionsLayout,
                false
            )
            val optionTitle = optionView.findViewById<TextView>(R.id.textChoiceTitle)
            val checkText = optionView.findViewById<TextView>(R.id.textChoiceCheck)

            optionTitle.text = option.title
            checkText.visibility = if (option.isSelected) View.VISIBLE else View.INVISIBLE
            optionView.setOnClickListener {
                onSelected(option.value)
                dialog.dismiss()
            }
            optionsLayout.addView(optionView)
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setOnDismissListener {
            if (activeChoiceDialog === dialog) {
                activeChoiceDialog = null
            }
        }

        activeChoiceDialog = dialog
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private data class ChoiceOption<T>(
        val title: String,
        val value: T,
        val isSelected: Boolean
    )

    private companion object {
        const val TIME_PICKER_TAG = "fitto_silent_hours_time_picker"
    }
}
