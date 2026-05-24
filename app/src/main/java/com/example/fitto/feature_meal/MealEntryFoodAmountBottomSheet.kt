package com.example.fitto.feature_meal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetMealEntryFoodAmountBinding
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

class MealEntryFoodAmountBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMealEntryFoodAmountBinding? = null
    private val binding get() = _binding!!

    private var currentAmount: Double = 1.0
    private var isUpdatingText: Boolean = false

    private val foodRowId: String by lazy {
        requireArguments().getString(ARG_FOOD_ROW_ID).orEmpty()
    }

    private val foodName: String by lazy {
        requireArguments().getString(ARG_FOOD_NAME).orEmpty()
    }

    private val unit: String by lazy {
        requireArguments().getString(ARG_UNIT).orEmpty().ifBlank { "g" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentAmount = requireArguments().getDouble(ARG_AMOUNT, 1.0)
            .coerceIn(1.0, MAX_AMOUNT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMealEntryFoodAmountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
        setupClicks()
        setupAmountInput()
    }

    private fun setupUi() = with(binding) {
        titleText.text = foodName
        unitText.text = unit
        updateAmountInput()
    }

    private fun setupClicks() = with(binding) {
        closeButton.setOnClickListener {
            dismiss()
        }

        decreaseButton.setOnClickListener {
            currentAmount = (currentAmount - stepAmount()).coerceAtLeast(1.0)
            updateAmountInput()
        }

        increaseButton.setOnClickListener {
            currentAmount = (currentAmount + stepAmount()).coerceAtMost(MAX_AMOUNT)
            updateAmountInput()
        }

        saveButton.setOnClickListener {
            normalizeAmountFromInput()

            parentFragmentManager.setFragmentResult(
                REQUEST_AMOUNT_CHANGED,
                Bundle().apply {
                    putString(BUNDLE_FOOD_ROW_ID, foodRowId)
                    putDouble(BUNDLE_NEW_AMOUNT, currentAmount)
                }
            )

            dismiss()
        }
    }

    private fun setupAmountInput() = with(binding) {
        amountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                amountEditText.selectAll()
            } else {
                normalizeAmountFromInput()
            }
        }

        amountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(editable: Editable?) {
                if (isUpdatingText) return

                val value = editable
                    ?.toString()
                    ?.replace(",", ".")
                    ?.trim()
                    ?.toDoubleOrNull()
                    ?: return

                currentAmount = value.coerceIn(1.0, MAX_AMOUNT)
            }
        })
    }

    private fun updateAmountInput() = with(binding) {
        isUpdatingText = true
        amountEditText.setText(formatAmount(currentAmount))
        amountEditText.setSelection(amountEditText.text?.length ?: 0)
        isUpdatingText = false
    }

    private fun normalizeAmountFromInput() = with(binding) {
        val value = amountEditText.text
            ?.toString()
            ?.replace(",", ".")
            ?.trim()
            ?.toDoubleOrNull()
            ?: currentAmount

        currentAmount = value.coerceIn(1.0, MAX_AMOUNT)
        updateAmountInput()
    }

    private fun stepAmount(): Double {
        return when (unit) {
            "ml" -> 50.0
            "adet" -> 1.0
            else -> 10.0
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.roundToInt().toString()
        } else {
            String.format("%.1f", amount)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "meal_entry_food_amount_bottom_sheet"

        const val REQUEST_AMOUNT_CHANGED = "request_amount_changed"
        const val BUNDLE_FOOD_ROW_ID = "bundle_food_row_id"
        const val BUNDLE_NEW_AMOUNT = "bundle_new_amount"

        private const val ARG_FOOD_ROW_ID = "arg_food_row_id"
        private const val ARG_FOOD_NAME = "arg_food_name"
        private const val ARG_AMOUNT = "arg_amount"
        private const val ARG_UNIT = "arg_unit"

        private const val MAX_AMOUNT = 5000.0

        fun newInstance(food: MealEntryFoodUiModel): MealEntryFoodAmountBottomSheet {
            return MealEntryFoodAmountBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FOOD_ROW_ID, food.id)
                    putString(ARG_FOOD_NAME, food.foodName)
                    putDouble(ARG_AMOUNT, food.amountGrams)
                    putString(ARG_UNIT, food.amountLabel.extractUnitOrDefault())
                }
            }
        }

        private fun String.extractUnitOrDefault(): String {
            val unit = trim().substringAfterLast(' ', missingDelimiterValue = "g").trim()
            return unit.takeIf { it.isNotBlank() && it.any { char -> char.isLetter() } } ?: "g"
        }
    }
}