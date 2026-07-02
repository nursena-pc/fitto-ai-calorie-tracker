package com.example.fitto.feature_daily

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.example.fitto.R
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate

/** Owns Material Date Picker construction and converts picker output into Daily actions. */
internal class DailyDatePickerCoordinator(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val dateSelectionAction: DailyDateSelectionAction,
    private val onDateSelected: (LocalDate, DailyDashboardRefreshPlan) -> Unit,
    private val onFutureDateSelected: () -> Unit
) {

    fun show(selectedDate: LocalDate) {
        val pickerState = dateSelectionAction.pickerState(selectedDate)
        val constraints = CalendarConstraints.Builder()
            .setEnd(pickerState.latestUtcMillis)
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlay_Fitto_MaterialDatePicker)
            .setTitleText(context.getString(R.string.daily_date_picker_title))
            .setSelection(pickerState.selectedUtcMillis)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selectedUtcMillis ->
            val pickedDate = dateSelectionAction.dateFromUtcMillis(selectedUtcMillis)

            when (val result = dateSelectionAction.selectPickedDate(pickedDate)) {
                DailyDateSelectionAction.PickedDateResult.FutureDate -> {
                    onFutureDateSelected()
                }

                is DailyDateSelectionAction.PickedDateResult.Selected -> {
                    onDateSelected(result.date, result.refreshPlan)
                }
            }
        }

        picker.show(fragmentManager, TAG)
    }

    private companion object {
        const val TAG = "daily_date_picker"
    }
}
