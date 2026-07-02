package com.example.fitto.feature_daily

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel

internal class DailyPlannedMealRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context
) {

    fun render(plan: DailyPlannedMealsUiModel) = with(binding) {
        activePlanMetricsContainer.isVisible = true
        showMetricViews()

        val planState = DailyPlannedMealUiStateResolver.resolve(plan)

        activePlanTitleText.text = planState.planTitle
        activePlanBadgeText.text = context.getString(
            R.string.daily_active_plan_day_badge,
            planState.dayNumber,
            planState.durationDays,
            planState.targetCalories
        )

        activePlanIconText.text = planState.planIcon
        activePlanMetricOneLabelText.text = context.getString(
            R.string.daily_next_planned_meal_label
        )
        activePlanMetricTwoLabelText.text = context.getString(
            R.string.daily_plan_status_label
        )
        activePlanMetricTwoValueText.text = context.getString(
            R.string.daily_plan_status_value,
            planState.completedMealCount,
            planState.totalMealCount
        )
        fastingProgress.progress = planState.progressPercent

        when (val nextMealState = planState.nextMealState) {
            DailyPlannedMealUiStateResolver.NextMealState.Completed -> {
                activePlanMetricOneValueText.text = context.getString(
                    R.string.daily_plan_completed_value
                )
                plannedMealFoodsText.text = context.getString(
                    R.string.daily_plan_completed_description
                )
                plannedMealFoodsText.isVisible = true
                applyPlannedMealButton.isVisible = false
            }

            is DailyPlannedMealUiStateResolver.NextMealState.Available -> {
                activePlanMetricOneValueText.text = context.getString(
                    R.string.daily_next_planned_meal_value,
                    context.getString(nextMealState.mealTypeTitleRes),
                    nextMealState.calories
                )
                plannedMealFoodsText.text = buildRecommendationText(
                    mealTitle = nextMealState.mealTitle,
                    foodSummary = nextMealState.foodSummary
                )
                plannedMealFoodsText.isVisible = true
                applyPlannedMealButton.isVisible = true
            }
        }
    }

    private fun showMetricViews() = with(binding) {
        activePlanIconText.isVisible = true
        activePlanDivider.isVisible = true
        activePlanMetricOneLabelText.isVisible = true
        activePlanMetricOneValueText.isVisible = true
        activePlanMetricTwoLabelText.isVisible = true
        activePlanMetricTwoValueText.isVisible = true
    }

    private fun buildRecommendationText(
        mealTitle: String,
        foodSummary: String
    ): SpannableStringBuilder {
        val green = ContextCompat.getColor(context, R.color.fitto_green)

        return SpannableStringBuilder().apply {
            val mealLabelStart = length
            append(context.getString(R.string.daily_meal_recommendation_label))
            setSpan(
                ForegroundColorSpan(green),
                mealLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                mealLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append("\n")
            append(mealTitle)
            append("\n\n")

            val foodLabelStart = length
            append(context.getString(R.string.daily_food_recommendation_label))
            setSpan(
                ForegroundColorSpan(green),
                foodLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                foodLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append("\n")
            append(foodSummary)
        }
    }
}
