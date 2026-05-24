package com.example.fitto.feature_home.presentation.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderDefaults
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.profile.view.ReminderClockMarker
import com.example.fitto.feature_home.presentation.profile.view.ReminderClockMarkerCategory
import com.example.fitto.feature_home.presentation.profile.view.ReminderClockMarkerKind
import com.example.fitto.feature_home.presentation.profile.view.ReminderPlanClockView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ProfileRemindersBinder {

    const val REQUEST_PICK_REMINDER_SOUND = 7051

    private var selectedCategory: ReminderCategory = ReminderCategory.ALL
    private var adapter: ProfileReminderAdapter? = null
    private var lastBackClick: (() -> Unit)? = null

    private var previewPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var pendingSoundResult: ((name: String, uri: String) -> Unit)? = null

    fun bind(
        root: View,
        onBackClick: () -> Unit,
    ) {
        lastBackClick = onBackClick

        val context = root.context

        val buttonBack = root.findViewById<View>(R.id.buttonReminderBack)
        val buttonAdd = root.findViewById<View>(R.id.fabAddReminder)
        val clockView = root.findViewById<ReminderPlanClockView>(R.id.reminderPlanClockView)
        val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerProfileReminders)
        val textActiveCount = root.findViewById<TextView>(R.id.textReminderActiveCount)

        buttonBack.setOnClickListener { onBackClick() }

        buttonAdd.setOnClickListener {
            showReminderEditDialog(
                root = root,
                existingItem = null
            )
        }

        val allItems = buildItems(root)
        val activeItems = allItems.filter { it.isEnabled }

        textActiveCount.text = context.getString(
            R.string.reminder_today_active_count_format,
            activeItems.size
        )

        bindClock(
            root = root,
            clockView = clockView,
            reminders = activeItems
        )

        bindChips(root = root) {
            bindRecycler(
                root = root,
                recyclerView = recyclerView,
                items = buildItems(root)
            )
        }

        bindRecycler(
            root = root,
            recyclerView = recyclerView,
            items = allItems
        )
    }

    private fun buildItems(root: View): MutableList<ProfileReminderItem> {
        val context = root.context

        return ReminderPreferenceStore.getReminders(context)
            .filter { selectedCategory == ReminderCategory.ALL || it.category == selectedCategory }
            .sortedWith(
                compareByDescending<ProfileReminderItem> { it.isEnabled }
                    .thenBy { reminderSortMinute(it) }
                    .thenBy { it.title.lowercase(Locale.getDefault()) }
            )
            .toMutableList()
    }

    private fun reminderSortMinute(item: ProfileReminderItem): Int {
        return if (item.isWaterInterval) item.waterStartMinute else item.minuteOfDay
    }

    private fun bindClock(
        root: View,
        clockView: ReminderPlanClockView,
        reminders: List<ProfileReminderItem>
    ) {
        val context = root.context
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        val dayText = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
        val dateText = SimpleDateFormat("d MMMM", Locale.getDefault()).format(calendar.time)
        val minuteOfDay = hour * 60 + minute

        val entries = buildClockEntries(reminders)
        val nextEntry = entries.firstOrNull { it.minuteOfDay >= minuteOfDay }
            ?: entries.firstOrNull()

        val markers = entries.map { entry ->
            ReminderClockMarker(
                id = entry.id,
                minuteOfDay = entry.minuteOfDay,
                kind = entry.kind,
                category = markerCategoryFor(entry.category),
                title = entry.title,
                timeText = entry.timeText,
                isPast = entry.minuteOfDay < minuteOfDay,
                isNext = entry.id == nextEntry?.id
            )
        }

        clockView.setOnMarkerSelectedListener { marker ->
            bindSelectedMarkerInfo(
                root = root,
                marker = marker
            )
        }

        bindPlanInfoCard(
            root = root,
            label = context.getString(R.string.reminder_plan_next_label),
            title = nextEntry?.title,
            subtitle = nextEntry?.let { entry ->
                val remainingText = remainingTimeText(
                    context = context,
                    targetMinuteOfDay = entry.minuteOfDay,
                    currentMinuteOfDay = minuteOfDay
                )
                "${entry.timeText} · $remainingText"
            },
            category = nextEntry?.category
        )

        bindSelectedMarkerInfo(
            root = root,
            marker = null
        )

        clockView.submitData(
            timeText = timeText,
            dayText = dayText.replaceFirstChar { it.uppercase() },
            dateText = dateText,
            currentMinuteOfDay = minuteOfDay,
            markers = markers
        )
    }

    private fun buildClockEntries(reminders: List<ProfileReminderItem>): List<ClockEntry> {
        val entries = mutableListOf<ClockEntry>()

        reminders.forEach { item ->
            if (item.isWaterInterval) {
                var waterMinute = item.waterStartMinute

                while (waterMinute <= item.waterEndMinute) {
                    entries.add(
                        ClockEntry(
                            id = "${item.id}_water_$waterMinute",
                            title = item.title,
                            minuteOfDay = waterMinute,
                            timeText = minuteToText(waterMinute),
                            category = ReminderCategory.WATER,
                            kind = ReminderClockMarkerKind.WATER
                        )
                    )
                    waterMinute += item.waterIntervalMinute.coerceAtLeast(30)
                }
            } else {
                entries.add(
                    ClockEntry(
                        id = item.id,
                        title = item.title,
                        minuteOfDay = item.minuteOfDay,
                        timeText = minuteToText(item.minuteOfDay),
                        category = item.category,
                        kind = ReminderClockMarkerKind.NORMAL
                    )
                )
            }
        }

        return entries.sortedBy { it.minuteOfDay }
    }

    private fun bindPlanInfoCard(
        root: View,
        label: String,
        title: String?,
        subtitle: String?,
        category: ReminderCategory?
    ) {
        val context = root.context
        val iconView = root.findViewById<TextView>(R.id.textPlanInfoIcon)
        val labelView = root.findViewById<TextView>(R.id.textPlanInfoLabel)
        val titleView = root.findViewById<TextView>(R.id.textPlanInfoTitle)

        if (title == null || category == null) {
            iconView.text = "•"
            iconView.background = createCircleCategoryBackground(context, ReminderCategory.CUSTOM)
            labelView.text = context.getString(R.string.reminder_plan_next_label)
            titleView.text = context.getString(R.string.reminder_plan_no_active)
            return
        }

        iconView.text = iconTextFor(category)
        iconView.background = createCircleCategoryBackground(context, category)
        labelView.text = label
        titleView.text = if (subtitle.isNullOrBlank()) {
            title
        } else {
            "$title · $subtitle"
        }
    }

    private fun bindSelectedMarkerInfo(
        root: View,
        marker: ReminderClockMarker?
    ) {
        val context = root.context
        val selectedInfoView = root.findViewById<TextView>(R.id.textPlanSelectedInfo)

        if (marker == null) {
            selectedInfoView.visibility = View.INVISIBLE
            selectedInfoView.text = ""
            return
        }

        selectedInfoView.visibility = View.VISIBLE
        selectedInfoView.text = context.getString(
            R.string.reminder_plan_selected_format,
            marker.title,
            marker.timeText
        )
    }

    private fun bindChips(
        root: View,
        onCategoryChanged: () -> Unit
    ) {
        val chips = listOf(
            R.id.chipReminderAll to ReminderCategory.ALL,
            R.id.chipReminderMeal to ReminderCategory.MEAL,
            R.id.chipReminderWater to ReminderCategory.WATER,
            R.id.chipReminderExercise to ReminderCategory.EXERCISE,
            R.id.chipReminderWeight to ReminderCategory.WEIGHT,
            R.id.chipReminderCustom to ReminderCategory.CUSTOM
        )

        chips.forEach { pair ->
            val chip = root.findViewById<Chip>(pair.first)
            val category = pair.second

            chip.isChecked = selectedCategory == category

            chip.setOnClickListener {
                selectedCategory = category
                onCategoryChanged()
            }
        }
    }

    private fun bindRecycler(
        root: View,
        recyclerView: RecyclerView,
        items: MutableList<ProfileReminderItem>
    ) {
        val context = root.context

        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.isNestedScrollingEnabled = false
        }

        val newAdapter = ProfileReminderAdapter(
            items = items,
            onEnabledChanged = { item, enabled ->
                ReminderPreferenceStore.setEnabled(
                    context = context,
                    id = item.id,
                    enabled = enabled
                )

                if (enabled) {
                    ReminderAlarmScheduler.scheduleReminder(
                        context = context,
                        item = item.copy(isEnabled = true)
                    )
                } else {
                    ReminderAlarmScheduler.cancelReminder(
                        context = context,
                        reminderId = item.id
                    )
                }

                bind(
                    root = root,
                    onBackClick = lastBackClick ?: {}
                )
            },
            onItemClick = {
                // Kart tıklaması adapter içinde switch değiştiriyor.
                // Düzenleme sağa kaydırma ile açılıyor.
            }
        )

        recyclerView.adapter = newAdapter
        adapter = newAdapter

        if (recyclerView.itemDecorationCount == 0) {
            recyclerView.addItemDecoration(
                object : RecyclerView.ItemDecoration() {}
            )
        }

        (recyclerView.getTag(R.id.recyclerProfileReminders) as? ItemTouchHelper)
            ?.attachToRecyclerView(null)

        val touchHelper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                ) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean = false

                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int
                    ) {
                        val position = viewHolder.bindingAdapterPosition

                        if (position == RecyclerView.NO_POSITION) return

                        val item = items.getOrNull(position) ?: return

                        when (direction) {
                            ItemTouchHelper.LEFT -> {
                                val removed = newAdapter.removeAt(position)

                                ReminderPreferenceStore.deleteReminder(
                                    context = context,
                                    id = removed.id
                                )

                                ReminderAlarmScheduler.cancelReminder(
                                    context = context,
                                    reminderId = removed.id
                                )

                                bind(
                                    root = root,
                                    onBackClick = lastBackClick ?: {}
                                )
                            }

                            ItemTouchHelper.RIGHT -> {
                                newAdapter.notifyItemChanged(position)

                                recyclerView.postDelayed(
                                    {
                                        showReminderEditDialog(
                                            root = root,
                                            existingItem = item
                                        )
                                    },
                                    180
                                )
                            }
                        }
                    }

                    override fun onChildDraw(
                        c: Canvas,
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        dX: Float,
                        dY: Float,
                        actionState: Int,
                        isCurrentlyActive: Boolean
                    ) {
                        val itemView = viewHolder.itemView
                        val cardView = itemView.findViewById<View>(R.id.cardReminder)

                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

                        val top = itemView.top.toFloat() + cardView.top.toFloat()
                        val bottom = itemView.top.toFloat() + cardView.bottom.toFloat()
                        val centerY = ((top + bottom) / 2f).toInt()

                        val cornerRadius = 28f
                        val iconSize = 54

                        if (dX < 0) {
                            paint.color = ContextCompat.getColor(context, R.color.reminder_delete)

                            val left = itemView.right + dX
                            val right = itemView.right.toFloat()

                            c.drawRoundRect(
                                left,
                                top,
                                right,
                                bottom,
                                cornerRadius,
                                cornerRadius,
                                paint
                            )

                            val icon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_delete)
                            val iconCenterX = itemView.right - 92

                            icon?.setBounds(
                                iconCenterX - iconSize / 2,
                                centerY - iconSize / 2,
                                iconCenterX + iconSize / 2,
                                centerY + iconSize / 2
                            )
                            icon?.draw(c)
                        } else if (dX > 0) {
                            paint.color = ContextCompat.getColor(context, R.color.fitto_green)

                            val left = itemView.left.toFloat()
                            val right = itemView.left + dX

                            c.drawRoundRect(
                                left,
                                top,
                                right,
                                bottom,
                                cornerRadius,
                                cornerRadius,
                                paint
                            )

                            val icon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_edit)
                            val iconCenterX = itemView.left + 92

                            icon?.setBounds(
                                iconCenterX - iconSize / 2,
                                centerY - iconSize / 2,
                                iconCenterX + iconSize / 2,
                                centerY + iconSize / 2
                            )
                            icon?.draw(c)
                        }

                        super.onChildDraw(
                            c,
                            recyclerView,
                            viewHolder,
                            dX,
                            dY,
                            actionState,
                            isCurrentlyActive
                        )
                    }
                }
        )

        touchHelper.attachToRecyclerView(recyclerView)
        recyclerView.setTag(R.id.recyclerProfileReminders, touchHelper)
    }

    private fun showReminderEditDialog(
        root: View,
        existingItem: ProfileReminderItem?
    ) {
        val context = root.context
        val dialog = android.app.Dialog(context)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 22), dp(context, 18), dp(context, 22), dp(context, 18))
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
                stopSoundPreview()
            } else {
                playSoundPreview(
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
            setSingleLine(true)
            setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            setHintTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
        }

        val timeRow = createDialogRow(
            context = context,
            title = context.getString(R.string.reminder_dialog_time),
            value = minuteToText(selectedMinute)
        )

        val waterTimeRangeRow = createWaterTimeRangeRow(
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

                        if (selectedWaterEndMinute <= selectedWaterStartMinute) {
                            selectedWaterEndMinute = (selectedWaterStartMinute + selectedWaterInterval)
                                .coerceAtMost(23 * 60 + 59)

                            endValueView.text = minuteToText(selectedWaterEndMinute)
                        }

                        startValueView.text = minuteToText(selectedWaterStartMinute)
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

                        selectedWaterEndMinute = if (newEndMinute <= selectedWaterStartMinute) {
                            (selectedWaterStartMinute + selectedWaterInterval)
                                .coerceAtMost(23 * 60 + 59)
                        } else {
                            newEndMinute
                        }

                        endValueView.text = minuteToText(selectedWaterEndMinute)
                    },
                    hour,
                    minute,
                    true
                ).show()
            }
        )

        val intervalRow = createDialogRow(
            context = context,
            title = context.getString(R.string.reminder_dialog_water_interval),
            value = intervalText(context, selectedWaterInterval)
        )

        val tagRow = createTagSelectionRow(
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

        val repeatRow = createDialogRow(
            context = context,
            title = context.getString(R.string.reminder_dialog_repeat),
            value = editingItem.repeatText
        )

        val daysRow = createDaySelectionRow(
            context = context,
            selectedDays = selectedDays,
            onDaysChanged = { newDays ->
                selectedDays = newDays.toMutableSet()
                repeatRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                    repeatDisplayText(context, selectedRepeatMode, selectedDays)
            }
        )

        val notifyRow = createNotifyModeSelectionRow(
            context = context,
            selectedMode = selectedNotifyMode,
            onModeChanged = { mode ->
                applyNotifyMode(mode)
            }
        )

        val volumeRow = createVolumeRow(
            context = context,
            initialVolume = selectedSoundVolumePercent,
            onVolumeSelected = { volume ->
                selectedSoundVolumePercent = volume

                if (
                    selectedNotifyMode != ReminderNotifyMode.SILENT &&
                    selectedNotifyMode != ReminderNotifyMode.VIBRATION_ONLY
                ) {
                    playSoundPreview(
                        context = context,
                        customUriText = selectedCustomSoundUri,
                        volumePercent = selectedSoundVolumePercent
                    )
                }
            }
        )

        val soundRow = createDialogRow(
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
                        String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMin)
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
                    selectedRepeatMode = when (which) {
                        1 -> ReminderRepeatMode.WEEKDAYS
                        2 -> ReminderRepeatMode.WEEKENDS
                        4 -> ReminderRepeatMode.WEEKLY
                        5 -> ReminderRepeatMode.ONCE
                        else -> ReminderRepeatMode.DAILY
                    }

                    selectedDays = when (which) {
                        0 -> setOf(1, 2, 3, 4, 5, 6, 7).toMutableSet()
                        1 -> setOf(1, 2, 3, 4, 5).toMutableSet()
                        2 -> setOf(6, 7).toMutableSet()
                        4 -> if (selectedDays.isNotEmpty()) selectedDays else setOf(1).toMutableSet()
                        5 -> emptySet<Int>().toMutableSet()
                        else -> selectedDays
                    }

                    repeatRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                        repeatDisplayText(context, selectedRepeatMode, selectedDays)

                    renderDaySelectionRow(
                        container = daysRow,
                        context = context,
                        selectedDays = selectedDays,
                        onDaysChanged = { newDays ->
                            selectedDays = newDays.toMutableSet()
                            repeatRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                                repeatDisplayText(context, selectedRepeatMode, selectedDays)
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
                            if (
                                selectedNotifyMode == ReminderNotifyMode.SILENT ||
                                selectedNotifyMode == ReminderNotifyMode.VIBRATION_ONLY
                            ) {
                                selectedNotifyMode = ReminderNotifyMode.SOUND_AND_VIBRATION
                                renderNotifyModeSelectionRow(
                                    notifyRow,
                                    context,
                                    selectedNotifyMode
                                ) { mode ->
                                    applyNotifyMode(mode)
                                }
                            }
                            playSoundPreview(
                                context = context,
                                customUriText = null,
                                volumePercent = selectedSoundVolumePercent
                            )
                        }

                        1 -> {
                            selectedSoundName = labels[1]
                            selectedCustomSoundUri = null
                            if (
                                selectedNotifyMode == ReminderNotifyMode.SILENT ||
                                selectedNotifyMode == ReminderNotifyMode.VIBRATION_ONLY
                            ) {
                                selectedNotifyMode = ReminderNotifyMode.SOUND_AND_VIBRATION
                                renderNotifyModeSelectionRow(
                                    notifyRow,
                                    context,
                                    selectedNotifyMode
                                ) { mode ->
                                    applyNotifyMode(mode)
                                }
                            }
                            playSoundPreview(
                                context = context,
                                customUriText = null,
                                volumePercent = selectedSoundVolumePercent
                            )
                        }

                        2 -> {
                            selectedSoundName = labels[2]
                            selectedCustomSoundUri = null
                            selectedNotifyMode = ReminderNotifyMode.SILENT
                            stopSoundPreview()

                            renderNotifyModeSelectionRow(
                                notifyRow,
                                context,
                                selectedNotifyMode
                            ) { mode ->
                                applyNotifyMode(mode)
                            }
                        }

                        3 -> {
                            pendingSoundResult = { name, uri ->
                                selectedSoundName = name
                                selectedCustomSoundUri = uri
                                soundRow.findViewById<TextView>(R.id.textDialogRowValue).text = name

                                if (
                                    selectedNotifyMode == ReminderNotifyMode.SILENT ||
                                    selectedNotifyMode == ReminderNotifyMode.VIBRATION_ONLY
                                ) {
                                    selectedNotifyMode = ReminderNotifyMode.SOUND_AND_VIBRATION
                                    renderNotifyModeSelectionRow(
                                        notifyRow,
                                        context,
                                        selectedNotifyMode
                                    ) { mode ->
                                        applyNotifyMode(mode)
                                    }
                                }

                                playSoundPreview(
                                    context = context,
                                    customUriText = uri,
                                    volumePercent = selectedSoundVolumePercent
                                )
                            }

                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "audio/*"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            }

                            findActivity(context)?.startActivityForResult(
                                intent,
                                REQUEST_PICK_REMINDER_SOUND
                            )

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

                    if (selectedWaterEndMinute <= selectedWaterStartMinute) {
                        selectedWaterEndMinute = (selectedWaterStartMinute + selectedWaterInterval)
                            .coerceAtMost(23 * 60 + 59)
                    }

                    intervalRow.findViewById<TextView>(R.id.textDialogRowValue).text =
                        intervalText(context, selectedWaterInterval)
                }
                .show()
        }

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(context, 18), 0, 0)
        }

        val cancelButton = TextView(context).apply {
            text = context.getString(R.string.cancel)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
            setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
        }

        val saveButton = TextView(context).apply {
            text = context.getString(R.string.save)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.fitto_green))
            setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val newTitle = titleInput.text.toString().trim()
                .ifBlank { context.getString(R.string.reminder_new_default_title) }

            val timeText = if (selectedIsWaterInterval) {
                intervalText(context, selectedWaterInterval)
            } else {
                minuteToText(selectedMinute)
            }

            val repeatText = if (selectedIsWaterInterval) {
                "${minuteToText(selectedWaterStartMinute)} - ${minuteToText(selectedWaterEndMinute)}"
            } else if (selectedRepeatMode == ReminderRepeatMode.ONCE) {
                context.getString(R.string.reminder_repeat_once)
            } else {
                repeatDisplayText(context, selectedRepeatMode, selectedDays)
            }

            val savedItem = editingItem.copy(
                title = newTitle,
                tag = selectedTagText,
                category = selectedCategory,
                iconKind = selectedIcon,
                timeText = timeText,
                repeatText = repeatText,
                minuteOfDay = selectedMinute,
                repeatMode = selectedRepeatMode,
                selectedDays = selectedDays,
                notifyMode = selectedNotifyMode,
                soundName = selectedSoundName.ifBlank {
                    context.getString(R.string.reminder_sound_default)
                },
                customSoundUri = selectedCustomSoundUri,
                soundVolumePercent = selectedSoundVolumePercent,
                vibrationEnabled = selectedNotifyMode == ReminderNotifyMode.SOUND_AND_VIBRATION ||
                        selectedNotifyMode == ReminderNotifyMode.VIBRATION_ONLY,
                isWaterInterval = selectedIsWaterInterval,
                waterStartMinute = selectedWaterStartMinute,
                waterEndMinute = selectedWaterEndMinute,
                waterIntervalMinute = selectedWaterInterval
            )

            ReminderPreferenceStore.upsertReminder(context, savedItem)
            ReminderAlarmScheduler.scheduleReminder(context, savedItem)
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
            stopSoundPreview()
            pendingSoundResult = null

            if (refreshAfterDismiss) {
                bind(
                    root = root,
                    onBackClick = lastBackClick ?: {}
                )
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun handlePickedSound(
        context: Context,
        uri: Uri?
    ) {
        if (uri == null) return

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val displayName = queryDisplayName(context, uri)
            ?: context.getString(R.string.reminder_sound_custom)

        pendingSoundResult?.invoke(displayName, uri.toString())
        pendingSoundResult = null
    }

    private fun queryDisplayName(
        context: Context,
        uri: Uri
    ): String? {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                return it.getString(index)
            }
        }

        return null
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context

        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }

        return null
    }

    private fun createDialogRow(
        context: Context,
        title: String,
        value: String
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 14), 0, 0)
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
        }

        val valueView = TextView(context).apply {
            id = R.id.textDialogRowValue
            text = value
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            setPadding(0, dp(context, 4), 0, dp(context, 8))
        }

        row.addView(titleView)
        row.addView(valueView)

        return row
    }

    private fun createWaterTimeRangeRow(
        context: Context,
        startMinute: Int,
        endMinute: Int,
        onStartClick: (startValueView: TextView, endValueView: TextView) -> Unit,
        onEndClick: (endValueView: TextView) -> Unit
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 14), 0, 0)
        }

        lateinit var startValueView: TextView
        lateinit var endValueView: TextView

        val startBox = createCompactTimeBox(
            context = context,
            title = context.getString(R.string.reminder_dialog_water_start_time),
            value = minuteToText(startMinute),
            onClick = {
                onStartClick(startValueView, endValueView)
            }
        )

        val endBox = createCompactTimeBox(
            context = context,
            title = context.getString(R.string.reminder_dialog_water_end_time),
            value = minuteToText(endMinute),
            onClick = {
                onEndClick(endValueView)
            }
        )

        startValueView = startBox.findViewById(R.id.textCompactTimeValue)
        endValueView = endBox.findViewById(R.id.textCompactTimeValue)

        container.addView(
            startBox,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(context, 8)
            }
        )

        container.addView(
            endBox,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        return container
    }

    private fun createCompactTimeBox(
        context: Context,
        title: String,
        value: String,
        onClick: () -> Unit
    ): LinearLayout {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
            background = createCompactBoxBackground(context)
            setOnClickListener { onClick() }
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
            maxLines = 1
        }

        val valueView = TextView(context).apply {
            id = R.id.textCompactTimeValue
            text = value
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            setPadding(0, dp(context, 4), 0, 0)
        }

        box.addView(titleView)
        box.addView(valueView)

        return box
    }

    private fun createTagSelectionRow(
        context: Context,
        selectedCategory: ReminderCategory,
        onTagChanged: (
            category: ReminderCategory,
            icon: ReminderIconKind,
            tagText: String,
            isWater: Boolean
        ) -> Unit
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 14), 0, 0)
        }

        val title = TextView(context).apply {
            text = context.getString(R.string.reminder_dialog_tag)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
        }

        val chipGroup = ChipGroup(context).apply {
            isSingleSelection = true
            isSelectionRequired = true
            isSingleLine = true
            chipSpacingHorizontal = dp(context, 8)
            chipSpacingVertical = 0

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tags = listOf(
            ReminderCategory.MEAL to context.getString(R.string.reminder_tag_meal),
            ReminderCategory.WATER to context.getString(R.string.reminder_tag_water),
            ReminderCategory.EXERCISE to context.getString(R.string.reminder_tag_exercise),
            ReminderCategory.WEIGHT to context.getString(R.string.reminder_tag_weight),
            ReminderCategory.CUSTOM to context.getString(R.string.reminder_tag_custom)
        )

        tags.forEach { pair ->
            val category = pair.first
            val label = pair.second
            val isChecked = selectedCategory == category

            val chip = Chip(context).apply {
                text = label
                isCheckable = true
                this.isChecked = isChecked
                isCheckedIconVisible = false
                chipCornerRadius = dp(context, 16).toFloat()
                textSize = 13f
                maxLines = 1
                isSingleLine = true

                setTextColor(ContextCompat.getColor(context, R.color.profile_text))
                typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, categoryColorRes(category))
                )

                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (isChecked) R.color.fitto_green else android.R.color.transparent
                    )
                )
                chipStrokeWidth = if (isChecked) dp(context, 2).toFloat() else 0f
            }

            chip.setOnCheckedChangeListener { buttonView, checked ->
                val chipView = buttonView as Chip

                chipView.typeface = if (checked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                chipView.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, categoryColorRes(category))
                )
                chipView.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (checked) R.color.fitto_green else android.R.color.transparent
                    )
                )
                chipView.chipStrokeWidth = if (checked) dp(context, 2).toFloat() else 0f

                if (checked) {
                    val icon = when (category) {
                        ReminderCategory.MEAL -> ReminderIconKind.SNACK
                        ReminderCategory.WATER -> ReminderIconKind.WATER
                        ReminderCategory.EXERCISE -> ReminderIconKind.EXERCISE
                        ReminderCategory.WEIGHT -> ReminderIconKind.WEIGHT
                        ReminderCategory.CUSTOM -> ReminderIconKind.CUSTOM
                        ReminderCategory.ALL -> ReminderIconKind.CUSTOM
                    }

                    onTagChanged(
                        category,
                        icon,
                        label,
                        category == ReminderCategory.WATER
                    )
                }
            }

            chipGroup.addView(chip)
        }

        val scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, dp(context, 4), 0, 0)
            addView(chipGroup)
        }

        container.addView(title)
        container.addView(scrollView)

        return container
    }

    private fun createDaySelectionRow(
        context: Context,
        selectedDays: Set<Int>,
        onDaysChanged: (Set<Int>) -> Unit
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(0, dp(context, 4), 0, 0)
        }

        renderDaySelectionRow(
            container = container,
            context = context,
            selectedDays = selectedDays,
            onDaysChanged = onDaysChanged
        )

        return container
    }

    private fun renderDaySelectionRow(
        container: LinearLayout,
        context: Context,
        selectedDays: Set<Int>,
        onDaysChanged: (Set<Int>) -> Unit
    ) {
        container.removeAllViews()

        val currentSelected = selectedDays.toMutableSet()

        val days = listOf(
            1 to context.getString(R.string.day_monday_initial),
            2 to context.getString(R.string.day_tuesday_initial),
            3 to context.getString(R.string.day_wednesday_initial),
            4 to context.getString(R.string.day_thursday_initial),
            5 to context.getString(R.string.day_friday_initial),
            6 to context.getString(R.string.day_saturday_initial),
            7 to context.getString(R.string.day_sunday_initial)
        )

        days.forEachIndexed { index, pair ->
            val checked = currentSelected.contains(pair.first)

            val dayView = TextView(context).apply {
                text = pair.second
                gravity = Gravity.CENTER
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (checked) R.color.fitto_white else R.color.profile_subtext
                    )
                )
                background = createCircleDayBackground(context, checked)
                layoutParams = LinearLayout.LayoutParams(
                    dp(context, 34),
                    dp(context, 34)
                ).apply {
                    if (index < days.lastIndex) {
                        marginEnd = dp(context, 6)
                    }
                }
            }

            dayView.setOnClickListener {
                if (currentSelected.contains(pair.first)) {
                    currentSelected.remove(pair.first)
                } else {
                    currentSelected.add(pair.first)
                }

                onDaysChanged(currentSelected)

                renderDaySelectionRow(
                    container = container,
                    context = context,
                    selectedDays = currentSelected,
                    onDaysChanged = onDaysChanged
                )
            }

            container.addView(dayView)
        }
    }

    private fun createNotifyModeSelectionRow(
        context: Context,
        selectedMode: ReminderNotifyMode,
        onModeChanged: (ReminderNotifyMode) -> Unit
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 14), 0, 0)
        }

        renderNotifyModeSelectionRow(
            container = container,
            context = context,
            selectedMode = selectedMode,
            onModeChanged = onModeChanged
        )

        return container
    }

    private fun renderNotifyModeSelectionRow(
        container: LinearLayout,
        context: Context,
        selectedMode: ReminderNotifyMode,
        onModeChanged: (ReminderNotifyMode) -> Unit
    ) {
        container.removeAllViews()

        val title = TextView(context).apply {
            text = context.getString(R.string.reminder_dialog_notify_mode)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(context, 8), 0, 0)
        }

        val options = listOf(
            NotifyVisualItem(
                mode = ReminderNotifyMode.SOUND_AND_VIBRATION,
                iconRes = R.drawable.ic_notify_sound_vibration,
                label = context.getString(R.string.reminder_notify_chip_sound_vibration)
            ),
            NotifyVisualItem(
                mode = ReminderNotifyMode.SOUND_ONLY,
                iconRes = R.drawable.ic_notify_sound,
                label = context.getString(R.string.reminder_notify_chip_sound)
            ),
            NotifyVisualItem(
                mode = ReminderNotifyMode.VIBRATION_ONLY,
                iconRes = R.drawable.ic_notify_vibration,
                label = context.getString(R.string.reminder_notify_chip_vibration)
            ),
            NotifyVisualItem(
                mode = ReminderNotifyMode.SILENT,
                iconRes = R.drawable.ic_notify_silent,
                label = context.getString(R.string.reminder_notify_chip_silent)
            )
        )

        options.forEachIndexed { index, item ->
            val optionView = createNotifyModeOptionView(
                context = context,
                item = item,
                selected = item.mode == selectedMode
            ) {
                onModeChanged(item.mode)
                renderNotifyModeSelectionRow(
                    container = container,
                    context = context,
                    selectedMode = item.mode,
                    onModeChanged = onModeChanged
                )
            }

            optionView.layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                if (index < options.lastIndex) {
                    marginEnd = dp(context, 8)
                }
            }

            row.addView(optionView)
        }

        container.addView(title)
        container.addView(row)
    }

    private fun createNotifyModeOptionView(
        context: Context,
        item: NotifyVisualItem,
        selected: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(context, 8), dp(context, 10), dp(context, 8), dp(context, 10))
            background = createNotifyOptionBackground(context, selected)
            setOnClickListener { onClick() }
        }

        val iconHolder = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = createCircleHolderBackground(context, selected)
            layoutParams = LinearLayout.LayoutParams(
                dp(context, 40),
                dp(context, 40)
            )
        }

        val iconView = ImageView(context).apply {
            setImageResource(item.iconRes)
            setColorFilter(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.fitto_green else R.color.profile_subtext
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                dp(context, 20),
                dp(context, 20)
            )
        }

        val labelView = TextView(context).apply {
            text = item.label
            gravity = Gravity.CENTER
            textSize = 11f
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.profile_text else R.color.profile_subtext
                )
            )
            setPadding(0, dp(context, 6), 0, 0)
        }

        iconHolder.addView(iconView)
        wrapper.addView(iconHolder)
        wrapper.addView(labelView)

        return wrapper
    }

    private fun createVolumeRow(
        context: Context,
        initialVolume: Int,
        onVolumeSelected: (Int) -> Unit
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 14), 0, 0)
        }

        val title = TextView(context).apply {
            text = context.getString(
                R.string.reminder_volume_format,
                initialVolume.coerceIn(0, 100)
            )
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
        }

        val seekBar = SeekBar(context).apply {
            max = 100
            progress = initialVolume.coerceIn(0, 100)
        }

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    title.text = context.getString(
                        R.string.reminder_volume_format,
                        progress
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    onVolumeSelected(seekBar?.progress ?: initialVolume)
                }
            }
        )

        container.addView(title)
        container.addView(seekBar)

        return container
    }

    private fun playSoundPreview(
        context: Context,
        customUriText: String?,
        volumePercent: Int
    ) {
        stopSoundPreview()

        val volume = volumePercent.coerceIn(0, 100) / 100f

        val uri = customUriText
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val player = MediaPlayer()
        previewPlayer = player

        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            player.setDataSource(context, uri)
            player.setVolume(volume, volume)

            player.setOnPreparedListener {
                it.start()

                mainHandler.postDelayed(
                    { stopSoundPreview() },
                    1200
                )
            }

            player.setOnCompletionListener {
                stopSoundPreview()
            }

            player.prepareAsync()
        }.onFailure {
            stopSoundPreview()
        }
    }

    private fun stopSoundPreview() {
        runCatching { previewPlayer?.stop() }
        runCatching { previewPlayer?.release() }
        previewPlayer = null
    }

    private fun remainingTimeText(
        context: Context,
        targetMinuteOfDay: Int,
        currentMinuteOfDay: Int
    ): String {
        val minutesInDay = 24 * 60
        val remainingMinutes = ((targetMinuteOfDay - currentMinuteOfDay + minutesInDay) % minutesInDay)
            .takeIf { it > 0 }
            ?: minutesInDay
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> context.getString(
                R.string.reminder_next_remaining_hours_minutes,
                hours,
                minutes
            )
            hours > 0 -> context.getString(R.string.reminder_next_remaining_hours, hours)
            else -> context.getString(R.string.reminder_next_remaining_minutes, minutes)
        }
    }

    private fun repeatDisplayText(
        context: Context,
        mode: ReminderRepeatMode,
        days: Set<Int>
    ): String {
        return when (mode) {
            ReminderRepeatMode.WEEKLY -> daysText(context, days.ifEmpty { setOf(1) })
            ReminderRepeatMode.ONCE -> context.getString(R.string.reminder_repeat_once)
            else -> daysText(context, days)
        }
    }

    private fun daysText(
        context: Context,
        days: Set<Int>
    ): String {
        val sorted = days.sorted()

        return when (sorted) {
            listOf(1, 2, 3, 4, 5, 6, 7) -> context.getString(R.string.reminder_repeat_daily)
            listOf(1, 2, 3, 4, 5) -> context.getString(R.string.reminder_repeat_weekdays)
            listOf(6, 7) -> context.getString(R.string.reminder_repeat_weekends)
            emptyList<Int>() -> context.getString(R.string.reminder_repeat_once)
            else -> sorted.joinToString(", ") { day ->
                when (day) {
                    1 -> context.getString(R.string.day_monday_short)
                    2 -> context.getString(R.string.day_tuesday_short)
                    3 -> context.getString(R.string.day_wednesday_short)
                    4 -> context.getString(R.string.day_thursday_short)
                    5 -> context.getString(R.string.day_friday_short)
                    6 -> context.getString(R.string.day_saturday_short)
                    else -> context.getString(R.string.day_sunday_short)
                }
            }
        }
    }

    private fun minuteToText(minuteOfDay: Int): String {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun intervalText(
        context: Context,
        intervalMinute: Int
    ): String {
        return when (intervalMinute) {
            60 -> context.getString(R.string.reminder_every_one_hour)
            180 -> context.getString(R.string.reminder_every_three_hours)
            else -> context.getString(R.string.reminder_every_two_hours)
        }
    }

    private fun markerCategoryFor(category: ReminderCategory): ReminderClockMarkerCategory {
        return when (category) {
            ReminderCategory.MEAL -> ReminderClockMarkerCategory.MEAL
            ReminderCategory.WATER -> ReminderClockMarkerCategory.WATER
            ReminderCategory.EXERCISE -> ReminderClockMarkerCategory.EXERCISE
            ReminderCategory.WEIGHT -> ReminderClockMarkerCategory.WEIGHT
            ReminderCategory.CUSTOM -> ReminderClockMarkerCategory.CUSTOM
            ReminderCategory.ALL -> ReminderClockMarkerCategory.CUSTOM
        }
    }

    private fun categoryForMarker(category: ReminderClockMarkerCategory): ReminderCategory {
        return when (category) {
            ReminderClockMarkerCategory.MEAL -> ReminderCategory.MEAL
            ReminderClockMarkerCategory.WATER -> ReminderCategory.WATER
            ReminderClockMarkerCategory.EXERCISE -> ReminderCategory.EXERCISE
            ReminderClockMarkerCategory.WEIGHT -> ReminderCategory.WEIGHT
            ReminderClockMarkerCategory.CUSTOM -> ReminderCategory.CUSTOM
        }
    }

    private fun iconTextFor(category: ReminderCategory): String {
        return when (category) {
            ReminderCategory.MEAL -> "🍽"
            ReminderCategory.WATER -> "💧"
            ReminderCategory.EXERCISE -> "🏃"
            ReminderCategory.WEIGHT -> "⚖"
            ReminderCategory.CUSTOM -> "✦"
            ReminderCategory.ALL -> "•"
        }
    }

    private fun categoryColorRes(category: ReminderCategory): Int {
        return when (category) {
            ReminderCategory.MEAL -> R.color.reminder_meal_soft
            ReminderCategory.WATER -> R.color.reminder_water_soft
            ReminderCategory.EXERCISE -> R.color.reminder_exercise_soft
            ReminderCategory.WEIGHT -> R.color.reminder_weight_soft
            ReminderCategory.CUSTOM -> R.color.reminder_custom_soft
            ReminderCategory.ALL -> R.color.reminder_chip_all
        }
    }

    private fun createCircleCategoryBackground(
        context: Context,
        category: ReminderCategory
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, categoryColorRes(category)))
        }
    }

    private fun createCircleDayBackground(
        context: Context,
        selected: Boolean
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.fitto_green else R.color.profile_card_background
                )
            )
            setStroke(
                dp(context, 1),
                ContextCompat.getColor(context, R.color.profile_stroke)
            )
        }
    }

    private fun createNotifyOptionBackground(
        context: Context,
        selected: Boolean
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 16).toFloat()
            setColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.profile_menu_background else R.color.profile_card_background
                )
            )
            setStroke(
                dp(context, if (selected) 2 else 1),
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.fitto_green else R.color.profile_stroke
                )
            )
        }
    }

    private fun createCircleHolderBackground(
        context: Context,
        selected: Boolean
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.reminder_chip_all else R.color.profile_menu_background
                )
            )
        }
    }

    private fun createCompactBoxBackground(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 16).toFloat()
            setColor(ContextCompat.getColor(context, R.color.profile_menu_background))
            setStroke(
                dp(context, 1),
                ContextCompat.getColor(context, R.color.profile_stroke)
            )
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private data class ClockEntry(
        val id: String,
        val title: String,
        val minuteOfDay: Int,
        val timeText: String,
        val category: ReminderCategory,
        val kind: ReminderClockMarkerKind
    )

    private data class NotifyVisualItem(
        val mode: ReminderNotifyMode,
        val iconRes: Int,
        val label: String
    )
}
