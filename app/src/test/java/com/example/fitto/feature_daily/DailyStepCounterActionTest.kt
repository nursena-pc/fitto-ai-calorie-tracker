package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyStepCounterActionTest {

    private val today = LocalDate.of(2026, 7, 2)

    @Test
    fun `past date publishes cached steps and stops counter`() {
        var stopCount = 0
        var startCount = 0
        val updates = mutableListOf<DailyStepCounterUpdate>()
        val action = action(
            cachedSteps = 1_240,
            onStart = { _, _ -> startCount += 1 },
            onStop = { stopCount += 1 }
        )

        val effect = action.start(
            request = request(selectedDate = today.minusDays(1)),
            onStepsChanged = updates::add
        )

        assertEquals(DailyStepCounterEffect.None, effect)
        assertEquals(listOf(DailyStepCounterUpdate(1_240, false)), updates)
        assertEquals(1, stopCount)
        assertEquals(0, startCount)
    }

    @Test
    fun `missing sensor publishes cached steps and reports unavailable effect`() {
        var started = false
        var stopped = false
        val updates = mutableListOf<DailyStepCounterUpdate>()
        val action = action(
            cachedSteps = 700,
            sensorAvailable = false,
            onStart = { _, _ -> started = true },
            onStop = { stopped = true }
        )

        val effect = action.start(
            request = request(),
            onStepsChanged = updates::add
        )

        assertEquals(DailyStepCounterEffect.SensorUnavailable, effect)
        assertEquals(listOf(DailyStepCounterUpdate(700, false)), updates)
        assertFalse(started)
        assertFalse(stopped)
    }

    @Test
    fun `missing required permission requests permission without starting`() {
        var started = false
        val action = action(
            cachedSteps = 350,
            onStart = { _, _ -> started = true }
        )

        val effect = action.start(
            request = request(hasPermission = false),
            onStepsChanged = {}
        )

        assertEquals(DailyStepCounterEffect.RequestActivityRecognitionPermission, effect)
        assertFalse(started)
    }

    @Test
    fun `granted permission starts selected date and forwards live updates`() {
        var startedDate: LocalDate? = null
        var sensorCallback: ((Long) -> Unit)? = null
        val updates = mutableListOf<DailyStepCounterUpdate>()
        val action = action(
            cachedSteps = 1_000,
            onStart = { date, callback ->
                startedDate = date
                sensorCallback = callback
            }
        )

        val effect = action.start(
            request = request(hasPermission = true),
            onStepsChanged = updates::add
        )
        sensorCallback?.invoke(1_125)

        assertEquals(DailyStepCounterEffect.None, effect)
        assertEquals(today, startedDate)
        assertEquals(
            listOf(
                DailyStepCounterUpdate(1_000, false),
                DailyStepCounterUpdate(1_125, true)
            ),
            updates
        )
    }

    @Test
    fun `permission not required starts even when permission flag is false`() {
        var started = false
        val action = action(
            onStart = { _, _ -> started = true }
        )

        val effect = action.start(
            request = request(
                requiresPermission = false,
                hasPermission = false
            ),
            onStepsChanged = {}
        )

        assertEquals(DailyStepCounterEffect.None, effect)
        assertTrue(started)
    }

    @Test
    fun `negative cached and live steps are clamped`() {
        var sensorCallback: ((Long) -> Unit)? = null
        val updates = mutableListOf<DailyStepCounterUpdate>()
        val action = action(
            cachedSteps = -10,
            onStart = { _, callback -> sensorCallback = callback }
        )

        action.start(
            request = request(),
            onStepsChanged = updates::add
        )
        sensorCallback?.invoke(-20)

        assertEquals(
            listOf(
                DailyStepCounterUpdate(0, false),
                DailyStepCounterUpdate(0, true)
            ),
            updates
        )
    }

    @Test
    fun `stop delegates to counter`() {
        var stopCount = 0
        val action = action(onStop = { stopCount += 1 })

        action.stop()
        action.stop()

        assertEquals(2, stopCount)
    }

    private fun request(
        selectedDate: LocalDate = today,
        requiresPermission: Boolean = true,
        hasPermission: Boolean = true
    ): DailyStepCounterRequest {
        return DailyStepCounterRequest(
            selectedDate = selectedDate,
            today = today,
            requiresActivityRecognitionPermission = requiresPermission,
            hasActivityRecognitionPermission = hasPermission
        )
    }

    private fun action(
        cachedSteps: Long = 0,
        sensorAvailable: Boolean = true,
        onStart: (LocalDate, (Long) -> Unit) -> Unit = { _, _ -> },
        onStop: () -> Unit = {}
    ): DailyStepCounterAction {
        return DailyStepCounterAction(
            getCachedSteps = { cachedSteps },
            isStepCounterAvailable = { sensorAvailable },
            startStepCounter = onStart,
            stopStepCounter = onStop
        )
    }
}
