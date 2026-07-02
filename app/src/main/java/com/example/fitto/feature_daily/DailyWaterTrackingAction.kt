package com.example.fitto.feature_daily

import java.time.LocalDate

internal class DailyWaterTrackingAction(
    private val getWaterMl: (date: LocalDate) -> Int,
    private val setWaterMl: (date: LocalDate, value: Int) -> Unit,
    private val addWaterMl: (date: LocalDate, amountMl: Int) -> Int,
    private val getVisibleBottleCount: (date: LocalDate, defaultCount: Int) -> Int,
    private val increaseVisibleBottleCount: (
        date: LocalDate,
        defaultCount: Int,
        amount: Int
    ) -> Int,
    private val decreaseVisibleBottleCount: (
        date: LocalDate,
        defaultCount: Int,
        amount: Int
    ) -> Int
) {

    fun load(request: DailyWaterTrackingRequest): DailyWaterTrackingResult {
        val rawWaterMl = getWaterMl(request.date)
        val defaultBottleCount = DailyWaterTrackingCalculator.defaultBottleCount(
            request.waterGoalMl
        )
        val visibleBottleCount = getVisibleBottleCount(
            request.date,
            defaultBottleCount
        )
        val waterState = DailyWaterTrackingCalculator.calculateState(
            rawWaterMl = rawWaterMl,
            waterGoalMl = request.waterGoalMl,
            visibleBottleCount = visibleBottleCount
        )

        if (waterState.normalizedWaterMl != rawWaterMl) {
            setWaterMl(request.date, waterState.normalizedWaterMl)
        }

        return DailyWaterTrackingResult(
            waterState = waterState,
            bottleUiState = DailyWaterBottleUiStateResolver.resolve(waterState)
        )
    }

    fun addWater(date: LocalDate, amountMl: Int): Boolean {
        val normalizedAmount = DailyWaterTrackingCalculator.normalizeAddAmount(amountMl)
        if (normalizedAmount <= 0) return false

        addWaterMl(date, normalizedAmount)
        return true
    }

    fun toggleBottle(date: LocalDate, isFilled: Boolean) {
        val updatedWaterMl = DailyWaterTrackingCalculator.toggleBottleWaterMl(
            rawWaterMl = getWaterMl(date),
            isFilled = isFilled
        )
        setWaterMl(date, updatedWaterMl)
    }

    fun addBottleSlots(
        date: LocalDate,
        waterGoalMl: Int,
        amount: Int = DEFAULT_BOTTLE_SLOT_BATCH
    ) {
        increaseVisibleBottleCount(
            date,
            DailyWaterTrackingCalculator.defaultBottleCount(waterGoalMl),
            amount
        )
    }

    fun removeBottleSlot(
        date: LocalDate,
        state: DailyWaterBottleUiStateResolver.RemoveBottleState
    ): Boolean {
        if (!state.isEnabled) return false

        val canRemoveBottle = DailyWaterTrackingCalculator.canRemoveVisibleBottle(
            visibleBottleCount = state.visibleBottleCount,
            rawWaterMl = getWaterMl(date)
        )
        if (!canRemoveBottle) return false

        decreaseVisibleBottleCount(
            date,
            state.defaultBottleCount,
            1
        )
        return true
    }

    private companion object {
        const val DEFAULT_BOTTLE_SLOT_BATCH = 4
    }
}

internal data class DailyWaterTrackingRequest(
    val date: LocalDate,
    val waterGoalMl: Int
)

internal data class DailyWaterTrackingResult(
    val waterState: DailyWaterTrackingCalculator.State,
    val bottleUiState: DailyWaterBottleUiStateResolver.RenderState
)
