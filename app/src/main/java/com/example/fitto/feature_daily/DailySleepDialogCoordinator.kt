package com.example.fitto.feature_daily

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.example.fitto.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/** Owns the sleep editor dialog and its nested time pickers. */
internal class DailySleepDialogCoordinator(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val sleepTrackingAction: DailySleepTrackingAction,
    private val onSaved: (DailySleepCardStateResolver.State) -> Unit
) {

    private var activeDialog: Dialog? = null
    private var activeTimePicker: MaterialTimePicker? = null

    fun show(entryDate: String, weightKg: Double) {
        clear()

        val dialog = Dialog(context)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_sleep_time, null, false)

        dialog.setContentView(view)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val bedtimeCard = view.findViewById<MaterialCardView>(R.id.bedtimeCard)
        val wakeTimeCard = view.findViewById<MaterialCardView>(R.id.wakeTimeCard)
        val bedtimeValueText = view.findViewById<TextView>(R.id.bedtimeValueText)
        val wakeTimeValueText = view.findViewById<TextView>(R.id.wakeTimeValueText)
        val durationResultText = view.findViewById<TextView>(R.id.sleepDurationResultText)
        val caloriesResultText = view.findViewById<TextView>(R.id.sleepCaloriesResultText)
        val cancelButton = view.findViewById<MaterialButton>(R.id.sleepCancelButton)
        val saveButton = view.findViewById<MaterialButton>(R.id.sleepSaveButton)

        var editor = sleepTrackingAction.startEditing(entryDate)

        fun refreshPreview() {
            if (activeDialog !== dialog) return

            val preview = sleepTrackingAction.preview(
                editor = editor,
                weightKg = weightKg
            )
            bedtimeValueText.text = DailyValueFormatter.formatHourMinute(
                preview.selection.bedtimeHour,
                preview.selection.bedtimeMinute
            )
            wakeTimeValueText.text = DailyValueFormatter.formatHourMinute(
                preview.selection.wakeHour,
                preview.selection.wakeMinute
            )
            durationResultText.text = context.getString(
                R.string.sleep_duration_result,
                DailyValueFormatter.formatDuration(preview.durationMinutes)
            )
            caloriesResultText.text = context.getString(
                R.string.sleep_calories_result,
                preview.caloriesBurned
            )
        }

        bedtimeCard.setOnClickListener {
            showTimePicker(
                title = context.getString(R.string.sleep_time_picker_title_bed),
                initialHour = editor.selection.bedtimeHour,
                initialMinute = editor.selection.bedtimeMinute
            ) { hour, minute ->
                if (activeDialog !== dialog) return@showTimePicker
                editor = sleepTrackingAction.updateBedtime(editor, hour, minute)
                refreshPreview()
            }
        }

        wakeTimeCard.setOnClickListener {
            showTimePicker(
                title = context.getString(R.string.sleep_time_picker_title_wake),
                initialHour = editor.selection.wakeHour,
                initialMinute = editor.selection.wakeMinute
            ) { hour, minute ->
                if (activeDialog !== dialog) return@showTimePicker
                editor = sleepTrackingAction.updateWakeTime(editor, hour, minute)
                refreshPreview()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            if (activeDialog !== dialog) return@setOnClickListener

            val result = sleepTrackingAction.save(
                editor = editor,
                weightKg = weightKg
            )
            dialog.dismiss()
            onSaved(result.cardState)
        }
        dialog.setOnDismissListener {
            if (activeDialog === dialog) {
                activeDialog = null
                activeTimePicker?.dismissAllowingStateLoss()
                activeTimePicker = null
            }
        }

        activeDialog = dialog
        refreshPreview()
        dialog.show()
    }

    fun clear() {
        activeTimePicker?.dismissAllowingStateLoss()
        activeTimePicker = null
        activeDialog?.dismiss()
        activeDialog = null
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
            if (activeTimePicker === picker) {
                activeTimePicker = null
            }
        }
        picker.addOnDismissListener {
            if (activeTimePicker === picker) {
                activeTimePicker = null
            }
        }

        activeTimePicker = picker
        picker.show(fragmentManager, TIME_PICKER_TAG)
    }

    private companion object {
        const val TIME_PICKER_TAG = "fitto_time_picker"
    }
}
