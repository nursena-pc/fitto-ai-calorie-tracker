package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.data.WaterTrackingStore

object DailyWaterTrackingCalculator {

    data class State(
        val normalizedWaterMl: Int,
        val filledBottleCount: Int,
        val defaultBottleCount: Int,
        val totalBottleCount: Int
    )

    fun calculateState(
        rawWaterMl: Int,
        waterGoalMl: Int,
        visibleBottleCount: Int
    ): State {
        val normalizedWaterMl = DailyMetricCalculator.normalizeWaterToBottleSize(rawWaterMl)
        val filledBottleCount = normalizedWaterMl / WaterTrackingStore.BOTTLE_SIZE_ML
        val defaultBottleCount = defaultBottleCount(waterGoalMl)
        val totalBottleCount = maxOf(
            visibleBottleCount,
            filledBottleCount.coerceAtMost(WaterTrackingStore.MAX_BOTTLE_COUNT)
        ).coerceIn(
            WaterTrackingStore.MIN_BOTTLE_COUNT,
            WaterTrackingStore.MAX_BOTTLE_COUNT
        )

        return State(
            normalizedWaterMl = normalizedWaterMl,
            filledBottleCount = filledBottleCount,
            defaultBottleCount = defaultBottleCount,
            totalBottleCount = totalBottleCount
        )
    }

    fun defaultBottleCount(waterGoalMl: Int): Int {
        return (waterGoalMl / WaterTrackingStore.BOTTLE_SIZE_ML)
            .coerceAtLeast(WaterTrackingStore.MIN_BOTTLE_COUNT)
            .coerceAtMost(WaterTrackingStore.MAX_BOTTLE_COUNT)
    }

    fun normalizeAddAmount(amountMl: Int): Int {
        return (amountMl / WaterTrackingStore.BOTTLE_SIZE_ML) *
            WaterTrackingStore.BOTTLE_SIZE_ML
    }

    fun toggleBottleWaterMl(
        rawWaterMl: Int,
        isFilled: Boolean
    ): Int {
        val currentWaterMl = DailyMetricCalculator.normalizeWaterToBottleSize(rawWaterMl)
        val maxWaterMl = WaterTrackingStore.MAX_BOTTLE_COUNT * WaterTrackingStore.BOTTLE_SIZE_ML

        return if (isFilled) {
            (currentWaterMl - WaterTrackingStore.BOTTLE_SIZE_ML).coerceAtLeast(0)
        } else {
            (currentWaterMl + WaterTrackingStore.BOTTLE_SIZE_ML).coerceAtMost(maxWaterMl)
        }
    }

    fun canRemoveVisibleBottle(
        visibleBottleCount: Int,
        rawWaterMl: Int
    ): Boolean {
        if (visibleBottleCount <= WaterTrackingStore.MIN_BOTTLE_COUNT) return false

        val filledBottleCount = DailyMetricCalculator
            .normalizeWaterToBottleSize(rawWaterMl) / WaterTrackingStore.BOTTLE_SIZE_ML
        val minAllowedByFilled = filledBottleCount.coerceAtLeast(
            WaterTrackingStore.MIN_BOTTLE_COUNT
        )

        return visibleBottleCount > minAllowedByFilled
    }
}
