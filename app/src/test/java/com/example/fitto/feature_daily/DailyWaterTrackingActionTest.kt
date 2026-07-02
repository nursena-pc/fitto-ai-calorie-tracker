package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.data.WaterTrackingStore
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyWaterTrackingActionTest {

    private val date = LocalDate.of(2026, 7, 2)

    @Test
    fun `load normalizes stored water and builds bottle state`() {
        val fake = FakeWaterStore(
            waterMl = 375,
            visibleBottleCount = 8
        )
        val action = fake.action()

        val result = action.load(
            DailyWaterTrackingRequest(
                date = date,
                waterGoalMl = 2000
            )
        )

        assertEquals(250, fake.waterMl)
        assertEquals(250, result.waterState.normalizedWaterMl)
        assertEquals(1, result.waterState.filledBottleCount)
        assertEquals(8, result.waterState.totalBottleCount)
        assertEquals(8, result.bottleUiState.bottles.size)
    }

    @Test
    fun `add water rejects sub bottle amount and normalizes valid amount`() {
        val fake = FakeWaterStore(waterMl = 500)
        val action = fake.action()

        assertFalse(action.addWater(date, 100))
        assertEquals(500, fake.waterMl)

        assertTrue(action.addWater(date, 620))
        assertEquals(1000, fake.waterMl)
    }

    @Test
    fun `toggle bottle updates water by one bottle`() {
        val fake = FakeWaterStore(waterMl = 500)
        val action = fake.action()

        action.toggleBottle(date, isFilled = true)
        assertEquals(250, fake.waterMl)

        action.toggleBottle(date, isFilled = false)
        assertEquals(500, fake.waterMl)
    }

    @Test
    fun `bottle slots grow in batches and cannot shrink below filled count`() {
        val fake = FakeWaterStore(
            waterMl = 2000,
            visibleBottleCount = 8
        )
        val action = fake.action()

        action.addBottleSlots(
            date = date,
            waterGoalMl = 2000
        )
        assertEquals(12, fake.visibleBottleCount)

        val blockedState = removeState(
            visibleBottleCount = 8,
            defaultBottleCount = 8
        )
        fake.visibleBottleCount = 8
        assertFalse(action.removeBottleSlot(date, blockedState))
        assertEquals(8, fake.visibleBottleCount)

        val removableState = removeState(
            visibleBottleCount = 9,
            defaultBottleCount = 8
        )
        fake.visibleBottleCount = 9
        assertTrue(action.removeBottleSlot(date, removableState))
        assertEquals(8, fake.visibleBottleCount)
    }

    private fun removeState(
        visibleBottleCount: Int,
        defaultBottleCount: Int
    ): DailyWaterBottleUiStateResolver.RemoveBottleState {
        return DailyWaterBottleUiStateResolver.RemoveBottleState(
            defaultBottleCount = defaultBottleCount,
            visibleBottleCount = visibleBottleCount,
            isEnabled = true,
            tintColorRes = 0,
            alpha = 1f,
            contentDescriptionRes = 0
        )
    }

    private class FakeWaterStore(
        var waterMl: Int = 0,
        var visibleBottleCount: Int = WaterTrackingStore.MIN_BOTTLE_COUNT
    ) {
        fun action(): DailyWaterTrackingAction {
            return DailyWaterTrackingAction(
                getWaterMl = { waterMl },
                setWaterMl = { _, value -> waterMl = value },
                addWaterMl = { _, amount ->
                    waterMl += amount
                    waterMl
                },
                getVisibleBottleCount = { _, defaultCount ->
                    visibleBottleCount.takeIf { it > 0 } ?: defaultCount
                },
                increaseVisibleBottleCount = { _, _, amount ->
                    visibleBottleCount += amount
                    visibleBottleCount
                },
                decreaseVisibleBottleCount = { _, _, amount ->
                    visibleBottleCount -= amount
                    visibleBottleCount
                }
            )
        }
    }
}
