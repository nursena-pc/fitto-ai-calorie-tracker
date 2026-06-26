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

    private var selectedGoal: String = FilterOptions.DEFAULT_GOAL
    private var selectedDuration: Int = FilterOptions.DEFAULT_DURATION
    private var selectedType: String = FilterOptions.DEFAULT_TYPE
    private val selectedFeatures = mutableSetOf<String>()

    private var minCalories: Int = FilterOptions.DEFAULT_MIN_CALORIES
    private var maxCalories: Int = FilterOptions.DEFAULT_MAX_CALORIES

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
        calorieRangeSlider.values = listOf(minCalories.toFloat(), maxCalories.toFloat())

        updateCalorieTexts()

        selectSingleButton(
            buttons = listOf(
                goalWeightLossButton,
                goalWeightGainButton,
                goalMuscleButton,
                goalBalancedButton
            ),
            selectedButton = goalWeightLossButton
        )

        selectSingleButton(
            buttons = listOf(
                duration7Button,
                duration14Button,
                duration21Button,
                duration30Button
            ),
            selectedButton = duration14Button
        )

        selectSingleButton(
            buttons = listOf(
                typeMediterraneanButton,
                typeDashButton,
                typeFastingButton,
                typeKetoButton,
                typePlantBasedButton
            ),
            selectedButton = typeMediterraneanButton
        )

        setToggleButtonSelected(featureHeartButton, true)
        setToggleButtonSelected(featureLowSodiumButton, false)
        setToggleButtonSelected(featureHighProteinButton, false)
        setToggleButtonSelected(featureEasyButton, false)
        selectedFeatures.add(FilterOptions.FEATURE_HEART_FRIENDLY)
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        goalWeightLossButton.setOnClickListener {
            selectedGoal = FilterOptions.GOAL_WEIGHT_LOSS
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalWeightLossButton
            )
        }

        goalWeightGainButton.setOnClickListener {
            selectedGoal = FilterOptions.GOAL_WEIGHT_GAIN
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalWeightGainButton
            )
        }

        goalMuscleButton.setOnClickListener {
            selectedGoal = FilterOptions.GOAL_MUSCLE_GAIN
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalMuscleButton
            )
        }

        goalBalancedButton.setOnClickListener {
            selectedGoal = FilterOptions.GOAL_BALANCED
            selectSingleButton(
                listOf(goalWeightLossButton, goalWeightGainButton, goalMuscleButton, goalBalancedButton),
                goalBalancedButton
            )
        }

        duration7Button.setOnClickListener {
            selectedDuration = 7
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration7Button
            )
        }

        duration14Button.setOnClickListener {
            selectedDuration = 14
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration14Button
            )
        }

        duration21Button.setOnClickListener {
            selectedDuration = 21
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration21Button
            )
        }

        duration30Button.setOnClickListener {
            selectedDuration = 30
            selectSingleButton(
                listOf(duration7Button, duration14Button, duration21Button, duration30Button),
                duration30Button
            )
        }

        typeMediterraneanButton.setOnClickListener {
            selectedType = FilterOptions.TYPE_MEDITERRANEAN
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeMediterraneanButton
            )
        }

        typeDashButton.setOnClickListener {
            selectedType = FilterOptions.TYPE_DASH
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeDashButton
            )
        }

        typeFastingButton.setOnClickListener {
            selectedType = FilterOptions.TYPE_INTERMITTENT_FASTING
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeFastingButton
            )
        }

        typeKetoButton.setOnClickListener {
            selectedType = FilterOptions.TYPE_KETOGENIC
            selectSingleButton(
                listOf(typeMediterraneanButton, typeDashButton, typeFastingButton, typeKetoButton, typePlantBasedButton),
                typeKetoButton
            )
        }

        typePlantBasedButton.setOnClickListener {
            selectedType = FilterOptions.TYPE_PLANT_BASED
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
            minCalories = values.getOrNull(0)?.toInt() ?: FilterOptions.DEFAULT_MIN_CALORIES
            maxCalories = values.getOrNull(1)?.toInt() ?: FilterOptions.DEFAULT_MAX_CALORIES
            updateCalorieTexts()
        }
    }

    private fun updateCalorieTexts() = with(binding) {
        minCalorieText.text = getString(R.string.diet_filter_calorie_value, minCalories)
        maxCalorieText.text = getString(R.string.diet_filter_calorie_value, maxCalories)
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
        val willSelect = selectedFeatures.contains(feature).not()

        if (willSelect) {
            selectedFeatures.add(feature)
        } else {
            selectedFeatures.remove(feature)
        }

        setToggleButtonSelected(button, willSelect)
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
        selectedGoal = FilterOptions.DEFAULT_GOAL
        selectedDuration = FilterOptions.DEFAULT_DURATION
        selectedType = FilterOptions.DEFAULT_TYPE
        selectedFeatures.clear()
        selectedFeatures.addAll(FilterOptions.DEFAULT_FEATURES)

        minCalories = FilterOptions.DEFAULT_MIN_CALORIES
        maxCalories = FilterOptions.DEFAULT_MAX_CALORIES
        calorieRangeSlider.valueFrom = 800f
        calorieRangeSlider.valueTo = 3000f
        calorieRangeSlider.stepSize = 100f
        calorieRangeSlider.values = listOf(minCalories.toFloat(), maxCalories.toFloat())
        updateCalorieTexts()

        setupInitialUi()

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putBoolean(BUNDLE_CLEAR_FILTER, true)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_filter_reset_mock),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private fun applyFilters() {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(BUNDLE_GOAL, selectedGoal)
                putInt(BUNDLE_DURATION, selectedDuration)
                putString(BUNDLE_TYPE, selectedType)
                putStringArrayList(BUNDLE_FEATURES, ArrayList(selectedFeatures))
                putInt(BUNDLE_MIN_CALORIES, minCalories)
                putInt(BUNDLE_MAX_CALORIES, maxCalories)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_filter_apply_success),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
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
