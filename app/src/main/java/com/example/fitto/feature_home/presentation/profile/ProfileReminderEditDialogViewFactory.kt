package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.core.ui.setUniformCornerRadius
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Locale

class ProfileReminderEditDialogViewFactory {

    fun createDialogRow(
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

    fun createWaterTimeRangeRow(
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

    fun createCompactTimeBox(
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

    fun createTagSelectionRow(
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
                setUniformCornerRadius(dp(context, 16).toFloat())
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

    fun createDaySelectionRow(
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

    fun renderDaySelectionRow(
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

    fun createNotifyModeSelectionRow(
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

    fun renderNotifyModeSelectionRow(
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

    fun createVolumeRow(
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


    fun repeatDisplayText(
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

    fun daysText(
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

    fun minuteToText(minuteOfDay: Int): String {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    fun intervalText(
        context: Context,
        intervalMinute: Int
    ): String {
        return when (intervalMinute) {
            60 -> context.getString(R.string.reminder_every_one_hour)
            180 -> context.getString(R.string.reminder_every_three_hours)
            else -> context.getString(R.string.reminder_every_two_hours)
        }
    }


    fun categoryColorRes(category: ReminderCategory): Int {
        return when (category) {
            ReminderCategory.MEAL -> R.color.reminder_meal_soft
            ReminderCategory.WATER -> R.color.reminder_water_soft
            ReminderCategory.EXERCISE -> R.color.reminder_exercise_soft
            ReminderCategory.WEIGHT -> R.color.reminder_weight_soft
            ReminderCategory.CUSTOM -> R.color.reminder_custom_soft
            ReminderCategory.ALL -> R.color.reminder_chip_all
        }
    }


    fun createCircleDayBackground(
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

    fun createNotifyOptionBackground(
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

    fun createCircleHolderBackground(
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

    fun createCompactBoxBackground(context: Context): GradientDrawable {
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

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }



    private data class NotifyVisualItem(
        val mode: ReminderNotifyMode,
        val iconRes: Int,
        val label: String
    )
}
