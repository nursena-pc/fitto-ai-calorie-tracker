package com.example.fitto.feature_meal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetFoodPortionBinding
import com.example.fitto.feature_meal.model.FoodUiModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

class FoodPortionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFoodPortionBinding? = null
    private val binding get() = _binding!!

    private lateinit var food: FoodUiModel

    private var currentAmount: Double = 100.0
    private var isUpdatingAmountText: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        food = requireArguments().toFoodUiModel()
        currentAmount = food.defaultServingAmount.takeIf { it > 0.0 } ?: 100.0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFoodPortionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupClicks()
        setupAmountInput()
        renderNutrition()
    }

    private fun setupUi() = with(binding) {
        foodEmojiText.text = food.emoji
        foodNameText.text = food.name
        foodServingText.text = getString(R.string.portion_sheet_default_serving, food.serving)

        amountUnitText.text = safeUnit()
        amountHintText.text = getString(
            R.string.portion_sheet_amount_hint,
            food.caloriesPer100g.roundToInt(),
            safeUnit()
        )
    }

    private fun setupClicks() = with(binding) {
        closeButton.setOnClickListener {
            dismiss()
        }

        decreaseAmountButton.setOnClickListener {
            currentAmount = (currentAmount - amountStep()).coerceAtLeast(amountMinimum())
            updateAmountInput()
            renderNutrition()
        }

        increaseAmountButton.setOnClickListener {
            currentAmount = (currentAmount + amountStep()).coerceAtMost(MAX_AMOUNT)
            updateAmountInput()
            renderNutrition()
        }

        addPortionButton.setOnClickListener {
            normalizeAmountFromInput()

            val nutrition = calculateNutrition()

            parentFragmentManager.setFragmentResult(
                REQUEST_PORTION_SELECTED,
                Bundle().apply {
                    putString(BUNDLE_SELECTED_ID, "${food.id}-${System.nanoTime()}")
                    putString(BUNDLE_FOOD_ID, food.id)
                    putString(BUNDLE_NAME, food.name)
                    putString(BUNDLE_AMOUNT_LABEL, formatAmount(currentAmount, safeUnit()))
                    putDouble(BUNDLE_AMOUNT_GRAMS, currentAmount.roundOneDecimal())
                    putInt(BUNDLE_CALORIES, nutrition.calories)
                    putDouble(BUNDLE_PROTEIN, nutrition.protein)
                    putDouble(BUNDLE_CARBS, nutrition.carbs)
                    putDouble(BUNDLE_FAT, nutrition.fat)
                    putString(BUNDLE_EMOJI, food.emoji)
                }
            )

            dismiss()
        }
    }

    private fun setupAmountInput() = with(binding) {
        updateAmountInput()

        portionAmountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                portionAmountEditText.selectAll()
            } else {
                normalizeAmountFromInput()
            }
        }

        portionAmountEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                normalizeAmountFromInput()
                portionAmountEditText.clearFocus()
                true
            } else {
                false
            }
        }

        portionAmountEditText.addTextChangedListener(object : TextWatcher {
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
                if (isUpdatingAmountText) return

                val rawValue = editable
                    ?.toString()
                    ?.replace(",", ".")
                    ?.trim()
                    .orEmpty()

                val parsedValue = rawValue.toDoubleOrNull() ?: return

                currentAmount = parsedValue.coerceIn(amountMinimum(), MAX_AMOUNT)
                renderNutrition()
            }
        })
    }

    private fun updateAmountInput() = with(binding) {
        isUpdatingAmountText = true
        portionAmountEditText.setText(formatAmount(currentAmount, safeUnit(), includeUnit = false))
        portionAmountEditText.setSelection(portionAmountEditText.text?.length ?: 0)
        isUpdatingAmountText = false
    }

    private fun normalizeAmountFromInput() = with(binding) {
        val parsedValue = portionAmountEditText.text
            ?.toString()
            ?.replace(",", ".")
            ?.trim()
            ?.toDoubleOrNull()
            ?: currentAmount

        currentAmount = parsedValue.coerceIn(amountMinimum(), MAX_AMOUNT)
        updateAmountInput()
        renderNutrition()
    }

    private fun renderNutrition() = with(binding) {
        val nutrition = calculateNutrition()

        portionAmountEditText.setTextIfNeeded(
            formatAmount(currentAmount, safeUnit(), includeUnit = false)
        )

        caloriesValueText.text = getString(
            R.string.portion_sheet_kcal_value,
            nutrition.calories
        )

        proteinValueText.text = getString(
            R.string.portion_sheet_gram_value,
            nutrition.protein.formatOneDecimal()
        )

        carbsValueText.text = getString(
            R.string.portion_sheet_gram_value,
            nutrition.carbs.formatOneDecimal()
        )

        fatValueText.text = getString(
            R.string.portion_sheet_gram_value,
            nutrition.fat.formatOneDecimal()
        )

        addPortionButton.text = getString(
            R.string.portion_sheet_add_button,
            nutrition.calories
        )
    }

    private fun calculateNutrition(): PortionNutrition {
        return PortionNutrition(
            calories = (food.caloriesPer100g * currentAmount / 100.0)
                .roundToInt()
                .coerceAtLeast(0),
            protein = (food.proteinPer100g * currentAmount / 100.0).roundOneDecimal(),
            carbs = (food.carbsPer100g * currentAmount / 100.0).roundOneDecimal(),
            fat = (food.fatPer100g * currentAmount / 100.0).roundOneDecimal()
        )
    }

    private fun safeUnit(): String {
        return food.defaultUnit.takeIf { it.isNotBlank() } ?: "g"
    }

    private fun amountStep(): Double {
        return when (safeUnit()) {
            "ml" -> 50.0
            "adet" -> 1.0
            else -> 10.0
        }
    }

    private fun amountMinimum(): Double {
        return when (safeUnit()) {
            "adet" -> 1.0
            else -> 1.0
        }
    }

    private fun formatAmount(
        amount: Double,
        unit: String,
        includeUnit: Boolean = true
    ): String {
        val formattedAmount = if (amount % 1.0 == 0.0) {
            amount.roundToInt().toString()
        } else {
            amount.formatOneDecimal()
        }

        return if (includeUnit) {
            "$formattedAmount $unit"
        } else {
            formattedAmount
        }
    }

    private fun EditText.setTextIfNeeded(value: String) {
        if (text?.toString() == value) return

        isUpdatingAmountText = true
        setText(value)
        setSelection(text?.length ?: 0)
        isUpdatingAmountText = false
    }

    private fun Double.roundOneDecimal(): Double {
        return (this * 10.0).roundToInt() / 10.0
    }

    private fun Double.formatOneDecimal(): String {
        return if (this % 1.0 == 0.0) {
            roundToInt().toString()
        } else {
            String.format("%.1f", this)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private data class PortionNutrition(
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )

    companion object {
        const val TAG = "food_portion_bottom_sheet"

        const val REQUEST_PORTION_SELECTED = "request_portion_selected"
        const val BUNDLE_SELECTED_ID = "bundle_selected_id"
        const val BUNDLE_FOOD_ID = "bundle_food_id"
        const val BUNDLE_NAME = "bundle_name"
        const val BUNDLE_AMOUNT_LABEL = "bundle_amount_label"
        const val BUNDLE_AMOUNT_GRAMS = "bundle_amount_grams"
        const val BUNDLE_CALORIES = "bundle_calories"
        const val BUNDLE_PROTEIN = "bundle_protein"
        const val BUNDLE_CARBS = "bundle_carbs"
        const val BUNDLE_FAT = "bundle_fat"
        const val BUNDLE_EMOJI = "bundle_emoji"

        private const val ARG_ID = "arg_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_SERVING = "arg_serving"
        private const val ARG_CALORIES = "arg_calories"
        private const val ARG_EMOJI = "arg_emoji"
        private const val ARG_DEFAULT_SERVING_AMOUNT = "arg_default_serving_amount"
        private const val ARG_DEFAULT_UNIT = "arg_default_unit"
        private const val ARG_CALORIES_PER_100G = "arg_calories_per_100g"
        private const val ARG_PROTEIN_PER_100G = "arg_protein_per_100g"
        private const val ARG_CARBS_PER_100G = "arg_carbs_per_100g"
        private const val ARG_FAT_PER_100G = "arg_fat_per_100g"

        private const val MAX_AMOUNT = 5000.0

        fun newInstance(food: FoodUiModel): FoodPortionBottomSheet {
            return FoodPortionBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, food.id)
                    putString(ARG_NAME, food.name)
                    putString(ARG_SERVING, food.serving)
                    putInt(ARG_CALORIES, food.calories)
                    putString(ARG_EMOJI, food.emoji)
                    putDouble(ARG_DEFAULT_SERVING_AMOUNT, food.defaultServingAmount)
                    putString(ARG_DEFAULT_UNIT, food.defaultUnit)
                    putDouble(ARG_CALORIES_PER_100G, food.caloriesPer100g)
                    putDouble(ARG_PROTEIN_PER_100G, food.proteinPer100g)
                    putDouble(ARG_CARBS_PER_100G, food.carbsPer100g)
                    putDouble(ARG_FAT_PER_100G, food.fatPer100g)
                }
            }
        }

        fun Bundle.toSelectedFoodUiModel(): com.example.fitto.feature_meal.model.SelectedFoodUiModel {
            return com.example.fitto.feature_meal.model.SelectedFoodUiModel(
                id = getString(BUNDLE_SELECTED_ID).orEmpty(),
                foodId = getString(BUNDLE_FOOD_ID).orEmpty(),
                name = getString(BUNDLE_NAME).orEmpty(),
                amountLabel = getString(BUNDLE_AMOUNT_LABEL).orEmpty(),
                amountGrams = getDouble(BUNDLE_AMOUNT_GRAMS, 0.0),
                calories = getInt(BUNDLE_CALORIES, 0),
                protein = getDouble(BUNDLE_PROTEIN, 0.0),
                carbs = getDouble(BUNDLE_CARBS, 0.0),
                fat = getDouble(BUNDLE_FAT, 0.0),
                emoji = getString(BUNDLE_EMOJI).orEmpty()
            )
        }

        private fun Bundle.toFoodUiModel(): FoodUiModel {
            return FoodUiModel(
                id = getString(ARG_ID).orEmpty(),
                name = getString(ARG_NAME).orEmpty(),
                serving = getString(ARG_SERVING).orEmpty(),
                calories = getInt(ARG_CALORIES, 0),
                emoji = getString(ARG_EMOJI).orEmpty(),
                defaultServingAmount = getDouble(ARG_DEFAULT_SERVING_AMOUNT, 100.0),
                defaultUnit = getString(ARG_DEFAULT_UNIT).orEmpty(),
                caloriesPer100g = getDouble(ARG_CALORIES_PER_100G, 0.0),
                proteinPer100g = getDouble(ARG_PROTEIN_PER_100G, 0.0),
                carbsPer100g = getDouble(ARG_CARBS_PER_100G, 0.0),
                fatPer100g = getDouble(ARG_FAT_PER_100G, 0.0)
            )
        }
    }
}