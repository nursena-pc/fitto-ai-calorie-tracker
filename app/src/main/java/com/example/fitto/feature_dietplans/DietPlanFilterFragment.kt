package com.example.fitto.feature_dietplans

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDietPlanFilterBinding
import com.example.fitto.feature_dietplans.DietPlanFilterOptions as FilterOptions
import com.google.android.material.button.MaterialButton

class DietPlanFilterFragment : Fragment(R.layout.fragment_diet_plan_filter) {

    private var _binding: FragmentDietPlanFilterBinding? = null
    private val binding get() = _binding!!

    private val filterStateStore = DietPlanFilterStateStore()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDietPlanFilterBinding.bind(view)

        setupInitialUi()
        setupClicks()
        setupCalorieSlider()
    }

    private fun setupInitialUi() = with(binding) {
        calorieRangeSlider.valueFrom = 800f
        calorieRangeSlider.valueTo = 3000f
        calorieRangeSlider.stepSize = 100f
        val state = filterStateStore.current()
        calorieRangeSlider.values = listOf(state.minCalories.toFloat(), state.maxCalories.toFloat())

        updateCalorieTexts()

        selectSingleButton(
            buttons = listOf(
                goalWeightLossButton,
                goalWeightGainButton,
                goalMuscleButton,
                goalBalancedButton
            ),
            selectedButton = goalButtonFor(state.goal)
        )

        selectSingleButton(
            buttons = listOf(
                duration7Button,
                duration14Button,
                duration21Button,
                duration30Button
            ),
            selectedButton = durationButtonFor(state.duration)
        )

        selectSingleButton(
            buttons = listOf(
                typeMediterraneanButton,
                typeDashButton,
                typeFastingButton,
                typeKetoButton,
                typePlantBasedButton
            ),
            selectedButton = typeButtonFor(state.type)
        )

        setToggleButtonSelected(featureHeartButton, FilterOptions.FEATURE_HEART_FRIENDLY in state.features)
        setToggleButtonSelected(featureLowSodiumButton, FilterOptions.FEATURE_LOW_SODIUM in state.features)
        setToggleButtonSelected(featureHighProteinButton, FilterOptions.FEATURE_HIGH_PROTEIN in state.features)
        setToggleButtonSelected(featureEasyButton, FilterOptions.FEATURE_EASY in state.features)
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        goalWeightLossButton.setOnClickListener {
            filterStateStore.selectGoal(FilterOptions.GOAL_WEIGHT_LOSS)
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalWeightLossButton
            )
        }

        goalWeightGainButton.setOnClickListener {
            filterStateStore.selectGoal(FilterOptions.GOAL_WEIGHT_GAIN)
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalWeightGainButton
            )
        }

        goalMuscleButton.setOnClickListener {
            filterStateStore.selectGoal(FilterOptions.GOAL_MUSCLE_GAIN)
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalMuscleButton
            )
        }

        goalBalancedButton.setOnClickListener {
            filterStateStore.selectGoal(FilterOptions.GOAL_BALANCED)
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalBalancedButton
            )
        }

        duration7Button.setOnClickListener {
            filterStateStore.selectDuration(7)
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration7Button
            )
        }

        duration14Button.setOnClickListener {
            filterStateStore.selectDuration(14)
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration14Button
            )
        }

        duration21Button.setOnClickListener {
            filterStateStore.selectDuration(21)
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration21Button
            )
        }

        duration30Button.setOnClickListener {
            filterStateStore.selectDuration(30)
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration30Button
            )
        }

        typeMediterraneanButton.setOnClickListener {
            filterStateStore.selectType(FilterOptions.TYPE_MEDITERRANEAN)
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeMediterraneanButton
            )
        }

        typeDashButton.setOnClickListener {
            filterStateStore.selectType(FilterOptions.TYPE_DASH)
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeDashButton
            )
        }

        typeFastingButton.setOnClickListener {
            filterStateStore.selectType(FilterOptions.TYPE_INTERMITTENT_FASTING)
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeFastingButton
            )
        }

        typeKetoButton.setOnClickListener {
            filterStateStore.selectType(FilterOptions.TYPE_KETOGENIC)
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeKetoButton
            )
        }

        typePlantBasedButton.setOnClickListener {
            filterStateStore.selectType(FilterOptions.TYPE_PLANT_BASED)
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typePlantBasedButton
            )
        }

        featureHeartButton.setOnClickListener {
            toggleFeature(FilterOptions.FEATURE_HEART_FRIENDLY, featureHeartButton)
        }

        featureLowSodiumButton.setOnClickListener {
            toggleFeature(FilterOptions.FEATURE_LOW_SODIUM, featureLowSodiumButton)
        }

        featureHighProteinButton.setOnClickListener {
            toggleFeature(FilterOptions.FEATURE_HIGH_PROTEIN, featureHighProteinButton)
        }

        featureEasyButton.setOnClickListener {
            toggleFeature(FilterOptions.FEATURE_EASY, featureEasyButton)
        }

        resetButton.setOnClickListener {
            resetFilters()
        }

        applyFilterButton.setOnClickListener {
            applyFilters()
        }
    }

    private fun setupCalorieSlider() = with(binding) {
        calorieRangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val minCalories = values.getOrNull(0)?.toInt() ?: FilterOptions.DEFAULT_MIN_CALORIES
            val maxCalories = values.getOrNull(1)?.toInt() ?: FilterOptions.DEFAULT_MAX_CALORIES
            filterStateStore.setCalorieRange(minCalories, maxCalories)
            updateCalorieTexts()
        }
    }

    private fun updateCalorieTexts() = with(binding) {
        val state = filterStateStore.current()
        minCalorieText.text = getString(R.string.diet_filter_calorie_value, state.minCalories)
        maxCalorieText.text = getString(R.string.diet_filter_calorie_value, state.maxCalories)
    }

    private fun goalButtonFor(goal: String): MaterialButton = with(binding) {
        when (goal) {
            FilterOptions.GOAL_WEIGHT_GAIN -> goalWeightGainButton
            FilterOptions.GOAL_MUSCLE_GAIN -> goalMuscleButton
            FilterOptions.GOAL_BALANCED -> goalBalancedButton
            else -> goalWeightLossButton
        }
    }

    private fun durationButtonFor(duration: Int): MaterialButton = with(binding) {
        when (duration) {
            7 -> duration7Button
            21 -> duration21Button
            30 -> duration30Button
            else -> duration14Button
        }
    }

    private fun typeButtonFor(type: String): MaterialButton = with(binding) {
        when (type) {
            FilterOptions.TYPE_DASH -> typeDashButton
            FilterOptions.TYPE_INTERMITTENT_FASTING -> typeFastingButton
            FilterOptions.TYPE_KETOGENIC -> typeKetoButton
            FilterOptions.TYPE_PLANT_BASED -> typePlantBasedButton
            else -> typeMediterraneanButton
        }
    }

    private fun selectSingleButton(
        buttons: List<MaterialButton>,
        selectedButton: MaterialButton
    ) {
        buttons.forEach { button ->
            setToggleButtonSelected(button, button == selectedButton)
        }
    }

    private fun toggleFeature(feature: String, button: MaterialButton) {
        val isSelected = filterStateStore.toggleFeature(feature)
        setToggleButtonSelected(button, isSelected)
    }

    private fun setToggleButtonSelected(button: MaterialButton, isSelected: Boolean) {
        val selectedBg = ContextCompat.getColor(requireContext(), R.color.fitto_green)
        val selectedText = ContextCompat.getColor(requireContext(), android.R.color.white)

        val defaultBg = ContextCompat.getColor(requireContext(), R.color.fitto_surface)
        val defaultText = ContextCompat.getColor(requireContext(), R.color.fitto_text_dark)
        val defaultStroke = ContextCompat.getColor(requireContext(), R.color.fitto_divider_soft)

        button.backgroundTintList = ColorStateList.valueOf(
            if (isSelected) selectedBg else defaultBg
        )
        button.setTextColor(if (isSelected) selectedText else defaultText)
        button.strokeColor = ColorStateList.valueOf(
            if (isSelected) selectedBg else defaultStroke
        )
        button.strokeWidth = dp(if (isSelected) 2 else 1)
    }

    private fun resetFilters() = with(binding) {
        filterStateStore.reset()
        setupInitialUi()
        handleResultEffect(DietPlanFilterResultAction.reset())
    }

    private fun applyFilters() {
        handleResultEffect(DietPlanFilterResultAction.apply(filterStateStore.current()))
    }

    private fun handleResultEffect(effect: DietPlanFilterResultEffect) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            effect.toBundle()
        )

        Toast.makeText(
            requireContext(),
            getString(effect.toastResId),
            Toast.LENGTH_SHORT
        ).show()

        if (effect.closeScreen) {
            parentFragmentManager.popBackStack()
        }
    }

    private fun DietPlanFilterResultEffect.toBundle(): Bundle {
        return Bundle().apply {
            if (clearFilters) {
                putBoolean(BUNDLE_CLEAR_FILTER, true)
            }

            state?.let { state ->
                putString(BUNDLE_GOAL, state.goal)
                putInt(BUNDLE_DURATION, state.duration)
                putString(BUNDLE_TYPE, state.type)
                putStringArrayList(BUNDLE_FEATURES, ArrayList(state.features))
                putInt(BUNDLE_MIN_CALORIES, state.minCalories)
                putInt(BUNDLE_MAX_CALORIES, state.maxCalories)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "diet_plan_filter_request"

        const val BUNDLE_GOAL = "bundle_goal"
        const val BUNDLE_DURATION = "bundle_duration"
        const val BUNDLE_TYPE = "bundle_type"
        const val BUNDLE_FEATURES = "bundle_features"
        const val BUNDLE_MIN_CALORIES = "bundle_min_calories"
        const val BUNDLE_MAX_CALORIES = "bundle_max_calories"
        const val BUNDLE_CLEAR_FILTER = "bundle_clear_filter"
    }
}
