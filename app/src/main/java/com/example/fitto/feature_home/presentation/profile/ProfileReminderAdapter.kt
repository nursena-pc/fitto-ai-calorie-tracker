package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.ImageView
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode

class ProfileReminderAdapter(
    private val items: MutableList<ProfileReminderItem>,
    private val onEnabledChanged: (ProfileReminderItem, Boolean) -> Unit,
    private val onItemClick: (ProfileReminderItem) -> Unit
) : RecyclerView.Adapter<ProfileReminderAdapter.ProfileReminderViewHolder>() {

    inner class ProfileReminderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardReminder =
            itemView.findViewById<MaterialCardView>(R.id.cardReminder)

        private val textReminderIcon =
            itemView.findViewById<TextView>(R.id.textReminderIcon)

        private val textReminderTitle =
            itemView.findViewById<TextView>(R.id.textReminderTitle)

        private val textReminderTag =
            itemView.findViewById<TextView>(R.id.textReminderTag)

        private val imageReminderNotifyMode =
            itemView.findViewById<ImageView>(R.id.imageReminderNotifyMode)
        private val textReminderTime =
            itemView.findViewById<TextView>(R.id.textReminderTime)

        private val textReminderRepeat =
            itemView.findViewById<TextView>(R.id.textReminderRepeat)

        private val switchReminder =
            itemView.findViewById<SwitchMaterial>(R.id.switchReminder)

        fun bind(item: ProfileReminderItem) {
            cardReminder.translationX = 0f

            val colorSet = colorsFor(item.category)

            textReminderTitle.text = item.title
            textReminderTag.text = item.tag
            textReminderTime.text = item.timeText
            textReminderRepeat.text = repeatDisplayText(itemView.context, item)
            textReminderIcon.text = iconText(item.iconKind)

            textReminderIcon.setTextColor(colorSet.iconColor)
            textReminderIcon.background = createCircleCategoryBackground(
                context = itemView.context,
                category = item.category
            )

            textReminderTag.setTextColor(colorSet.iconColor)
            textReminderTag.background = createTagBackground(
                context = itemView.context,
                backgroundColor = colorSet.iconBackground
            )
            imageReminderNotifyMode.setImageResource(notifyModeIcon(item.notifyMode))
            imageReminderNotifyMode.imageTintList = ColorStateList.valueOf(colorSet.iconColor)
            imageReminderNotifyMode.background = createTagBackground(
                context = itemView.context,
                backgroundColor = colorSet.iconBackground
            )
            imageReminderNotifyMode.alpha = if (item.isEnabled) 1f else 0.56f
            textReminderTag.isSingleLine = true
            textReminderTag.maxLines = 1
            textReminderTag.includeFontPadding = false

            textReminderTime.isSingleLine = true
            textReminderTime.maxLines = 1
            textReminderTime.includeFontPadding = false
            textReminderTime.textSize = if (item.isWaterInterval) {
                18f
            } else {
                24f
            }

            textReminderRepeat.isSingleLine = true
            textReminderRepeat.maxLines = 1

            textReminderTitle.typeface = Typeface.DEFAULT_BOLD

            switchReminder.setOnCheckedChangeListener(null)
            switchReminder.isChecked = item.isEnabled
            switchReminder.thumbTintList = switchThumbTint(itemView.context)
            switchReminder.trackTintList = switchTrackTint(itemView.context)
            switchReminder.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                onEnabledChanged(item, checked)
            }

            cardReminder.alpha = if (item.isEnabled) 1f else 0.56f

            itemView.setOnClickListener {
                switchReminder.isChecked = !switchReminder.isChecked
            }
        }

        private fun colorsFor(category: ReminderCategory): ReminderCardColors {
            val context = itemView.context

            return when (category) {
                ReminderCategory.MEAL -> ReminderCardColors(
                    iconColor = ContextCompat.getColor(context, R.color.reminder_meal),
                    iconBackground = ContextCompat.getColor(context, R.color.reminder_meal_soft)
                )

                ReminderCategory.WATER -> ReminderCardColors(
                    iconColor = ContextCompat.getColor(context, R.color.reminder_water),
                    iconBackground = ContextCompat.getColor(context, R.color.reminder_water_soft)
                )

                ReminderCategory.EXERCISE -> ReminderCardColors(
                    iconColor = ContextCompat.getColor(context, R.color.reminder_exercise),
                    iconBackground = ContextCompat.getColor(context, R.color.reminder_exercise_soft)
                )

                ReminderCategory.WEIGHT -> ReminderCardColors(
                    iconColor = ContextCompat.getColor(context, R.color.reminder_weight),
                    iconBackground = ContextCompat.getColor(context, R.color.reminder_weight_soft)
                )

                ReminderCategory.CUSTOM,
                ReminderCategory.ALL -> ReminderCardColors(
                    iconColor = ContextCompat.getColor(context, R.color.fitto_green),
                    iconBackground = ContextCompat.getColor(context, R.color.reminder_custom_soft)
                )
            }
        }

        private fun repeatDisplayText(context: Context, item: ProfileReminderItem): String {
            return if (item.repeatMode == ReminderRepeatMode.WEEKLY) {
                daysText(context, item.selectedDays.ifEmpty { setOf(1) })
            } else {
                item.repeatText
            }
        }

        private fun daysText(context: Context, days: Set<Int>): String {
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

        private fun iconText(iconKind: ReminderIconKind): String {
            return when (iconKind) {
                ReminderIconKind.BREAKFAST -> "🍽"
                ReminderIconKind.LUNCH -> "🥗"
                ReminderIconKind.SNACK -> "🍎"
                ReminderIconKind.WATER -> "💧"
                ReminderIconKind.EXERCISE -> "🏃"
                ReminderIconKind.WEIGHT -> "⚖"
                ReminderIconKind.CUSTOM -> "✦"
            }
        }

        private fun notifyModeIcon(mode: ReminderNotifyMode): Int {
            return when (mode) {
                ReminderNotifyMode.SOUND_AND_VIBRATION -> R.drawable.ic_notify_sound_vibration
                ReminderNotifyMode.SOUND_ONLY -> R.drawable.ic_notify_sound
                ReminderNotifyMode.VIBRATION_ONLY -> R.drawable.ic_notify_vibration
                ReminderNotifyMode.SILENT -> R.drawable.ic_notify_silent
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfileReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_reminder, parent, false)

        return ProfileReminderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProfileReminderViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun removeAt(position: Int): ProfileReminderItem {
        val removedItem = items.removeAt(position)
        notifyItemRemoved(position)
        return removedItem
    }

    private fun createCircleCategoryBackground(
        context: Context,
        category: ReminderCategory
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                ContextCompat.getColor(
                    context,
                    when (category) {
                        ReminderCategory.MEAL -> R.color.reminder_meal_soft
                        ReminderCategory.WATER -> R.color.reminder_water_soft
                        ReminderCategory.EXERCISE -> R.color.reminder_exercise_soft
                        ReminderCategory.WEIGHT -> R.color.reminder_weight_soft
                        ReminderCategory.CUSTOM -> R.color.reminder_custom_soft
                        ReminderCategory.ALL -> R.color.reminder_chip_all
                    }
                )
            )
        }
    }

    private fun createTagBackground(
        context: Context,
        backgroundColor: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 14).toFloat()
            setColor(backgroundColor)
        }
    }

    private fun switchThumbTint(context: Context): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                ContextCompat.getColor(context, R.color.fitto_white),
                ContextCompat.getColor(context, R.color.fitto_white)
            )
        )
    }

    private fun switchTrackTint(context: Context): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                ContextCompat.getColor(context, R.color.fitto_green),
                ContextCompat.getColor(context, R.color.profile_stroke)
            )
        )
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private data class ReminderCardColors(
        val iconColor: Int,
        val iconBackground: Int
    )
}
