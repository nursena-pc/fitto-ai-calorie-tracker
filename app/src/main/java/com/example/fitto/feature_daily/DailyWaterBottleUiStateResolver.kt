package com.example.fitto.feature_daily

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.example.fitto.R
import com.example.fitto.feature_daily.data.WaterTrackingStore

object DailyWaterBottleUiStateResolver {

    data class RenderState(
        val removeBottle: RemoveBottleState,
        val bottles: List<BottleState>
    )

    data class RemoveBottleState(
        val defaultBottleCount: Int,
        val visibleBottleCount: Int,
        val isEnabled: Boolean,
        @ColorRes val tintColorRes: Int,
        val alpha: Float,
        @StringRes val contentDescriptionRes: Int
    )

    data class BottleState(
        val index: Int,
        val number: Int,
        val isFilled: Boolean,
        @ColorRes val tintColorRes: Int,
        val alpha: Float,
        @StringRes val contentDescriptionRes: Int
    )

    fun resolve(waterState: DailyWaterTrackingCalculator.State): RenderState {
        return RenderState(
            removeBottle = resolveRemoveBottleState(waterState),
            bottles = (0 until waterState.totalBottleCount).map { index ->
                resolveBottleState(
                    index = index,
                    isFilled = index < waterState.filledBottleCount
                )
            }
        )
    }

    private fun resolveRemoveBottleState(
        waterState: DailyWaterTrackingCalculator.State
    ): RemoveBottleState {
        val isEnabled = waterState.totalBottleCount > WaterTrackingStore.MIN_BOTTLE_COUNT

        return RemoveBottleState(
            defaultBottleCount = waterState.defaultBottleCount,
            visibleBottleCount = waterState.totalBottleCount,
            isEnabled = isEnabled,
            tintColorRes = if (isEnabled) R.color.fitto_orange else R.color.fitto_text_secondary,
            alpha = if (isEnabled) ENABLED_REMOVE_ALPHA else DISABLED_REMOVE_ALPHA,
            contentDescriptionRes = R.string.daily_remove_bottle
        )
    }

    private fun resolveBottleState(
        index: Int,
        isFilled: Boolean
    ): BottleState {
        return BottleState(
            index = index,
            number = index + 1,
            isFilled = isFilled,
            tintColorRes = if (isFilled) R.color.fitto_water_blue else R.color.fitto_divider_soft,
            alpha = if (isFilled) FILLED_BOTTLE_ALPHA else EMPTY_BOTTLE_ALPHA,
            contentDescriptionRes = if (isFilled) {
                R.string.daily_water_bottle_filled_cd
            } else {
                R.string.daily_water_bottle_empty_cd
            }
        )
    }

    private const val ENABLED_REMOVE_ALPHA = 1f
    private const val DISABLED_REMOVE_ALPHA = 0.35f
    private const val FILLED_BOTTLE_ALPHA = 1f
    private const val EMPTY_BOTTLE_ALPHA = 0.45f
}
