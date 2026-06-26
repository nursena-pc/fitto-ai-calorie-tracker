package com.example.fitto.feature_dietplans

import androidx.core.widget.doAfterTextChanged
import com.example.fitto.databinding.FragmentCreateDietPlanBinding
import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_meal.model.MealType
import com.google.android.material.chip.Chip

class CreateDietPlanInteractionBinder(
    private val binding: FragmentCreateDietPlanBinding
) {

    fun bind(listener: Listener) = with(binding) {
        planNameInput.doAfterTextChanged {
            listener.onPlanNameChanged()
        }

        planDescriptionInput.doAfterTextChanged {
            listener.onPlanDescriptionChanged()
        }

        backButton.setOnClickListener {
            listener.onBackClicked()
        }

        topDraftSaveButton.setOnClickListener {
            listener.onSaveDraftClicked()
        }

        createListButton.setOnClickListener {
            listener.onCreatePlanClicked()
        }

        deleteDraftButton.setOnClickListener {
            listener.onDeleteDraftClicked()
        }

        privacyPrivateCard.setOnClickListener {
            listener.onPrivacySelected(CreatePlanPrivacy.PRIVATE)
        }

        privacyPublicCard.setOnClickListener {
            listener.onPrivacySelected(CreatePlanPrivacy.PUBLIC)
        }

        goalWeightLossCard.setOnClickListener {
            listener.onGoalSelected(CreatePlanGoal.WEIGHT_LOSS)
        }

        goalWeightGainCard.setOnClickListener {
            listener.onGoalSelected(CreatePlanGoal.WEIGHT_GAIN)
        }

        goalMuscleCard.setOnClickListener {
            listener.onGoalSelected(CreatePlanGoal.MUSCLE)
        }

        goalMaintainCard.setOnClickListener {
            listener.onGoalSelected(CreatePlanGoal.MAINTAIN)
        }

        goalBalancedCard.setOnClickListener {
            listener.onGoalSelected(CreatePlanGoal.BALANCED)
        }

        calorieMinusButton.setOnClickListener {
            listener.onCalorieDecreaseClicked()
        }

        caloriePlusButton.setOnClickListener {
            listener.onCalorieIncreaseClicked()
        }

        calorieValueText.setOnClickListener {
            listener.onCalorieValueClicked()
        }

        durationMinusButton.setOnClickListener {
            listener.onDurationDecreaseClicked()
        }

        durationPlusButton.setOnClickListener {
            listener.onDurationIncreaseClicked()
        }

        durationValueText.setOnClickListener {
            listener.onDurationValueClicked()
        }

        addExcludedFoodButton.setOnClickListener {
            listener.onAddExclusionClicked()
        }

        bindPreferenceChip(
            preferenceVegetarianChip,
            CustomDietMealPlanGenerator.PREF_VEGETARIAN,
            listener
        )
        bindPreferenceChip(
            preferenceVeganChip,
            CustomDietMealPlanGenerator.PREF_VEGAN,
            listener
        )
        bindPreferenceChip(
            preferenceGlutenFreeChip,
            CustomDietMealPlanGenerator.PREF_GLUTEN_FREE,
            listener
        )
        bindPreferenceChip(
            preferenceLactoseFreeChip,
            CustomDietMealPlanGenerator.PREF_LACTOSE_FREE,
            listener
        )
        bindPreferenceChip(
            preferenceHighProteinChip,
            CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN,
            listener
        )
        bindPreferenceChip(
            preferenceLowCarbChip,
            CustomDietMealPlanGenerator.PREF_LOW_CARB,
            listener
        )

        bindMealOrderChip(mealBreakfastChip, MealType.BREAKFAST, listener)
        bindMealOrderChip(mealSnackChip, MealType.SNACK, listener)
        bindMealOrderChip(mealLunchChip, MealType.LUNCH, listener)
        bindMealOrderChip(mealDinnerChip, MealType.DINNER, listener)
    }

    private fun bindPreferenceChip(
        chip: Chip,
        preferenceKey: String,
        listener: Listener
    ) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            listener.onPreferenceToggled(preferenceKey, isChecked)
        }
    }

    private fun bindMealOrderChip(
        chip: Chip,
        mealType: MealType,
        listener: Listener
    ) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            listener.onMealTypeToggled(mealType, isChecked)
        }
    }

    interface Listener {
        fun onPlanNameChanged()
        fun onPlanDescriptionChanged()
        fun onBackClicked()
        fun onSaveDraftClicked()
        fun onCreatePlanClicked()
        fun onDeleteDraftClicked()
        fun onPrivacySelected(privacy: CreatePlanPrivacy)
        fun onGoalSelected(goal: CreatePlanGoal)
        fun onCalorieDecreaseClicked()
        fun onCalorieIncreaseClicked()
        fun onCalorieValueClicked()
        fun onDurationDecreaseClicked()
        fun onDurationIncreaseClicked()
        fun onDurationValueClicked()
        fun onAddExclusionClicked()
        fun onPreferenceToggled(preferenceKey: String, isChecked: Boolean)
        fun onMealTypeToggled(mealType: MealType, isChecked: Boolean)
    }
}
