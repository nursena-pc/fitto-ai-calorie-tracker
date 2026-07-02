package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderDefaults
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler

class ProfileReminderEditDialogCoordinator {

    companion object {
        const val REQUEST_PICK_REMINDER_SOUND =
            ProfileReminderSoundCoordinator.REQUEST_PICK_REMINDER_SOUND
    }

    private val viewFactory = ProfileReminderEditDialogViewFactory()
    private val soundCoordinator = ProfileReminderSoundCoordinator()

    fun show(
        root: View,
        existingItem: ProfileReminderItem?,
        onSaved: () -> Unit
    ) {
        val context = root.context
        val dialog = android.app.Dialog(context)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(viewFactory.dp(context, 22), viewFactory.dp(context, 18), viewFactory.dp(context, 22), viewFactory.dp(context, 18))
            background = ContextCompat.getDrawable(context, R.drawable.bg_profile_card)
        }

        val editingItem = existingItem ?: ProfileReminderDefaults.newCustomReminder(context)

        var selectedCategory = editingItem.category
        var selectedIcon = editingItem.iconKind
        var selectedMinute = editingItem.minuteOfDay
        var selectedRepeatMode = editingItem.repeatMode
        var selectedNotifyMode = editingItem.notifyMode
        var selectedDays = editingItem.selectedDays.toMutableSet()
        var selectedSoundName = editingItem.soundName.ifBlank {
            context.getString(R.string.reminder_sound_default)
        }
        var selectedCustomSoundUri = editingItem.customSoundUri
        var selectedSoundVolumePercent = editingItem.soundVolumePercent.coerceIn(0, 100)
        var selectedTagText = editingItem.tag
        var selectedWaterStartMinute = editingItem.waterStartMinute
        var selectedWaterEndMinute = editingItem.waterEndMinute
        var selectedWaterInterval = editingItem.waterIntervalMinute
        var selectedIsWaterInterval = editingItem.isWaterInterval
        var refreshAfterDismiss = false

        fun applyNotifyMode(mode: ReminderNotifyMode) {
            selectedNotifyMode = mode

            if (
                mode == ReminderNotifyMode.SILENT ||
                mode == ReminderNotifyMode.VIBRATION_ONLY
            ) {
                soundCoordinator.stopPreview()
            } else {
                soundCoordinator.playPreview(
                    context = context,
                    customUriText = selectedCustomSoundUri,
                    volumePercent = selectedSoundVolumePercent
                )
            }
        }

        val titleText = TextView(context).apply {
            text = if (existingItem == null) {
                context.getString(R.string.reminder_dialog_new_title)
            } else {
                context.getString(R.string.reminder_dialog_edit_title)
            }
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            typeface = Typeface.DEFAULT_BOLD
        }

        val titleInput = android.widget.EditText(context).apply {
            setText(editingItem.title)
            hint = context.getString(R.string.reminder_dialog_title_hint)
            textSize = 16f
            isSingleLine = true
            setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            setHintTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
        }

        val timeRow = viewFactory.createDialogRow(
            context = context,
            title = context.getString(R.string.reminder_dialog_time),
            value = viewFactory.minuteToText(selectedMinute)
        )

        val waterTimeRangeRow = viewFactory.createWaterTimeRangeRow(
            context = context,
            startMinute = selectedWaterStartMinute,
            endMinute = selectedWaterEndMinute,
            onStartClick = { startValueView, endValueView ->
                val hour = selectedWaterStartMinute / 60
                val minute = selectedWaterStartMinute % 60

                android.app.TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMin ->
                        selectedWaterStartMinute = selectedHour * 60 + selectedMin

                        val resolvedEndMinute = ProfileReminderEditorRules.resolveWaterEndAfterStart(
                            startMinute = selectedWaterStartMinute,
                            currentEndMinute = selectedWaterEndMinute,
                            intervalMinute = selectedWaterInterval
                        )

                        if (resolvedEndMinute != selectedWaterEndMinute) {
                            selectedWaterEndMinute = resolvedEndMinute
                            endValueView.text = viewFactory.minuteToText(selectedWaterEndMinute)
                        }

                        startValueView.text = viewFactory.minuteToText(selectedWaterStartMinute)
                    },
                    hour,
                    minute,
                    true
                ).show()
            },
            onEndClick = { endValueView ->
                val hour = selectedWaterEndMinute / 60
                val minute = selectedWaterEndMinute % 60

                android.app.TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMin ->
                        val newEndMinute = selectedHour * 60 + selectedMin

                        selectedWaterEndMinute = ProfileReminderEditorRules.resolveWaterEndSelection(
                            startMinute = selectedWaterStartMinute,
                            selectedEndMinute = newEndMinute,
                            intervalMinute = selectedWaterInterval
                        )

                        endValueView.text = viewFactory.minuteToText(selectedWaterEndMinute)
                    },
                    hour,
                    minute,
                    true
                ).show()
            }
        )

        val intervalRow = viewFactory.createDialogRow(
            context = context,
            title = context.getString(R.string.reminder_dialog_water_interval),
            value = viewFactory.intervalText(context, selectedWaterInterval)
        )

        val tagRow = viewFactory.createTagSelectionRow(
            context = context,
            selectedCategory = selectedCategory,
            onTagChanged = { category, icon, tagText, isWater ->
                selectedCategory = category
                selectedIcon = icon
                selectedTagText = tagText
                selectedIsWaterInterval = isWater

                timeRow.visibility = if (selectedIsWaterInterval) View.GONE else View.VISIBLE
                waterTimeRangeRow.visibility = if (selectedIsWaterInterval) View.VISIBLE else View.GONE
                intervalRow.visibility = if (selectedIsWaterInterval) View.VISIBLE else View.GONE
            }
        )

        val repeatRow = viewFactory.createDialogRow(
            context = context,
            title = context.getString(R.string.reminder_dialog_repeat),
            value = editingItem.repeatText
        )

        val daysRow = viewFactory.createDaySelectionRow(
            context = context,
            selectedDays = selectedDays,
            onDaysChanged = { newDays ->
                selectedDays = newDays.toMutableSet()
                repeatRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                    viewFactory.repeatDisplayText(context, selectedRepeatMode, selectedDays)
            }
        )

        val notifyRow = viewFactory.createNotifyModeSelectionRow(
            context = context,
            selectedMode = selectedNotifyMode,
            onModeChanged = { mode ->
                applyNotifyMode(mode)
            }
        )

        val volumeRow = viewFactory.createVolumeRow(
            context = context,
            initialVolume = selectedSoundVolumePercent,
            onVolumeSelected = { volume ->
                selectedSoundVolumePercent = volume

                if (
                    selectedNotifyMode != ReminderNotifyMode.SILENT &&
                    selectedNotifyMode != ReminderNotifyMode.VIBRATION_ONLY
                ) {
                    soundCoordinator.playPreview(
                        context = context,
                        customUriText = selectedCustomSoundUri,
                        volumePercent = selectedSoundVolumePercent
                    )
                }
            }
        )

        val soundRow = viewFactory.createDialogRow(
            context = context,
            title = context.getString(R.string.reminder_dialog_sound),
            value = selectedSoundName
        )

        timeRow.visibility = if (selectedIsWaterInterval) View.GONE else View.VISIBLE
        waterTimeRangeRow.visibility = if (selectedIsWaterInterval) View.VISIBLE else View.GONE
        intervalRow.visibility = if (selectedIsWaterInterval) View.VISIBLE else View.GONE

        timeRow.setOnClickListener {
            val hour = selectedMinute / 60
            val minute = selectedMinute % 60

            android.app.TimePickerDialog(
                context,
                { _, selectedHour, selectedMin ->
                    selectedMinute = selectedHour * 60 + selectedMin
                    timeRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                        viewFactory.minuteToText(selectedMinute)
                },
                hour,
                minute,
                true
            ).show()
        }

        repeatRow.setOnClickListener {
            val labels = listOf(
                context.getString(R.string.reminder_repeat_daily),
                context.getString(R.string.reminder_repeat_weekdays),
                context.getString(R.string.reminder_repeat_weekends),
                context.getString(R.string.reminder_repeat_custom_days),
                context.getString(R.string.reminder_repeat_weekly),
                context.getString(R.string.reminder_repeat_once)
            )

            android.app.AlertDialog.Builder(context)
                .setTitle(R.string.reminder_dialog_repeat)
                .setItems(labels.toTypedArray()) { _, which ->
                    val repeatSelection = ProfileReminderEditorRules.resolveRepeatSelection(
                        selectedIndex = which,
                        currentDays = selectedDays
                    )
                    selectedRepeatMode = repeatSelection.mode
                    selectedDays = repeatSelection.selectedDays.toMutableSet()

                    repeatRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                        viewFactory.repeatDisplayText(context, selectedRepeatMode, selectedDays)

                    viewFactory.renderDaySelectionRow(
                        container = daysRow,
                        context = context,
                        selectedDays = selectedDays,
                        onDaysChanged = { newDays ->
                            selectedDays = newDays.toMutableSet()
                            repeatRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                                viewFactory.repeatDisplayText(context, selectedRepeatMode, selectedDays)
                        }
                    )
                }
                .show()
        }

        soundRow.setOnClickListener {
            val labels = listOf(
                context.getString(R.string.reminder_sound_default),
                context.getString(R.string.reminder_sound_soft),
                context.getString(R.string.reminder_sound_silent),
                context.getString(R.string.reminder_sound_pick_from_device)
            )

            android.app.AlertDialog.Builder(context)
                .setTitle(R.string.reminder_dialog_sound)
                .setItems(labels.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> {
                            selectedSoundName = labels[0]
                            selectedCustomSoundUri = null
                            val resolvedNotifyMode =
                                ProfileReminderEditorRules.notifyModeForAudibleSound(selectedNotifyMode)
                            if (resolvedNotifyMode != selectedNotifyMode) {
                                selectedNotifyMode = resolvedNotifyMode
                                viewFactory.renderNotifyModeSelectionRow(
                                    notifyRow,
                                    context,
                                    selectedNotifyMode
                                ) { mode ->
                                    applyNotifyMode(mode)
                                }
                            }
                            soundCoordinator.playPreview(
                                context = context,
                                customUriText = null,
                                volumePercent = selectedSoundVolumePercent
                            )
                        }

                        1 -> {
                            selectedSoundName = labels[1]
                            selectedCustomSoundUri = null
                            val resolvedNotifyMode =
                                ProfileReminderEditorRules.notifyModeForAudibleSound(selectedNotifyMode)
                            if (resolvedNotifyMode != selectedNotifyMode) {
                                selectedNotifyMode = resolvedNotifyMode
                                viewFactory.renderNotifyModeSelectionRow(
                                    notifyRow,
                                    context,
                                    selectedNotifyMode
                                ) { mode ->
                                    applyNotifyMode(mode)
                                }
                            }
                            soundCoordinator.playPreview(
                                context = context,
                                customUriText = null,
                                volumePercent = selectedSoundVolumePercent
                            )
                        }

                        2 -> {
                            selectedSoundName = labels[2]
                            selectedCustomSoundUri = null
                            selectedNotifyMode = ReminderNotifyMode.SILENT
                            soundCoordinator.stopPreview()

                            viewFactory.renderNotifyModeSelectionRow(
                                notifyRow,
                                context,
                                selectedNotifyMode
                            ) { mode ->
                                applyNotifyMode(mode)
                            }
                        }

                        3 -> {
                            soundCoordinator.requestSoundPicker(context) { name, uri ->
                                selectedSoundName = name
                                selectedCustomSoundUri = uri
                                soundRow.findViewById<TextView>(R.id.textDialogRowValue).text = name

                                val resolvedNotifyMode =
                                    ProfileReminderEditorRules.notifyModeForAudibleSound(selectedNotifyMode)
                                if (resolvedNotifyMode != selectedNotifyMode) {
                                    selectedNotifyMode = resolvedNotifyMode
                                    viewFactory.renderNotifyModeSelectionRow(
                                        notifyRow,
                                        context,
                                        selectedNotifyMode
                                    ) { mode ->
                                        applyNotifyMode(mode)
                                    }
                                }

                                soundCoordinator.playPreview(
                                    context = context,
                                    customUriText = uri,
                                    volumePercent = selectedSoundVolumePercent
                                )
                            }

                            return@setItems
                        }
                    }

                    soundRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                        selectedSoundName
                }
                .show()
        }

        intervalRow.setOnClickListener {
            val labels = listOf(
                context.getString(R.string.reminder_every_one_hour),
                context.getString(R.string.reminder_every_two_hours),
                context.getString(R.string.reminder_every_three_hours)
            )

            android.app.AlertDialog.Builder(context)
                .setTitle(R.string.reminder_dialog_water_interval)
                .setItems(labels.toTypedArray()) { _, which ->
                    selectedWaterInterval = when (which) {
                        0 -> 60
                        1 -> 120
                        else -> 180
                    }

                    selectedWaterEndMinute = ProfileReminderEditorRules.resolveWaterEndAfterStart(
                        startMinute = selectedWaterStartMinute,
                        currentEndMinute = selectedWaterEndMinute,
                        intervalMinute = selectedWaterInterval
                    )

                    intervalRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                        viewFactory.intervalText(context, selectedWaterInterval)
                }
                .show()
        }

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, viewFactory.dp(context, 18), 0, 0)
        }

        val cancelButton = TextView(context).apply {
            text = context.getString(R.string.cancel)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
            setPadding(viewFactory.dp(context, 14), viewFactory.dp(context, 10), viewFactory.dp(context, 14), viewFactory.dp(context, 10))
        }

        val saveButton = TextView(context).apply {
            text = context.getString(R.string.save)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.fitto_green))
            setPadding(viewFactory.dp(context, 14), viewFactory.dp(context, 10), viewFactory.dp(context, 14), viewFactory.dp(context, 10))
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val newTitle = titleInput.text.toString().trim()
                .ifBlank { context.getString(R.string.reminder_new_default_title) }

            val timeText = if (selectedIsWaterInterval) {
                viewFactory.intervalText(context, selectedWaterInterval)
            } else {
                viewFactory.minuteToText(selectedMinute)
            }

            val repeatText = if (selectedIsWaterInterval) {
                "${viewFactory.minuteToText(selectedWaterStartMinute)} - ${viewFactory.minuteToText(selectedWaterEndMinute)}"
            } else if (selectedRepeatMode == ReminderRepeatMode.ONCE) {
                context.getString(R.string.reminder_repeat_once)
            } else {
                viewFactory.repeatDisplayText(context, selectedRepeatMode, selectedDays)
            }

            val savedItem = ProfileReminderEditorRules.buildSavedItem(
                editingItem = editingItem,
                title = newTitle,
                fallbackTitle = context.getString(R.string.reminder_new_default_title),
                tag = selectedTagText,
                category = selectedCategory,
                iconKind = selectedIcon,
                timeText = timeText,
                repeatText = repeatText,
                minuteOfDay = selectedMinute,
                repeatMode = selectedRepeatMode,
                selectedDays = selectedDays,
                notifyMode = selectedNotifyMode,
                soundName = selectedSoundName,
                fallbackSoundName = context.getString(R.string.reminder_sound_default),
                customSoundUri = selectedCustomSoundUri,
                soundVolumePercent = selectedSoundVolumePercent,
                isWaterInterval = selectedIsWaterInterval,
                waterStartMinute = selectedWaterStartMinute,
                waterEndMinute = selectedWaterEndMinute,
                waterIntervalMinute = selectedWaterInterval
            )

            ProfileReminderSaveAction(
                persistReminder = { item ->
                    ReminderPreferenceStore.upsertReminder(context, item)
                },
                scheduleReminder = { item ->
                    ReminderAlarmScheduler.scheduleReminder(context, item)
                }
            ).execute(savedItem)
            refreshAfterDismiss = true

            dialog.dismiss()
        }

        buttonRow.addView(cancelButton)
        buttonRow.addView(saveButton)

        container.addView(titleText)
        container.addView(titleInput)
        container.addView(tagRow)
        container.addView(timeRow)
        container.addView(waterTimeRangeRow)
        container.addView(intervalRow)
        container.addView(repeatRow)
        container.addView(daysRow)
        container.addView(notifyRow)
        container.addView(volumeRow)
        container.addView(soundRow)
        container.addView(buttonRow)

        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        dialog.setOnDismissListener {
            soundCoordinator.clear()

            if (refreshAfterDismiss) {
                onSaved()
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun clear() {
        soundCoordinator.clear()
    }

    fun handlePickedSound(
        context: Context,
        uri: Uri?
    ) {
        soundCoordinator.handlePickedSound(context, uri)
    }
}
