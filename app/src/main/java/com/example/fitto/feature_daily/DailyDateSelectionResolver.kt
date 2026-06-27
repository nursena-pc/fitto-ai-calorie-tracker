package com.example.fitto.feature_daily

import java.time.LocalDate

object DailyDateSelectionResolver {

    data class Selection(
        val date: LocalDate,
        val dateIso: String = date.toString()
    )

    sealed interface PickedDateResult {
        data class Selected(val selection: Selection) : PickedDateResult
        data object FutureDate : PickedDateResult
    }

    fun resolveEntryDate(
        currentDate: LocalDate,
        entryDate: String?
    ): Selection? {
        val normalizedEntryDate = entryDate
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val resolvedDate = runCatching { LocalDate.parse(normalizedEntryDate) }
            .getOrDefault(currentDate)

        return Selection(resolvedDate)
    }

    fun resolvePickedDate(
        pickedDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): PickedDateResult {
        return if (pickedDate.isAfter(today)) {
            PickedDateResult.FutureDate
        } else {
            PickedDateResult.Selected(Selection(pickedDate))
        }
    }
}
