package com.example.fitto.feature_meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetCreateCustomFoodBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CreateCustomFoodBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateCustomFoodBinding? = null
    private val binding get() = _binding!!

    private val targetMealType: String by lazy {
        requireArguments().getString(ARG_MEAL_TYPE).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateCustomFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupClicks()
    }

    private fun setupUi() = with(binding) {
        unitEditText.setText("g")
        servingAmountEditText.setText("100")
    }

    private fun setupClicks() = with(binding) {
        closeButton.setOnClickListener {
            dismiss()
        }

        saveButton.setOnClickListener {
            validateAndSendResult()
        }
    }

    private fun validateAndSendResult() = with(binding) {
        val name = nameEditText.text?.toString()?.trim().orEmpty()
        val servingAmount = servingAmountEditText.parseDoubleOrNull()
        val unit = unitEditText.text?.toString()?.trim().orEmpty().ifBlank { "g" }
        val calories = caloriesEditText.parseDoubleOrNull()
        val protein = proteinEditText.parseDoubleOrNull()
        val carbs = carbsEditText.parseDoubleOrNull()
        val fat = fatEditText.parseDoubleOrNull()

        clearErrors()

        var hasError = false

        if (name.length < 2) {
            nameLayout.error = getString(R.string.custom_food_error_name)
            hasError = true
        }

        if (servingAmount == null || servingAmount !in 1.0..5000.0) {
            servingAmountLayout.error = getString(R.string.custom_food_error_serving)
            hasError = true
        }

        if (calories == null || calories !in 0.0..1000.0) {
            caloriesLayout.error = getString(R.string.custom_food_error_calories)
            hasError = true
        }

        if (protein == null || protein !in 0.0..100.0) {
            proteinLayout.error = getString(R.string.custom_food_error_macro)
            hasError = true
        }

        if (carbs == null || carbs !in 0.0..100.0) {
            carbsLayout.error = getString(R.string.custom_food_error_macro)
            hasError = true
        }

        if (fat == null || fat !in 0.0..100.0) {
            fatLayout.error = getString(R.string.custom_food_error_macro)
            hasError = true
        }

        if (hasError) return

        parentFragmentManager.setFragmentResult(
            REQUEST_CUSTOM_FOOD_CREATED,
            Bundle().apply {
                putString(BUNDLE_NAME, name)
                putDouble(BUNDLE_SERVING_AMOUNT, servingAmount ?: 100.0)
                putString(BUNDLE_UNIT, unit)
                putDouble(BUNDLE_CALORIES_PER_100G, calories ?: 0.0)
                putDouble(BUNDLE_PROTEIN_PER_100G, protein ?: 0.0)
                putDouble(BUNDLE_CARBS_PER_100G, carbs ?: 0.0)
                putDouble(BUNDLE_FAT_PER_100G, fat ?: 0.0)
                putString(BUNDLE_MEAL_TYPE, targetMealType)
            }
        )

        dismiss()
    }

    private fun clearErrors() = with(binding) {
        nameLayout.error = null
        servingAmountLayout.error = null
        caloriesLayout.error = null
        proteinLayout.error = null
        carbsLayout.error = null
        fatLayout.error = null
    }

    private fun com.google.android.material.textfield.TextInputEditText.parseDoubleOrNull(): Double? {
        return text
            ?.toString()
            ?.replace(",", ".")
            ?.trim()
            ?.toDoubleOrNull()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "create_custom_food_bottom_sheet"

        const val REQUEST_CUSTOM_FOOD_CREATED = "request_custom_food_created"
        const val BUNDLE_NAME = "bundle_name"
        const val BUNDLE_SERVING_AMOUNT = "bundle_serving_amount"
        const val BUNDLE_UNIT = "bundle_unit"
        const val BUNDLE_CALORIES_PER_100G = "bundle_calories_per_100g"
        const val BUNDLE_PROTEIN_PER_100G = "bundle_protein_per_100g"
        const val BUNDLE_CARBS_PER_100G = "bundle_carbs_per_100g"
        const val BUNDLE_FAT_PER_100G = "bundle_fat_per_100g"
        const val BUNDLE_MEAL_TYPE = "bundle_meal_type"

        private const val ARG_MEAL_TYPE = "arg_meal_type"

        fun newInstance(mealType: String): CreateCustomFoodBottomSheet {
            return CreateCustomFoodBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEAL_TYPE, mealType)
                }
            }
        }
    }
}