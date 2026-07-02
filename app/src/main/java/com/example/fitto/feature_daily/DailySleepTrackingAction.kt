package com.example.fitto.feature_daily

import com.example.fitto.feature_sleep.data.SleepLogLocalModel

internal class DailySleepTrackingAction(
    private val getLogForDate: (date: String) -> SleepLogLocalModel?,
    private val saveLog: (log: SleepLogLocalModel) -> Unit
) {

    fun loadCard(date: String): DailySleepCardStateResolver.State {
        return DailySleepCardStateResolver.resolve(getLogForDate(date))
    }

    fun startEditing(date: String): DailySleepEditorState {
        return DailySleepEditorState(
            date = date,
            selection = DailySleepTimeSelection.from(getLogForDate(date))
        )
    }

    fun updateBedtime(
        editor: DailySleepEditorState,
        hour: Int,
        minute: Int
    ): DailySleepEditorState {
        return editor.copy(
            selection = editor.selection.withBedtime(hour, minute)
        )
    }

    fun updateWakeTime(
        editor: DailySleepEditorState,
        hour: Int,
        minute: Int
    ): DailySleepEditorState {
        return editor.copy(
            selection = editor.selection.withWakeTime(hour, minute)
        )
    }

    fun preview(
        editor: DailySleepEditorState,
        weightKg: Double
    ): DailySleepPreview {
        return DailySleepPreview(
            selection = editor.selection,
            durationMinutes = editor.selection.durationMinutes(),
            caloriesBurned = editor.selection.calories(weightKg)
        )
    }

    fun save(
        editor: DailySleepEditorState,
        weightKg: Double
    ): DailySleepSaveResult {
        val log = editor.selection.toLog(
            date = editor.date,
            weightKg = weightKg
        )
        saveLog(log)

        return DailySleepSaveResult(
            log = log,
            cardState = DailySleepCardStateResolver.resolve(log)
        )
    }
}

internal data class DailySleepEditorState(
    val date: String,
    val selection: DailySleepTimeSelection
)

internal data class DailySleepPreview(
    val selection: DailySleepTimeSelection,
    val durationMinutes: Int,
    val caloriesBurned: Int
)

internal data class DailySleepSaveResult(
    val log: SleepLogLocalModel,
    val cardState: DailySleepCardStateResolver.State
)
