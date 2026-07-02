package com.example.fitto.feature_daily

import com.example.fitto.feature_sleep.data.SleepLogLocalModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailySleepTrackingActionTest {

    @Test
    fun `load card returns empty when date has no sleep log`() {
        val fake = FakeSleepStore()

        assertEquals(
            DailySleepCardStateResolver.State.Empty,
            fake.action().loadCard(DATE)
        )
    }

    @Test
    fun `start editing uses default selection when log is missing`() {
        val editor = FakeSleepStore().action().startEditing(DATE)

        assertEquals(DATE, editor.date)
        assertEquals(
            DailySleepTimeSelection(
                bedtimeHour = 23,
                bedtimeMinute = 0,
                wakeHour = 7,
                wakeMinute = 0
            ),
            editor.selection
        )
    }

    @Test
    fun `start editing restores stored sleep window`() {
        val stored = sleepLog(
            bedtimeHour = 22,
            bedtimeMinute = 15,
            wakeHour = 6,
            wakeMinute = 30,
            durationMinutes = 495,
            caloriesBurned = 510
        )
        val editor = FakeSleepStore(stored).action().startEditing(DATE)

        assertEquals(
            DailySleepTimeSelection(
                bedtimeHour = 22,
                bedtimeMinute = 15,
                wakeHour = 6,
                wakeMinute = 30
            ),
            editor.selection
        )
    }

    @Test
    fun `time updates keep editor date and update only selected values`() {
        val action = FakeSleepStore().action()
        val initial = action.startEditing(DATE)

        val updated = action.updateWakeTime(
            editor = action.updateBedtime(initial, hour = 21, minute = 45),
            hour = 5,
            minute = 20
        )

        assertEquals(DATE, updated.date)
        assertEquals(
            DailySleepTimeSelection(
                bedtimeHour = 21,
                bedtimeMinute = 45,
                wakeHour = 5,
                wakeMinute = 20
            ),
            updated.selection
        )
    }

    @Test
    fun `preview calculates duration and calories without saving`() {
        val fake = FakeSleepStore()
        val action = fake.action()
        val editor = DailySleepEditorState(
            date = DATE,
            selection = DailySleepTimeSelection(
                bedtimeHour = 23,
                bedtimeMinute = 0,
                wakeHour = 7,
                wakeMinute = 0
            )
        )

        val preview = action.preview(editor, weightKg = 70.0)

        assertEquals(480, preview.durationMinutes)
        assertEquals(532, preview.caloriesBurned)
        assertNull(fake.savedLog)
    }

    @Test
    fun `save persists captured date and returns rendered card state`() {
        val fake = FakeSleepStore()
        val action = fake.action()
        val editor = DailySleepEditorState(
            date = DATE,
            selection = DailySleepTimeSelection(
                bedtimeHour = 22,
                bedtimeMinute = 30,
                wakeHour = 6,
                wakeMinute = 45
            )
        )

        val result = action.save(editor, weightKg = 70.0)

        val expected = sleepLog(
            bedtimeHour = 22,
            bedtimeMinute = 30,
            wakeHour = 6,
            wakeMinute = 45,
            durationMinutes = 495,
            caloriesBurned = 549
        )
        assertEquals(expected, fake.savedLog)
        assertEquals(expected, result.log)
        assertEquals(
            DailySleepCardStateResolver.State.Logged(
                durationMinutes = 495,
                selection = editor.selection
            ),
            result.cardState
        )
    }

    private class FakeSleepStore(
        initialLog: SleepLogLocalModel? = null
    ) {
        private val logs = mutableMapOf<String, SleepLogLocalModel>()
        var savedLog: SleepLogLocalModel? = null
            private set

        init {
            initialLog?.let { logs[it.date] = it }
        }

        fun action(): DailySleepTrackingAction {
            return DailySleepTrackingAction(
                getLogForDate = logs::get,
                saveLog = { log ->
                    savedLog = log
                    logs[log.date] = log
                }
            )
        }
    }

    private companion object {
        const val DATE = "2026-07-02"

        fun sleepLog(
            bedtimeHour: Int,
            bedtimeMinute: Int,
            wakeHour: Int,
            wakeMinute: Int,
            durationMinutes: Int,
            caloriesBurned: Int
        ): SleepLogLocalModel {
            return SleepLogLocalModel(
                date = DATE,
                bedtimeHour = bedtimeHour,
                bedtimeMinute = bedtimeMinute,
                wakeHour = wakeHour,
                wakeMinute = wakeMinute,
                durationMinutes = durationMinutes,
                caloriesBurned = caloriesBurned
            )
        }
    }
}
