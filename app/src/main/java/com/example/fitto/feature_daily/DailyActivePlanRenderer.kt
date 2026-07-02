package com.example.fitto.feature_daily

import androidx.core.view.isVisible
import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel

internal class DailyActivePlanRenderer(
    private val binding: FragmentDailyBinding
) {

    sealed interface FastingEffect {
        data class Show(val planId: String) : FastingEffect
        data object Hide : FastingEffect
    }

    fun render(activePlan: ActiveDietPlanUiModel): FastingEffect = with(binding) {
        val planState = DailyBasicActivePlanUiStateResolver.resolve(activePlan)

        activePlanTitleText.text = planState.title
        activePlanBadgeText.text = planState.badge

        val fastingEffect = when (val content = planState.content) {
            DailyBasicActivePlanUiStateResolver.Content.Fasting -> {
                activePlanMetricsContainer.isVisible = false
                FastingEffect.Show(planState.planId)
            }

            is DailyBasicActivePlanUiStateResolver.Content.Metrics -> {
                activePlanMetricsContainer.isVisible = true
                showMetricViews()

                activePlanMetricOneLabelText.text = content.primaryLabel
                activePlanMetricOneValueText.text = content.primaryValue
                activePlanMetricTwoLabelText.text = content.secondaryLabel
                activePlanMetricTwoValueText.text = content.secondaryValue
                FastingEffect.Hide
            }
        }

        plannedMealFoodsText.isVisible = false
        applyPlannedMealButton.isVisible = false
        fastingProgress.progress = planState.progress
        fastingEffect
    }

    private fun showMetricViews() = with(binding) {
        activePlanIconText.isVisible = true
        activePlanDivider.isVisible = true
        activePlanMetricOneLabelText.isVisible = true
        activePlanMetricOneValueText.isVisible = true
        activePlanMetricTwoLabelText.isVisible = true
        activePlanMetricTwoValueText.isVisible = true
    }
}
