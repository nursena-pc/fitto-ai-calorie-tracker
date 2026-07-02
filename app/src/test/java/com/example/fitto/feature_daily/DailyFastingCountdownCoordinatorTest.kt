package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyFastingCountdownCoordinatorTest {

    @Test
    fun `start creates and starts a one second interval timer`() {
        val factory = FakeTimerFactory()
        val coordinator = createCoordinator(factory)

        coordinator.start(
            planId = "fasting",
            remainingMillis = 25_000L
        )

        val timer = factory.timers.single()
        assertEquals(25_000L, timer.request.durationMillis)
        assertEquals(1_000L, timer.request.intervalMillis)
        assertEquals(1, timer.startCount)
    }

    @Test
    fun `starting a new timer cancels the previous timer`() {
        val factory = FakeTimerFactory()
        val coordinator = createCoordinator(factory)

        coordinator.start("first", 10_000L)
        coordinator.start("second", 20_000L)

        assertEquals(1, factory.timers[0].cancelCount)
        assertEquals(1, factory.timers[1].startCount)
    }

    @Test
    fun `current timer forwards ticks with plan id and clamps negative time`() {
        val factory = FakeTimerFactory()
        val ticks = mutableListOf<Pair<String, Long>>()
        val coordinator = createCoordinator(
            factory = factory,
            onTick = { planId, remainingMillis ->
                ticks += planId to remainingMillis
            }
        )

        coordinator.start("fasting", 10_000L)
        factory.timers.single().tick(-5L)

        assertEquals(listOf("fasting" to 0L), ticks)
    }

    @Test
    fun `callbacks from replaced timer are ignored`() {
        val factory = FakeTimerFactory()
        val ticks = mutableListOf<Pair<String, Long>>()
        val finishes = mutableListOf<String>()
        val coordinator = createCoordinator(
            factory = factory,
            onTick = { planId, remainingMillis ->
                ticks += planId to remainingMillis
            },
            onFinish = finishes::add
        )

        coordinator.start("first", 10_000L)
        val oldTimer = factory.timers.single()
        coordinator.start("second", 20_000L)

        oldTimer.tick(5_000L)
        oldTimer.finish()

        assertTrue(ticks.isEmpty())
        assertTrue(finishes.isEmpty())
    }

    @Test
    fun `stop cancels timer and ignores later callbacks`() {
        val factory = FakeTimerFactory()
        val ticks = mutableListOf<Pair<String, Long>>()
        val finishes = mutableListOf<String>()
        val coordinator = createCoordinator(
            factory = factory,
            onTick = { planId, remainingMillis ->
                ticks += planId to remainingMillis
            },
            onFinish = finishes::add
        )

        coordinator.start("fasting", 10_000L)
        val timer = factory.timers.single()
        coordinator.stop()
        timer.tick(2_000L)
        timer.finish()

        assertEquals(1, timer.cancelCount)
        assertTrue(ticks.isEmpty())
        assertTrue(finishes.isEmpty())
    }

    @Test
    fun `current timer finish is forwarded once and later callbacks are ignored`() {
        val factory = FakeTimerFactory()
        val ticks = mutableListOf<Pair<String, Long>>()
        val finishes = mutableListOf<String>()
        val coordinator = createCoordinator(
            factory = factory,
            onTick = { planId, remainingMillis ->
                ticks += planId to remainingMillis
            },
            onFinish = finishes::add
        )

        coordinator.start("fasting", 10_000L)
        val timer = factory.timers.single()
        timer.finish()
        timer.tick(1_000L)
        timer.finish()

        assertEquals(listOf("fasting"), finishes)
        assertTrue(ticks.isEmpty())
    }

    @Test
    fun `non positive duration does not create a timer`() {
        val factory = FakeTimerFactory()
        val coordinator = createCoordinator(factory)

        coordinator.start("fasting", 0L)

        assertTrue(factory.timers.isEmpty())
    }

    private fun createCoordinator(
        factory: FakeTimerFactory,
        onTick: (String, Long) -> Unit = { _, _ -> },
        onFinish: (String) -> Unit = {}
    ): DailyFastingCountdownCoordinator {
        return DailyFastingCountdownCoordinator(
            timerFactory = factory,
            onTick = onTick,
            onFinish = onFinish
        )
    }

    private class FakeTimerFactory : DailyCountdownTimerFactory {
        val timers = mutableListOf<FakeTimer>()

        override fun create(request: DailyCountdownTimerRequest): DailyCountdownTimer {
            return FakeTimer(request).also(timers::add)
        }
    }

    private class FakeTimer(
        val request: DailyCountdownTimerRequest
    ) : DailyCountdownTimer {
        var startCount: Int = 0
        var cancelCount: Int = 0

        override fun start() {
            startCount += 1
        }

        override fun cancel() {
            cancelCount += 1
        }

        fun tick(remainingMillis: Long) {
            request.onTick(remainingMillis)
        }

        fun finish() {
            request.onFinish()
        }
    }
}
