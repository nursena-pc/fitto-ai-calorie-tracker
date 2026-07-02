package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.profile.view.ReminderClockMarker
import com.example.fitto.feature_home.presentation.profile.view.ReminderClockMarkerCategory
import com.example.fitto.feature_home.presentation.profile.view.ReminderClockMarkerKind
import com.example.fitto.feature_home.presentation.profile.view.ReminderPlanClockView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ProfileRemindersBinder {

    const val REQUEST_PICK_REMINDER_SOUND =
        ProfileReminderEditDialogCoordinator.REQUEST_PICK_REMINDER_SOUND

    private var selectedCategory: ReminderCategory = ReminderCategory.ALL
    private var adapter: ProfileReminderAdapter? = null
    private var lastBackClick: (() -> Unit)? = null
    private val clockStateResolver = ProfileReminderClockStateResolver()
    private val editDialogCoordinator = ProfileReminderEditDialogCoordinator()

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
            showReminderEditor(
                root = root,
                existingItem = null
            )
        }

        val listAction = createListAction(context)
        val listState = listAction.load(selectedCategory)
        val allItems = listState.items
        val activeItems = listState.activeItems

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
                items = listAction.load(selectedCategory).items.toMutableList(),
                listAction = listAction
            )
        }

        bindRecycler(
            root = root,
            recyclerView = recyclerView,
            items = allItems.toMutableList(),
            listAction = listAction
        )
    }

    private fun createListAction(context: Context): ProfileReminderListAction {
        return ProfileReminderListAction(
            loadReminders = { ReminderPreferenceStore.getReminders(context) },
            persistEnabled = { id, enabled ->
                ReminderPreferenceStore.setEnabled(context, id, enabled)
            },
            deleteReminder = { id ->
                ReminderPreferenceStore.deleteReminder(context, id)
            },
            scheduleReminder = { item ->
                ReminderAlarmScheduler.scheduleReminder(context, item)
            },
            cancelReminder = { id ->
                ReminderAlarmScheduler.cancelReminder(context, id)
            }
        )
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

        val clockState = clockStateResolver.resolve(
            reminders = reminders,
            currentMinuteOfDay = minuteOfDay
        )
        val nextEntry = clockState.nextEntry
        val markers = clockState.markers.map { marker ->
            ReminderClockMarker(
                id = marker.id,
                minuteOfDay = marker.minuteOfDay,
                kind = if (marker.isWater) {
                    ReminderClockMarkerKind.WATER
                } else {
                    ReminderClockMarkerKind.NORMAL
                },
                category = markerCategoryFor(marker.category),
                title = marker.title,
                timeText = marker.timeText,
                isPast = marker.isPast,
                isNext = marker.isNext
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
        items: MutableList<ProfileReminderItem>,
        listAction: ProfileReminderListAction
    ) {
        val context = root.context

        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.isNestedScrollingEnabled = false
        }

        val newAdapter = ProfileReminderAdapter(
            items = items,
            onEnabledChanged = { item, enabled ->
                listAction.updateEnabled(item, enabled)

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

                                listAction.delete(removed)

                                bind(
                                    root = root,
                                    onBackClick = lastBackClick ?: {}
                                )
                            }

                            ItemTouchHelper.RIGHT -> {
                                newAdapter.notifyItemChanged(position)

                                recyclerView.postDelayed(
                                    {
                                        showReminderEditor(
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

    private fun showReminderEditor(
        root: View,
        existingItem: ProfileReminderItem?
    ) {
        editDialogCoordinator.show(
            root = root,
            existingItem = existingItem,
            onSaved = {
                bind(
                    root = root,
                    onBackClick = lastBackClick ?: {}
                )
            }
        )
    }

    fun handlePickedSound(
        context: Context,
        uri: Uri?
    ) {
        editDialogCoordinator.handlePickedSound(context, uri)
    }

    fun clear() {
        editDialogCoordinator.clear()
        adapter = null
        lastBackClick = null
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

}
