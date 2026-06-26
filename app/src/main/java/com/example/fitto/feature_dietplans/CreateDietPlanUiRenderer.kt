package com.example.fitto.feature_dietplans

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.example.fitto.databinding.FragmentCreateDietPlanBinding
import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_meal.model.MealType
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

data class CreateDietPlanRenderState(
    val dailyCalories: Int,
    val durationDays: Int,
    val selectedGoal: CreatePlanGoal,
    val selectedPrivacy: CreatePlanPrivacy,
    val selectedMealTypes: List<MealType>,
    val selectedPreferences: Set<String>
)

class CreateDietPlanUiRenderer(
    private val context: Context,
    private val binding: FragmentCreateDietPlanBinding
) {

    fun render(state: CreateDietPlanRenderState) = with(binding) {
        calorieValueText.text = state.dailyCalories.toString()
        durationValueText.text = context.getString(
            R.string.create_diet_plan_duration_day_value,
            state.durationDays
        )

        selectedMealCountText.text = context.getString(
            R.string.create_diet_plan_selected_meal_count,
            state.selectedMealTypes.size
        )

        renderGoalCards(state.selectedGoal)
        renderPrivacyCards(state.selectedPrivacy)
        renderPreferenceChips(state.selectedPreferences)
        renderMealOrderChips(state.selectedMealTypes)
        renderPreview(state)
    }

    private fun renderPreferenceChips(selectedPreferences: Set<String>) = with(binding) {
        renderPreferenceChip(
            preferenceVegetarianChip,
            CustomDietMealPlanGenerator.PREF_VEGETARIAN,
            selectedPreferences
        )
        renderPreferenceChip(
            preferenceVeganChip,
            CustomDietMealPlanGenerator.PREF_VEGAN,
            selectedPreferences
        )
        renderPreferenceChip(
            preferenceGlutenFreeChip,
            CustomDietMealPlanGenerator.PREF_GLUTEN_FREE,
            selectedPreferences
        )
        renderPreferenceChip(
            preferenceLactoseFreeChip,
            CustomDietMealPlanGenerator.PREF_LACTOSE_FREE,
            selectedPreferences
        )
        renderPreferenceChip(
            preferenceHighProteinChip,
            CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN,
            selectedPreferences
        )
        renderPreferenceChip(
            preferenceLowCarbChip,
            CustomDietMealPlanGenerator.PREF_LOW_CARB,
            selectedPreferences
        )
    }

    private fun renderPreferenceChip(
        chip: Chip,
        preferenceKey: String,
        selectedPreferences: Set<String>
    ) {
        val selected = preferenceKey in selectedPreferences

        chip.isChecked = selected
        chip.setTextColor(
            color(if (selected) R.color.fitto_green else R.color.fitto_text_dark)
        )
    }

    private fun renderGoalCards(selectedGoal: CreatePlanGoal) = with(binding) {
        renderSelectableCard(
            card = goalWeightLossCard,
            title = goalWeightLossTitle,
            selected = selectedGoal == CreatePlanGoal.WEIGHT_LOSS
        )
        renderSelectableCard(
            card = goalWeightGainCard,
            title = goalWeightGainTitle,
            selected = selectedGoal == CreatePlanGoal.WEIGHT_GAIN
        )
        renderSelectableCard(
            card = goalMuscleCard,
            title = goalMuscleTitle,
            selected = selectedGoal == CreatePlanGoal.MUSCLE
        )
        renderSelectableCard(
            card = goalMaintainCard,
            title = goalMaintainTitle,
            selected = selectedGoal == CreatePlanGoal.MAINTAIN
        )
        renderSelectableCard(
            card = goalBalancedCard,
            title = goalBalancedTitle,
            selected = selectedGoal == CreatePlanGoal.BALANCED
        )
    }

    private fun renderPrivacyCards(selectedPrivacy: CreatePlanPrivacy) = with(binding) {
        renderSelectableCard(
            card = privacyPrivateCard,
            title = privacyPrivateTitle,
            selected = selectedPrivacy == CreatePlanPrivacy.PRIVATE
        )
        renderSelectableCard(
            card = privacyPublicCard,
            title = privacyPublicTitle,
            selected = selectedPrivacy == CreatePlanPrivacy.PUBLIC
        )
    }

    private fun renderSelectableCard(
        card: MaterialCardView,
        title: TextView,
        selected: Boolean
    ) {
        val selectedColor = color(R.color.fitto_green)
        val normalTextColor = color(R.color.fitto_text_dark)

        card.setCardBackgroundColor(
            color(if (selected) R.color.fitto_primary_soft else R.color.fitto_surface)
        )
        card.strokeColor = color(
            if (selected) R.color.fitto_green else R.color.fitto_divider_soft
        )
        card.strokeWidth = if (selected) 2 else 1

        title.setTextColor(
            if (selected) selectedColor else normalTextColor
        )
    }

    private fun renderMealOrderChips(selectedMealTypes: List<MealType>) = with(binding) {
        renderMealChip(mealBreakfastChip, MealType.BREAKFAST, selectedMealTypes)
        renderMealChip(mealSnackChip, MealType.SNACK, selectedMealTypes)
        renderMealChip(mealLunchChip, MealType.LUNCH, selectedMealTypes)
        renderMealChip(mealDinnerChip, MealType.DINNER, selectedMealTypes)
    }

    private fun renderMealChip(
        chip: Chip,
        mealType: MealType,
        selectedMealTypes: List<MealType>
    ) {
        val selected = mealType in selectedMealTypes

        chip.isChecked = selected
        chip.setTextColor(
            color(if (selected) R.color.fitto_green else R.color.fitto_text_dark)
        )
    }

    private fun renderPreview(state: CreateDietPlanRenderState) = with(binding) {
        previewDailyGoalText.text = context.getString(
            R.string.create_diet_plan_preview_daily_goal_dynamic,
            state.dailyCalories
        )

        previewMealCountText.text = context.getString(
            R.string.create_diet_plan_preview_meal_count_dynamic,
            state.selectedMealTypes.size
        )

        previewDurationText.text = context.getString(
            R.string.create_diet_plan_preview_duration_dynamic,
            state.durationDays
        )

        previewGoalText.text = context.getString(
            R.string.create_diet_plan_preview_goal_dynamic,
            context.getString(state.selectedGoal.titleRes)
        )

        previewPrivacyText.text = context.getString(
            R.string.create_diet_plan_preview_privacy_dynamic,
            context.getString(state.selectedPrivacy.titleRes)
        )

        val previewMeals = buildPreviewMeals(state)

        renderPreviewMealText(previewBreakfastText, previewMeals[MealType.BREAKFAST])
        renderPreviewMealText(previewSnackText, previewMeals[MealType.SNACK])
        renderPreviewMealText(previewLunchText, previewMeals[MealType.LUNCH])
        renderPreviewMealText(previewDinnerText, previewMeals[MealType.DINNER])
    }

    private fun renderPreviewMealText(
        textView: TextView,
        meal: PreviewMealUiModel?
    ) {
        if (meal == null) {
            textView.visibility = View.GONE
            return
        }

        textView.visibility = View.VISIBLE
        textView.text = buildString {
            append(meal.icon)
            append('\n')
            append(context.getString(meal.mealType.titleRes))
            append('\n')
            append(meal.percent)
            append('%')
            append('\n')
            append(meal.calories)
            append(" kcal")
        }
    }

    private fun buildPreviewMeals(
        state: CreateDietPlanRenderState
    ): Map<MealType, PreviewMealUiModel> {
        return CreateDietPlanPreviewResolver.buildPreviewMeals(
            selectedMealTypes = state.selectedMealTypes,
            dailyCalories = state.dailyCalories
        )
    }

    private fun color(colorRes: Int): Int {
        return ContextCompat.getColor(context, colorRes)
    }
}
