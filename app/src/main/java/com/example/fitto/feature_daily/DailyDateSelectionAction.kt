package com.example.fitto.feature_daily

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DailyDateSelectionAction(
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    data class PickerState(
        val selectedUtcMillis: Long,
        val latestUtcMillis: Long
    )

    sealed interface PickedDateResult {
        data object FutureDate : PickedDateResult

        data class Selected(
            val date: LocalDate,
            val refreshPlan: DailyDashboardRefreshPlan
        ) : PickedDateResult
    }

    fun pickerState(selectedDate: LocalDate): PickerState {
        return PickerState(
            selectedUtcMillis = selectedDate.toUtcStartOfDayMillis(),
            latestUtcMillis = todayProvider().toUtcStartOfDayMillis()
        )
    }

    fun dateFromUtcMillis(utcMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
    }

    fun selectPickedDate(pickedDate: LocalDate): PickedDateResult {
        return when (
            val result = DailyDateSelectionResolver.resolvePickedDate(
                pickedDate = pickedDate,
                today = todayProvider()
            )
        ) {
            DailyDateSelectionResolver.PickedDateResult.FutureDate -> {
                PickedDateResult.FutureDate
            }

            is DailyDateSelectionResolver.PickedDateResult.Selected -> {
                PickedDateResult.Selected(
                    date = result.selection.date,
                    refreshPlan = DailyDashboardRefreshPlans.SELECTED_DATE_CHANGED
                )
            }
        }
    }

    private fun LocalDate.toUtcStartOfDayMillis(): Long {
        return atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }
}
