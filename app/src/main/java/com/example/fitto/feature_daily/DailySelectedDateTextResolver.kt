package com.example.fitto.feature_daily

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DailySelectedDateTextResolver {

    data class Result(
        val formattedDate: String,
        val isToday: Boolean
    )

    fun resolve(
        selectedDate: LocalDate,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault()
    ): Result {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale)
        return Result(
            formattedDate = selectedDate.format(formatter),
            isToday = selectedDate == today
        )
    }
}
