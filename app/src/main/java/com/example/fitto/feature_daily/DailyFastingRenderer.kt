package com.example.fitto.feature_daily

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding

internal class DailyFastingRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context,
    private val onStart: (planId: String) -> Unit,
    private val onFinish: (planId: String) -> Unit
) {

    private var hourglassAnimator: ObjectAnimator? = null

    fun render(state: DailyFastingCardState) = with(binding) {
        show()

        fastingMealSuggestionText.text = buildString {
            append(context.getString(R.string.daily_fasting_meal_suggestion_title))
            append("\n")
            append(context.getString(state.mealSuggestionRes))
        }

        val countdownState = state.countdownState
        fastingCountdownTitleText.text = context.getString(countdownState.titleRes)
        fastingCountdownText.text = countdownState.messageRes
            ?.let { context.getString(it) }
            ?: countdownState.remainingMillis
                ?.let(DailyValueFormatter::formatRemainingTime)
                .orEmpty()
        startFastingButton.isEnabled = countdownState.isButtonEnabled
        startFastingButton.text = context.getString(countdownState.buttonTextRes)
        startFastingButton.setOnClickListener {
            when (countdownState.buttonAction) {
                DailyFastingUiStateResolver.ButtonAction.START -> onStart(state.planId)
                DailyFastingUiStateResolver.ButtonAction.FINISH -> onFinish(state.planId)
                DailyFastingUiStateResolver.ButtonAction.NONE -> Unit
            }
        }

        if (countdownState.shouldAnimateHourglass) {
            startHourglassAnimation()
        } else {
            stopHourglassAnimation()
        }

        fastingAnalysisTitleText.text = context.getString(state.analysisState.titleRes)
        fastingAnalysisRangeText.text = context.getString(state.analysisState.rangeTitleRes)
        fastingAnalysisBodyText.text = context.getString(state.analysisState.bodyTextRes)
    }

    fun show() = with(binding) {
        fastingActionContainer.isVisible = true
        fastingAnalysisContainer.isVisible = true
        fastingActionContainer.setOnClickListener {
            // Consume taps so the active-plan card does not open plan details.
        }
    }

    fun hide() = with(binding) {
        fastingActionContainer.isVisible = false
        fastingAnalysisContainer.isVisible = false
        fastingActionContainer.setOnClickListener(null)
        startFastingButton.setOnClickListener(null)
        stopHourglassAnimation()
    }

    fun clear() {
        hide()
    }

    private fun startHourglassAnimation() {
        if (hourglassAnimator?.isRunning == true) return

        hourglassAnimator = ObjectAnimator.ofFloat(
            binding.fastingHourglassText,
            View.ROTATION,
            0f,
            180f,
            360f
        ).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        binding.fastingHourglassText.animate()
            .alpha(0.65f)
            .setDuration(700L)
            .withEndAction {
                binding.fastingHourglassText.animate()
                    .alpha(1f)
                    .setDuration(700L)
                    .start()
            }
            .start()
    }

    private fun stopHourglassAnimation() {
        hourglassAnimator?.cancel()
        hourglassAnimator = null
        binding.fastingHourglassText.animate().cancel()
        binding.fastingHourglassText.rotation = 0f
        binding.fastingHourglassText.alpha = 1f
    }
}
