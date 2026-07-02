package com.example.fitto.feature_daily

import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_meal.model.MealType

/** Binds the Daily screen's static click targets to semantic callbacks. */
internal class DailyDashboardClickBinder(
    private val binding: FragmentDailyBinding,
    private val callbacks: Callbacks
) {

    data class Callbacks(
        val onDateClick: () -> Unit,
        val onQuickAddClick: () -> Unit,
        val onDietPlansClick: () -> Unit,
        val onApplyTodayPlanClick: () -> Unit,
        val onActivePlanClick: () -> Unit,
        val onApplyPlannedMealClick: () -> Unit,
        val onMealDetailClick: (MealType) -> Unit,
        val onAddMealClick: (MealType) -> Unit,
        val onAddSmallWaterClick: () -> Unit,
        val onAddLargeWaterClick: () -> Unit,
        val onAddBottleSlotClick: () -> Unit,
        val onActivityClick: () -> Unit,
        val onHealthConnectClick: () -> Unit,
        val onSleepClick: () -> Unit,
        val onDailyAiClick: () -> Unit
    )

    fun bind() = with(binding) {
        dateSelectorContainer.setOnClickListener { callbacks.onDateClick() }
        dailySelectedDateText.setOnClickListener { callbacks.onDateClick() }
        dailyQuickAddFab.setOnClickListener { callbacks.onQuickAddClick() }
        dietPlansButton.setOnClickListener { callbacks.onDietPlansClick() }
        applyTodayPlanButton.setOnClickListener { callbacks.onApplyTodayPlanClick() }
        activePlanCard.setOnClickListener { callbacks.onActivePlanClick() }
        applyPlannedMealButton.setOnClickListener { callbacks.onApplyPlannedMealClick() }

        breakfastMealRow.setOnClickListener {
            callbacks.onMealDetailClick(MealType.BREAKFAST)
        }
        snackMealRow.setOnClickListener {
            callbacks.onMealDetailClick(MealType.SNACK)
        }
        lunchMealRow.setOnClickListener {
            callbacks.onMealDetailClick(MealType.LUNCH)
        }
        dinnerMealRow.setOnClickListener {
            callbacks.onMealDetailClick(MealType.DINNER)
        }

        breakfastAddButton.setOnClickListener {
            callbacks.onAddMealClick(MealType.BREAKFAST)
        }
        snackAddButton.setOnClickListener {
            callbacks.onAddMealClick(MealType.SNACK)
        }
        lunchAddButton.setOnClickListener {
            callbacks.onAddMealClick(MealType.LUNCH)
        }
        dinnerAddButton.setOnClickListener {
            callbacks.onAddMealClick(MealType.DINNER)
        }

        addWater250Button.setOnClickListener { callbacks.onAddSmallWaterClick() }
        addWater500Button.setOnClickListener { callbacks.onAddLargeWaterClick() }
        addWaterBottleSlotButton.setOnClickListener { callbacks.onAddBottleSlotClick() }

        activitySummaryCard.setOnClickListener { callbacks.onActivityClick() }
        activitySummaryCard.setOnLongClickListener {
            callbacks.onHealthConnectClick()
            true
        }
        activityBurnedCaloriesText.setOnClickListener {
            callbacks.onHealthConnectClick()
        }
        activityDurationText.setOnClickListener {
            callbacks.onHealthConnectClick()
        }

        sleepCard.setOnClickListener { callbacks.onSleepClick() }
        dailyAiCard.setOnClickListener { callbacks.onDailyAiClick() }
    }

    fun clear() = with(binding) {
        dateSelectorContainer.setOnClickListener(null)
        dailySelectedDateText.setOnClickListener(null)
        dailyQuickAddFab.setOnClickListener(null)
        dietPlansButton.setOnClickListener(null)
        applyTodayPlanButton.setOnClickListener(null)
        activePlanCard.setOnClickListener(null)
        applyPlannedMealButton.setOnClickListener(null)

        breakfastMealRow.setOnClickListener(null)
        snackMealRow.setOnClickListener(null)
        lunchMealRow.setOnClickListener(null)
        dinnerMealRow.setOnClickListener(null)
        breakfastAddButton.setOnClickListener(null)
        snackAddButton.setOnClickListener(null)
        lunchAddButton.setOnClickListener(null)
        dinnerAddButton.setOnClickListener(null)

        addWater250Button.setOnClickListener(null)
        addWater500Button.setOnClickListener(null)
        addWaterBottleSlotButton.setOnClickListener(null)
        activitySummaryCard.setOnClickListener(null)
        activitySummaryCard.setOnLongClickListener(null)
        activityBurnedCaloriesText.setOnClickListener(null)
        activityDurationText.setOnClickListener(null)
        sleepCard.setOnClickListener(null)
        dailyAiCard.setOnClickListener(null)
    }
}
