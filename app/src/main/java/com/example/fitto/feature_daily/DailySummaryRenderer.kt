package com.example.fitto.feature_daily

import android.content.Context
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding

internal class DailySummaryRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context,
    private val onActivitySourceClick: () -> Unit
) {

    fun render(
        result: DailySummaryLoadResult,
        activitySource: DailyActivityPolicy.Source
    ) {
        val mealCalories = result.mealCalories
        val summaryState = result.summaryState
        val energyBalance = summaryState.energyBalance

        binding.breakfastCaloriesText.text = mealCaloriesText(mealCalories.breakfast)
        binding.snackCaloriesText.text = mealCaloriesText(mealCalories.snack)
        binding.lunchCaloriesText.text = mealCaloriesText(mealCalories.lunch)
        binding.dinnerCaloriesText.text = mealCaloriesText(mealCalories.dinner)

        binding.remainingCaloriesText.text = energyBalance.remainingCalories.toString()
        binding.dailyIntakeText.text = context.getString(
            R.string.daily_summary_intake_value,
            energyBalance.eatenCalories
        )
        binding.dailyBurnedText.text = context.getString(
            R.string.daily_summary_burned_value,
            energyBalance.burnedCalories
        )
        binding.activityBurnedCaloriesText.text = energyBalance.burnedCalories.toString()
        binding.activityStepCountText.text = DailyValueFormatter.formatStepCount(
            summaryState.displaySteps
        )
        binding.activityDurationText.text = context.getString(
            R.string.exercise_duration_value,
            summaryState.activityDurationMinutes
        )
        binding.dailyRemainingText.text = context.getString(
            R.string.daily_summary_remaining_value,
            energyBalance.remainingCalories
        )

        binding.dailyCalorieProgress.progress = summaryState.calorieProgressPercent
        renderMacro(
            currentGrams = summaryState.carbs.currentGrams,
            targetGrams = summaryState.carbs.targetGrams,
            progressPercent = summaryState.carbs.progressPercent,
            setValue = { binding.dailyCarbValueText.text = it },
            setProgress = { binding.dailyCarbProgress.progress = it }
        )
        renderMacro(
            currentGrams = summaryState.protein.currentGrams,
            targetGrams = summaryState.protein.targetGrams,
            progressPercent = summaryState.protein.progressPercent,
            setValue = { binding.dailyProteinValueText.text = it },
            setProgress = { binding.dailyProteinProgress.progress = it }
        )
        renderMacro(
            currentGrams = summaryState.fat.currentGrams,
            targetGrams = summaryState.fat.targetGrams,
            progressPercent = summaryState.fat.progressPercent,
            setValue = { binding.dailyFatValueText.text = it },
            setProgress = { binding.dailyFatProgress.progress = it }
        )

        binding.activitySyncStatusText.text = context.getString(
            DailyActivitySourceTextResolver.resolveStatusTextRes(activitySource)
        )
        binding.activitySyncStatusText.setOnClickListener {
            onActivitySourceClick()
        }
    }

    fun renderSteps(displaySteps: Long) {
        binding.activityStepCountText.text = DailyValueFormatter.formatStepCount(displaySteps)
    }

    private fun mealCaloriesText(calories: Int): String {
        return context.getString(R.string.daily_meal_calories_value, calories)
    }

    private fun renderMacro(
        currentGrams: Double,
        targetGrams: Int,
        progressPercent: Int,
        setValue: (String) -> Unit,
        setProgress: (Int) -> Unit
    ) {
        setValue(
            context.getString(
                R.string.daily_macro_value,
                DailyValueFormatter.formatGram(currentGrams),
                targetGrams
            )
        )
        setProgress(progressPercent)
    }
}
