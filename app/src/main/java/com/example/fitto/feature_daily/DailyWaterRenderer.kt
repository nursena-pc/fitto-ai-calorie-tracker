package com.example.fitto.feature_daily

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding

internal class DailyWaterRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context,
    private val onRemoveBottle: (DailyWaterBottleUiStateResolver.RemoveBottleState) -> Unit,
    private val onToggleBottle: (isFilled: Boolean) -> Unit
) {

    fun render(
        result: DailyWaterTrackingResult,
        waterGoalMl: Int,
        scrollTarget: DailyWaterScrollTarget = DailyWaterScrollTarget.NONE
    ) {
        val waterState = result.waterState
        binding.waterAmountText.text = context.getString(
            R.string.daily_water_value_format,
            waterState.normalizedWaterMl / 1000.0,
            waterGoalMl / 1000.0
        )

        binding.waterBottleContainer.removeAllViews()
        binding.waterBottleContainer.addView(
            createRemoveBottleView(result.bottleUiState.removeBottle)
        )
        result.bottleUiState.bottles.forEach { bottleState ->
            binding.waterBottleContainer.addView(createBottleView(bottleState))
        }

        when (scrollTarget) {
            DailyWaterScrollTarget.NONE -> Unit
            DailyWaterScrollTarget.START -> binding.waterBottleScrollView.post {
                binding.waterBottleScrollView.fullScroll(View.FOCUS_LEFT)
            }
            DailyWaterScrollTarget.END -> binding.waterBottleScrollView.post {
                binding.waterBottleScrollView.fullScroll(View.FOCUS_RIGHT)
            }
        }
    }

    private fun createRemoveBottleView(
        state: DailyWaterBottleUiStateResolver.RemoveBottleState
    ): ImageView {
        return AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginEnd = dp(10)
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
            background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_chip)
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setImageResource(R.drawable.ic_remove)
            imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, state.tintColorRes)
            )
            alpha = state.alpha
            contentDescription = context.getString(state.contentDescriptionRes)
            setOnClickListener {
                if (state.isEnabled) {
                    onRemoveBottle(state)
                }
            }
        }
    }

    private fun createBottleView(
        state: DailyWaterBottleUiStateResolver.BottleState
    ): ImageView {
        return AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(54)).apply {
                marginEnd = dp(10)
            }
            setImageResource(R.drawable.ic_water_bottle)
            imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, state.tintColorRes)
            )
            alpha = state.alpha
            contentDescription = context.getString(
                state.contentDescriptionRes,
                state.number
            )
            setOnClickListener {
                onToggleBottle(state.isFilled)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}

internal enum class DailyWaterScrollTarget {
    NONE,
    START,
    END
}
